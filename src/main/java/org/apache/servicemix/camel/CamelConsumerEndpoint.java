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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.servicemix.common.endpoints.ConsumerEndpoint;
import org.apache.servicemix.common.util.URIResolver;
import org.apache.servicemix.id.IdGenerator;

/**
 * A consumer endpoint that will be used to send JBI exchanges
 * originating from camel.
 */
public class CamelConsumerEndpoint extends ConsumerEndpoint implements AsyncProcessor {

    public static final QName SERVICE_NAME = new QName("http://activemq.apache.org/camel/schema/jbi", "consumer");

    private JbiBinding binding;

    private JbiEndpoint jbiEndpoint;
    
    private Map<String, AsyncCallback> callbacks = new ConcurrentHashMap<String, AsyncCallback>();
    private Map<String, Exchange> exchanges = new ConcurrentHashMap<String, Exchange>();

    public CamelConsumerEndpoint(JbiBinding binding, JbiEndpoint jbiEndpoint) {
        setService(SERVICE_NAME);
        setEndpoint(new IdGenerator().generateId());
        this.binding = binding;
        this.jbiEndpoint = jbiEndpoint;
    }

    public void process(MessageExchange messageExchange) throws Exception {
        Exchange exchange = exchanges.remove(messageExchange.getExchangeId());
        if (exchange == null) {
            String message = String.format("Unable to find Camel Exchange for JBI MessageExchange %s", messageExchange.getExchangeId());
            logger.warn(message);
            if (messageExchange.getStatus() == ExchangeStatus.ACTIVE) {
                fail(messageExchange, new JbiException(message));
            }
        } else {
            processReponse(messageExchange, exchange);
        }
        
        AsyncCallback asyncCallback = callbacks.remove(messageExchange.getExchangeId());
        if (asyncCallback == null) {
            logger.warn(String.format("Unable to find Camel AsyncCallback for JBI MessageExchange %s", messageExchange.getExchangeId()));
        } else {
            asyncCallback.done(false);
        }
    }

    public boolean process(Exchange exchange, AsyncCallback asyncCallback) {
        try {
            MessageExchange messageExchange = binding.makeJbiMessageExchange(exchange, getExchangeFactory(), jbiEndpoint.getMep());

            if (jbiEndpoint.getOperation() != null) {
                messageExchange.setOperation(QName.valueOf(jbiEndpoint.getOperation()));
            }

            URIResolver.configureExchange(messageExchange, getContext(), jbiEndpoint.getDestinationUri());
            exchanges.put(messageExchange.getExchangeId(), exchange);
            callbacks.put(messageExchange.getExchangeId(), asyncCallback);

            send(messageExchange);
            return false;
        } catch (Exception e) {
            exchange.setException(e);
            asyncCallback.done(true);
            return true;
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

            processReponse(messageExchange, exchange);

        } catch (MessagingException e) {
            exchange.setException(e);
            throw new JbiException(e);
        } catch (URISyntaxException e) {
            exchange.setException(e);
            throw new JbiException(e);
        }
    }
    
    private void processReponse(MessageExchange messageExchange, Exchange exchange) throws MessagingException {
        if (messageExchange.getStatus() == ExchangeStatus.ERROR) {
            exchange.setException(messageExchange.getError());
        } else if (messageExchange.getStatus() == ExchangeStatus.ACTIVE) {
            addHeaders(messageExchange, exchange);
            if (messageExchange.getFault() != null) {
                exchange.getFault().setBody(messageExchange.getFault().getContent());
                addHeaders(messageExchange.getFault(), exchange.getFault());
                addAttachments(messageExchange.getFault(), exchange.getFault());
            } else if (messageExchange.getMessage("out") != null) {
                exchange.getOut().setBody(messageExchange.getMessage("out").getContent());
                addHeaders(messageExchange.getMessage("out"), exchange.getOut());
                addAttachments(messageExchange.getMessage("out"), exchange.getOut());
            }
            done(messageExchange);
        }
    }

    @Override
    public void validate() throws DeploymentException {
        // No validation required
    }

    @SuppressWarnings("unchecked")
    private void addHeaders(MessageExchange messageExchange, Exchange camelExchange) {
        Set entries = messageExchange.getPropertyNames();
        for (Object o : entries) {
            String key = o.toString();
            camelExchange.setProperty(key, messageExchange.getProperty(key));
        }
    }

    @SuppressWarnings("unchecked")
    private void addHeaders(NormalizedMessage normalizedMessage, Message camelMessage) {
        Set entries = normalizedMessage.getPropertyNames();
        for (Object o : entries) {
            String key = o.toString();
            camelMessage.setHeader(key, normalizedMessage.getProperty(key));
        }
    }

    @SuppressWarnings("unchecked")
    private void addAttachments(NormalizedMessage normalizedMessage, Message camelMessage) {
        Set entries = normalizedMessage.getAttachmentNames();
        for (Object o : entries) {
            String id = o.toString();
            camelMessage.addAttachment(id, normalizedMessage.getAttachment(id));
        }
    }

}
