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

import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import org.apache.cxf.testutil.common.ServerLauncher;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.cxfse.CxfSeComponent;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class CxfBcProviderSecurityTest extends SpringTestSupport {
    
    private DefaultServiceMixClient client;
    private InOut io;
    private CxfSeComponent component;
    private ServerLauncher sl;
    
    protected void setUp() throws Exception {
        super.setUp();
        
        component = new CxfSeComponent();
        jbi.activateComponent(component, "CxfSeComponent");
        //Deploy proxy SU
        component.getServiceUnitManager().deploy("proxy", getServiceUnitPath("provider"));
        component.getServiceUnitManager().init("proxy", getServiceUnitPath("provider"));
        component.getServiceUnitManager().start("proxy");
        assertTrue(
            "Server failed to launch",
            // run the server in another process
            // set this to false to fork
            launchServer(SecurityServer.class, false));
    }
    
    protected void tearDown() throws Exception {
        component.getServiceUnitManager().stop("proxy");
        component.getServiceUnitManager().shutDown("proxy");
        component.getServiceUnitManager().undeploy("proxy", getServiceUnitPath("provider"));
        try {
            sl.stopServer();
        } catch (IOException ex) {
            ex.printStackTrace();
            fail("failed to stop server " + sl.getClass());
        } 
    }
    
    public boolean launchServer(Class<?> clz, boolean inProcess) {
        boolean ok = false;
        try { 
            // java.security.properties is set when using the ibm jdk to work
            // around some security test issues.  Check our system properties
            // for this key, and if it's set, then propagate the property on
            // to the server we launch as well.
            Map<String, String> properties = null;
            if (System.getProperty("java.security.properties") != null) {
                properties = new HashMap<String, String>();
                properties.put("java.security.properties",
                    System.getProperty("java.security.properties"));
            }
            sl = new ServerLauncher(clz.getName(), properties, null, inProcess);
            ok = sl.launchServer();            
            assertTrue("server failed to launch", ok);
            
        } catch (IOException ex) {
            ex.printStackTrace();
            fail("failed to launch server " + clz);
        }
        
        return ok;
    }
    
    public void testProviderWithHttps() throws Exception {

        client = new DefaultServiceMixClient(jbi);
        io = client.createInOutExchange();
        io.setService(new QName("http://apache.org/hello_world_soap_http", "SOAPServiceProvider"));
        io.setInterfaceName(new QName("http://apache.org/hello_world_soap_http", "Greeter"));
        io.setOperation(new QName("http://apache.org/hello_world_soap_http", "greetMe"));
        //send message to proxy
        io.getInMessage().setContent(new StringSource(
                "<message xmlns='http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper'>"
              + "<part> "
              + "<greetMe xmlns='http://apache.org/hello_world_soap_http/types'><requestType>"
              + "provider security test"
              + "</requestType></greetMe>"
              + "</part> "
              + "</message>"));
        client.sendSync(io);
        assertTrue(new SourceTransformer().contentToString(
                io.getOutMessage()).indexOf("provider security test Hello ffang") >= 0);

    }

    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("org/apache/servicemix/cxfbc/ws/security/security_provider.xml");
    }

    protected String getServiceUnitPath(String name) {
        URL url = getClass().getClassLoader().getResource("org/apache/servicemix/cxfbc/ws/security/" + name + "/xbean.xml");
        File path = new File(url.getFile());
        path = path.getParentFile();
        return path.getAbsolutePath();
    }
}
