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
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;

import org.apache.camel.CamelContext;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;

/**
 * JBI endpoint that provides access to an underlying {@link org.apache.camel.CamelContext} and
 * manages the lifecycle for that endpoint
 */
public class CamelContextEndpoint extends ProviderEndpoint {

    /**
     * The default {@link org.apache.servicemix.camel.CamelContextEndpoint} service name
     */
    public static final QName SERVICE_NAME = new QName("http://camel.apache.org/schema/jbi", "camelcontext");

    private final CamelContext camelContext;

    public CamelContextEndpoint(CamelContext camelContext, String su) {
        this.camelContext = camelContext;
        setService(SERVICE_NAME);
        setEndpoint(su + "-controlbus");
    }

    public CamelContext getCamelContext() {
        return camelContext;
    }

    @Override
    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getRole() == Role.PROVIDER) {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                //TODO: actually implement some control bus operations
                fail(exchange, new UnsupportedOperationException("No control bus operations available"));
            }
        } else {
            throw new MessagingException(
                    "Unexpected exchange role: CamelContextEndpoint is not capable of handling " + exchange.getRole());
        }

    }

    @Override
    public void start() throws Exception {
        super.start();

        // no need to check for current context state - start() method will perform that check
        camelContext.start();
    }

    @Override
    public void stop() throws Exception {
        super.stop();

        // no need to check for current context state - stop() method will perform that check
        camelContext.stop();

    }

}
