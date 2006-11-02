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
package org.apache.servicemix.drools.model;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.client.ServiceMixClientFacade;
import org.apache.servicemix.drools.DroolsEndpoint;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.resolver.URIResolver;
import org.apache.servicemix.jbi.util.MessageUtil;
import org.drools.FactHandle;
import org.drools.WorkingMemory;

/**
 * A helper class for use inside a rule to forward a message to an endpoint
 *
 * @version $Revision: 426415 $
 */
public class JbiHelper {
    
    private DroolsEndpoint endpoint;
    private Exchange exchange;
    private WorkingMemory memory;
    private FactHandle exchangeFactHandle;

    public JbiHelper(DroolsEndpoint endpoint, 
                     MessageExchange exchange,
                     WorkingMemory memory) {
        this.endpoint = endpoint;
        this.exchange = new Exchange(exchange);
        this.memory = memory;
        this.exchangeFactHandle = this.memory.assertObject(this.exchange);
    }

    public DroolsEndpoint getEndpoint() {
        return endpoint;
    }
    
    public ComponentContext getContext() {
        return endpoint.getContext();
    }
    
    public DeliveryChannel getChannel() throws MessagingException {
        return getContext().getDeliveryChannel();
    }
    
    public ServiceMixClient getClient() {
        return new ServiceMixClientFacade(getContext());
    }

    public Exchange getExchange() {
        return exchange;
    }

    /**
     * Forwards the inbound message to the given
     *
     * @param uri
     * @param localPart
     */
    /*
    public void forward(String uri) throws MessagingException {
        if (exchange instanceof InOnly || exchange instanceof RobustInOnly) {
            MessageExchange me = getChannel().createExchangeFactory().createExchange(exchange.getPattern());
            URIResolver.configureExchange(me, getContext(), uri);
            MessageUtil.transferToIn(in, me);
            getChannel().sendSync(me);
        } else {
            throw new MessagingException("Only InOnly and RobustInOnly exchanges can be forwarded");
        }
    }
    */

    public void route(String uri) throws MessagingException {
        MessageExchange exchange = this.exchange.getInternalExchange();
        NormalizedMessage in = exchange.getMessage("in");
        MessageExchange me = getChannel().createExchangeFactory().createExchange(exchange.getPattern());
        URIResolver.configureExchange(me, getContext(), uri);
        MessageUtil.transferToIn(in, me);
        getChannel().sendSync(me);
        if (me.getStatus() == ExchangeStatus.DONE) {
            exchange.setStatus(ExchangeStatus.DONE);
            getChannel().send(exchange);
        } else if (me.getStatus() == ExchangeStatus.ERROR) {
            exchange.setStatus(ExchangeStatus.ERROR);
            exchange.setError(me.getError());
            getChannel().send(exchange);
        } else {
            if (me.getFault() != null) {
                MessageUtil.transferFaultToFault(me, exchange);
            } else {
                MessageUtil.transferOutToOut(me, exchange);
            }
            getChannel().sendSync(exchange);
        }
        update();
    }
    
    public void fault(String content) throws Exception {
        MessageExchange exchange = this.exchange.getInternalExchange();
        if (exchange instanceof InOnly) {
            exchange.setError(new Exception(content));
            getChannel().send(exchange);
        } else {
            Fault fault = exchange.createFault();
            fault.setContent(new StringSource(content));
            exchange.setFault(fault);
            getChannel().sendSync(exchange);
        }
        update();
    }
    
    protected void update() {
        this.memory.modifyObject(this.exchangeFactHandle, this.exchange);
    }
}
