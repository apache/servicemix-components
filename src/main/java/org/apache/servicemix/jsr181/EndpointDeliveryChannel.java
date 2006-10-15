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
package org.apache.servicemix.jsr181;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

/**
 * This class is a wrapper around an existing DeliveryChannel
 * that will be given to service engine endpoints so that
 * they are able to send messages and to interact with the
 * JBI container.
 * 
 * @author gnodet
 */
public class EndpointDeliveryChannel implements DeliveryChannel {

    private final DeliveryChannel channel;
    
    public EndpointDeliveryChannel(DeliveryChannel channel) {
        this.channel = channel;
    }

    public MessageExchange accept() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    public MessageExchange accept(long timeout) throws MessagingException {
        throw new UnsupportedOperationException();
    }

    public void close() throws MessagingException {
        throw new UnsupportedOperationException();
    }

    public MessageExchangeFactory createExchangeFactory() {
        return channel.createExchangeFactory();
    }

    public MessageExchangeFactory createExchangeFactory(QName interfaceName) {
        return channel.createExchangeFactory(interfaceName);
    }

    public MessageExchangeFactory createExchangeFactory(ServiceEndpoint endpoint) {
        return channel.createExchangeFactory(endpoint);
    }

    public MessageExchangeFactory createExchangeFactoryForService(QName serviceName) {
        return channel.createExchangeFactoryForService(serviceName);
    }

    public void send(MessageExchange exchange) throws MessagingException {
        if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            throw new UnsupportedOperationException("Asynchronous send of active exchanges are not supported");
        }
        channel.send(exchange);
    }

    public boolean sendSync(MessageExchange exchange, long timeout) throws MessagingException {
        return channel.sendSync(exchange, timeout);
    }

    public boolean sendSync(MessageExchange exchange) throws MessagingException {
        return channel.sendSync(exchange);
    }
}
