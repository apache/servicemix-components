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
package org.apache.servicemix.cxfbc;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.servicemix.components.util.MockServiceComponent;
import org.apache.servicemix.cxfse.CxfSeComponent;
import org.apache.servicemix.cxfse.CxfSeEndpoint;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.util.FileUtil;
import org.springframework.core.io.ClassPathResource;

import uri.helloworld.HelloHeader;
import uri.helloworld.HelloPortType;
import uri.helloworld.HelloRequest;
import uri.helloworld.HelloResponse;
import uri.helloworld.HelloService;

public class CxfBcComponentTest extends TestCase {

    static final Logger LOG = LogUtils.getL7dLogger(CxfBcComponentTest.class);

    private final QName serviceName = new QName("uri:HelloWorld",
            "HelloService");

    private JBIContainer jbi;

    protected void setUp() throws Exception {
        jbi = new JBIContainer();
        jbi.setEmbedded(true);
        jbi.init();
        jbi.start();

    }

    protected void tearDown() throws Exception {
        jbi.shutDown();
    }

    public void testEndpointDOC() throws Exception {
        CxfBcComponent comp = new CxfBcComponent();
        CxfBcConsumer ep = new CxfBcConsumer();
        ep.setWsdl(new ClassPathResource("HelloWorld-DOC.wsdl"));
        ep.setTargetService(new QName("urn:test", "target"));
        comp.setEndpoints(new CxfBcEndpointType[] {ep});
        jbi.activateComponent(comp, "servicemix-cxfbc");

        MockServiceComponent echo = new MockServiceComponent();
        echo.setService(new QName("urn:test", "target"));
        echo.setEndpoint("endpoint");
        echo.setResponseXml(
                "<jbi:message xmlns:jbi='http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper'>" 
                + "<jbi:part><HelloResponse xmlns='uri:HelloWorld'><text>hello</text></HelloResponse></jbi:part></jbi:message>"
        );
        jbi.activateComponent(echo, "echo");

        URLConnection connection = new URL("http://localhost:8080/hello")
                .openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        // Post the request file.
        InputStream fis = new ClassPathResource("HelloWorld-DOC-Input.xml")
                .getInputStream();
        FileUtil.copyInputStream(fis, os);
        // Read the response.
        InputStream is = connection.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtil.copyInputStream(is, baos);
        System.err.println(baos.toString());

        Thread.sleep(100);
    }
    
    public void testListService() throws Exception {
        CxfBcComponent comp = new CxfBcComponent();
        CxfBcConsumer ep = new CxfBcConsumer();
        ep.setWsdl(new ClassPathResource("HelloWorld-DOC.wsdl"));
        ep.setTargetService(new QName("urn:test", "target"));
        comp.setEndpoints(new CxfBcEndpointType[] {ep});
        jbi.activateComponent(comp, "servicemix-cxfbc");

        MockServiceComponent echo = new MockServiceComponent();
        echo.setService(new QName("urn:test", "target"));
        echo.setEndpoint("endpoint");
        echo.setResponseXml("<jbi:message xmlns:jbi='http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper'><jbi:part>" 
                + "<ns2:HelloResponse xmlns:ns2='uri:HelloWorld'><text>helloffang</text></ns2:HelloResponse></jbi:part></jbi:message>");
        jbi.activateComponent(echo, "echo");
        URL wsdl = getClass().getResource("/HelloWorld-DOC.wsdl");
        assertNotNull(wsdl);
        GetMethod getMethod = new GetMethod("http://localhost:8080");
        HttpClient httpClient = new HttpClient();
        httpClient.executeMethod(getMethod);
        String response = getMethod.getResponseBodyAsString();
        assertTrue(response.indexOf("Known services on cxf bc component are:") >= 0);
        assertTrue(response.indexOf("http://localhost:8080/hello") >= 0);
    }

