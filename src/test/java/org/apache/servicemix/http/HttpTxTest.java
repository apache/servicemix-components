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
package org.apache.servicemix.http;

import java.net.URI;

import javax.jbi.messaging.InOut;
import javax.transaction.TransactionManager;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.activemq.broker.BrokerService;
import org.apache.geronimo.transaction.context.GeronimoTransactionManager;
import org.apache.geronimo.transaction.context.TransactionContextManager;
import org.apache.geronimo.transaction.manager.TransactionManagerImpl;
import org.apache.geronimo.transaction.manager.XidFactoryImpl;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.Destination;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.nmr.flow.Flow;
import org.apache.servicemix.jbi.nmr.flow.jca.JCAFlow;
import org.apache.servicemix.jbi.nmr.flow.seda.SedaFlow;
import org.apache.servicemix.tck.ExchangeCompletedListener;

public class HttpTxTest extends TestCase {

    private ExchangeCompletedListener listener;
    private JBIContainer jbi;
    private TransactionManager tm;
    private BrokerService broker;
    
    protected void setUp() throws Exception {
        broker = new BrokerService();
        broker.setUseJmx(false);
        broker.setPersistent(false);
        broker.addConnector("tcp://localhost:61616");
        broker.start();

        TransactionManagerImpl exTransactionManager = new TransactionManagerImpl(600, new XidFactoryImpl(), null, null);
        TransactionContextManager transactionContextManager = new TransactionContextManager(exTransactionManager, exTransactionManager);
        tm = (TransactionManager) new GeronimoTransactionManager(transactionContextManager);

        JCAFlow jcaFlow = new JCAFlow();
        jcaFlow.setTransactionContextManager(transactionContextManager);
        
        jbi = new JBIContainer();
        jbi.setFlows(new Flow[] { new SedaFlow(), jcaFlow });
        jbi.setEmbedded(true);
        jbi.setUseMBeanServer(false);
        jbi.setCreateMBeanServer(false);
        jbi.setTransactionManager(tm);
        jbi.setAutoEnlistInTransaction(true);
        listener = new ExchangeCompletedListener();
        jbi.addListener(listener);
        jbi.init();
        jbi.start();
    }
    
    protected void tearDown() throws Exception {
        listener.assertExchangeCompleted();
        jbi.shutDown();
        broker.stop();
    }
    
    public void testSync() throws Exception {
        EchoComponent echo = new EchoComponent();
        echo.setService(new QName("urn:test", "echo"));
        echo.setEndpoint("echo");
        jbi.activateComponent(echo, "echo");
        
        HttpEndpoint ep0 = new HttpEndpoint();
        ep0.setService(new QName("urn:test", "s0"));
        ep0.setEndpoint("ep0");
        ep0.setLocationURI("http://localhost:8192/ep1/");
        ep0.setRoleAsString("provider");
        ep0.setSoap(true);
        
        HttpEndpoint ep1 = new HttpEndpoint();
        ep1.setService(new QName("urn:test", "s1"));
        ep1.setEndpoint("ep1");
        ep1.setTargetService(new QName("urn:test", "echo"));
        ep1.setLocationURI("http://localhost:8192/ep1/");
        ep1.setRoleAsString("consumer");
        ep1.setDefaultMep(URI.create("http://www.w3.org/2004/08/wsdl/in-out"));
        ep1.setSoap(true);
        
        HttpSpringComponent http = new HttpSpringComponent();
        http.setEndpoints(new HttpEndpoint[] { ep0, ep1 });
        jbi.activateComponent(http, "http");

        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        Destination d = client.createDestination("service:urn:test:s0");
        InOut me = d.createInOutExchange();
        me.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        
        tm.begin();
        boolean ok = client.sendSync(me);
        assertTrue(ok);
        client.done(me);
        tm.commit();
    }
    
    public void testAsync() throws Exception {
        EchoComponent echo = new EchoComponent();
        echo.setService(new QName("urn:test", "echo"));
        echo.setEndpoint("echo");
        jbi.activateComponent(echo, "echo");
        
        HttpEndpoint ep0 = new HttpEndpoint();
        ep0.setService(new QName("urn:test", "s0"));
        ep0.setEndpoint("ep0");
        ep0.setLocationURI("http://localhost:8192/ep1/");
        ep0.setRoleAsString("provider");
        ep0.setSoap(true);
        
        HttpEndpoint ep1 = new HttpEndpoint();
        ep1.setService(new QName("urn:test", "s1"));
        ep1.setEndpoint("ep1");
        ep1.setTargetService(new QName("urn:test", "echo"));
        ep1.setLocationURI("http://localhost:8192/ep1/");
        ep1.setRoleAsString("consumer");
        ep1.setDefaultMep(URI.create("http://www.w3.org/2004/08/wsdl/in-out"));
        ep1.setSoap(true);
        
        HttpSpringComponent http = new HttpSpringComponent();
        http.setEndpoints(new HttpEndpoint[] { ep0, ep1 });
        jbi.activateComponent(http, "http");

        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        Destination d = client.createDestination("service:urn:test:s0");
        InOut me = d.createInOutExchange();
        me.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        
        tm.begin();
        client.send(me);
        tm.commit();
        me = (InOut) client.receive();
        client.done(me);
    }
    
}
