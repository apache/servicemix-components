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
package org.apache.servicemix.common.endpoints;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.EndpointComponentContext;
import org.apache.servicemix.common.ServiceUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SimpleEndpoint extends AbstractEndpoint {

    protected static Logger logger = LoggerFactory.getLogger(SimpleEndpoint.class);

    private DeliveryChannel channel;
    private MessageExchangeFactory exchangeFactory;
    private ComponentContext context;

    public SimpleEndpoint() {
    }

    public SimpleEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    public SimpleEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component.getServiceUnit(), endpoint.getServiceName(), endpoint.getEndpointName());
    }

    public synchronized void activate() throws Exception {
        context = new EndpointComponentContext(this);
        channel = context.getDeliveryChannel();
        exchangeFactory = channel.createExchangeFactory();
    }

    public synchronized void deactivate() throws Exception {
    }

    public synchronized void start() throws Exception {
    }

    public synchronized void stop() throws Exception {
    }

    protected void send(MessageExchange me) throws MessagingException {
        channel.send(me);
    }
    
    protected void sendSync(MessageExchange me) throws MessagingException {
        if (!channel.sendSync(me)) {
            throw new MessagingException("SendSync failed");
        }
    }
    
    protected void done(MessageExchange me) throws MessagingException {
        logger.debug("SimpleEndpoint.done called: ");

        me.setStatus(ExchangeStatus.DONE);
        send(me);
    }
    
    protected void fail(MessageExchange me, Exception error) throws MessagingException {
        logger.warn("SimpleEndpoint.fail called: ", error);

        me.setError(error);
        send(me);
    }
    
    /**
     * @return the exchangeFactory
     */
    public MessageExchangeFactory getExchangeFactory() {
        return exchangeFactory;
    }

    /**
     * @return the channel
     */
    public DeliveryChannel getChannel() {
        return channel;
    }

    /**
     * @return the context
     */
    public ComponentContext getContext() {
        return context;
    }

}
