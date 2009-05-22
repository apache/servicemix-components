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

import java.util.List;

import org.apache.camel.builder.RouteBuilder;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.tck.mock.MockMessageExchange;

/**
 * Test cases for {@link CamelConsumerEndpoint} 
 */
public class CamelConsumerEndpointTest extends JbiTestSupport {
    
    public void testInvalidMessageExchangeDoesNotThrowException() throws Exception {
        String endpointName = 
            jbiContainer.getRegistry().getExternalEndpointsForService(CamelConsumerEndpoint.SERVICE_NAME)[0].getEndpointName();
        CamelConsumerEndpoint endpoint = 
            (CamelConsumerEndpoint) component.getRegistry().getEndpoint(CamelConsumerEndpoint.SERVICE_NAME + ":" + endpointName);
        
        try {
            // now, let's shamelessly process a completely fake MessageExchange...
            endpoint.process(new MockMessageExchange() {
                @Override
                public String getExchangeId() {
                    return "a-fake-exchange-id";
                }
            });
        } catch (Exception e) {
            // ... and still expect the endpoint to behave properly
            fail("Should not throw " + e);
        }
    }

    @Override
    protected void appendJbiActivationSpecs(List<ActivationSpec> activationSpecList) {
        // no additional JBI activation specs required
    }
    
    @Override
    protected void configureContainer(JBIContainer container) throws Exception {
        super.configureContainer(container);
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:a").to("jbi:endpoint:urn:test:service:endpoint");
                from("jbi:endpoint:urn:test:service:endpoint").to("log:info");
            }            
        };
    }
}
