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
import java.util.Map;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.util.URISupport;
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

    private String destinationUri;

    private String mep;

    public ToJbiProcessor(JbiBinding binding, ComponentContext componentContext, String destinationUri) {
        this.binding = binding;
        this.componentContext = componentContext;
        this.destinationUri = destinationUri;
        try {
            int idx = destinationUri.indexOf('?');
            if (idx > 0) {
                Map params = URISupport.parseQuery(destinationUri.substring(idx + 1));
                mep = (String) params.get("mep");
                if (mep != null && !mep.startsWith("http://www.w3.org/ns/wsdl/")) {
                    mep = "http://www.w3.org/ns/wsdl/" + mep;
                }
                this.destinationUri = destinationUri.substring(0, idx);
            }
        } catch (URISyntaxException e) {
            throw new JbiException(e);
        }
    }

    public void process(Exchange exchange) {
        try {
            DeliveryChannel deliveryChannel = componentContext.getDeliveryChannel();
            MessageExchangeFactory exchangeFactory = deliveryChannel.createExchangeFactory();
            MessageExchange messageExchange = binding.makeJbiMessageExchange(exchange, exchangeFactory, mep);

            URIResolver.configureExchange(messageExchange, componentContext, destinationUri);
            deliveryChannel.sendSync(messageExchange);

            if (messageExchange.getStatus() == ExchangeStatus.ERROR) {
                exchange.setException(messageExchange.getError());
            } else if (messageExchange.getStatus() == ExchangeStatus.ACTIVE) {
                if (messageExchange.getFault() != null) {
                    exchange.getFault().setBody(messageExchange.getFault().getContent());
                } else {
                    exchange.getOut().setBody(messageExchange.getMessage("out").getContent());
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
