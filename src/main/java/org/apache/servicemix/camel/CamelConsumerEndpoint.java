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

import java.util.Set;
import java.net.URISyntaxException;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessagingException;
import javax.jbi.management.DeploymentException;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.endpoints.ConsumerEndpoint;
import org.apache.servicemix.common.util.URIResolver;
import org.apache.servicemix.id.IdGenerator;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.AsyncCallback;

/**
 * A consumer endpoint that will be used to send JBI exchanges
 * originating from camel.
 */
public class CamelConsumerEndpoint extends ConsumerEndpoint implements AsyncProcessor {

    public static final QName SERVICE_NAME = new QName("http://activemq.apache.org/camel/schema/jbi", "consumer");

    private JbiBinding binding;

    private JbiEndpoint jbiEndpoint;

    public CamelConsumerEndpoint(JbiBinding binding, JbiEndpoint jbiEndpoint) {
        setService(SERVICE_NAME);
        setEndpoint(new IdGenerator().generateId());
        this.binding = binding;
        this.jbiEndpoint = jbiEndpoint;
    }

    public void process(MessageExchange messageExchange) throws Exception {
        Exchange exchange = (Exchange) messageExchange.getProperty(Exchange.class.getName());
        AsyncCallback asyncCallback =(AsyncCallback) messageExchange.getProperty(AsyncCallback.class.getName());
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
            done(messageExchange);
        }
        asyncCallback.done(false);
    }

    public boolean process(Exchange exchange, AsyncCallback asyncCallback) {
        try {
            MessageExchange messageExchange = binding.makeJbiMessageExchange(exchange, getExchangeFactory(), jbiEndpoint.getMep());

            if (jbiEndpoint.getOperation() != null) {
                messageExchange.setOperation(QName.valueOf(jbiEndpoint.getOperation()));
            }

            URIResolver.configureExchange(messageExchange, getContext(), jbiEndpoint.getDestinationUri());
            messageExchange.setProperty(Exchange.class.getName(), exchange);
            messageExchange.setProperty(AsyncCallback.class.getName(), asyncCallback);

            send(messageExchange);
            return false;
        } catch (MessagingException e) {
            throw new JbiException(e);
        } catch (URISyntaxException e) {
            throw new JbiException(e);
        }
    }

    public void process(Exchange exchange) throws Exception {
        try {
            MessageExchange messageExchange = binding.makeJbiMessageExchange(exchange, getExchangeFactory(), jbiEndpoint.getMep());

            if (jbiEndpoint.getOperation() != null) {
                messageExchange.setOperation(QName.valueOf(jbiEndpoint.getOperation()));
            }

            URIResolver.configureExchange(messageExchange, getContext(), jbiEndpoint.getDestinationUri());

            sendSync(messageExchange);

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
                done(messageExchange);
            }

        } catch (MessagingException e) {
            throw new JbiException(e);
        } catch (URISyntaxException e) {
            throw new JbiException(e);
        }
    }

    @Override
    public void validate() throws DeploymentException {
        // No validation required
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

}
