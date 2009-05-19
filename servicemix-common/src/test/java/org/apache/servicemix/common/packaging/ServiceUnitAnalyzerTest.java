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
package org.apache.servicemix.common.packaging;

import java.io.File;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

import javax.jbi.messaging.MessageExchange;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.Container;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.packaging.Consumes;
import org.apache.servicemix.common.ServiceMixComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.xbean.AbstractXBeanDeployer;
import org.apache.servicemix.common.xbean.AbstractXBeanServiceUnitAnalyzer;

import junit.framework.TestCase;

public class ServiceUnitAnalyzerTest extends TestCase {    
        
    // Test to Analyze a Service Unit
    public void testAnalyze() throws Exception {
    	MyDeployer deployer = new MyDeployer(new MyComponent() { });
        ServiceUnit su = deployer.deploy("xbean-cp", getServiceUnitPath("org/apache/servicemix/common/packaging/consumes"));
        assertNotNull(su);
        ClassLoader cl = su.getConfigurationClassLoader();
        assertNotNull(cl);   
        Iterator<Endpoint> iter = su.getEndpoints().iterator();
        MyServiceUnitAnalyzer myServiceUnitAnalyzer = new MyServiceUnitAnalyzer();
        myServiceUnitAnalyzer.init(getServiceUnitPath("org/apache/servicemix/common/packaging/consumes"));
        while (iter.hasNext()) {
        	Endpoint endpoint = (Endpoint)iter.next();
        	if (endpoint instanceof ConsumerBeanEndpoint) {
        		List<Consumes> consumes = myServiceUnitAnalyzer.getConsumes(endpoint);
                assertNotNull(consumes);	                
                assertEquals("serviceConsumer", consumes.get(0).getServiceName().getLocalPart());
                assertEquals("endpointConsumer", consumes.get(0).getEndpointName());
                assertEquals("Greeter", consumes.get(0).getInterfaceName().getLocalPart());
                assertEquals(2, consumes.get(0).getLinkType());
                assertEquals(true, consumes.get(0).isValid());                               
        	} else if (endpoint instanceof ProviderBeanEndpoint) {
        		List<Provides> provides = myServiceUnitAnalyzer.getProvides();
                assertNotNull(provides);	
                assertEquals("serviceProvider", provides.get(0).getServiceName().getLocalPart());
                assertEquals("endpointProvider", provides.get(0).getEndpointName());
                assertEquals("GreeterProvider", provides.get(0).getInterfaceName().getLocalPart());
        	}
        }
        
    }
    
    public void testConsumes() throws Exception {
    	List<Consumes> consumesList = new ArrayList<Consumes>();
        Consumes consume = new Consumes();
        consume.setEndpointName("endpointConsumer");
        consume.setInterfaceName(new QName("urn:test", "Greeter", ""));                    
        consume.setLinkType(1);
        consumesList.add(consume);
        Consumes consume2 = new Consumes();
        consume2.setEndpointName("endpointConsumerTwo");                         
        consume2.setLinkType(1);
        consumesList.add(consume2);
        
        Iterator<Consumes> iter = consumesList.iterator();
    	while (iter.hasNext()) {
    		Consumes consumes = (Consumes)iter.next();        
            if ("endpointConsumer".equals(consumes.getEndpointName())) {                
                assertEquals("endpointConsumer", consumes.getEndpointName());
                assertEquals("Greeter", consumes.getInterfaceName().getLocalPart());
                assertEquals(1, consumes.getLinkType());
                assertEquals(true, consumes.isValid());                
        	} else if ("endpointConsumerTwo".equals(consumes.getEndpointName())) {
        		assertEquals("endpointConsumerTwo", consumes.getEndpointName());                
                assertEquals(1, consumes.getLinkType());
                assertEquals(false, consumes.isValid());   
        	}
        }
        
    }   
    
    /**
     * Implementation of a ServiceUnitAnalyzer that is used to provide a
     * way to parse the artifact for the service unit and provide a list of consumes
     * and provides
     *
     * @see org.apache.servicemix.common.packaging.ServiceUnitAnalyzer
     *
     */
    public class MyServiceUnitAnalyzer extends AbstractXBeanServiceUnitAnalyzer {
        	    
    	public void init(String explodedServiceUnitRoot) {
    		init(new File (explodedServiceUnitRoot));
    	}
    	
        protected List<Consumes> getConsumes(Endpoint endpoint) {
            List<Consumes> consumesList = new ArrayList<Consumes>();
            Consumes consumes;
            if (endpoint.getRole().equals(MessageExchange.Role.CONSUMER)) {
                consumes = new Consumes();
                ConsumerBeanEndpoint myEndpoint = (ConsumerBeanEndpoint) endpoint;
                consumes.setEndpointName(myEndpoint.getEndpoint());
                consumes.setInterfaceName(myEndpoint.getInterfaceName());
                consumes.setServiceName(myEndpoint.getService());
                consumes.setLinkType(2);
                if (consumes.isValid())
                    consumesList.add(consumes);
                else {
                    consumes = new Consumes();
                    consumes.setEndpointName(endpoint.getEndpoint());
                    consumes.setInterfaceName(endpoint.getInterfaceName());
                    consumes.setServiceName(endpoint.getService());
                    consumes.setLinkType(1);
                    consumesList.add(consumes);
                }
            }
    
            return consumesList;
        }                                       
    
        protected String getXBeanFile() {
            return "xbean.xml";
        }
    
        protected boolean isValidEndpoint(Object bean) {
            return bean instanceof ConsumerBeanEndpoint || bean instanceof ProviderBeanEndpoint;
        }
    
    }
    
    public static class MyDeployer extends AbstractXBeanDeployer {

        public MyDeployer(ServiceMixComponent component) {
            super(component);
        }
        
    }

    protected static class MyComponent extends DefaultComponent {
        public MyComponent() {
            this.container = new Container.Smx3Container(null);
        }
    }
    
    protected String getServiceUnitPath(String name) {
        URL url = getClass().getClassLoader().getResource(name + "/xbean.xml");
        File path = new File(url.getFile());
        path = path.getParentFile();
        return path.getAbsolutePath();
    }
            
}
