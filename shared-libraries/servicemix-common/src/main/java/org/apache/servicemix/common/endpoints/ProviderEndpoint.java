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
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.JbiConstants;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;

public abstract class ProviderEndpoint extends SimpleEndpoint {

    private ServiceEndpoint activated;


    public ProviderEndpoint() {
    }

    public ProviderEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    public ProviderEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component.getServiceUnit(), endpoint.getServiceName(), endpoint.getEndpointName());
    }

    public Role getRole() {
        return Role.PROVIDER;
    }

    public void activate() throws Exception {
        super.activate();
        ComponentContext ctx = getServiceUnit().getComponent().getComponentContext();
        activated = ctx.activateEndpoint(service, endpoint);
    }

    public void deactivate() throws Exception {
        if (activated == null) {
            throw new IllegalStateException("Endpoint not activated: " + this);
        }
        ServiceEndpoint ep = activated;
        activated = null;
        ComponentContext ctx = getServiceUnit().getComponent().getComponentContext();
        ctx.deactivateEndpoint(ep);
        super.deactivate();
    }

    /**
     * <p>
     * A default implementation of the message processor which checks the status of the exchange
     * and if its valid will dispatch to either {@link #processInOnly(MessageExchange,NormalizedMessage)} for
     * an {@link InOnly} or {@link RobustInOnly} message exchange otherwise the
     * {@link #processInOut(MessageExchange,NormalizedMessage,NormalizedMessage)}
     * method will be invoked.
     * </p>
     *
     * @param exchange the message exchange
     * @throws Exception
     */
    public void process(MessageExchange exchange) throws Exception {
        // The component acts as a provider, this means that another component has requested our service
        // As this exchange is active, this is either an in or a fault (out are sent by this component)
        if (exchange.getRole() == Role.PROVIDER) {
            // Exchange is finished
            if (exchange.getStatus() == ExchangeStatus.DONE) {
                return;
            // Exchange has been aborted with an exception
            } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
                return;
            // Exchange is active
            } else {
                NormalizedMessage in;
                // Fault message
                if (exchange.getFault() != null) {
                    done(exchange);
                // In message
                } else if ((in = exchange.getMessage("in")) != null) {
                    if (exchange instanceof InOnly || exchange instanceof RobustInOnly) {
                        processInOnly(exchange, in);
                        done(exchange);
                    }
                    else {
                        NormalizedMessage out = exchange.getMessage("out");
                        if (out == null) {
                            out = exchange.createMessage();
                            exchange.setMessage(out, "out");
                        }
                        processInOut(exchange, in, out);
                        boolean txSync = exchange.isTransacted() && Boolean.TRUE.equals(exchange.getProperty(JbiConstants.SEND_SYNC));
                        if (txSync) {
                            sendSync(exchange);
                        } else {
                            send(exchange);
                        }
                    }
                // This is not compliant with the default MEPs
                } else {
                    throw new IllegalStateException("Provider exchange is ACTIVE, but no in or fault is provided");
                }
            }
        // Unsupported role: this should never happen has we never create exchanges
        } else {
            throw new IllegalStateException("Unsupported role: " + exchange.getRole());
        }
    }


    protected void processInOnly(MessageExchange exchange, NormalizedMessage in) throws Exception {
        throw new UnsupportedOperationException("Unsupported MEP: " + exchange.getPattern());
    }

    protected void processInOut(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws Exception {
        throw new UnsupportedOperationException("Unsupported MEP: " + exchange.getPattern());
    }
    
}
