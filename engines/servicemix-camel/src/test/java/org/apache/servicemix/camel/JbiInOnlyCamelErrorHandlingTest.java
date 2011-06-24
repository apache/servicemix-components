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
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.RobustInOnly;
import javax.xml.namespace.QName;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxp.StringSource;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.exception.FaultException;

/**
 * Tests on handling fault messages with the Camel Exception handler  
 */
public class JbiInOnlyCamelErrorHandlingTest extends JbiCamelErrorHandlingTestSupport {
    
    private static final String MESSAGE = "<just><a>test</a></just>";
    
    public void testInOnlyWithNoHandleFault() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(1);
        
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "no-handle-fault"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
        // This exception is the 'old' FaultException type being thrown by ComponentSupport 
        // TODO: Remove explicit package name when SM-1891 has been fixed and released
        assertTrue("A FaultException was expected", exchange.getError() instanceof org.apache.servicemix.jbi.FaultException);

        receiverComponent.getMessageList().assertMessagesReceived(0);
        
        errors.assertIsSatisfied();
    }

    public void testRobustInOnlyWithNoHandleFault() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(0);
        
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        RobustInOnly exchange = client.createRobustInOnlyExchange();
        exchange.setService(new QName("urn:test", "no-handle-fault"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ACTIVE, exchange.getStatus());
        assertNotNull(exchange.getFault());
        client.done(exchange);

        receiverComponent.getMessageList().assertMessagesReceived(0);
            
        errors.assertIsSatisfied();
        
        // let's wait a moment to make sure that the last DONE MessageExchange is handled
        Thread.sleep(500);
    }

    public void testInOnlyWithHandleFault() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(1);
        
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "handle-fault"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
        // This exception is the 'old' FaultException type being thrown by ComponentSupport 
        // TODO: Remove explicit package name when SM-1891 has been fixed and released
        assertTrue("A FaultException was expected", exchange.getError() instanceof org.apache.servicemix.jbi.FaultException);

        receiverComponent.getMessageList().assertMessagesReceived(0);
        
        errors.assertIsSatisfied();
    }

    public void testRobustInOnlyWithHandleFault() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(1);
        
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        RobustInOnly exchange = client.createRobustInOnlyExchange();
        exchange.setService(new QName("urn:test", "handle-fault"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        // fault is being handled as an exception but no exception handler configured for it
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
        assertTrue("A FaultException was expected", exchange.getError() instanceof FaultException);

        receiverComponent.getMessageList().assertMessagesReceived(0);
        
        errors.assertIsSatisfied();
    }

    public void testInOnlyAndRobustInOnlyWithErrorNotHandled() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(2);
        
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        for (MessageExchange exchange : createInOnlyAndRobustInOnly(client)) {
            exchange.setService(new QName("urn:test", "error-not-handled"));
            exchange.getMessage("in").setContent(new StringSource(MESSAGE));
            client.sendSync(exchange);
            assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
            assertTrue("A IllegalArgumentException was expected", exchange.getError() instanceof IllegalArgumentException);
        }
        errors.assertIsSatisfied();
    }

    public void testInOnlyAndRobustInOnlyWithErrorHandledFalse() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(0);
        
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        for (MessageExchange exchange : createInOnlyAndRobustInOnly(client)) {            
            exchange.setService(new QName("urn:test", "error-handled-false"));
            exchange.getMessage("in").setContent(new StringSource(MESSAGE));
            client.sendSync(exchange);
            assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
            assertTrue("A IllegalStateException was expected", exchange.getError() instanceof IllegalStateException);
        }
        receiverComponent.getMessageList().assertMessagesReceived(2);
        
        errors.assertIsSatisfied();
    }
    
    public void testInOnlyAndRobustInOnlyWithErrorHandledTrue() throws Exception {
        MockEndpoint errors = getMockEndpoint("mock:errors");
        errors.expectedMessageCount(0);
        
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        for (MessageExchange exchange : createInOnlyAndRobustInOnly(client)) {
            exchange.setService(new QName("urn:test", "error-handled-true"));
            exchange.getMessage("in").setContent(new StringSource(MESSAGE));
            client.sendSync(exchange);
            assertEquals(ExchangeStatus.DONE, exchange.getStatus());
        }
        receiverComponent.getMessageList().assertMessagesReceived(2);
        
        errors.assertIsSatisfied();
    }
    
    private MessageExchange[] createInOnlyAndRobustInOnly(ServiceMixClient client) throws MessagingException {
        return new MessageExchange[] {client.createInOnlyExchange(), client.createRobustInOnlyExchange()};
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                onException(IllegalStateException.class).handled(false).to("jbi:service:urn:test:receiver-service");
                onException(NullPointerException.class).handled(true).to("jbi:service:urn:test:receiver-service");
                onException().handled(false);
                errorHandler(deadLetterChannel("mock:errors").maximumRedeliveries(1).maximumRedeliveryDelay(300));
                from("jbi:service:urn:test:no-handle-fault").to("jbi:service:urn:test:faulty-service");
                from("jbi:service:urn:test:handle-fault").handleFault().to("jbi:service:urn:test:faulty-service");
                from("jbi:service:urn:test:error-not-handled").to("jbi:service:urn:test:iae-error-service");
                from("jbi:service:urn:test:error-handled-false").to("jbi:service:urn:test:ise-error-service");
                from("jbi:service:urn:test:error-handled-true").to("jbi:service:urn:test:npe-error-service");
            }
        };
    }
}
