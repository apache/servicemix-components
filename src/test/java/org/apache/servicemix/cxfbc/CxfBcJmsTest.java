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
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;
import org.apache.cxf.testutil.common.ServerLauncher;
import org.apache.hello_world_soap_http.BadRecordLitFault;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.hello_world_soap_http.HelloWorldService;
import org.apache.hello_world_soap_http.NoSuchCodeLitFault;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class CxfBcJmsTest extends SpringTestSupport {

    protected static boolean serversStarted;
    private ServerLauncher sl;
    private ServerLauncher embeddedLauncher;
    private ServerLauncher jmsLauncher;

    
    
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
        assertTrue("server did not launch correctly", 
                launchServer(MyJMSServer.class, null, false));
        jmsLauncher = sl;
        
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
        try {
            jmsLauncher.stopServer();         
        } catch (IOException ex) {
            ex.printStackTrace();
            fail("failed to stop server " + jmsLauncher.getClass());
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

    public void testJMSTransport() throws Exception {
        URL wsdl = getWSDLURL("org/apache/servicemix/cxfbc/ws/security/hello_world.wsdl");

        QName serviceName = getServiceName(new QName(
                "http://apache.org/hello_world_soap_http", "HelloWorldService"));
        QName portName = getPortName(new QName(
                "http://apache.org/hello_world_soap_http", "HelloWorldPort"));
        
        assertNotNull(wsdl);

        HelloWorldService service = new HelloWorldService(wsdl, serviceName);
        assertNotNull(service);

        String response1 = new String("Hello Milestone-");
        String response2 = new String("Bonjour");
        try {
            Greeter greeter = service.getPort(portName,
                    Greeter.class);
            for (int idx = 0; idx < 5; idx++) {
                String greeting = greeter.greetMe("Milestone-" + idx);
                assertNotNull("no response received from service", greeting);
                String exResponse = response1 + idx;
                assertEquals(exResponse, greeting);

                String reply = greeter.sayHi();
                assertNotNull("no response received from service", reply);
                assertEquals(response2, reply);
                greeter.greetMeOneWay("test");
                try {
                    greeter.testDocLitFault("BadRecordLitFault");
                    fail("Should have thrown BadRecoedLitFault");
                } catch (BadRecordLitFault ex) {
                    assertNotNull(ex.getFaultInfo());
                }

                try {
                    greeter.testDocLitFault("NoSuchCodeLitFault");
                    fail("Should have thrown NoSuchCodeLitFault exception");
                } catch (NoSuchCodeLitFault nslf) {
                    assertNotNull(nslf.getFaultInfo());
                    assertNotNull(nslf.getFaultInfo().getCode());
                }
            }
        } catch (UndeclaredThrowableException ex) {
            throw (Exception) ex.getCause();
        }

    }

    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext(
                "org/apache/servicemix/cxfbc/jms_transport.xml");
    }
    
    public QName getServiceName(QName q) {
        return q;
    }
    
    public QName getPortName(QName q) {
        return q;
    }
    
    public URL getWSDLURL(String s) throws Exception {
        return CxfBcJmsTest.class.getClassLoader().getResource(s);
    }

}
