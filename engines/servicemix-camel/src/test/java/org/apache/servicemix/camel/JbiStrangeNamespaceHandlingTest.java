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
import org.apache.camel.ExchangePattern;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.ReceiverComponent;

/**
 * Tests on sending JBI InOut and InOnly exchanges from within a Camel route to 
 * a service with a strange namespace
 */
public class JbiStrangeNamespaceHandlingTest extends JbiTestSupport {

    private static final String MESSAGE = "<just><a>test</a></just>";

    public void testInOutWithStrangeNamespace() throws Exception {
        MockEndpoint inout = getMockEndpoint("mock:in-out");
        inout.expectedMessageCount(1);
        
        client.send("direct:in-out", new Processor() {

            public void process(Exchange exchange) throws Exception {
                exchange.getIn().setBody(new StringSource(MESSAGE));
                exchange.setPattern(ExchangePattern.InOut);
            }
            
        });
        
        inout.assertIsSatisfied();
              
        // let's wait a moment to make sure that the last DONE MessageExchange is handled
        Thread.sleep(1000);
    }
    
    public void testInOnlyWithStrangeNamespace() throws Exception {
        MockEndpoint inonly = getMockEndpoint("mock:in-only");
        inonly.expectedMessageCount(1);
        
        client.send("direct:in-only", new Processor() {

            public void process(Exchange exchange) throws Exception {
                exchange.getIn().setBody(new StringSource(MESSAGE));
                exchange.setPattern(ExchangePattern.InOnly);
            }
            
        });
        
        inonly.assertIsSatisfied();
    }
    
    @Override
    protected void appendJbiActivationSpecs(List<ActivationSpec> activationSpecList) {
        // no additional activation specs required
        activationSpecList.add(createActivationSpec(new EchoComponent(), new QName("urn:send.a.to.b.com/1234", "InOutService")));
        activationSpecList.add(createActivationSpec(new ReceiverComponent(), new QName("urn:send.a.to.b.com/1234", "InOnlyService")));
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:in-out")
                    .to("jbi:service:urn:send.a.to.b.com/1234/InOutService")
                    .to("mock:in-out");
                
                from("direct:in-only")
                    .to("jbi:service:urn:send.a.to.b.com/1234/InOnlyService")
                    .to("mock:in-only");
            }
        };
    }
}
