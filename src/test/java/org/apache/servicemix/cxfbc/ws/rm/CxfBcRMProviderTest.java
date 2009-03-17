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

import java.net.URI;
import java.util.logging.Logger;

import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.calculator.CalculatorImpl;
import org.apache.cxf.calculator.CalculatorPortType;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.cxfbc.CxfBcComponent;
import org.apache.servicemix.cxfbc.CxfBcEndpointType;
import org.apache.servicemix.cxfbc.CxfBcProvider;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;

import org.springframework.core.io.ClassPathResource;


public class CxfBcRMProviderTest extends TestCase {

    private static final Logger LOG = LogUtils.getL7dLogger(org.apache.servicemix.cxfbc.CxfBcProviderTest.class);
    
    private DefaultServiceMixClient client;
    private InOut io;
    private JaxWsServerFactoryBean factory;
    private Server server;
    private Endpoint endpoint;
    private ServiceInfo service;
    
    private JBIContainer jbi;

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

        
    private void localTestProvider(boolean withRM) throws Exception {
        LOG.info("test provider withRM=" + withRM);
           
        
        //start external service
        if (withRM) {
            SpringBusFactory bf = new SpringBusFactory();
            Bus serverBus = bf.createBus("org/apache/servicemix/cxfbc/ws/rm/rminterceptors.xml");
            BusFactory.setDefaultBus(serverBus);
        }
        factory = new JaxWsServerFactoryBean();
        factory.setServiceClass(CalculatorPortType.class);
        factory.setServiceBean(new CalculatorImpl());
        String address = "http://localhost:9001/providertest";
        factory.setAddress(address);
        factory.setBindingId("http://schemas.xmlsoap.org/wsdl/soap12/");
        server = factory.create();
        endpoint = server.getEndpoint();
        endpoint.getInInterceptors().add(new LoggingInInterceptor());
        endpoint.getOutInterceptors().add(new LoggingOutInterceptor());
        service = endpoint.getEndpointInfo().getService();
        assertNotNull(service);

        jbi = new JBIContainer();
        jbi.setEmbedded(true);
        jbi.init();
        jbi.start();

        CxfBcComponent comp = new CxfBcComponent();
        CxfBcProvider ep = new CxfBcProvider();
        if (withRM) {
            ep.setBusCfg("org/apache/servicemix/cxfbc/ws/rm/rminterceptors.xml");
        }
        ep.setWsdl(new ClassPathResource("/wsdl/calculator.wsdl"));
        ep.setLocationURI(new URI("http://localhost:9001/providertest"));
        ep.setEndpoint("CalculatorPort");
        ep.setService(new QName("http://apache.org/cxf/calculator", "CalculatorService"));
        ep.setInterfaceName(new QName("http://apache.org/cxf/calculator", "CalculatorPortType"));
        comp.setEndpoints(new CxfBcEndpointType[] {ep});
        jbi.activateComponent(comp, "servicemix-cxfbc");
        client = new DefaultServiceMixClient(jbi);
        io = client.createInOutExchange();
        io.setService(new QName("http://apache.org/cxf/calculator", "CalculatorService"));
        io.setInterfaceName(new QName("http://apache.org/cxf/calculator", "CalculatorPortType"));
        io.setOperation(new QName("http://apache.org/cxf/calculator", "add"));
        //send message
        io.getInMessage().setContent(new StringSource(
                "<message xmlns='http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper'>"
              + "<part>"
              + "<add xmlns='http://apache.org/cxf/calculator/types'><arg0>10</arg0>"
              + "<arg1>5</arg1></add>"
              + "</part>"
              + "</message>"));
        client.sendSync(io);
        client.done(io);
        assertTrue(new SourceTransformer().contentToString(
                io.getOutMessage()).indexOf("<return>15</return>") >= 0);

        // Shutdown CXF Service/Endpoint so that next test doesn't fail.
        factory.getBus().shutdown(true);
        // Shutdown jbi
        jbi.shutDown();
    }    

    public void testProvider() throws Exception {
        localTestProvider(true);
    }

}
