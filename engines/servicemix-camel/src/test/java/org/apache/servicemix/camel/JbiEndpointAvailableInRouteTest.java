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
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.junit.Test;

/**
 * Tests to ensure that the Exchange.getFromEndpoint() call returns the originating Camel endpoint
 */
public class JbiEndpointAvailableInRouteTest extends JbiTestSupport {

    @Test
    public void testSendExchange() throws Exception {
        InOnly exchange = getServicemixClient().createInOnlyExchange();
        exchange.setService(new QName("urn:test", "service"));
        exchange.getInMessage().setContent(new StringSource("<message/>"));

        getServicemixClient().sendSync(exchange);
        assertEquals("Assertions inside Camel Processor should have passed",
                     ExchangeStatus.DONE, exchange.getStatus());

    }

    @Override
    protected void appendJbiActivationSpecs(List<ActivationSpec> activationSpecList) {
        // no additional JBI endpoints required
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jbi:endpoint:urn:test:service:endpoint")
                    .process(new Processor() {
                        
                        public void process(Exchange exchange) {
                            assertTrue("The Camel JbiEndpoint should be set as the 'from' endpoint on the exchange", 
                                       exchange.getFromEndpoint() instanceof JbiEndpoint);

                            JbiEndpoint origin = (JbiEndpoint) exchange.getFromEndpoint();
                            assertEquals("endpoint:urn:test:service:endpoint",
                                         origin.getDestinationUri());
                        }
                    });
            }
        };
    }
}
