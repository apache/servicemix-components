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

import java.net.URISyntaxException;
import java.util.Set;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.servicemix.jbi.resolver.URIResolver;

/**
 * A
 *
 * @{link Processor} which takes a Camel {@link Exchange} and invokes it into
 *        JBI using the straight JBI API
 * @version $Revision: 563665 $
 */
public class ToJbiProcessor implements Processor {
    private JbiBinding binding;

    private ComponentContext componentContext;

    private JbiEndpoint jbiEndpoint;

    public ToJbiProcessor(JbiBinding binding, ComponentContext componentContext, JbiEndpoint endpoint) {
        this.binding = binding;
        this.componentContext = componentContext;
        jbiEndpoint = endpoint;
    }

    private void addHeaders(MessageExchange messageExchange, Exchange camelExchange) {
        Set entries = messageExchange.getPropertyNames();
        for (Object o : entries) {
            String key = o.toString();
            camelExchange.setProperty(key, messageExchange.getProperty(key));
        }
    }

    private void addHeaders(NormalizedMessage normalizedMessage, Message camelMessage) {
        Set entries = normalizedMessage.getPropertyNames();
        for (Object o : entries) {
            String key = o.toString();
            camelMessage.setHeader(key, normalizedMessage.getProperty(key));
        }
    }

    private void addAttachments(NormalizedMessage normalizedMessage, Message camelMessage) {
        Set entries = normalizedMessage.getAttachmentNames();
        for (Object o : entries) {
            String id = o.toString();
            camelMessage.addAttachment(id, normalizedMessage.getAttachment(id));
        }
    }

    public void process(Exchange exchange) {
        try {
            DeliveryChannel deliveryChannel = componentContext.getDeliveryChannel();
            MessageExchangeFactory exchangeFactory = deliveryChannel.createExchangeFactory();
            MessageExchange messageExchange = binding.makeJbiMessageExchange(exchange, exchangeFactory, jbiEndpoint.getMep());

            if (jbiEndpoint.getOperation() != null) {
                messageExchange.setOperation(QName.valueOf(jbiEndpoint.getOperation()));
            }

            URIResolver.configureExchange(messageExchange, componentContext, jbiEndpoint.getDestinationUri());
            deliveryChannel.sendSync(messageExchange);

            if (messageExchange.getStatus() == ExchangeStatus.ERROR) {
                exchange.setException(messageExchange.getError());
            } else if (messageExchange.getStatus() == ExchangeStatus.ACTIVE) {
                addHeaders(messageExchange, exchange);
                if (messageExchange.getFault() != null) {
                    exchange.getFault().setBody(messageExchange.getFault().getContent());
                    addHeaders(messageExchange.getFault(), exchange.getFault());
                    addAttachments(messageExchange.getFault(), exchange.getFault());
                } else {
                    exchange.getOut().setBody(messageExchange.getMessage("out").getContent());
                    addHeaders(messageExchange.getMessage("out"), exchange.getOut());
                    addAttachments(messageExchange.getMessage("out"), exchange.getOut());
                }
                messageExchange.setStatus(ExchangeStatus.DONE);
                deliveryChannel.send(messageExchange);
            }

        } catch (MessagingException e) {
            throw new JbiException(e);
        } catch (URISyntaxException e) {
            throw new JbiException(e);
        }
    }
}
