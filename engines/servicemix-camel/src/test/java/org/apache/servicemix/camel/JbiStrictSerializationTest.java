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

import java.io.ByteArrayOutputStream;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxp.StringSource;
import org.apache.servicemix.camel.test.InvalidSerializableObject;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;

/**
 * Testcase for strictSerialization=true (removes all non-serializable headers, even those that incorrectly
 * implement the Serializable interface)
 */
public class JbiStrictSerializationTest extends JbiCamelErrorHandlingTestSupport {

    private static final String SERIALIZABLE_KEY = "serializable";
    private static final String INVALID_SERIALIZABLE_KEY = "invalid.serializable";
    private static final String NON_SERIALIZABLE_KEY = "non.serializable";

    public void testInOutWithStrictSerialization() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(1);

        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOut exchange = client.createInOutExchange();
        exchange.setService(new QName("urn:test", "strictserialization"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ACTIVE, exchange.getStatus());
        client.done(exchange);
       
        errors.assertIsSatisfied();

        assertTrue("Serializable values are still in the headers",
                   exchange.getOutMessage().getPropertyNames().contains(SERIALIZABLE_KEY));
        assertFalse("Non-serializable values are not in the headers",
                   exchange.getOutMessage().getPropertyNames().contains(NON_SERIALIZABLE_KEY));
        assertFalse("Non-serializable values are not in the headers",
                    exchange.getOutMessage().getPropertyNames().contains(INVALID_SERIALIZABLE_KEY));

        // let's wait a moment to make sure that the last DONE MessageExchange is handled
        Thread.sleep(500);
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("jbi:endpoint:urn:test:strictserialization:endpoint?serialization=strict")
                    .process(new Processor() {
                        public void process(Exchange exchange) throws Exception {
                            Message message = exchange.getOut();
                            message.copyFrom(exchange.getIn());
                            message.setHeader(SERIALIZABLE_KEY, "A string is Serializable");
                            message.setHeader(INVALID_SERIALIZABLE_KEY, new InvalidSerializableObject());
                            message.setHeader(NON_SERIALIZABLE_KEY, new ByteArrayOutputStream());
                        }
                    }).to("mock:errors");
            }
        };
    }
}
