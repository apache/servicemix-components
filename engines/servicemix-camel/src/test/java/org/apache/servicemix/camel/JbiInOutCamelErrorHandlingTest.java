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
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxp.StringSource;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.exception.FaultException;

/**
 * Tests on handling fault messages with the Camel Exception handler
 */
public class JbiInOutCamelErrorHandlingTest extends JbiCamelErrorHandlingTestSupport {

    private static final String MESSAGE = "<just><a>test</a></just>";

    public void testInOutWithNoHandleFault() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(0);

        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOut exchange = client.createInOutExchange();
        exchange.setService(new QName("urn:test", "no-handle-fault"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ACTIVE, exchange.getStatus());
        assertNotNull(exchange.getFault());
        client.done(exchange);

        errors.assertIsSatisfied();
        
        // let's wait a moment to make sure that the last DONE MessageExchange is handled
        Thread.sleep(500);
    }

    public void testInOutWithHandleFault() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(0);
        MockEndpoint faults = getMockEndpoint("mock:faults-handled");
        faults.expectedMessageCount(1);

        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOut exchange = client.createInOutExchange();
        exchange.setService(new QName("urn:test", "handle-fault"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ACTIVE, exchange.getStatus());
        assertNull("Fault has been handled inside Camel route", exchange.getFault());
        client.done(exchange);

        errors.assertIsSatisfied();

        // let's wait a moment to make sure that the last DONE MessageExchange is handled
        Thread.sleep(500);
    }

    public void testInOutWithErrorNotHandled() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(1);

        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOut exchange = client.createInOutExchange();
        exchange.setService(new QName("urn:test", "error-not-handled"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
        assertTrue("A IllegalArgumentException was expected", exchange.getError() instanceof IllegalArgumentException);

        errors.assertIsSatisfied();
    }

    public void testInOutWithErrorHandledFalse() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(0);

        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOut exchange = client.createInOutExchange();
        exchange.setService(new QName("urn:test", "error-handled-false"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
        assertTrue("A IllegalStateException was expected", exchange.getError() instanceof IllegalStateException);

        receiverComponent.getMessageList().assertMessagesReceived(1);

        errors.assertIsSatisfied();
    }

    public void testInOutWithErrorHandledTrue() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(0);

        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOut exchange = client.createInOutExchange();
        exchange.setService(new QName("urn:test", "error-handled-true"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ACTIVE, exchange.getStatus());
        client.done(exchange);

        receiverComponent.getMessageList().assertMessagesReceived(1);

        errors.assertIsSatisfied();
        
        // let's wait a moment to make sure that the last DONE MessageExchange is handled
        Thread.sleep(500);
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                onException(IllegalStateException.class).handled(false).to("jbi:service:urn:test:receiver-service?mep=in-only");
                onException(NullPointerException.class).handled(true).to("jbi:service:urn:test:receiver-service?mep=in-only");
                onException(FaultException.class).handled(true).to("mock:faults-handled");
                errorHandler(deadLetterChannel("mock:errors").maximumRedeliveries(1).redeliverDelay(300).handled(false));
                from("jbi:service:urn:test:no-handle-fault").to("jbi:service:urn:test:faulty-service");
                from("jbi:service:urn:test:handle-fault").handleFault().to("jbi:service:urn:test:faulty-service");
                from("jbi:service:urn:test:error-not-handled").to("jbi:service:urn:test:iae-error-service");
                from("jbi:service:urn:test:error-handled-false").to("jbi:service:urn:test:ise-error-service");
                from("jbi:service:urn:test:error-handled-true").to("jbi:service:urn:test:npe-error-service");
            }
        };
    }

    @Override
    protected void appendJbiActivationSpecs(List<ActivationSpec> activationSpecList) {
        super.appendJbiActivationSpecs(activationSpecList);
    }

}
