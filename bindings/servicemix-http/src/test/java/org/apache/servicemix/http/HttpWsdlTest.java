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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketAddress;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.wsdl.Definition;
import javax.wsdl.Input;
import javax.wsdl.Message;
import javax.wsdl.Operation;
import javax.wsdl.Output;
import javax.wsdl.Part;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.messaging.MessageExchangeSupport;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.AbstractHandler;
import org.springframework.core.io.UrlResource;

public class HttpWsdlTest extends TestCase {
    private static transient Log log = LogFactory.getLog(HttpWsdlTest.class);
      
    private Integer port4 = Integer.parseInt(System.getProperty("http.port4"));
    private Integer port5 = Integer.parseInt(System.getProperty("http.port5"));
    private Integer port6 = Integer.parseInt(System.getProperty("http.port6"));
    private Integer port7 = Integer.parseInt(System.getProperty("http.port7"));
    private Integer port8 = Integer.parseInt(System.getProperty("http.port8"));
    

    protected JBIContainer container;

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

    protected void testWSDL(final Definition def, int portNumber) throws Exception {
        // Add a receiver component
        ActivationSpec asEcho = new ActivationSpec("echo", new EchoComponent() {
            public Document getServiceDescription(ServiceEndpoint endpoint) {
                try {
                    Document doc = WSDLFactory.newInstance().newWSDLWriter().getDocument(def);
                    return doc;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        asEcho.setEndpoint("myConsumer");
        asEcho.setService(new QName("http://test", "MyConsumerService"));
        container.activateComponent(asEcho);

        // HTTP Component
        HttpEndpoint ep = new HttpEndpoint();
        ep.setService(new QName("http://test", "MyConsumerService"));
        ep.setEndpoint("myConsumer");
        ep.setRoleAsString("consumer");
        ep.setLocationURI("http://localhost:" + portNumber + "/Service");
        ep.setSoap(true);
        HttpComponent http = new HttpComponent();
        http.setEndpoints(new HttpEndpoint[] {ep});
        container.activateComponent(http, "HttpWsdlTest");

        // Start container
        container.start();

        GetMethod get = new GetMethod("http://localhost:" + portNumber + "/Service/?wsdl");
        int state = new HttpClient().executeMethod(get);
        assertEquals(HttpServletResponse.SC_OK, state);
        Document doc = (Document) new SourceTransformer().toDOMNode(new StringSource(get.getResponseBodyAsString()));
        get.releaseConnection();

        // Test WSDL
        WSDLFactory factory = WSDLFactory.newInstance();
        WSDLReader reader = factory.newWSDLReader();
        Definition definition;
        definition = reader.readWSDL("http://localhost:" + portNumber + "/Service/?wsdl", doc);
        assertNotNull(definition);
        assertNotNull(definition.getImports());
        assertEquals(1, definition.getImports().size());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WSDLFactory.newInstance().newWSDLWriter().writeWSDL(definition, baos);
        log.info(baos.toString());
    }

    protected Definition createDefinition(boolean rpc) throws WSDLException {
        Definition def = WSDLFactory.newInstance().newDefinition();
        def.setTargetNamespace("http://porttype.test");
        def.addNamespace("tns", "http://porttype.test");
        def.addNamespace("xsd", "http://www.w3.org/2000/10/XMLSchema");
        def.addNamespace("w", "uri:hello");
        Message inMsg = def.createMessage();
        inMsg.setQName(new QName("http://porttype.test", "InMessage"));
        inMsg.setUndefined(false);
        Part part1 = def.createPart();
        part1.setName("part1");
        if (rpc) {
            part1.setTypeName(new QName("http://www.w3.org/2000/10/XMLSchema", "int"));
        } else {
            part1.setElementName(new QName("uri:hello", "world"));
        }
        inMsg.addPart(part1);
        Part part2 = def.createPart();
        part2.setName("part2");
        part2.setElementName(new QName("uri:hello", "world"));
        inMsg.addPart(part2);
        def.addMessage(inMsg);
        Message outMsg = def.createMessage();
        outMsg.setQName(new QName("http://porttype.test", "OutMessage"));
        outMsg.setUndefined(false);
        Part part3 = def.createPart();
        part3.setName("part3");
        part3.setElementName(new QName("uri:hello", "world"));
        outMsg.addPart(part3);
        def.addMessage(outMsg);
        PortType type = def.createPortType();
        type.setUndefined(false);
        type.setQName(new QName("http://porttype.test", "MyConsumerInterface"));
        Operation op = def.createOperation();
        op.setName("Hello");
        Input in = def.createInput();
        in.setMessage(inMsg);
        op.setInput(in);
        op.setUndefined(false);
        Output out = def.createOutput();
        out.setMessage(outMsg);
        op.setOutput(out);
        type.addOperation(op);
        def.addPortType(type);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WSDLFactory.newInstance().newWSDLWriter().writeWSDL(def, baos);
        log.info(baos.toString());
        return def;
    }

    public void testWithNonStandaloneWsdlDoc() throws Exception {
        testWSDL(createDefinition(false), port4);
    }

    public void testWithNonStandaloneWsdlRpc() throws Exception {
        testWSDL(createDefinition(true), port5);
    }

    public void testWithExistingBinding() throws Exception {
        String uri = getClass().getResource("bound-wsdl.wsdl").toString();
        Definition def = WSDLFactory.newInstance().newWSDLReader().readWSDL(uri);
        testWSDL(def, port6);
    }

    public void testExternalNonStandaloneWsdl() throws Exception {

        //startup-jetty as mirror for 
        //http://www.ws-i.org/SampleApplications/SupplyChainManagement/2002-08/Retailer.wsdl
        int remoteHttpServerPort = port7;
        Server remoteServer = new Server(remoteHttpServerPort);
        Handler handler = new AbstractHandler() {

            public void handle(String arg0, HttpServletRequest req,
                    HttpServletResponse res, int arg3) throws IOException,
                    ServletException {

                res.setContentType("text/xml");
                PrintWriter writer = res.getWriter();
                BufferedReader br = new BufferedReader(new InputStreamReader(
                        this.getClass().getClassLoader().getResourceAsStream("wsdls" + req.getPathInfo())));
                String line = br.readLine();
                while (line != null)  {
                    writer.write(line);
                    line = br.readLine();
                }
                br.close();
                writer.close();
            }

        };
        
        remoteServer.addHandler(handler);
        remoteServer.start();
        
        try {
            int localHttpServerPort = port8;
            // HTTP Component
            HttpEndpoint ep = new HttpEndpoint();
            ep.setService(new QName("http://servicemix.apache.org/wsn/jaxws", "PullPointService"));
            ep.setEndpoint("JBI");
            ep.setRoleAsString("consumer");
            ep.setLocationURI("http://localhost:" + localHttpServerPort + "/Service/");
            ep.setDefaultMep(MessageExchangeSupport.IN_OUT);
            ep.setWsdlResource(new UrlResource("http://localhost:" + remoteHttpServerPort + "/wsn.wsdl"));
            HttpComponent http = new HttpComponent();
            http.setEndpoints(new HttpEndpoint[] {ep});
            container.activateComponent(http, "PullPointService");

            // Start container
            container.start();

            GetMethod get = new GetMethod("http://localhost:" + localHttpServerPort + "/Service/?wsdl");
            int state = new HttpClient().executeMethod(get);
            assertEquals(HttpServletResponse.SC_OK, state);
            Document doc = (Document) new SourceTransformer().toDOMNode(new StringSource(get.getResponseBodyAsString()));
            get.releaseConnection();

            // Test WSDL
            WSDLFactory factory = WSDLFactory.newInstance();
            WSDLReader reader = factory.newWSDLReader();
            Definition def;

            def = reader.readWSDL("http://localhost:" + localHttpServerPort + "/Service/?wsdl", doc);
            assertNotNull(def);
            assertNotNull(def.getImports());
            assertEquals(1, def.getImports().size());
        
        } finally {
            remoteServer.stop();
        }
        
    }
    
}
