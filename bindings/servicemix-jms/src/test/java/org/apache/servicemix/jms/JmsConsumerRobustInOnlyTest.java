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
package org.apache.servicemix.jms;  

import java.util.LinkedList;
import java.util.List;

import javax.jbi.JBIException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.namespace.QName;

import org.apache.servicemix.MessageExchangeListener;
import org.apache.servicemix.components.util.ComponentSupport;
import org.apache.servicemix.jbi.event.ExchangeEvent;
import org.apache.servicemix.jbi.helper.MessageExchangePattern;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jms.endpoints.AbstractJmsMarshaler;
import org.apache.servicemix.jms.endpoints.DefaultConsumerMarshaler;
import org.apache.servicemix.jms.endpoints.JmsConsumerEndpoint;
import org.apache.servicemix.tck.ExchangeCompletedListener;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.listener.DefaultMessageListenerContainer;

public class JmsConsumerRobustInOnlyTest extends AbstractJmsTestSupport {

    private static final String FAULT_MESSAGE = "<fault>This is failing now!</fault>";
 
    protected SourceTransformer sourceTransformer = new SourceTransformer();
    private List<MessageExchange> exchanges = new LinkedList<MessageExchange>();

    private ExchangeCompletedListener listener;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        listener = new ExchangeCompletedListener(1000) {
            @Override
            public void exchangeSent(ExchangeEvent event) {
                super.exchangeSent(event);
                
                exchanges.add(event.getExchange());
            }
        };
        container.addListener(listener);
        
        createJmsConsumerEndpoint();
    }
    
    @Override
    protected void createJmsTemplate() throws Exception {
        super.createJmsTemplate();
        
        jmsTemplate.setReceiveTimeout(5000);
    }
    
    public void testRobustInOnlyDone() throws Exception {
        createReceiver(Result.done);
        
        sendJmsMessage();
                
        Message done = jmsTemplate.receive("reply");
        assertTrue(done.getBooleanProperty(AbstractJmsMarshaler.DONE_JMS_PROPERTY));
        
        listener.assertExchangeCompleted();
        
        MessageExchange exchange = (MessageExchange) exchanges.get(0);
        assertEquals(ExchangeStatus.DONE, exchange.getStatus());
    }
    
    public void testRobustInOnlyFault() throws Exception {
        // let's make the RobustInOnly end with a Fault
        createReceiver(Result.fault);
        
        sendJmsMessage();

        TextMessage fault = (TextMessage) jmsTemplate.receive("reply");
        assertEquals(FAULT_MESSAGE, fault.getText());
        
        listener.assertExchangeCompleted();
        
        MessageExchange exchange = (MessageExchange) exchanges.get(0);
        assertEquals(ExchangeStatus.DONE, exchange.getStatus());
    }
    
    public void testRobustInOnlyError() throws Exception {
        //let's make the RobustInOnly end with an error
        createReceiver(Result.error);
        
        sendJmsMessage();
        
        Message error = jmsTemplate.receive("reply");
        assertTrue(error.getBooleanProperty(AbstractJmsMarshaler.ERROR_JMS_PROPERTY));
        
        MessageExchange exchange = (MessageExchange) exchanges.get(0);
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
        
        listener.assertExchangeCompleted();
    }

    private void sendJmsMessage() {
        jmsTemplate.send("destination", new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                return session.createTextMessage("<hello>world</hello>");
            }
        });
    }

    private void createJmsConsumerEndpoint() throws JBIException {
        JmsComponent component = new JmsComponent();
        JmsConsumerEndpoint endpoint = new JmsConsumerEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setTransacted("jms");
        endpoint.setCacheLevel(DefaultMessageListenerContainer.CACHE_CONSUMER);
        DefaultConsumerMarshaler marshaler = new DefaultConsumerMarshaler();
        marshaler.setMep(MessageExchangePattern.ROBUST_IN_ONLY);
        endpoint.setMarshaler(marshaler);
        endpoint.setTargetService(new QName("receiver"));
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        endpoint.setReplyDestinationName("reply");
        component.setEndpoints(new JmsConsumerEndpoint[] {endpoint});
        container.activateComponent(component, "servicemix-jms");
    }
    
    /*
     * Creates the RobustInOnly receiver endpoint
     */
    private void createReceiver(Result result) throws JBIException {
        RobustInOnlyReceiver receiver = new RobustInOnlyReceiver();
        receiver.result = result;
        receiver.setService(new QName("receiver"));
        receiver.setEndpoint("endpoint");
        container.activateComponent(receiver, "receiver");
    }

    /*
     * Possible results for the RobustInOnlyReceiver
     */
    private enum Result { error, fault, done }
    
    // TODO: switch to new interface when moving to a new test container 
    private static final class RobustInOnlyReceiver extends ComponentSupport implements MessageExchangeListener {
        
        private Result result = Result.done;
                
        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                switch (result) {
                case error:
                    fail(exchange, new RuntimeException("Uhoh... this is going completely wrong!"));
                    break;
                case fault:
                    Fault fault = exchange.createFault();
                    fault.setContent(new StringSource(FAULT_MESSAGE));                
                    fail(exchange, fault);
                    break;
                default:
                    done(exchange);
                    break;
                }
            }
        }
    }
}
