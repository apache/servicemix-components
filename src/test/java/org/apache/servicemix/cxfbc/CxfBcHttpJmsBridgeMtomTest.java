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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsClientProxy;
import org.apache.cxf.jaxws.binding.soap.SOAPBindingImpl;
import org.apache.cxf.jaxws.support.JaxWsEndpointImpl;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.mime.TestMtom;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.factory.ReflectionServiceFactoryBean;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.testutil.common.ServerLauncher;
import org.apache.servicemix.cxfbc.mtom.TestMtomImpl;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class CxfBcHttpJmsBridgeMtomTest extends SpringTestSupport {
    
    public static final QName MTOM_PORT = new QName(
            "http://cxf.apache.org/mime", "TestMtomPort");

    public static final QName MTOM_SERVICE = new QName(
            "http://cxf.apache.org/mime", "TestMtomService");

    
    protected static boolean serversStarted;
    private ServerLauncher sl;
    private ServerLauncher embeddedLauncher;
    

    
    
    public void startServers() throws Exception {
        if (serversStarted) {
            return;
        }
        Map<String, String> props = new HashMap<String, String>();                
        if (System.getProperty("activemq.store.dir") != null) {
            props.put("activemq.store.dir", System.getProperty("activemq.store.dir"));
        }
        props.put("java.util.logging.config.file", 
                  System.getProperty("java.util.logging.config.file"));
        
        assertTrue("server did not launch correctly", 
                   launchServer(EmbededJMSBrokerLauncher.class, props, false));
        embeddedLauncher =  sl;
        
        
        serversStarted = true;
    }
    
    protected void setUp() throws Exception {
        startServers();
        super.setUp();
            
    }
    
    protected void tearDown() throws Exception {
        try {
            embeddedLauncher.stopServer();         
        } catch (IOException ex) {
            ex.printStackTrace();
            fail("failed to stop server " + embeddedLauncher.getClass());
        }
        
        serversStarted = false;
    }
    
    public boolean launchServer(Class<?> clz, Map<String, String> p, boolean inProcess) {
        boolean ok = false;
        try { 
            sl = new ServerLauncher(clz.getName(), p, null, inProcess);
            ok = sl.launchServer();
            assertTrue("server failed to launch", ok);
            
        } catch (IOException ex) {
            ex.printStackTrace();
            fail("failed to launch server " + clz);
        }
        
        return ok;
    }
    
    public void testBridge() throws Exception {
        // start external service
        EndpointImpl endpoint =
            (EndpointImpl)javax.xml.ws.Endpoint.publish("http://localhost:9001/mtombridgetest", 
                new TestMtomImpl());
             
        SOAPBinding binding = (SOAPBinding)endpoint.getBinding();
        binding.setMTOMEnabled(true);
        endpoint.getInInterceptors().add(new LoggingInInterceptor());
        endpoint.getOutInterceptors().add(new LoggingOutInterceptor());
        
        // start external client
        TestMtom mtomPort = createPort(MTOM_SERVICE, MTOM_PORT, TestMtom.class,
                true);
        try {
            
            Holder<DataHandler> param = new Holder<DataHandler>();
            
            param.value = new DataHandler(new ByteArrayDataSource("foobar".getBytes(), 
                "application/octet-stream"));
            
            Holder<String> name = new Holder<String>("call detail");
            mtomPort.testXop(name, param);
            assertEquals("call detailfoobar",
                    name.value);
            assertNotNull(param.value);
            InputStream bis = param.value.getDataSource().getInputStream();
            byte b[] = new byte[10];
            bis.read(b, 0, 10);
            String attachContent = new String(b);
            assertEquals(attachContent, "testfoobar");
        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }

    }

    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext(
                "org/apache/servicemix/cxfbc/mtom_bridge_http_jms.xml");
    }
    
    private <T> T createPort(QName serviceName, QName portName,
            Class<T> serviceEndpointInterface, boolean enableMTOM)
        throws Exception {
        Bus bus = BusFactory.getDefaultBus();
        ReflectionServiceFactoryBean serviceFactory = new JaxWsServiceFactoryBean();
        serviceFactory.setBus(bus);
        serviceFactory.setServiceName(serviceName);
        serviceFactory.setServiceClass(serviceEndpointInterface);
        serviceFactory.setWsdlURL(getClass().getResource("/wsdl/mtom_xop.wsdl"));
        Service service = serviceFactory.create();
        EndpointInfo ei = service.getEndpointInfo(portName);
        JaxWsEndpointImpl jaxwsEndpoint = new JaxWsEndpointImpl(bus, service,
                ei);
        SOAPBinding jaxWsSoapBinding = new SOAPBindingImpl(ei.getBinding());
        jaxWsSoapBinding.setMTOMEnabled(enableMTOM);

        Client client = new ClientImpl(bus, jaxwsEndpoint);
        InvocationHandler ih = new JaxWsClientProxy(client, jaxwsEndpoint
                .getJaxwsBinding());
        Object obj = Proxy.newProxyInstance(serviceEndpointInterface
                .getClassLoader(), new Class[] {serviceEndpointInterface,
                    BindingProvider.class}, ih);
        return serviceEndpointInterface.cast(obj);
    }

}
