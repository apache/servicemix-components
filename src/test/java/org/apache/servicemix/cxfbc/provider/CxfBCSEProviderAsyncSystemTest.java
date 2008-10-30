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

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.cxfbc.EmbededJMSBrokerLauncher;
import org.apache.servicemix.cxfbc.MyJMSServer;
import org.apache.servicemix.jbi.container.SpringJBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

/*
 * This test is designed to verify that if use asynchrounous mode, the cxf bc provider will use non block
 * mode to invoke the external server through underlying jms or http transport.To prove it works, configure the
 * jbi container allocate only one thread to the cxf bc, and use a outgoing chain TimeCompareInterceptor to illustrate 
 * the second invocation send out before the first one returns, which means the noblock cxf bc provide works.  
 */
public class CxfBCSEProviderAsyncSystemTest extends SpringTestSupport {
    
    private static final Logger LOG = LogUtils.getL7dLogger(CxfBCSEProviderAsyncSystemTest.class);
    private static boolean serversStarted;
        

    private ServerLauncher sl;
    private ServerLauncher embeddedLauncher;
    private ServerLauncher jmsLauncher;
    private boolean success;
    
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
        success = true;
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
        
        try {
            sl.stopServer();         
        } catch (IOException ex) {
            ex.printStackTrace();
            fail("failed to stop server " + sl.getClass());
        }
        serversStarted = false;
    }

    
    
    public void testGreetMeProviderWithHttpTransportAsync() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/provider/xbean_provider_async.xml");
        greetMeProviderHttpTestBase();
        assertTrue(success);
    }

    public void testGreetMeProviderWithJmsTransportAsync() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/provider/xbean_provider_async.xml");
        greetMeProviderJmsTestBase();
        assertTrue(success);
    }
    
    public void testGreetMeProviderWithHttpTransportSync() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/provider/xbean_provider_sync.xml");
        greetMeProviderHttpTestBase();
        assertTrue(success);
    }

    public void testGreetMeProviderWithJmsTransportSync() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/provider/xbean_provider_sync.xml");
        greetMeProviderJmsTestBase();
        assertTrue(success);
    }

    public void testSameConduit() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/provider/xbean_provider_conduit.xml");
        greetMeProviderJmsTestBase();
        assertTrue(success);
    }


        
    private void greetMeProviderHttpTestBase() throws Exception {
       ClientInvocationForHttp thread1 = new ClientInvocationForHttp("wait");
       thread1.start();
       Thread.sleep(1000);
       ClientInvocationForHttp thread2 = new ClientInvocationForHttp("fang");
       thread2.start();
       thread1.join();
       thread2.join();
    }
    
    private void greetMeProviderJmsTestBase() throws Exception {
    	ClientInvocationForJms thread1 = new ClientInvocationForJms("wait");
        thread1.start();
        Thread.sleep(1000);
        ClientInvocationForJms thread2 = new ClientInvocationForJms("fang");
        thread2.start();
        thread1.join();
        thread2.join();
        
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
    
    class ClientInvocationForHttp extends Thread {
    	private String greeting;
    	
		public ClientInvocationForHttp(String greeting) {
			this.greeting = greeting;
		}
    	
    	public void run() {
    		DefaultServiceMixClient client;
			try {
				client = new DefaultServiceMixClient(jbi);
		        InOut io = client.createInOutExchange();
		        io.setService(new QName("http://apache.org/hello_world_soap_http_provider", "SOAPService"));
		        io.setInterfaceName(new QName("http://apache.org/hello_world_soap_http_provider", "Greeter"));
		        io.setOperation(new QName("http://apache.org/hello_world_soap_http_provider", "greetMe"));
		        //send message to proxy
		        io.getInMessage().setContent(new StringSource(
		              "<greetMe xmlns='http://apache.org/hello_world_soap_http_provider/types'><requestType>"
		              + greeting
		              + "</requestType></greetMe>"));
		        
		        client.send(io);
	            client.receive(100000);
	            client.done(io);
	            if (io.getFault() != null) {
	            	success = false;
	            }
			} catch (Exception e) {
				e.printStackTrace();
			} 
            
    	}
    	
    	
	}
    
    class ClientInvocationForJms extends Thread {
		private String greeting;
				
		public ClientInvocationForJms(String greeting) {
			this.greeting = greeting;
		}
    	
    	public void run() {
    		DefaultServiceMixClient client;
			try {
				client = new DefaultServiceMixClient(jbi);
				InOut io = client.createInOutExchange();
	            io.setService(new QName("http://apache.org/hello_world_soap_http", "HelloWorldService"));
	            io.setInterfaceName(new QName("http://apache.org/hello_world_soap_http", "Greeter"));
	            io.setOperation(new QName("http://apache.org/hello_world_soap_http", "greetMe"));
	            //send message to proxy
	            io.getInMessage().setContent(new StringSource(
	                  "<greetMe xmlns='http://apache.org/hello_world_soap_http/types'><requestType>"
	                  + greeting
	                  + "</requestType></greetMe>"));
	            
	            client.send(io);
	            client.receive(100000);
	            client.done(io); 
	            if (io.getFault() != null) {
	            	success = false;
	            }
			} catch (Exception e) {
				e.printStackTrace();
			} 
            
    	}
    	
    	
	}

}
