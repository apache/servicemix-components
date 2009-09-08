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
package org.apache.servicemix.cxfbc.ws.rm;

import java.net.URL;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import junit.framework.TestCase;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.servicemix.components.util.MockServiceComponent;
import org.apache.servicemix.cxfbc.CxfBcComponent;
import org.apache.servicemix.cxfbc.CxfBcConsumer;
import org.apache.servicemix.cxfbc.CxfBcEndpointType;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.springframework.core.io.ClassPathResource;

import uri.helloworld.HelloHeader;
import uri.helloworld.HelloPortType;
import uri.helloworld.HelloRequest;
import uri.helloworld.HelloResponse;
import uri.helloworld.HelloService;

public class CxfBcRMBasicTest extends TestCase {

    static final Logger LOG = LogUtils.getL7dLogger(CxfBcRMBasicTest.class);

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

    public void testRMEndpointWithExternalConsumer() throws Exception {
        CxfBcComponent comp = new CxfBcComponent();
        CxfBcConsumer ep = new CxfBcConsumer();
        ep.setBusCfg("org/apache/servicemix/cxfbc/ws/rm/rminterceptors.xml");
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

        SpringBusFactory bf = new SpringBusFactory();
        Bus clientBus = bf.createBus("org/apache/servicemix/cxfbc/ws/rm/rminterceptors.xml");
        BusFactory.setDefaultBus(clientBus);
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
        Holder<HelloHeader> header1 = new Holder<HelloHeader>();
        header1.value = header;
        HelloResponse rep = port.hello(req, header1);
        Thread.sleep(1000);
        assertEquals(rep.getText(), "helloffang");
    }

}
