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

import org.apache.camel.builder.RouteBuilder;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.executors.impl.ExecutorFactoryImpl;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.StringSource;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.xml.namespace.QName;
import java.util.List;

/**
 * Tests to make sure that even a small thread pool can handle a lot of servicemix-camel interactions
 * by avoiding the use of sendSync internally.
 */
public class AsyncJbiMessagingTest extends JbiTestSupport {

    private static final String MESSAGE = "<just><a>test</a></just>";

    public void testNoSyncMessagingDeadlock() throws Exception {
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "service"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange, 5000);
        assertEquals("Should finish within the designated time-frame, otherwise we probably caused a thread deadlock",
                     ExchangeStatus.DONE, exchange.getStatus());
    }

    public void testCallbackExchangesEmptyOnError() throws Exception {
        // disable the exchange completed listener as it is unable to detect failed exchanges
        disableExchangeCompletedListener();        

        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "error"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());

        // the tearDown method will now ensure that the ContinuationData map on the endpoint is empty
        // even though the send failed
    }

    @Override
    protected void configureContainer(JBIContainer container) throws Exception {
        super.configureContainer(container);

        // let's tune down the default thread pool size to make sure the test fails with any JBI sync exchange
        ExecutorFactoryImpl impl = new ExecutorFactoryImpl();
        impl.getDefaultConfig().setCorePoolSize(1);

        container.setExecutorFactory(impl);
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
                // this chain of endpoints is too long to be handled with all sync calls
                from("jbi:service:urn:test:service").to("jbi:service:urn:test:service1");
                from("jbi:service:urn:test:service1").to("jbi:service:urn:test:service2");
                from("jbi:service:urn:test:service2").to("jbi:service:urn:test:service3");
                from("jbi:service:urn:test:service3").to("jbi:service:urn:test:service4");
                from("jbi:service:urn:test:service4").to("jbi:service:urn:test:service5");
                from("jbi:service:urn:test:service5").to("log:test");

                // deliberately send to an non-existing endpoint
                from("jbi:service:urn:test:error").to("jbi:service:urn:test:non-existing");
            }
        };
    }
}
