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

import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.http.endpoints.HttpConsumerEndpoint;
import org.apache.servicemix.http.endpoints.HttpProviderEndpoint;
import org.apache.servicemix.http.endpoints.HttpSoapProviderEndpoint;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.springframework.core.io.ClassPathResource;

public class ProviderEndpointTest extends TestCase {

    protected JBIContainer container;
    protected SourceTransformer transformer = new SourceTransformer();

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
        ep0.setLocationURI("http://localhost:8192/person/");

        HttpProviderEndpoint ep1 = new HttpProviderEndpoint();
        ep1.setService(new QName("http://servicemix.apache.org/samples/wsdl-first", "PersonService"));
        ep1.setEndpoint("provider");
        ep1.setLocationURI("http://localhost:8192/person/");

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
    }

    public void testSoap() throws Exception {
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
        ep0.setLocationURI("http://localhost:8192/PersonService/");

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
    }
}
