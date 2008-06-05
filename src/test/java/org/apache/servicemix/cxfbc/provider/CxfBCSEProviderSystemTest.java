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
package org.apache.servicemix.cxfbc.provider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import org.apache.cxf.common.logging.LogUtils;

import org.apache.cxf.testutil.common.ServerLauncher;

import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.container.SpringJBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class CxfBCSEProviderSystemTest extends SpringTestSupport {
    
    private static final Logger LOG = LogUtils.getL7dLogger(CxfBCSEProviderSystemTest.class);
    private static boolean serversStarted;
    private DefaultServiceMixClient client;
    private InOut io;    

    private ServerLauncher sl;
    
    
    public void startServers() throws Exception {
        if (serversStarted) {
            return;
        }
        Map<String, String> props = new HashMap<String, String>();                
        
        assertTrue("server did not launch correctly", 
                   launchServer(MyServer.class, props, false));

        
        serversStarted = true;
    }
    
    protected void setUp() throws Exception {
        startServers();
        //super.setUp();
        LOG.info("setUp is invoked");            
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

    
    
               
    public void setUpJBI(String beanFile) throws Exception {
        if (context != null) {
            context.refresh();
        }
        transformer = new SourceTransformer();
        if (beanFile == null) {
            context = createBeanFactory();
        } else {
            context = createBeanFactory(beanFile);
        }
                
        jbi = (SpringJBIContainer) context.getBean("jbi");
        assertNotNull("JBI Container not found in spring!", jbi);
        
    }
    
    public void tearDown() throws Exception {
        if (context != null) {
            context.destroy();
            context = null;
        }
        if (jbi != null) {
            jbi.shutDown();
            jbi.destroy();
            jbi = null;
        }
    }

    public void testGreetMeProviderWithOutJBIWrapper() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/provider/xbean_provider_without_jbi_wrapper.xml");
        greetMeProviderTestBase(false);
    }
    
    public void testGreetMeProviderWithDynamicUri() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/provider/xbean_provider_without_jbi_wrapper.xml");
        greetMeProviderTestBase(true);
    }
        
    private void greetMeProviderTestBase(boolean useDynamicUri) throws Exception {

        client = new DefaultServiceMixClient(jbi);
        io = client.createInOutExchange();
        io.setService(new QName("http://apache.org/hello_world_soap_http_provider", "SOAPService"));
        io.setInterfaceName(new QName("http://apache.org/hello_world_soap_http_provider", "Greeter"));
        io.setOperation(new QName("http://apache.org/hello_world_soap_http_provider", "greetMe"));
        //send message to proxy
        io.getInMessage().setContent(new StringSource(
              "<greetMe xmlns='http://apache.org/hello_world_soap_http_provider/types'><requestType>"
              + "Edell"
              + "</requestType></greetMe>"));
        if (useDynamicUri) {
            io.getInMessage().setProperty(JbiConstants.HTTP_DESTINATION_URI, "http://localhost:9002/dynamicuritest");
        }
        client.sendSync(io);
        assertTrue(new SourceTransformer().contentToString(
                io.getOutMessage()).indexOf("Hello Edell") >= 0);        
        
    }
    

    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        // load cxf se and bc from spring config file
        return new ClassPathXmlApplicationContext(
                "org/apache/servicemix/cxfbc/provider/xbean_provider.xml");
    }
    
    protected AbstractXmlApplicationContext createBeanFactory(String beanFile) {
        //load cxf se and bc from specified spring config file
        return new ClassPathXmlApplicationContext(
            beanFile);
    }

}
