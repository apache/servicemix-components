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

import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxp.StringSource;
import org.apache.servicemix.jbi.container.ActivationSpec;

/**
 * Tests on handling JBI InOnly exchanges by Camel
 */
public class JbiOperationCamelTest extends JbiTestSupport {
    
    private static final String OPERATION = "doit";
    
    public void testInOnlySetOperationOnCamelEndpoint() throws Exception {
        MockEndpoint inonly = getMockEndpoint("mock:in-only");
        inonly.expectedMessageCount(1);
        
        // either set the operation on the Camel JBI Endpoint
        client.sendBody("direct:in-only", new StringSource("<request>How about adding an operation?</request>"));
        
        inonly.assertIsSatisfied();
        Exchange exchange = inonly.getExchanges().get(0);
        assertEquals(OPERATION, exchange.getProperty("jbi.operation"));
    }
    
    public void testInOnlySetOperationOnCamelExchange() throws Exception {
        MockEndpoint inonly = getMockEndpoint("mock:in-only");
        inonly.expectedMessageCount(1);
        
        // this time, we set the target operation on the Camel Exchange
        client.send("direct:in-only-noop", new Processor() {
            @Override
            public void process(Exchange exchange) throws Exception {
                exchange.setProperty("jbi.operation", OPERATION);
                exchange.getIn().setBody(new StringSource("<request>Sending you the operation, could you please perform it?</request>"));
            }    
        });
        
        inonly.assertIsSatisfied();
        Exchange exchange = inonly.getExchanges().get(0);
        assertEquals(OPERATION, exchange.getProperty("jbi.operation"));
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
                from("direct:in-only").to("jbi:service:urn:test:in-only?operation=" + OPERATION);
                from("direct:in-only-noop").to("log:info").to("jbi:service:urn:test:in-only");
                
                from("jbi:service:urn:test:in-only").process(new Processor() {
                    @Override
                    public void process(Exchange exchange) throws Exception {
                        JbiExchange jbi = (JbiExchange) exchange;
                        assertEquals(new QName(OPERATION), jbi.getMessageExchange().getOperation());
                    }                    
                }).to("mock:in-only");
            }
        };
    }
}
