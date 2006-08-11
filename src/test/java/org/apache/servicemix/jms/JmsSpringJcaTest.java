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

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.naming.Context;
import javax.transaction.TransactionManager;
import javax.xml.namespace.QName;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.Destination;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.ExchangeCompletedListener;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.apache.xbean.spring.jndi.SpringInitialContextFactory;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class JmsSpringJcaTest extends SpringTestSupport {

    protected ExchangeCompletedListener listener;
    protected DefaultServiceMixClient client;
    protected Receiver receiver;
    
    protected void setUp() throws Exception {
        super.setUp();
        listener = new ExchangeCompletedListener(5000);
        jbi.addListener(listener);
        client = new DefaultServiceMixClient(jbi);
        receiver = (Receiver) getBean("receiver");
    }
    
    protected void tearDown() throws Exception {
        listener.assertExchangeCompleted();
        super.tearDown();
    }
    
    public void testInOut() throws Exception {
        TransactionManager tm = (TransactionManager) getBean("transactionManager");
        tm.begin();
        InOut me = client.createInOutExchange();
        me.setService(new QName("http://test", "MyProviderService"));
        me.getInMessage().setContent(new StringSource("<echo xmlns='http://test'><echoin0>world</echoin0></echo>"));
        client.send(me);
        tm.commit();
        me = (InOut) client.receive();
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        assertNotNull(me.getError());
        assertTrue(me.getError() instanceof UnsupportedOperationException);
    }

    public void testInOnlyWithAsyncConsumer() throws Exception {
        TransactionManager tm = (TransactionManager) getBean("transactionManager");
        tm.begin();
        Destination dest = client.createDestination("endpoint:http://test/MyProviderService/async");
        InOnly me = dest.createInOnlyExchange();
        me.getInMessage().setContent(new StringSource("<echo xmlns='http://test'><echoin0>world</echoin0></echo>"));
        client.send(me);
        tm.commit();
        me = (InOnly) client.receive();
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        receiver.getMessageList().assertMessagesReceived(1);
    }

    public void testInOnlySyncWithAsyncConsumer() throws Exception {
        TransactionManager tm = (TransactionManager) getBean("transactionManager");
        tm.begin();
        Destination dest = client.createDestination("endpoint:http://test/MyProviderService/async");
        InOnly me = dest.createInOnlyExchange();
        me.getInMessage().setContent(new StringSource("<echo xmlns='http://test'><echoin0>world</echoin0></echo>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        tm.commit();
        receiver.getMessageList().assertMessagesReceived(1);
    }

    public void testInOnlyWithSyncConsumer() throws Exception {
        TransactionManager tm = (TransactionManager) getBean("transactionManager");
        tm.begin();
        Destination dest = client.createDestination("endpoint:http://test/MyProviderService/synchronous");
        InOnly me = dest.createInOnlyExchange();
        me.getInMessage().setContent(new StringSource("<echo xmlns='http://test'><echoin0>world</echoin0></echo>"));
        client.send(me);
        tm.commit();
        me = (InOnly) client.receive();
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        receiver.getMessageList().assertMessagesReceived(1);
    }

    public void testInOnlySyncWithSyncConsumer() throws Exception {
        TransactionManager tm = (TransactionManager) getBean("transactionManager");
        tm.begin();
        Destination dest = client.createDestination("endpoint:http://test/MyProviderService/synchronous");
        InOnly me = dest.createInOnlyExchange();
        me.getInMessage().setContent(new StringSource("<echo xmlns='http://test'><echoin0>world</echoin0></echo>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        tm.commit();
        receiver.getMessageList().assertMessagesReceived(1);
    }

    protected AbstractXmlApplicationContext createBeanFactory() {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, SpringInitialContextFactory.class.getName());
        System.setProperty(Context.PROVIDER_URL, "org/apache/servicemix/jms/jndi.xml");
        return new ClassPathXmlApplicationContext("org/apache/servicemix/jms/spring-jca.xml");
    }

}
