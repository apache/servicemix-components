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

import java.io.IOException;
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
import org.apache.servicemix.cxfbc.EmbededJMSBrokerLauncher;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class CxfBCSecurityTest extends SpringTestSupport {

    protected static boolean serversStarted;
    private static final Logger LOG = LogUtils.getL7dLogger(CxfBCSecurityTest.class);
    private static final java.net.URL WSDL_LOC;
       
    private ServerLauncher sl;
    private ServerLauncher embeddedLauncher;
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
    }
    
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
    
    public void testTimestampSignEncrypt() {
        LOG.info("test security");
        Bus bus = new SpringBusFactory().createBus(
                "org/apache/servicemix/cxfbc/ws/security/client.xml"); 
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
    
    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        // load cxf se and bc from spring config file
        return new ClassPathXmlApplicationContext(
            "org/apache/servicemix/cxfbc/ws/security/xbean.xml");
    }

}
