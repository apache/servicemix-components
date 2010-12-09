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

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.xml.namespace.QName;

import org.apache.camel.CamelContext;
import org.apache.camel.RecipientList;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxp.StringSource;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.JndiRegistry;
import org.apache.camel.util.jndi.JndiContext;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;

/**
 * Tests on using the Dynamic Recipient List pattern inside servicemix-camel
 */
public class JbiDynamicRecipientListTest extends JbiCamelErrorHandlingTestSupport {

    public void testInOnlyDynamicRecipientListException() throws Exception {
        MockEndpoint a = getMockEndpoint("mock:a");
        a.expectedMessageCount(1);

        MockEndpoint b = getMockEndpoint("mock:b");
        b.expectedMessageCount(1);

        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "recipientlist"));
        exchange.getInMessage().setContent(new StringSource("<request>Please forward this to the recipients</request>"));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.DONE, exchange.getStatus());

        a.assertIsSatisfied();
        b.assertIsSatisfied();
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                errorHandler(noErrorHandler());

                from("jbi:endpoint:urn:test:recipientlist:endpoint")
                    .beanRef("annotated-pojo", "route");                

                from("jbi:endpoint:urn:test:a:endpoint").to("mock:a");
                from("jbi:endpoint:urn:test:b:endpoint").to("mock:b");
            }
        };
    }

    @Override
    protected CamelContext createCamelContext() {
        try {
            JndiContext context = new JndiContext();
            context.bind("annotated-pojo", new MyDynamicRecipientListPojo());
            JndiRegistry registry = new JndiRegistry(context);
            return new DefaultCamelContext(registry);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        return null;
    }


    protected final class MyDynamicRecipientListPojo {

        @RecipientList
        public String[] route(String message) {
            return new String[] {
                "jbi:endpoint:urn:test:a:endpoint",
                "jbi:endpoint:urn:test:b:endpoint"
            };
        }
    }
}
