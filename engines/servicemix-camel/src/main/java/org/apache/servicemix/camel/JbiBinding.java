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

import java.net.URI;
import java.net.URISyntaxException;
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
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.servicemix.camel.util.BasicSerializationHeaderFilterStrategy;
import org.apache.servicemix.camel.util.HeaderFilterStrategies;
import org.apache.servicemix.camel.util.HeaderFilterStrategyConstants;
import org.apache.servicemix.camel.util.NoCheckSerializationHeaderFilterStrategy;
import org.apache.servicemix.camel.util.StrictSerializationHeaderFilterStrategy;
import org.apache.servicemix.jbi.exception.FaultException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The binding of how Camel messages get mapped to JBI and back again
 *
 * @version $Revision: 563665 $
 */
public class JbiBinding {

    public static final String MESSAGE_EXCHANGE = "JbiMessageExchange";
    public static final String OPERATION = "JbiOperation";
    public static final String SECURITY_SUBJECT = "JbiSecuritySubject";
    
    private final Logger logger = LoggerFactory.getLogger(JbiBinding.class);

    private final CamelContext context;

    private HeaderFilterStrategies strategies = new HeaderFilterStrategies();
    private boolean convertExceptions;

    /**
     * Create the binding instance for a given CamelContext
     * 
     * @param context the CamelContext
     */
    public JbiBinding(CamelContext context) {
        this(context, HeaderFilterStrategyConstants.BASIC);
    }

    public JbiBinding(CamelContext context, String serialization) {
        this.context = context;
        if (serialization == null) {
            strategies.add(new BasicSerializationHeaderFilterStrategy());
        } else {
            if (serialization.equalsIgnoreCase(HeaderFilterStrategyConstants.STRICT)) {
                strategies.add(new StrictSerializationHeaderFilterStrategy());
            } else if (serialization.equalsIgnoreCase(HeaderFilterStrategyConstants.NOCHECK)) {
                strategies.add(new NoCheckSerializationHeaderFilterStrategy());
            } else {
                strategies.add(new BasicSerializationHeaderFilterStrategy());
            }
        }
    }

    public void addHeaderFilterStrategy(HeaderFilterStrategy strategy) {
        strategies.add(strategy);
    }

    public void setConvertExceptions(boolean convertExceptions) {
        this.convertExceptions = convertExceptions;
    }

    /**
     * Run a block of code with the {@link CamelContext#getApplicationContextClassLoader()} set as the thread context classloader.
     * 
     * @param callable the block of code to be run
     * @throws Exception exceptions being thrown while running the block of code
     */
    public<T> T runWithCamelContextClassLoader(Callable<T> callable) throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        try {
            ClassLoader loader = context.getApplicationContextClassLoader();
            if (loader != null) {
                logger.debug("Set the thread context classloader {}", loader);
                Thread.currentThread().setContextClassLoader(loader);
            }
            return callable.call();
        } finally {
            // restore CL
            Thread.currentThread().setContextClassLoader(original);
        }        
    }

    public MessageExchange makeJbiMessageExchange(Exchange camelExchange,
                                                  MessageExchangeFactory exchangeFactory, String defaultMep)
        throws MessagingException, URISyntaxException {

        MessageExchange jbiExchange = createJbiMessageExchange(camelExchange, exchangeFactory, defaultMep);
        
        copyPropertiesFromCamelToJbi(camelExchange, jbiExchange);
        
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
        copyPropertiesFromJbiToCamel(exchange, result);
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
     * Copies properties from the JBI MessageExchange to the Camel Exchange, taking into account the
     * {@link HeaderFilterStrategy} that has been configured on this binding.
     * 
     * @param from the JBI MessageExchange
     * @param to the Camel Exchange
     */
    public void copyPropertiesFromJbiToCamel(MessageExchange from, Exchange to) {
        for (Object object : from.getPropertyNames()) {
            String key = object.toString();
            Object value = from.getProperty(key);
            if (!strategies.applyFilterToCamelHeaders(key, value, null)) {
                to.setProperty(key, value);
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
        Subject securitySubject = from.getSecuritySubject();
        if (securitySubject != null) {
            to.setHeader(SECURITY_SUBJECT, securitySubject);
        }
        Exchange exchange = to.getExchange();
        for (Object object : from.getPropertyNames()) {
            String key = object.toString();
            Object value = from.getProperty(key);
            if (!strategies.applyFilterToCamelHeaders(key, value, exchange)) { 
                to.setHeader(key, value);
            }
        }
        for (Object id : from.getAttachmentNames()) {
            to.addAttachment(id.toString(), from.getAttachment(id.toString()));
        }
    }

    public void copyFromCamelToJbi(Message message, NormalizedMessage normalizedMessage) throws MessagingException {
        if (message != null && message.getBody() != null) {
            Source body = message.getBody(Source.class);
            if (body == null) {
                logger.warn("Unable to convert message body of type {} into an XML Source", message.getBody().getClass());
            } else {
                normalizedMessage.setContent(body);
            }
        }
        
        Subject securitySubject = getSecuritySubject(message);
        if (securitySubject != null) {
            normalizedMessage.setSecuritySubject(securitySubject);
        }

        Exchange exchange = message.getExchange();
        for (Map.Entry<String, Object> entry : message.getHeaders().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value != null && !strategies.applyFilterToCamelHeaders(key, value, exchange)) {
                normalizedMessage.setProperty(key, value);
            }
        }
        
        for (String id : message.getAttachmentNames()) {
            normalizedMessage.addAttachment(id, message.getAttachment(id));
        }
    }

    public void copyFromCamelToJbi(Exchange exchange, MessageExchange messageExchange) throws MessagingException {
        // add Exchange properties to the MessageExchange without overwriting any existing properties
        copyPropertiesFromCamelToJbi(exchange, messageExchange);
        
        NormalizedMessage in = messageExchange.getMessage("in");
        for (Map.Entry<String, Object> entry : exchange.getIn().getHeaders().entrySet()) {
            in.setProperty(entry.getKey(), entry.getValue());
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

    /**
     * Extract an Exception from the exchange.  This method will always return a
     *
     * @param exchange the Camel exchange
     * @return an exception
     */
    public Exception extractException(Exchange exchange) {
        Exception e  = exchange.getException();
        if (e == null || convertExceptions) {
            e = context.getTypeConverter().convertTo(FaultException.class, exchange);
        }
        return e;
    }

    private void copyPropertiesFromCamelToJbi(Exchange exchange, MessageExchange messageExchange) {
        for (String key : exchange.getProperties().keySet()) {
            if (messageExchange.getProperty(key) == null) {
                Object value = exchange.getProperty(key);
                if (!MESSAGE_EXCHANGE.equals(key) && value != null
                        && !strategies.applyFilterToCamelHeaders(key, value, exchange)) {
                    messageExchange.setProperty(key, value);
                }
            }
        }
    }

    private boolean isOutCapable(MessageExchange exchange) {
        return exchange instanceof InOut || exchange instanceof InOptionalOut;
    }

    /**
     * Access the JBI MessageExchange that has been stored on the Camel Exchange
     * @return the JBI MessageExchange
     */
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
        return message.getHeader(SECURITY_SUBJECT, Subject.class);
    }
}
