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
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.util.ExchangeHelper;

/**
 * The binding of how Camel messages get mapped to JBI and back again
 * 
 * @version $Revision: 563665 $
 */
public class JbiBinding {
    private String messageExchangePattern;

    /**
     * Extracts the body from the given normalized message
     */
    public Object extractBodyFromJbi(JbiExchange exchange, NormalizedMessage normalizedMessage) {
        // TODO we may wish to turn this into a POJO such as a JAXB/DOM
        return normalizedMessage.getContent();
    }

    public Source convertBodyToJbi(Exchange exchange, Object body) {
        return ExchangeHelper.convertToType(exchange, Source.class, body);
    }

    public MessageExchange makeJbiMessageExchange(Exchange camelExchange,
                                                  MessageExchangeFactory exchangeFactory,
                                                  String defaultMep)
        throws MessagingException, URISyntaxException {
        
        MessageExchange jbiExchange = createJbiMessageExchange(camelExchange, exchangeFactory, defaultMep);
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

    protected MessageExchange createJbiMessageExchange(Exchange camelExchange,
                                                       MessageExchangeFactory exchangeFactory,
                                                       String defaultMep)
        throws MessagingException, URISyntaxException {

        ExchangePattern mep = camelExchange.getPattern();
        if (mep == null) {
            mep = ExchangePattern.fromWsdlUri(defaultMep);
        }
        if (mep == null) {
            mep = ExchangePattern.fromWsdlUri(getMessageExchangePattern());
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
        // TODO: this is not really usefull as the out will not be
        // TODO: populated at that time
        if (answer == null) {
            // lets try choose the best MEP based on the camel message
            Message out = camelExchange.getOut(false);
            if (out == null || out.getBody() == null) {
                answer = exchangeFactory.createInOnlyExchange();
            } else {
                answer = exchangeFactory.createInOutExchange();
            }
        }
        return answer;
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
