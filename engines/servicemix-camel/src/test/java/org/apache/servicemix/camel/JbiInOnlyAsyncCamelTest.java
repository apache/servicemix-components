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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxp.StringSource;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.container.ActivationSpec;

/**
 * Tests on handling JBI InOnly exchanges by Camel
 */
public class JbiInOnlyAsyncCamelTest extends JbiTestSupport {

    private static final String MESSAGE = "<just><a>test</a></just>";

    public void testInOnlyExchangeThroughAsyncRoute() throws Exception {
        // first remove the ExchangeListener -- it will not be notified of the undeliverable MessageExchange
        jbiContainer.removeListener(exchangeCompletedListener);
        
        MockEndpoint done = getMockEndpoint("mock:done");
        done.expectedBodiesReceived(MESSAGE);
        
        MockEndpoint dlc = getMockEndpoint("mock:dlc");
        dlc.expectedBodiesReceived(MESSAGE);

        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "in-only"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        assertTrue(client.sendSync(exchange));
        done.assertIsSatisfied();
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
    }

    @Override
    protected void appendJbiActivationSpecs(List<ActivationSpec> activationSpecList) {
        // no additional activation specs required
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                onException().to("mock:dlc").maximumRedeliveries(0).handled(false);

                from("jbi:service:urn:test:in-only")
                    .threads(1)
                        .to("mock:done")
                        .to("jbi:service:urn:test:non-existent-service");
            }
        };
    }
}

