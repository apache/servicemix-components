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

import junit.framework.TestCase;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.http.endpoints.DefaultHttpConsumerMarshaler;
import org.apache.servicemix.http.endpoints.HttpConsumerEndpoint;
import org.apache.servicemix.http.endpoints.HttpProviderEndpoint;
import org.apache.servicemix.http.endpoints.HttpSoapProviderEndpoint;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlets.ProxyServlet;
import org.springframework.core.io.ClassPathResource;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

public class ProviderEndpointTest extends TestCase {

    Integer port1 = Integer.parseInt(System.getProperty("http.port1", "61101"));
    Integer port2 = Integer.parseInt(System.getProperty("http.port2", "61102"));
    
    protected JBIContainer container;
    protected SourceTransformer transformer = new SourceTransformer();
    protected Server proxy;

    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        container.setEmbedded(true);
        container.init();
    }

    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
        if (proxy != null) {
            proxy.stop();
        }
    }

    public void testNonSoap() throws Exception {
        EchoComponent echo = new EchoComponent();
        echo.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        echo.setEndpoint("service");
        container.activateComponent(echo, "echo");

        HttpComponent http = new HttpComponent();

        HttpConsumerEndpoint ep0 = new HttpConsumerEndpoint();
        ep0.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep0.setEndpoint("consumer");
        ep0.setTargetService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep0.setTargetEndpoint("service");
        ep0.setLocationURI("http://localhost:"+port1+"/person/");

        HttpProviderEndpoint ep1 = new HttpProviderEndpoint();
        ep1.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep1.setEndpoint("provider");
        ep1.setLocationURI("http://localhost:"+port1+"/person/");

        http.setEndpoints(new HttpEndpointType[] {ep0, ep1 });
        container.activateComponent(http, "http");
        container.start();

        ServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        me.setOperation(new QName("http://servicemix.apache.org/samples/wsdl-first", "GetPerson"));
        me.getInMessage().setContent(new StringSource(
                                "<jbi:message xmlns:jbi=\"http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper\""
                             +  "             xmlns:msg=\"http://servicemix.apache.org/samples/wsdl-first/types\" "
                             +  "             name=\"Hello\" "
                             +  "             type=\"msg:HelloRequest\" "
                             +  "             version=\"1.0\">"
                             +  "  <jbi:part>"
                             +  "    <msg:GetPerson><msg:personId>id</msg:personId></msg:GetPerson>"
                             +  "  </jbi:part>"
                             +  "</jbi:message>"));
        client.sendSync(me);
        System.err.println(new SourceTransformer().contentToString(me.getOutMessage()));
        client.done(me);
    }

    public void testSoap() throws Exception {
        final AtomicReference<String> soapAction = new AtomicReference<String>();

        EchoComponent echo = new EchoComponent();
        echo.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "EchoService"));
        echo.setEndpoint("service");
        container.activateComponent(echo, "echo");
        
        HttpComponent http = new HttpComponent();
        
        HttpConsumerEndpoint ep0 = new HttpConsumerEndpoint();
        ep0.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep0.setEndpoint("consumer");
        ep0.setTargetService(new QName("http://servicemix.apache.org/samples/wsdl-first", "EchoService"));
        ep0.setTargetEndpoint("service");
        ep0.setLocationURI("http://localhost:"+port1+"/PersonService/");
        ep0.setMarshaler(new DefaultHttpConsumerMarshaler() {
            public MessageExchange createExchange(HttpServletRequest request, ComponentContext context) throws Exception {
                soapAction.set(request.getHeader("SOAPAction"));
                return super.createExchange(request, context);    //To change body of overridden methods use File | Settings | File Templates.
            }
        });

        HttpSoapProviderEndpoint ep1 = new HttpSoapProviderEndpoint();
        ep1.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep1.setEndpoint("soap");
        ep1.setWsdl(new ClassPathResource("person.wsdl"));
        ep1.setValidateWsdl(false); // TODO: Soap 1.2 not handled yet
        ep1.setUseJbiWrapper(true);
        
        http.setEndpoints(new HttpEndpointType[] {ep0, ep1 });
        container.activateComponent(http, "http");
        
        container.start();

        ServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        me.setOperation(new QName("http://servicemix.apache.org/samples/wsdl-first", "GetPerson"));
        me.getInMessage().setContent(new StringSource(
                                "<jbi:message xmlns:jbi=\"http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper\""
                             +  "             xmlns:msg=\"http://servicemix.apache.org/samples/wsdl-first/types\" "
                             +  "             name=\"Hello\" "
                             +  "             type=\"msg:HelloRequest\" "
                             +  "             version=\"1.0\">"
                             +  "  <jbi:part>"
                             +  "    <msg:GetPerson><msg:personId>id</msg:personId></msg:GetPerson>"
                             +  "  </jbi:part>"
                             +  "</jbi:message>"));
        client.sendSync(me);
        client.done(me);
        assertEquals("\"urn:myaction\"", soapAction.get());
    }
    
    public void testSendProblemWithoutServer() throws Exception {
        HttpComponent http = new HttpComponent();

        HttpSoapProviderEndpoint ep1 = new HttpSoapProviderEndpoint();
        ep1.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep1.setEndpoint("soap");
        ep1.setWsdl(new ClassPathResource("person.wsdl"));
        ep1.setValidateWsdl(false); // TODO: Soap 1.2 not handled yet
        ep1.setUseJbiWrapper(true);

        http.addEndpoint(ep1);
        container.activateComponent(http, "http");

        container.start();

        ServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        me.setOperation(new QName("http://servicemix.apache.org/samples/wsdl-first", "GetPerson"));
        me.getInMessage().setContent(new StringSource(
                                "<jbi:message xmlns:jbi=\"http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper\""
                             +  "             xmlns:msg=\"http://servicemix.apache.org/samples/wsdl-first/types\" "
                             +  "             name=\"Hello\" "
                             +  "             type=\"msg:HelloRequest\" "
                             +  "             version=\"1.0\">"
                             +  "  <jbi:part>"
                             +  "    <msg:GetPerson><msg:personId>id</msg:personId></msg:GetPerson>"
                             +  "  </jbi:part>"
                             +  "</jbi:message>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
    }

    public void testSendProblemWithServerDying() throws Exception {
        HttpComponent http = new HttpComponent();

        HttpSoapProviderEndpoint ep1 = new HttpSoapProviderEndpoint();
        ep1.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep1.setEndpoint("soap");
        ep1.setWsdl(new ClassPathResource("person.wsdl"));
        ep1.setValidateWsdl(false); // TODO: Soap 1.2 not handled yet
        ep1.setUseJbiWrapper(true);

        http.addEndpoint(ep1);
        container.activateComponent(http, "http");

        container.start();

        new Thread() {
            public void run() {
                ServerSocket ss = null;
                try {
                    ss = new ServerSocket(port1);
                    Socket s = ss.accept();
                    Thread.sleep(50);
                    s.close();
                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    try {
                        ss.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();

        ServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        me.setOperation(new QName("http://servicemix.apache.org/samples/wsdl-first", "GetPerson"));
        me.getInMessage().setContent(new StringSource(
                                "<jbi:message xmlns:jbi=\"http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper\""
                             +  "             xmlns:msg=\"http://servicemix.apache.org/samples/wsdl-first/types\" "
                             +  "             name=\"Hello\" "
                             +  "             type=\"msg:HelloRequest\" "
                             +  "             version=\"1.0\">"
                             +  "  <jbi:part>"
                             +  "    <msg:GetPerson><msg:personId>id</msg:personId></msg:GetPerson>"
                             +  "  </jbi:part>"
                             +  "</jbi:message>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
    }

    public void testSendProblemWith404Html() throws Exception {
        HttpComponent http = new HttpComponent();

        HttpConsumerEndpoint ep0 = new HttpConsumerEndpoint();
        ep0.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep0.setEndpoint("consumer");
        ep0.setTargetService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep0.setTargetEndpoint("service");
        ep0.setLocationURI("http://localhost:"+port1+"/ps/");

        HttpSoapProviderEndpoint ep1 = new HttpSoapProviderEndpoint();
        ep1.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep1.setEndpoint("soap");
        ep1.setWsdl(new ClassPathResource("person.wsdl"));
        ep1.setValidateWsdl(false); // TODO: Soap 1.2 not handled yet
        ep1.setUseJbiWrapper(true);

        http.addEndpoint(ep0);
        http.addEndpoint(ep1);
        container.activateComponent(http, "http");

        container.start();

        ServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        me.setOperation(new QName("http://servicemix.apache.org/samples/wsdl-first", "GetPerson"));
        me.getInMessage().setContent(new StringSource(
                                "<jbi:message xmlns:jbi=\"http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper\""
                             +  "             xmlns:msg=\"http://servicemix.apache.org/samples/wsdl-first/types\" "
                             +  "             name=\"Hello\" "
                             +  "             type=\"msg:HelloRequest\" "
                             +  "             version=\"1.0\">"
                             +  "  <jbi:part>"
                             +  "    <msg:GetPerson><msg:personId>id</msg:personId></msg:GetPerson>"
                             +  "  </jbi:part>"
                             +  "</jbi:message>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
    }

    public void testProxy() throws Exception {
        EchoComponent echo = new EchoComponent();
        echo.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        echo.setEndpoint("service");
        container.activateComponent(echo, "echo");

        HttpComponent http = new HttpComponent();

        HttpConsumerEndpoint ep0 = new HttpConsumerEndpoint();
        ep0.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep0.setEndpoint("consumer");
        ep0.setTargetService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep0.setTargetEndpoint("service");
        ep0.setLocationURI("http://localhost:"+port1+"/person/");

        HttpProviderEndpoint ep1 = new HttpProviderEndpoint();
        ep1.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep1.setEndpoint("provider");
        ep1.setLocationURI("http://localhost:"+port1+"/person/");
        ep1.setProxyHost("localhost");
        ep1.setProxyPort(port2);

        http.setEndpoints(new HttpEndpointType[] {ep0, ep1 });
        container.activateComponent(http, "http");
        container.start();

        proxy = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setHost("localhost");
        connector.setPort(port2);
        proxy.addConnector(connector);
        ServletHandler handler = new ServletHandler();
        handler.addServletWithMapping(ProxyServlet.class, "/");
        proxy.setHandler(handler);
        proxy.start();

        ServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        me.setOperation(new QName("http://servicemix.apache.org/samples/wsdl-first", "GetPerson"));
        me.getInMessage().setContent(new StringSource(
                                "<jbi:message xmlns:jbi=\"http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper\""
                             +  "             xmlns:msg=\"http://servicemix.apache.org/samples/wsdl-first/types\" "
                             +  "             name=\"Hello\" "
                             +  "             type=\"msg:HelloRequest\" "
                             +  "             version=\"1.0\">"
                             +  "  <jbi:part>"
                             +  "    <msg:GetPerson><msg:personId>id</msg:personId></msg:GetPerson>"
                             +  "  </jbi:part>"
                             +  "</jbi:message>"));
        client.sendSync(me);
        System.err.println(new SourceTransformer().contentToString(me.getOutMessage()));
        client.done(me);
    }

    public void testGzipEncodingNonSoap() throws Exception {
        EchoComponent echo = new EchoComponent();
        echo.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "EchoService"));
        echo.setEndpoint("service");
        container.activateComponent(echo, "echo");

        HttpComponent http = new HttpComponent();

        HttpConsumerEndpoint ep0 = new HttpConsumerEndpoint();
        ep0.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep0.setEndpoint("consumer");
        ep0.setTargetService(new QName("http://servicemix.apache.org/samples/wsdl-first", "EchoService"));
        ep0.setTargetEndpoint("service");
        ep0.setLocationURI("http://localhost:"+port1+"/person/");

        HttpProviderEndpoint ep1 = new HttpProviderEndpoint();
        ep1.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep1.setEndpoint("provider");
        ep1.setLocationURI("http://localhost:"+port1+"/person/");
        ep1.setGzipRequest(true);
        ep1.setExpectGzippedResponse(true);

        http.setEndpoints(new HttpEndpointType[] {ep0, ep1});
        container.activateComponent(http, "http");
        container.start();

        ServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        me.setOperation(new QName("http://servicemix.apache.org/samples/wsdl-first", "GetPerson"));
        me.getInMessage().setContent(new StringSource(
            "<jbi:message xmlns:jbi=\"http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper\""
                + "             xmlns:msg=\"http://servicemix.apache.org/samples/wsdl-first/types\" "
                + "             name=\"Hello\" "
                + "             type=\"msg:HelloRequest\" "
                + "             version=\"1.0\">"
                + "  <jbi:part>"
                + "    <msg:GetPerson><msg:personId>id</msg:personId></msg:GetPerson>"
                + "  </jbi:part>"
                + "</jbi:message>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        System.err.println(new SourceTransformer().contentToString(me.getOutMessage()));
        client.done(me);
    }

    public void testGzipEncodingSoap() throws Exception {
        EchoComponent echo = new EchoComponent();
        echo.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "EchoService"));
        echo.setEndpoint("service");
        container.activateComponent(echo, "echo");

        HttpComponent http = new HttpComponent();

        HttpConsumerEndpoint ep0 = new HttpConsumerEndpoint();
        ep0.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep0.setEndpoint("consumer");
        ep0.setTargetService(new QName("http://servicemix.apache.org/samples/wsdl-first", "EchoService"));
        ep0.setTargetEndpoint("service");
        ep0.setLocationURI("http://localhost:"+port1+"/PersonService/");

        HttpSoapProviderEndpoint ep1 = new HttpSoapProviderEndpoint();
        ep1.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep1.setEndpoint("soap");
        ep1.setWsdl(new ClassPathResource("person.wsdl"));
        ep1.setValidateWsdl(false); // TODO: Soap 1.2 not handled yet
        ep1.setUseJbiWrapper(true);
        ep1.setGzipRequest(true);
        ep1.setExpectGzippedResponse(true);

        http.setEndpoints(new HttpEndpointType[] {ep0, ep1});
        container.activateComponent(http, "http");

        container.start();

        ServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        me.setOperation(new QName("http://servicemix.apache.org/samples/wsdl-first", "GetPerson"));
        me.getInMessage().setContent(new StringSource(
                                "<jbi:message xmlns:jbi=\"http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper\""
                             +  "             xmlns:msg=\"http://servicemix.apache.org/samples/wsdl-first/types\" "
                             +  "             name=\"Hello\" "
                             +  "             type=\"msg:HelloRequest\" "
                             +  "             version=\"1.0\">"
                             +  "  <jbi:part>"
                             +  "    <msg:GetPerson><msg:personId>id</msg:personId></msg:GetPerson>"
                             +  "  </jbi:part>"
                             +  "</jbi:message>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        System.err.println(new SourceTransformer().contentToString(me.getOutMessage()));
        client.done(me);
    }

}
