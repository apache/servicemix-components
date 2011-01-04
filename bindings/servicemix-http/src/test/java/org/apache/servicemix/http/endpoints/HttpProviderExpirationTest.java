/*
 *  Copyright 2010 iocanel.
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.apache.servicemix.http.endpoints;

import java.net.URI;

import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.activemq.broker.BrokerService;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.Destination;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.http.HttpComponent;
import org.apache.servicemix.http.HttpEndpointType;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.nmr.flow.Flow;
import org.apache.servicemix.jbi.nmr.flow.seda.SedaFlow;

/**
 *
 * @author ioannis canellos
 */
public class HttpProviderExpirationTest extends TestCase {

    String activemq = System.getProperty("activemq.port");
    String port1 = System.getProperty("http.port1");
    
    private JBIContainer jbi;
    private BrokerService broker;

    protected void setUp() throws Exception {
        broker = new BrokerService();
        broker.setUseJmx(false);
        broker.setPersistent(false);
        broker.addConnector("tcp://localhost:"+activemq);
        broker.start();


        jbi = new JBIContainer();
        jbi.setFlows(new Flow[]{new SedaFlow()});
        jbi.setEmbedded(true);
        jbi.setUseMBeanServer(false);
        jbi.setCreateMBeanServer(false);
        jbi.setAutoEnlistInTransaction(true);
        jbi.init();
        jbi.start();
    }

    protected void tearDown() throws Exception {
        jbi.shutDown();
        broker.stop();
    }

    public void testExpirationOnComponent() throws Exception {
        EchoComponent echo = new EchoComponent();
        echo.setService(new QName("urn:test", "echo"));
        echo.setEndpoint("echo");
        jbi.activateComponent(echo, "echo");

        HttpProviderEndpoint provider = new HttpProviderEndpoint();
        provider.setService(new QName("urn:test", "provider"));
        provider.setEndpoint("provider");
        provider.setLocationURI("http://localhost:"+port1+"/expiration/");
        provider.setProviderExpirationTime(100000);

        HttpConsumerEndpoint consumer = new HttpConsumerEndpoint();
        consumer.setService(new QName("urn:test", "consumer"));
        consumer.setEndpoint("consumer");
        consumer.setTargetService(new QName("urn:test", "echo"));
        consumer.setLocationURI("http://localhost:"+port1+"/expiration/");
        consumer.setDefaultMep(URI.create("http://www.w3.org/2004/08/wsdl/in-out"));
        consumer.setMarshaler(new DefaultHttpConsumerMarshaler() {

            @Override
            public void sendOut(MessageExchange exchange, NormalizedMessage outMsg, HttpServletRequest request, HttpServletResponse response) throws Exception {
                Thread.sleep(10000);
                super.sendOut(exchange, outMsg, request, response);
            }
        });


        HttpComponent http = new HttpComponent();
        http.getConfiguration().setProviderExpirationTime(10);
        http.setEndpoints(new HttpEndpointType[]{provider, consumer});
        jbi.activateComponent(http, "http");

        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        Destination d = client.createDestination("service:urn:test:provider");
        InOut me = d.createInOutExchange();
        me.getInMessage().setContent(new StringSource("<hello>world</hello>"));

        boolean ok = client.sendSync(me);
        Exception exception = me.getError();
        assertTrue(exception != null);
    }

    public void testExpirationOnProvider() throws Exception {
        EchoComponent echo = new EchoComponent();
        echo.setService(new QName("urn:test", "echo"));
        echo.setEndpoint("echo");
        jbi.activateComponent(echo, "echo");

        HttpProviderEndpoint provider = new HttpProviderEndpoint();
        provider.setService(new QName("urn:test", "provider"));
        provider.setEndpoint("provider");
        provider.setLocationURI("http://localhost:"+port1+"/expiration/");
        provider.setProviderExpirationTime(10);

        HttpConsumerEndpoint consumer = new HttpConsumerEndpoint();
        consumer.setService(new QName("urn:test", "consumer"));
        consumer.setEndpoint("consumer");
        consumer.setTargetService(new QName("urn:test", "echo"));
        consumer.setLocationURI("http://localhost:"+port1+"/expiration/");
        consumer.setDefaultMep(URI.create("http://www.w3.org/2004/08/wsdl/in-out"));
        consumer.setMarshaler(new DefaultHttpConsumerMarshaler() {

            @Override
            public void sendOut(MessageExchange exchange, NormalizedMessage outMsg, HttpServletRequest request, HttpServletResponse response) throws Exception {
                Thread.sleep(10000);
                super.sendOut(exchange, outMsg, request, response);
            }
        });


        HttpComponent http = new HttpComponent();
        //To avoid adding delaying mechanisms we are using an extremly low expiration time.
        http.getConfiguration().setProviderExpirationTime(300000);
        http.getConfiguration().setJettyClientPerProvider(true);
        http.setEndpoints(new HttpEndpointType[]{provider, consumer});
        jbi.activateComponent(http, "http");

        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        Destination d = client.createDestination("service:urn:test:provider");
        InOut me = d.createInOutExchange();
        me.getInMessage().setContent(new StringSource("<hello>world</hello>"));

        boolean ok = client.sendSync(me);
        Exception exception = me.getError();
        assertTrue(exception != null);
    }

    public void testNoExpiration() throws Exception {
        EchoComponent echo = new EchoComponent();
        echo.setService(new QName("urn:test", "echo"));
        echo.setEndpoint("echo");
        jbi.activateComponent(echo, "echo");

        HttpProviderEndpoint provider = new HttpProviderEndpoint();
        provider.setService(new QName("urn:test", "provider"));
        provider.setEndpoint("provider");
        provider.setLocationURI("http://localhost:"+port1+"/expiration/");
        provider.setProviderExpirationTime(300000);

        HttpConsumerEndpoint consumer = new HttpConsumerEndpoint();
        consumer.setService(new QName("urn:test", "consumer"));
        consumer.setEndpoint("consumer");
        consumer.setTargetService(new QName("urn:test", "echo"));
        consumer.setLocationURI("http://localhost:"+port1+"/expiration/");
        consumer.setDefaultMep(URI.create("http://www.w3.org/2004/08/wsdl/in-out"));
        consumer.setMarshaler(new DefaultHttpConsumerMarshaler() {

            @Override
            public void sendOut(MessageExchange exchange, NormalizedMessage outMsg, HttpServletRequest request, HttpServletResponse response) throws Exception {
                Thread.sleep(10000);
                super.sendOut(exchange, outMsg, request, response);
            }
        });


        HttpComponent http = new HttpComponent();
        //To avoid adding delaying mechanisms we are using an extremly low expiration time.
        http.getConfiguration().setProviderExpirationTime(300000);
        http.getConfiguration().setJettyClientPerProvider(true);
        http.setEndpoints(new HttpEndpointType[]{provider, consumer});
        jbi.activateComponent(http, "http");

        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        Destination d = client.createDestination("service:urn:test:provider");
        InOut me = d.createInOutExchange();
        me.getInMessage().setContent(new StringSource("<hello>world</hello>"));

        boolean ok = client.sendSync(me);
        Exception exception = me.getError();
        assertTrue(exception == null);
        client.done(me);
    }
}
