/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.camel;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;
import javax.security.auth.Subject;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultHeaderFilterStrategy;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.spi.HeaderFilterStrategyAware;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The binding of how Camel messages get mapped to JBI and back again
 *
 * @version $Revision: 563665 $
 */
public class JbiBinding implements HeaderFilterStrategyAware {

    public static final String MESSAGE_EXCHANGE = "JbiMessageExchange";
    public static final String OPERATION = "JbiOperation";
    public static final String SECURITY_SUBJECT = "JbiSecuritySubject";
    
    private static final Log LOG = LogFactory.getLog(JbiBinding.class);

    private final CamelContext context;
    private HeaderFilterStrategy strategy;
    
    /**
     * Create the binding instance for a given CamelContext
     * 
     * @param context the CamelContext
     */
    public JbiBinding(CamelContext context) {
        super();
        this.context = context;
    }
    
    /**
     * Run a block of code with the {@link CamelContext#getApplicationContextClassLoader()} set as the thread context classloader.
     * 
     * @param callable the block of code to be run
     * @throws Exception exceptions being thrown while running the block of code
     */
    public void runWithCamelContextClassLoader(Callable<Object> callable) throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader loader = context.getApplicationContextClassLoader();
            if (loader != null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Set the thread context classloader " + loader);
                }
                Thread.currentThread().setContextClassLoader(loader);
            }
            callable.call();
        } finally {
            // restore CL
            Thread.currentThread().setContextClassLoader(original);
        }        
    }

    public MessageExchange makeJbiMessageExchange(Exchange camelExchange,
                                                  MessageExchangeFactory exchangeFactory, String defaultMep)
        throws MessagingException, URISyntaxException {

        MessageExchange jbiExchange = createJbiMessageExchange(camelExchange, exchangeFactory, defaultMep);
        
        copyHeadersFromCamelToJbi(camelExchange, jbiExchange);
        
        NormalizedMessage normalizedMessage = jbiExchange.getMessage("in");
        if (normalizedMessage == null) {
            normalizedMessage = jbiExchange.createMessage();
            jbiExchange.setMessage(normalizedMessage, "in");
        }
        copyFromCamelToJbi(camelExchange.getIn(), normalizedMessage);
        return jbiExchange;
    }
    
    protected MessageExchange createJbiMessageExchange(Exchange camelExchange,
        MessageExchangeFactory exchangeFactory, String defaultMep)
        throws MessagingException, URISyntaxException {

        // option 1 -- use the MEP that was configured on the endpoint URI
        ExchangePattern mep = ExchangePattern.fromWsdlUri(defaultMep);
        if (mep == null) {
            // option 2 -- use the MEP from the Camel Exchange
            mep = camelExchange.getPattern();
        }
        MessageExchange answer = null;
        if (mep != null) {
            if (mep == ExchangePattern.InOnly) {
                answer = exchangeFactory.createInOnlyExchange();
            } else if (mep == ExchangePattern.InOptionalOut) {
                answer = exchangeFactory.createInOptionalOutExchange();
            } else if (mep == ExchangePattern.InOut) {
                answer = exchangeFactory.createInOutExchange();
            } else if (mep == ExchangePattern.RobustInOnly) {
                answer = exchangeFactory.createRobustInOnlyExchange();
            } else {
                answer = exchangeFactory.createExchange(new URI(mep.toString()));
            }
        }

        if (getOperation(camelExchange) != null) {
            answer.setOperation(getOperation(camelExchange));
        }

        return answer;
    }

    public Exchange createExchange(MessageExchange exchange) {
        Exchange result = new DefaultExchange(context);
        result.setProperty(MESSAGE_EXCHANGE, exchange);
        result.setPattern(getExchangePattern(exchange));
        if (exchange.getOperation() != null) {
            result.setProperty(OPERATION, exchange.getOperation());
        }
        if (exchange.getMessage("in") != null) {
            copyFromJbiToCamel(exchange.getMessage("in"), result.getIn());
        }
        return result;
    }

    /*
     * Get the corresponding Camel ExchangePattern for a given JBI Exchange
     */
    private ExchangePattern getExchangePattern(MessageExchange exchange) {
        if (exchange instanceof InOut) {
            return ExchangePattern.InOut;
        } else if (exchange instanceof InOptionalOut) {
            return ExchangePattern.InOptionalOut;
        } else if (exchange instanceof RobustInOnly) {
            return ExchangePattern.RobustInOnly;
        } else {
            return ExchangePattern.InOnly;
        }
    }
    
    /**
     * Copies headers from the JBI MessageExchange to the Camel Exchange, taking into account the
     * {@link HeaderFilterStrategy} that has been configured on this binding.
     * 
     * @param from the JBI MessageExchange
     * @param to the Camel Exchange
     */
    public void copyHeadersFromJbiToCamel(MessageExchange from, Exchange to) {
        for (Object object : from.getPropertyNames()) {
            String key = object.toString();
            if (!getHeaderFilterStrategy().applyFilterToCamelHeaders(key, from.getProperty(key), null)) {
                to.setProperty(key, from.getProperty(key));
            }
        }
    }

    /**
     * Copies content, headers, security subject and attachments from the JBI NormalizedMessage to the Camel Message.
     * 
     * @param from the source {@link NormalizedMessage}
     * @param to the target {@link Message}
     */
    public void copyFromJbiToCamel(NormalizedMessage from, Message to) {
        to.setBody(from.getContent());
        if (from.getSecuritySubject() != null) {
            to.setHeader(SECURITY_SUBJECT, from.getSecuritySubject());
        }
        for (Object object : from.getPropertyNames()) {
            String key = object.toString();
            if (!strategy.applyFilterToCamelHeaders(key, from.getProperty(key), to.getExchange())) { 
                to.setHeader(key, from.getProperty(key));
            }
        }
        for (Object id : from.getAttachmentNames()) {
            to.addAttachment(id.toString(), from.getAttachment(id.toString()));
        }
    }

    public void copyFromCamelToJbi(Message message, NormalizedMessage normalizedMessage) throws MessagingException {
        if (message != null && message.getBody() != null) {
            if (message.getBody(Source.class) == null) {
                LOG.warn("Unable to convert message body of type " + message.getBody().getClass() + " into an XML Source");
            } else {
                normalizedMessage.setContent(message.getBody(Source.class));
            }
        }
        
        if (getSecuritySubject(message) != null) {
            normalizedMessage.setSecuritySubject(getSecuritySubject(message));
        }
        
        for (String key : message.getHeaders().keySet()) {
            Object value = message.getHeader(key);
            if (isSerializable(value) && !getHeaderFilterStrategy().applyFilterToCamelHeaders(key, value, message.getExchange())) {
                normalizedMessage.setProperty(key, value);
            }
        }
        
        for (String id : message.getAttachmentNames()) {
            normalizedMessage.addAttachment(id, message.getAttachment(id));
        }
    }

    public void copyFromCamelToJbi(Exchange exchange, MessageExchange messageExchange) throws MessagingException {
        // add Exchange properties to the MessageExchange without overwriting any existing properties
        copyHeadersFromCamelToJbi(exchange, messageExchange);
        
        NormalizedMessage in = messageExchange.getMessage("in");
        for (String key : exchange.getIn().getHeaders().keySet()) {
            in.setProperty(key, exchange.getIn().getHeader(key));
        }        
        
        if (isOutCapable(messageExchange)) {
            if (exchange.hasOut()) {
                NormalizedMessage out = messageExchange.createMessage();
                copyFromCamelToJbi(exchange.getOut(), out);
                messageExchange.setMessage(out, "out");
            } else {
                //JBI MEP requires a reply and the Camel exchange failed to produce one -- echoing back the request
                NormalizedMessage out = messageExchange.createMessage();
                copyFromCamelToJbi(exchange.getIn(), out);
                messageExchange.setMessage(out, "out");
            }
        }
    }

    private void copyHeadersFromCamelToJbi(Exchange exchange, MessageExchange messageExchange) {
        for (String key : exchange.getProperties().keySet()) {
            if (messageExchange.getProperty(key) == null) {
                messageExchange.setProperty(key, exchange.getProperty(key));
            }
        }
    }

    private boolean isOutCapable(MessageExchange exchange) {
        return exchange instanceof InOut || exchange instanceof InOptionalOut;
    }

    public static MessageExchange getMessageExchange(Exchange exchange) {
        return exchange.getProperty(MESSAGE_EXCHANGE, MessageExchange.class);
    }
    
    /**
     * Access the JBI Operation that has been stored on a Camel Exchange
     * @param exchange the Camel Exchange
     * @return the JBI Operation as a QName
     */
    public static QName getOperation(Exchange exchange) {
        return exchange.getProperty(OPERATION, QName.class);
    }
    
    /**
     * Access the security subject that has been stored on the Camel Message
     * @param message the Camel message
     * @return the Subject or <code>null</code> is no Subject is available in the headers
     */
    public static Subject getSecuritySubject(Message message) {
        if (message.getHeader(SECURITY_SUBJECT) != null) {
            return message.getHeader(SECURITY_SUBJECT, Subject.class);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected boolean isSerializable(Object object) {
        return (object instanceof Serializable) && !(object instanceof Map) && !(object instanceof Collection);
    }

    public HeaderFilterStrategy getHeaderFilterStrategy() {
        if (strategy == null) {
            strategy = new DefaultHeaderFilterStrategy();
        }
        return strategy;
    }

    public void setHeaderFilterStrategy(HeaderFilterStrategy strategy) {
        this.strategy = strategy;
    }
}
