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
package org.apache.servicemix.common;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessagingException;
import javax.jbi.component.Component;
import javax.xml.namespace.QName;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.nmr.flow.Flow;
import org.apache.servicemix.jbi.nmr.flow.seda.SedaFlow;
import org.apache.servicemix.jbi.util.MessageUtil;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.common.endpoints.SimpleEndpoint;
import org.apache.servicemix.common.endpoints.ConsumerEndpoint;
import org.apache.servicemix.common.endpoints.PollingEndpoint;
import org.apache.servicemix.tck.ExchangeCompletedListener;
import org.jencks.GeronimoPlatformTransactionManager;

import junit.framework.Assert;
import junit.framework.TestCase;

public class EndpointDeliveryChannelTest extends TestCase {

    
    protected JBIContainer jbi;
    protected TestComponent component;

    protected void setUp() throws Exception {
        jbi = new JBIContainer();
        jbi.setFlows(new Flow[] { new SedaFlow() });
        jbi.setEmbedded(true);
        jbi.setUseMBeanServer(false);
        jbi.setAutoEnlistInTransaction(true);
        jbi.setUseNewTransactionModel(true);
        jbi.init();
        jbi.start();
        component = new TestComponent();
        jbi.activateComponent(component, "test");
    }

    protected void tearDown() throws Exception {
        jbi.shutDown();
    }
    
    public void testNoRemainingExchangesAfterSendToUnknownEndpoint() throws Exception {
        try {
            component.consumer.sendExchange(new QName("urn:test", "non-existing-endpoint"));
            fail("Send Exchange should have thrown exception");
        } catch (MessagingException e) {
            assertTrue(component.getKnownExchanges(component.consumer).isEmpty());
        }
    }

    protected class TestComponent extends DefaultComponent {
        
        private TestConsumerEndpoint consumer;
        
        public TestComponent() throws Exception {
            super();
            consumer = new TestConsumerEndpoint();
            consumer.setService(new QName("consumer"));
            consumer.setEndpoint("endpoint");
            consumer.setTargetService(new QName("provider"));
            addEndpoint(consumer);
        }
        
        protected class TestConsumerEndpoint extends ConsumerEndpoint {
            public void process(MessageExchange exchange) throws Exception {
                Assert.fail("No exchange expected as sending should have failed");
            }

            public void sendExchange(QName service) throws MessagingException {
                InOnly exchange = getExchangeFactory().createInOnlyExchange();
                exchange.setService(service);
                getChannel().send(exchange);
            }
        }
    }
}
