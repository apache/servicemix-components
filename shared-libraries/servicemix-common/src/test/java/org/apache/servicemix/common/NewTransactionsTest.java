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
import junit.framework.TestCase;

public class NewTransactionsTest extends TestCase {

    /*
    protected JBIContainer jbi;
    protected TransactionManager txManager;
    protected TestComponent component;
    protected ServiceMixClient client;
    protected Exception exceptionToThrow;
    protected boolean exceptionShouldRollback;
    protected boolean useInOut;
    protected ExchangeCompletedListener listener;

    protected void setUp() throws Exception {
        exceptionToThrow = null;
        exceptionShouldRollback = false;

        txManager = new GeronimoPlatformTransactionManager();

        listener = new ExchangeCompletedListener();
        jbi = new JBIContainer();
        jbi.setFlows(new Flow[] { new SedaFlow() });
        jbi.setEmbedded(true);
        jbi.setUseMBeanServer(false);
        jbi.setTransactionManager(txManager);
        jbi.setAutoEnlistInTransaction(true);
        jbi.setUseNewTransactionModel(true);
        jbi.addListener(listener);
        jbi.init();
        jbi.start();
        component = new TestComponent();
        jbi.activateComponent(component, "test");
        client = new DefaultServiceMixClient(jbi);
    }

    protected void tearDown() throws Exception {
        jbi.shutDown();
    }

    public void testTxOkAsync() throws Exception {
        component.addProvider();
        txManager.begin();
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("provider"));
        me.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        assertEquals(Status.STATUS_ACTIVE, txManager.getStatus());
        me.setProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME, txManager.suspend());
        assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        client.send(me);
        me = (InOnly) client.receive(1000);
        assertNotNull(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        txManager.resume((Transaction) me.getProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME));
        assertEquals(Status.STATUS_ACTIVE, txManager.getStatus());
        txManager.commit();
    }

    public void testTxOkSync() throws Exception {
        component.addProvider();
        txManager.begin();
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("provider"));
        me.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        assertEquals(Status.STATUS_ACTIVE, txManager.getStatus());
        me.setProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME, txManager.suspend());
        assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        boolean ok = client.sendSync(me, 1000);
        assertTrue(ok);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        txManager.resume((Transaction) me.getProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME));
        assertEquals(Status.STATUS_ACTIVE, txManager.getStatus());
        txManager.commit();
    }

    public void testTxExceptionAsync() throws Exception {
        component.addProvider();
        exceptionToThrow = new Exception("Business exception");
        txManager.begin();
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("provider"));
        me.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        assertEquals(Status.STATUS_ACTIVE, txManager.getStatus());
        me.setProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME, txManager.suspend());
        assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        client.send(me);
            me = (InOnly) client.receive(1000);
        assertNotNull(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        txManager.resume((Transaction) me.getProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME));
        assertEquals(Status.STATUS_ACTIVE, txManager.getStatus());
        txManager.commit();
    }

    public void testTxExceptionSync() throws Exception {
        component.addProvider();
        exceptionToThrow = new Exception("Business exception");
        txManager.begin();
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("provider"));
        me.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        assertEquals(Status.STATUS_ACTIVE, txManager.getStatus());
        me.setProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME, txManager.suspend());
        assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        boolean ok = client.sendSync(me, 1000);
        assertTrue(ok);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        txManager.resume((Transaction) me.getProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME));
        assertEquals(Status.STATUS_ACTIVE, txManager.getStatus());
        txManager.commit();
    }

    public void testTxExceptionRollbackAsync() throws Exception {
        component.addProvider();
        exceptionToThrow = new Exception("Business exception");
        exceptionShouldRollback = true;
        txManager.begin();
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("provider"));
        me.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        assertEquals(Status.STATUS_ACTIVE, txManager.getStatus());
        me.setProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME, txManager.suspend());
        assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        client.send(me);
        me = (InOnly) client.receive(10000);
        assertNotNull(me);
        assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        txManager.resume((Transaction) me.getProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME));
        assertEquals(Status.STATUS_MARKED_ROLLBACK, txManager.getStatus());
        txManager.rollback();
    }

    public void testTxExceptionRollbackSync() throws Exception {
        component.addProvider();
        exceptionToThrow = new RuntimeException("Runtime exception");
        exceptionShouldRollback = true;
        txManager.begin();
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("provider"));
        me.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        assertEquals(Status.STATUS_ACTIVE, txManager.getStatus());
        me.setProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME, txManager.suspend());
        assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        boolean ok = client.sendSync(me, 1000);
        assertTrue(ok);
        assertEquals(Status.STATUS_NO_TRANSACTION, txManager.getStatus());
        txManager.resume((Transaction) me.getProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME));
        assertEquals(Status.STATUS_MARKED_ROLLBACK, txManager.getStatus());
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        txManager.rollback();
    }

    public void testExceptionWithConsumerInOnly() throws Exception {
        useInOut = false;
        component.addProvider();
        component.addConsumer();
        assertTrue(component.commited.await(1, TimeUnit.SECONDS));
        listener.assertExchangeCompleted();
    }

    public void testExceptionWithConsumerInOut() throws Exception {
        useInOut = true;
        component.addProvider();
        component.addConsumer();
        assertTrue(component.commited.await(1, TimeUnit.SECONDS));
        listener.assertExchangeCompleted();
    }

    protected class TestComponent extends DefaultComponent {
        public CountDownLatch commited = new CountDownLatch(1);
        public TestComponent() {
            super();
        }
        public void addProvider() throws Exception {
            TestProviderEndpoint ep = new TestProviderEndpoint();
            ep.setService(new QName("provider"));
            ep.setEndpoint("endpoint");
            addEndpoint(ep);
        }
        public void addConsumer() throws Exception {
            TestConsumerEndpoint ep = new TestConsumerEndpoint();
            ep.setService(new QName("consumer"));
            ep.setEndpoint("endpoint");
            ep.setTargetService(new QName("provider"));
            addEndpoint(ep);
        }
        protected boolean exceptionShouldRollbackTx(Exception e) {
            return exceptionShouldRollback;
        }
        protected class TestProviderEndpoint extends ProviderEndpoint {
            public void process(MessageExchange exchange) throws Exception {
                if (exchange.getStatus() != ExchangeStatus.ACTIVE) {
                    return;
                }
                if (exceptionToThrow != null) {
                    throw exceptionToThrow;
                }
                if (exchange instanceof InOut) {
                    MessageUtil.transferInToOut(exchange, exchange);
                } else {
                    exchange.setStatus(ExchangeStatus.DONE);
                }
                send(exchange);
            }
        }
        protected class TestConsumerEndpoint extends ConsumerEndpoint {
            public void process(MessageExchange exchange) throws Exception {
                if (exceptionToThrow != null) {
                    throw exceptionToThrow;
                }
                if (exchange instanceof InOut) {
                    done(exchange);
                }
                txManager.commit();
                commited.countDown();
            }
            public void start() throws Exception {
                super.start();
                txManager.begin();
                if (useInOut) {
                    InOut exchange = channel.createExchangeFactory().createInOutExchange();
                    exchange.setInMessage(exchange.createMessage());
                    exchange.getInMessage().setContent(new StringSource("<hello>world</hello>"));
                    configureExchangeTarget(exchange);
                    send(exchange);
                } else {
                    InOnly exchange = channel.createExchangeFactory().createInOnlyExchange();
                    exchange.setInMessage(exchange.createMessage());
                    exchange.getInMessage().setContent(new StringSource("<hello>world</hello>"));
                    configureExchangeTarget(exchange);
                    send(exchange);
                }
            }
        }
    }
    */

}
