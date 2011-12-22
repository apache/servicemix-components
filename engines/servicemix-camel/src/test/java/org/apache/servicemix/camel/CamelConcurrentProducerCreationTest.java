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
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.Producer;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.tck.mock.MockMessageExchange;

/**
 * Test cases to ensure multiple threads can safely create an use the {@link org.apache.camel.Producer}
 * for our JBI Camel endpoints
 */
public class CamelConcurrentProducerCreationTest extends JbiTestSupport {

    private static final int COUNT = 1000;

    public void testConcurrentlyCreateProducers() throws Exception {
        final MockEndpoint mock = getMockEndpoint("mock:test");
        mock.setResultWaitTime(60000);
        mock.expectedMessageCount(COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(25);

        for (int i = 0 ; i < COUNT ; i++) {
            executor.submit(new Callable<Object>() {
                public Object call() throws Exception {
                    doSendExchangeWithProducer("<message />");
                    return null;
                }
            });
        }

        mock.assertIsSatisfied();
    }

    private void doSendExchangeWithProducer(String body) throws Exception {
        String uri ="jbi:endpoint:urn:test:service:endpoint";
        Endpoint endpoint = camelContext.getEndpoint(uri);

        Exchange exchange = endpoint.createExchange(ExchangePattern.InOnly);
        Message in = exchange.getIn();
        in.setBody(body);

        Producer producer = null;
        try {
            producer = endpoint.createProducer();
            producer.start();
            producer.process(exchange);
        } finally {
            if (producer != null) {
                producer.stop();
            }
        }
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jbi:endpoint:urn:test:service:endpoint").to("mock:test");
            }
        };
    }

    @Override
    protected void appendJbiActivationSpecs(List<ActivationSpec> activationSpecList) {
        // no additional JBI activation specs required
    }
}
