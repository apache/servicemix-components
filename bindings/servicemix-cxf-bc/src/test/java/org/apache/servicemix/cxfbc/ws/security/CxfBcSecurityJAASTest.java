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
package org.apache.servicemix.cxfbc.ws.security;


import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.testutil.common.ServerLauncher;
import org.apache.hello_world_soap_http.Greeter;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


public class CxfBcSecurityJAASTest extends Assert {

    private static final Logger LOG = LogUtils.getL7dLogger(CxfBCSecurityTest.class);
    
    private static final java.net.URL WSDL_LOC;
    static {
        java.net.URL tmp = null;
        try {
            tmp = CxfBCSecurityTest.class.getClassLoader().getResource(
                "org/apache/servicemix/cxfbc/ws/security/hello_world.wsdl"
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }
        WSDL_LOC = tmp;
        String path = System.getProperty("java.security.auth.login.config");
        if (path == null) {
            URL resource = CxfBcSecurityJAASTest.class.getResource("login.properties");
            if (resource != null) {
                path = new File(resource.getFile()).getAbsolutePath();
                System.setProperty("java.security.auth.login.config", path);
            }
        }
    }
    
    
    protected static boolean serversStarted;
    private static ServerLauncher sl;
    
    @BeforeClass
    public static void startServer() throws Exception {
        startJBIContainers();
        Thread.sleep(3000);
    }
    
    @AfterClass
    public static void stopServer() throws Exception {
        try {
            sl.stopServer();
        } catch (IOException ex) {
            ex.printStackTrace();
            fail("failed to stop jbi container " + sl.getClass());
        } 
        serversStarted = false;
       
    }
   
    @Test
    public void testJAASPolicy() {
        LOG.info("test security ws-policy");
        Bus bus = new SpringBusFactory().createBus(
                "org/apache/servicemix/cxfbc/ws/security/client-jaas.xml"); 
        BusFactory.setDefaultBus(bus);
        LoggingInInterceptor in = new LoggingInInterceptor();
        bus.getInInterceptors().add(in);
        bus.getInFaultInterceptors().add(in);
        LoggingOutInterceptor out = new LoggingOutInterceptor();
        bus.getOutInterceptors().add(out);
        bus.getOutFaultInterceptors().add(out);
        final javax.xml.ws.Service svc = javax.xml.ws.Service.create(WSDL_LOC,
                new javax.xml.namespace.QName(
                        "http://apache.org/hello_world_soap_http",
                        "SOAPServiceWSSecurity"));
        final Greeter greeter = svc.getPort(new javax.xml.namespace.QName(
                "http://apache.org/hello_world_soap_http",
                "TimestampSignEncryptPolicy"), Greeter.class);
        String ret = greeter.sayHi();
        assertEquals(ret, "Bonjour");
        ret = greeter.greetMe("ffang");
        assertEquals(ret, "Hello ffang");
    }
    
    @Test
    public void testJAAS() {
        LOG.info("test security");
        Bus bus = new SpringBusFactory().createBus(
                "org/apache/servicemix/cxfbc/ws/security/client-jaas.xml"); 
        BusFactory.setDefaultBus(bus);
        LoggingInInterceptor in = new LoggingInInterceptor();
        bus.getInInterceptors().add(in);
        bus.getInFaultInterceptors().add(in);
        LoggingOutInterceptor out = new LoggingOutInterceptor();
        bus.getOutInterceptors().add(out);
        bus.getOutFaultInterceptors().add(out);
        final javax.xml.ws.Service svc = javax.xml.ws.Service.create(WSDL_LOC,
                new javax.xml.namespace.QName(
                        "http://apache.org/hello_world_soap_http",
                        "SOAPServiceWSSecurity"));
        final Greeter greeter = svc.getPort(new javax.xml.namespace.QName(
                "http://apache.org/hello_world_soap_http",
                "TimestampSignEncrypt"), Greeter.class);
        String ret = greeter.sayHi();
        assertEquals(ret, "Bonjour");
        ret = greeter.greetMe("ffang");
        assertEquals(ret, "Hello ffang");
    }
    
    @Test
    public void testUserNotExist() {
        LOG.info("test user not exist");
        Bus bus = new SpringBusFactory().createBus(
                "org/apache/servicemix/cxfbc/ws/security/client-jaas-dummy.xml"); 
        BusFactory.setDefaultBus(bus);
        LoggingInInterceptor in = new LoggingInInterceptor();
        bus.getInInterceptors().add(in);
        bus.getInFaultInterceptors().add(in);
        LoggingOutInterceptor out = new LoggingOutInterceptor();
        bus.getOutInterceptors().add(out);
        bus.getOutFaultInterceptors().add(out);
        final javax.xml.ws.Service svc = javax.xml.ws.Service.create(WSDL_LOC,
                new javax.xml.namespace.QName(
                        "http://apache.org/hello_world_soap_http",
                        "SOAPServiceWSSecurity"));
        final Greeter greeter = svc.getPort(new javax.xml.namespace.QName(
                "http://apache.org/hello_world_soap_http",
                "TimestampSignEncrypt"), Greeter.class);
        try {
            greeter.sayHi();
            fail("should catch exception");
        } catch (Exception e) {
            assertEquals(e.getMessage(), "User does not exist");
        }
    }
    
    @Test
    public void testPasswordMismatch() {
        LOG.info("test password not match");
        Bus bus = new SpringBusFactory().createBus(
                "org/apache/servicemix/cxfbc/ws/security/client-jaas-password-mismatch.xml"); 
        BusFactory.setDefaultBus(bus);
        LoggingInInterceptor in = new LoggingInInterceptor();
        bus.getInInterceptors().add(in);
        bus.getInFaultInterceptors().add(in);
        LoggingOutInterceptor out = new LoggingOutInterceptor();
        bus.getOutInterceptors().add(out);
        bus.getOutFaultInterceptors().add(out);
        final javax.xml.ws.Service svc = javax.xml.ws.Service.create(WSDL_LOC,
                new javax.xml.namespace.QName(
                        "http://apache.org/hello_world_soap_http",
                        "SOAPServiceWSSecurity"));
        final Greeter greeter = svc.getPort(new javax.xml.namespace.QName(
                "http://apache.org/hello_world_soap_http",
                "TimestampSignEncrypt"), Greeter.class);
        try {
            greeter.sayHi();
            fail("should catch exception");
        } catch (Exception e) {
            assertEquals(e.getMessage(), "Password does not match");
        }
    }
    /***
    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        // load cxf se and bc from spring config file
        return new ClassPathXmlApplicationContext(
            "org/apache/servicemix/cxfbc/ws/security/xbean-jaas.xml");
    }
    **/
    
    protected static void startJBIContainers() throws Exception {
        if (serversStarted) {
            return;
        }
        Map<String, String> props = new HashMap<String, String>();                
        if (System.getProperty("javax.xml.transform.TransformerFactory") != null) {
            props.put("javax.xml.transform.TransformerFactory", System.getProperty("javax.xml.transform.TransformerFactory"));
        }
        if (System.getProperty("javax.xml.stream.XMLInputFactory") != null) {
            props.put("javax.xml.stream.XMLInputFactory", System.getProperty("javax.xml.stream.XMLInputFactory"));
        }
        if (System.getProperty("javax.xml.stream.XMLOutputFactory") != null) {
            props.put("javax.xml.stream.XMLOutputFactory", System.getProperty("javax.xml.stream.XMLOutputFactory"));
        }
        if (System.getProperty("java.security.auth.login.config") != null) {
            props.put("java.security.auth.login.config", System.getProperty("java.security.auth.login.config"));
        }
        
        assertTrue("JBIContainers did not launch correctly", 
                launchServer(JAASServer.class, props, true));
       
        
        serversStarted = true;
    }
    
    protected static boolean launchServer(Class<?> clz, Map<String, String> p, boolean inProcess) {
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
    

}
