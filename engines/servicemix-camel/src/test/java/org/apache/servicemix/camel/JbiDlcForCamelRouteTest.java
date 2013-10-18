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
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.StringSource;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.tck.ReceiverComponent;
import org.junit.Test;

/**
 * Tests on a JBI DLC endpoint handling an error in the Camel route
 */
public class JbiDlcForCamelRouteTest extends JbiTestSupport {

    private static final String MESSAGE = "<just><a>test</a></just>";

    @Test
    public void testErrorHandlingByJbiEndpointInCamelRoute() throws Exception {
        InOnly exchange = getServicemixClient().createInOnlyExchange();
        exchange.setService(new QName("urn:test", "exception"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));

        getServicemixClient().sendSync(exchange);

        // Camel 2.x DLC marks the Exchange handled by default
        assertEquals("Exchange should have finished OK",
                     ExchangeStatus.DONE, exchange.getStatus());
        assertNull("Exception should be cleared from the JBI Exchange",
                   exchange.getError());
    }

    @Override
    protected void appendJbiActivationSpecs(List<ActivationSpec> activationSpecList) {
        activationSpecList.add(createActivationSpec(new ReceiverComponent() {
            @Override
            public void onMessageExchange(MessageExchange exchange) throws MessagingException {
                if (exchange.getProperty(Exchange.EXCEPTION_CAUGHT) == null) {
                    fail(exchange, new IllegalStateException("Caught exception header not set"));
                } else {
                    super.onMessageExchange(exchange);
                }
            }
        }, new QName("urn:test", "assert-handled-exception")));
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                errorHandler(deadLetterChannel("jbi:service:urn:test:assert-handled-exception").maximumRedeliveries(0));

                from("jbi:endpoint:urn:test:exception:endpoint")
                    .to("log:test")
                    .process(new Processor() {
                        
                        public void process(Exchange exchange) throws Exception {
                            exchange.setException(new RuntimeException("Can you handle me, JBI endpoint?"));
                        }
                    });
            }

        };
    }
}
