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
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.jaxp.StringSource;

/**
 * Test to make sure that a Camel Pipeline is capable of preserving JBI headers 
 */
public class JbiInOnlyPropertiesPipelineTest extends JbiTestSupport {
    
    private static final String MESSAGE = "<just><a>test</a></just>";
    
    private static final String HEADER_ORIGINAL = "original";
    private static final String HEADER_TRANSFORMER = "transformer";    

    public void testPipelinePreservesMessageHeaders() throws Exception {
        MockEndpoint output = getMockEndpoint("mock:output");
        output.expectedBodiesReceived(MESSAGE);
        
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "input"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        exchange.getInMessage().setProperty(HEADER_ORIGINAL, "my-original-header-value");
        client.send(exchange);
        client.receive(1000);
        assertEquals(ExchangeStatus.DONE, exchange.getStatus());
        
        output.assertIsSatisfied();
        JbiExchange result = (JbiExchange) output.getExchanges().get(0);
        
        NormalizedMessage normalizedMessage = result.getInMessage();
        assertNotNull(normalizedMessage.getProperty(HEADER_ORIGINAL));
        assertNotNull(normalizedMessage.getProperty(HEADER_TRANSFORMER));
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
                from("jbi:service:urn:test:input")
                    .to("jbi:service:urn:test:transformer?mep=in-out")
                    .to("jbi:service:urn:test:output");
                
                from("jbi:service:urn:test:transformer").process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        // let's copy everything
                        exchange.getOut().copyFrom(exchange.getIn());
                        // check the headers and add another one
                        assertNotNull(exchange.getIn().getHeader(HEADER_ORIGINAL));
                        exchange.getOut().setHeader(HEADER_TRANSFORMER, "my-transformer-header-value");
                    }                    
                });
                
                from("jbi:service:urn:test:output").to("mock:output");
            }
        };
    }
}
