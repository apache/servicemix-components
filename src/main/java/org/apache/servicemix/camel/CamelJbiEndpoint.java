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

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.RobustInOnly;
import javax.xml.namespace.QName;

import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;

/**
 * A JBI endpoint which when invoked will delegate to a Camel endpoint
 * 
 * @version $Revision: 426415 $
 */
public class CamelJbiEndpoint extends ProviderEndpoint {
    public static final QName SERVICE_NAME = new QName("http://activemq.apache.org/camel/schema/jbi", "endpoint");

    private static final transient Log LOG = LogFactory.getLog(CamelJbiEndpoint.class);

    private Endpoint camelEndpoint;

    private JbiBinding binding;

    private Processor camelProcessor;

    public CamelJbiEndpoint(ServiceUnit serviceUnit, QName service, String endpoint, Endpoint camelEndpoint, JbiBinding binding,
            Processor camelProcessor) {
        super(serviceUnit, service, endpoint);
        this.camelProcessor = camelProcessor;
        this.camelEndpoint = camelEndpoint;
        this.binding = binding;
    }

    public CamelJbiEndpoint(ServiceUnit serviceUnit, Endpoint camelEndpoint, JbiBinding binding, Processor camelProcessor) {
        this(serviceUnit, SERVICE_NAME, camelEndpoint.getEndpointUri(), camelEndpoint, binding, camelProcessor);
    }

    @Override
    public void process(MessageExchange exchange) throws Exception {
        // The component acts as a provider, this means that another component has requested our service
        // As this exchange is active, this is either an in or a fault (out are sent by this component)
        if (exchange.getRole() == MessageExchange.Role.PROVIDER) {
            // Exchange is finished
            if (exchange.getStatus() == ExchangeStatus.DONE) {
                return;
            // Exchange has been aborted with an exception
            } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
                return;
            // Exchange is active
            } else {
                handleActiveProviderExchange(exchange);

            }
        // Unsupported role: this should never happen has we never create exchanges
        } else {
            throw new IllegalStateException("Unsupported role: " + exchange.getRole());
        }
    }

    protected void handleActiveProviderExchange(MessageExchange exchange) throws Exception {
        // Fault message
        if (exchange.getFault() != null) {
            done(exchange);
        // In message
        } else if (exchange.getMessage("in") != null) {
            if (exchange instanceof InOnly || exchange instanceof RobustInOnly) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Received exchange: " + exchange);
                }
                JbiExchange camelExchange = new JbiExchange(camelEndpoint.getCamelContext(), binding, exchange);
                camelProcessor.process(camelExchange);
                done(exchange);
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Received exchange: " + exchange);
                }
                JbiExchange camelExchange = new JbiExchange(camelEndpoint.getCamelContext(), binding, exchange);
                camelProcessor.process(camelExchange);
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

}
