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
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.processor.aggregate.UseLatestAggregationStrategy;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.jaxp.StringSource;

/**
 * Tests on handling JBI InOnly exchanges by Camel 
 */
public class JbiInOnlyTest extends JbiTestSupport {
    
    private static final String MESSAGE = "<just><a>test</a></just>";
    private static final int COUNT = 50;

    public void testInOnlyExchangeConvertBody() throws Exception {
        MockEndpoint done = getMockEndpoint("mock:done");
        done.expectedBodiesReceived(MESSAGE);
        
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "in-only"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.send(exchange);
        assertNotNull("Expecting to receive a DONE/ERROR MessageExchange", client.receive(20000));
        assertEquals(ExchangeStatus.DONE, exchange.getStatus());
        done.assertIsSatisfied();
    }
    
    public void testInOnlyExchangeForwardAndConvertBody() throws Exception {
        MockEndpoint done = getMockEndpoint("mock:done");
        done.expectedBodiesReceived(MESSAGE);
        
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "forward"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        
        done.assertIsSatisfied();
    }

    public void testInOnlyWithException() throws Exception {
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "in-only-error"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
    }

    public void testInOutWithException() throws Exception {
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOut exchange = client.createInOutExchange();
        exchange.setService(new QName("urn:test", "in-only-error"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
    }
    
    public void testInOnlyToAggregator() throws Exception {
        ServiceMixClient smxClient = getServicemixClient();
        getMockEndpoint("mock:aggregated").expectedMessageCount(1);
        for (int i = 0; i < COUNT; i++) {
            InOnly exchange = smxClient.createInOnlyExchange();
            exchange.setService(new QName("urn:test", "in-only-aggregator"));
            exchange.getInMessage().setProperty("key", "aggregate-this");
            exchange.getInMessage().setContent(new StringSource("<request>Could you please aggregate this?</request>"));
            smxClient.send(exchange);
        }
        getMockEndpoint("mock:aggregated").assertIsSatisfied();

        // give the DONE exchanges a few moments to be delivered
        Thread.sleep(1000);
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
                // let's not retry things too often as this will only slow down the unit tests
                onException().to("mock:errors").maximumRedeliveries(0).handled(false);
                
                from("jbi:service:urn:test:forward").to("jbi:service:urn:test:in-only?mep=in-only");
                from("jbi:service:urn:test:in-only").convertBodyTo(String.class).to("mock:done");
                from("jbi:service:urn:test:in-only-error").process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        throw new Exception("Error");
                    }
                });
                from("jbi:service:urn:test:in-only-aggregator")
                    .to("log:info")
                    .aggregate(header("key"), new UseLatestAggregationStrategy()).completionSize(COUNT)
                    .setHeader("aggregated").constant(true)
                    .to("log:info")
                    .to("mock:aggregated");
            }
        };
    }
}