    public void testEndpointDOCWithExternalConsumer() throws Exception {
        CxfBcComponent comp = new CxfBcComponent();
        CxfBcConsumer ep = new CxfBcConsumer();
        ep.setWsdl(new ClassPathResource("HelloWorld-DOC.wsdl"));
        ep.setTargetService(new QName("urn:test", "target"));
        comp.setEndpoints(new CxfBcEndpointType[] {ep});
        jbi.activateComponent(comp, "servicemix-cxfbc");

        MockServiceComponent echo = new MockServiceComponent();
        echo.setService(new QName("urn:test", "target"));
        echo.setEndpoint("endpoint");
        echo.setResponseXml("<jbi:message xmlns:jbi='http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper'><jbi:part>" 
                + "<ns2:HelloResponse xmlns:ns2='uri:HelloWorld'><text>helloffang</text></ns2:HelloResponse></jbi:part></jbi:message>");
        jbi.activateComponent(echo, "echo");

        URL wsdl = getClass().getResource("/HelloWorld-DOC.wsdl");
        assertNotNull(wsdl);
        HelloService helloService = new HelloService(wsdl, serviceName);
        HelloPortType port = helloService.getHelloPort();
        Client client = ClientProxy.getClient(port);
        client.getInInterceptors().add(new LoggingInInterceptor());
        HelloRequest req = new HelloRequest();
        req.setText("hello");
        HelloHeader header = new HelloHeader();
        header.setId("ffang");
        HelloResponse rep = port.hello(req, header);
        Thread.sleep(1000);
        assertEquals(rep.getText(), "helloffang");
    }

    public void testEndpointDOCWithExternalConsumerAndCxfSe() throws Exception {
        CxfBcComponent comp = new CxfBcComponent();
        CxfBcConsumer ep = new CxfBcConsumer();
        ep.setWsdl(new ClassPathResource("HelloWorld-DOC.wsdl"));
        ep.setTargetService(new QName("http://cxfbc.servicemix.apache.org/",
                "HelloPortTypeImplService"));
        ep.setTargetInterface(new QName("uri:HelloWorld", "HelloPortType",
                "HelloPortType"));
        ep.setEndpoint("HelloPortTypeImplPort");
        comp.setEndpoints(new CxfBcEndpointType[] {ep});
        jbi.activateComponent(comp, "servicemix-cxfbc");

        CxfSeComponent seComp = new CxfSeComponent();
        CxfSeEndpoint endpoint = new CxfSeEndpoint();
        endpoint.setPojo(new HelloPortTypeImpl());

        seComp.setEndpoints(new CxfSeEndpoint[] {endpoint});
        jbi.activateComponent(seComp, "servicemix-cxfse");
        URL wsdl = getClass().getResource("/HelloWorld-DOC.wsdl");
        assertNotNull(wsdl);
        HelloService helloService = new HelloService(wsdl, serviceName);
        HelloPortType port = helloService.getHelloPort();
        HelloRequest req = new HelloRequest();
        req.setText("hello");
        HelloHeader header = new HelloHeader();
        header.setId("ffang");
        HelloResponse rep = port.hello(req, header);
        Thread.sleep(1000);
        assertEquals(rep.getText(), "helloffang");
    }

    public void testEndpointRPC() throws Exception {
        CxfBcComponent comp = new CxfBcComponent();
        CxfBcConsumer ep = new CxfBcConsumer();
        ep.setWsdl(new ClassPathResource("HelloWorld-RPC.wsdl"));
        ep.setTargetService(new QName("urn:test", "target"));
        comp.setEndpoints(new CxfBcEndpointType[] {ep});
        jbi.activateComponent(comp, "servicemix-cxfbc");

        MockServiceComponent echo = new MockServiceComponent();
        echo.setService(new QName("urn:test", "target"));
        echo.setEndpoint("endpoint");
        echo.setResponseXml("<jbi:message xmlns:jbi='http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper'>" 
                + "<jbi:part><text>hello</text></jbi:part></jbi:message>");
        jbi.activateComponent(echo, "echo");

        URLConnection connection = new URL("http://localhost:8080/hello")
                .openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        OutputStream os = connection.getOutputStream();
        // Post the request file.
        InputStream fis = new ClassPathResource("HelloWorld-RPC-Input.xml")
                .getInputStream();
        FileUtil.copyInputStream(fis, os);
        // Read the response.
        InputStream is = connection.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtil.copyInputStream(is, baos);
        System.err.println(baos.toString());

        Thread.sleep(100);
    }

}
