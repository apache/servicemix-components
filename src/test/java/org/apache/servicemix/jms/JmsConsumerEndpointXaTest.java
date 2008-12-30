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

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.xml.namespace.QName;

import org.apache.servicemix.jbi.messaging.MessageExchangeImpl;
import org.apache.servicemix.jms.endpoints.JmsConsumerEndpoint;
import org.apache.servicemix.tck.ReceiverComponent;
import org.jencks.amqpool.XaPooledConnectionFactory;

public class JmsConsumerEndpointXaTest extends AbstractJmsTestSupport {

    private XaPooledConnectionFactory xaConnectionFactory;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        xaConnectionFactory = new XaPooledConnectionFactory("vm://localhost");
        xaConnectionFactory.setTransactionManager((TransactionManager)container.getTransactionManager());

        container.setAutoEnlistInTransaction(true);
    }
    
    public void testConsumerCommitXaTx() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsConsumerEndpoint endpoint = new JmsConsumerEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setTargetService(new QName("error"));
        endpoint.setListenerType("default");
        endpoint.setConnectionFactory(xaConnectionFactory);
        endpoint.setDestinationName("destination");
        endpoint.setTransacted("xa");
        component.setEndpoints(new JmsConsumerEndpoint[] {endpoint});
        container.activateComponent(component, "servicemix-jms");

        ReceiverComponent errors = new ReceiverComponent() {

            @Override
            public void onMessageExchange(MessageExchange exchange) throws MessagingException {
                assertTransaction(exchange, Status.STATUS_COMMITTED);
                super.onMessageExchange(exchange);
            }
        };
        errors.setService(new QName("error"));
        errors.setEndpoint("endpoint");
        container.activateComponent(errors, "errors");

        jmsTemplate.convertAndSend("destination", "<hello>world</hello>");
        errors.getMessageList().assertMessagesReceived(1);
        Thread.sleep(500);
    }

    public void testConsumerRollbackXaTx() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsConsumerEndpoint endpoint = new JmsConsumerEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setTargetService(new QName("error"));
        endpoint.setListenerType("default");
        endpoint.setConnectionFactory(xaConnectionFactory);
        endpoint.setDestinationName("destination");
        endpoint.setTransacted("xa");
        component.setEndpoints(new JmsConsumerEndpoint[] {endpoint});
        container.activateComponent(component, "servicemix-jms");

        ReceiverComponent errors = new ReceiverComponent() {

            @Override
            public void onMessageExchange(MessageExchange exchange) throws MessagingException {
                assertTransaction(exchange, Status.STATUS_ROLLEDBACK);
                
                getMessageList().addMessage(exchange.getMessage("in"));
                exchange.setError(new RuntimeException("Could you please rollback?"));
                send(exchange);
            }
        };
        errors.setService(new QName("error"));
        errors.setEndpoint("endpoint");
        container.activateComponent(errors, "errors");

        jmsTemplate.convertAndSend("destination", "<hello>world</hello>");
        errors.getMessageList().assertMessagesReceived(1);
        Thread.sleep(500);
    }
    
    private void assertTransaction(MessageExchange exchange, final int expected) {
        assertTrue(exchange.isTransacted());
        try {
            ((MessageExchangeImpl) exchange).getTransactionContext().registerSynchronization(new Synchronization() {
                public void afterCompletion(int status) {
                    assertEquals(expected, status);
                }

                public void beforeCompletion() {
                }                
            });
        } catch (IllegalStateException e) {
            fail(e.getMessage());
        } catch (RollbackException e) {
            fail(e.getMessage());
        } catch (SystemException e) {
            fail(e.getMessage());
        }
    }
}
