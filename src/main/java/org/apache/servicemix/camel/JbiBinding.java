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

import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Set;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.camel.Exchange;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import static org.apache.servicemix.camel.CamelConstants.MessageExchangePattern.*;

/**
 * The binding of how Camel messages get mapped to JBI and back again
 * 
 * @version $Revision: 563665 $
 */
public class JbiBinding {
    private static final transient Log LOG = LogFactory.getLog(JbiBinding.class);
    private String messageExchangePattern = IN_ONLY;

    /**
     * Extracts the body from the given normalized message
     */
    public Object extractBodyFromJbi(JbiExchange exchange, NormalizedMessage normalizedMessage) {
        // TODO we may wish to turn this into a POJO such as a JAXB/DOM
        return normalizedMessage.getContent();
    }

    public MessageExchange makeJbiMessageExchange(Exchange camelExchange, MessageExchangeFactory exchangeFactory)
        throws MessagingException, URISyntaxException {
        
        MessageExchange jbiExchange = createJbiMessageExchange(camelExchange, exchangeFactory);
        NormalizedMessage normalizedMessage = jbiExchange.getMessage("in");
        if (normalizedMessage == null) {
            normalizedMessage = jbiExchange.createMessage();
            jbiExchange.setMessage(normalizedMessage, "in");
        }
        normalizedMessage.setContent(getJbiInContent(camelExchange));
        addJbiHeaders(jbiExchange, normalizedMessage, camelExchange);
        return jbiExchange;
    }

    // Properties
    //-------------------------------------------------------------------------

    public String getMessageExchangePattern() {
        return messageExchangePattern;
    }

    /**
     * Sets the message exchange pattern to use for communicating with JBI
     *
     * @param messageExchangePattern
     */
    public void setMessageExchangePattern(String messageExchangePattern) {
        this.messageExchangePattern = messageExchangePattern;
    }

    protected MessageExchange createJbiMessageExchange(Exchange camelExchange, MessageExchangeFactory exchangeFactory)
        throws MessagingException, URISyntaxException {

        String mep = camelExchange.getProperty(CamelConstants.Property.MESSAGE_EXCHANGE_PATTERN, String.class);
        if (mep == null) {
            mep = getMessageExchangePattern();
        }
        if (mep != null) {
            if (IN_ONLY.equals(mep)) {
                return exchangeFactory.createInOnlyExchange();
            } else if (IN_OPTIONAL_OUT.equals(mep)) {
                return exchangeFactory.createInOptionalOutExchange();
            } else if (IN_OUT.equals(mep)) {
                return exchangeFactory.createInOutExchange();
            } else if (ROBUST_IN_ONLY.equals(mep)) {
                return exchangeFactory.createRobustInOnlyExchange();
            } else {
                return exchangeFactory.createExchange(new URI(mep));
            }
        }
        LOG.warn("No MessageExchangePattern specified so using InOnly");
        return exchangeFactory.createInOnlyExchange();
    }

    protected Source getJbiInContent(Exchange camelExchange) {
        // TODO this should be more smart
        Object value = camelExchange.getIn().getBody();
        if (value instanceof String) {
            return new StreamSource(new StringReader(value.toString()));
        }
        return camelExchange.getIn().getBody(Source.class);
    }

    protected void addJbiHeaders(MessageExchange jbiExchange, NormalizedMessage normalizedMessage, Exchange camelExchange) {
        Set<Map.Entry<String, Object>> entries = camelExchange.getIn().getHeaders().entrySet();
        for (Map.Entry<String, Object> entry : entries) {
            normalizedMessage.setProperty(entry.getKey(), entry.getValue());
        }
    }
}
