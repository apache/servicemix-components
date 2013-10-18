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

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.junit4.TestSupport;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.junit.Test;

import javax.jbi.JBIException;
import javax.jbi.messaging.InOnly;
import javax.xml.namespace.QName;
import java.util.List;

/**
 * Test case to ensure JBI MessageExchange properties are being preserved when routing through Camel 
 */
public class JbiExchangePropertiesPreservationTest extends JbiTestSupport {

    private static final String KEY = "key";
    private static final String NEW_KEY = "newkey";
    private static final Object VALUE = "value";
    private static final Object NEW_VALUE = "newvalue";
    private static final String MESSAGE = "<just><a>test</a></just>";

    @Test
    public void testPropertyPreservation() throws JBIException, InterruptedException {
        MockEndpoint output = getMockEndpoint("mock:output");
        output.expectedMessageCount(1);

        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        exchange.setService(new QName("urn:test", "input"));
        exchange.setProperty(KEY,VALUE);
        client.sendSync(exchange, 10000);

        output.assertIsSatisfied();

        assertEquals("Existing JBI Exchange property value has not been altered",
                     VALUE, exchange.getProperty(KEY));
        assertEquals("New JBI Exchange property has been added",
                     VALUE, exchange.getProperty(NEW_KEY));
        
    }

    @Override
    protected void appendJbiActivationSpecs(List<ActivationSpec> activationSpecList) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {

            @Override
            public void configure() throws Exception {

                from("jbi:service:urn:test:input")
                    .to("jbi:service:urn:test:transformer?mep=in-out")
                    .to("mock:output");

                from("jbi:service:urn:test:transformer").process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        if (exchange.getProperties().containsKey(KEY)) {
                             exchange.setProperty(KEY, NEW_VALUE);
                        } else {
                            throw new RuntimeException("Expected exchange property is missing");
                        }
                        exchange.setProperty(NEW_KEY,VALUE);
                     }
                });
            }
        };
    }
    
}
