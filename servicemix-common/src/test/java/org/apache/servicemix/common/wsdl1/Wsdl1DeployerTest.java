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
package org.apache.servicemix.common.wsdl1;

import java.io.File;
import java.net.URL;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchange.Role;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.xml.WSDLReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.common.ServiceMixComponent;
import org.apache.servicemix.common.endpoints.AbstractEndpoint;

import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.Container;

import junit.framework.TestCase;

public class Wsdl1DeployerTest extends TestCase {

    private static transient Log logger =  LogFactory.getLog(Wsdl1DeployerTest.class);
        
    // Test to Deploy Service Unit
    public void testDeploy() throws Exception {
    	
    	MyWsdl1Deployer deployer = new MyWsdl1Deployer(new MyComponent() { });
    	boolean candeploy = deployer.canDeploy("xbean-wsdl", getServiceUnitPath("org/apache/servicemix/common/wsdl1/xbean-wsdl"));
    	assertTrue(candeploy);
    	ServiceUnit su = deployer.deploy("xbean-wsdl", getServiceUnitPath("org/apache/servicemix/common/wsdl1/xbean-wsdl"));
        assertNotNull(su);    
        assertNotNull(su.getEndpoints());
        assertEquals(su.getName(), "xbean-wsdl");
    }
    
    // Test to Deploy Service Unit with no endpoint
    public void testEndpointDeploy() throws Exception {
    	
    	MyWsdl1EndpointDeployer deployer = new MyWsdl1EndpointDeployer(new MyComponent() { });
    	boolean candeploy = deployer.canDeploy("xbean-wsdl", getServiceUnitPath("org/apache/servicemix/common/wsdl1/xbean-wsdl"));
    	assertTrue(candeploy);
    	try {
    	    ServiceUnit su = deployer.deploy("xbean-wsdl", getServiceUnitPath("org/apache/servicemix/common/wsdl1/xbean-wsdl"));
    	    fail();
    	} catch (DeploymentException ex) {
    	     // Deploy - Invalid wsdl: no endpoints found
    	}        
    }
    
    public void testNoWsdlDeploy() throws Exception {
    	
    	MyWsdl1Deployer deployer = new MyWsdl1Deployer(new MyComponent() { });
    	boolean candeploy = deployer.canDeploy("xbean-inline", getServiceUnitPath("xbean-inline"));
    	assertFalse(candeploy);
    	try {
    	    ServiceUnit su = deployer.deploy("xbean-inline", getServiceUnitPath("xbean-inline"));
    	    fail();
    	} catch (DeploymentException ex) {
    		// Should catch this exception - No wsdl found 
    	}           	    	    
    }      
    
    public void testInvalidWsdlDeploy() throws Exception {
    	
    	MyWsdl1Deployer deployer = new MyWsdl1Deployer(new MyComponent() { });
    	boolean candeploy = deployer.canDeploy("xbean-wsdl-invalid", getServiceUnitPath("org/apache/servicemix/common/wsdl1/xbean-wsdl-invalid"));
    	assertTrue(candeploy);
    	try {
    	    ServiceUnit su = deployer.deploy("xbean-wsdl-invalid", getServiceUnitPath("org/apache/servicemix/common/wsdl1/xbean-wsdl-invalid"));
    	    fail();
    	} catch (Exception e) {
    		// "Deploy - Could not parse wsdl File"
        }            	    	    
    }
    
    public void testWsdlNoServiceDeploy() throws Exception {
    	
    	MyWsdl1Deployer deployer = new MyWsdl1Deployer(new MyComponent() { });
    	try {
    	    ServiceUnit su = deployer.deploy("xbean-wsdl-noservice", getServiceUnitPath("org/apache/servicemix/common/wsdl1/xbean-wsdl-noservice"));
    	    fail();
    	} catch (Exception e) {
    		// "Deploy - no defined services"
        }            	    	    
    }
    
    public void testWsdlNoBindingDeploy() throws Exception {
    	
    	MyWsdl1Deployer deployer = new MyWsdl1Deployer(new MyComponent() { });
    	try {
    	    ServiceUnit su = deployer.deploy("xbean-wsdl-nobinding", getServiceUnitPath("org/apache/servicemix/common/wsdl1/xbean-wsdl-nobinding"));
    	    fail();
    	} catch (Exception e) {
    		// "Deploy - no matching binding element found"
        }            	    	    
    }
    
    public void testWsdlMultiplePorts() throws Exception {
    	
    	MyWsdl1Deployer deployer = new MyWsdl1Deployer(new MyComponent() { });
    	try {
    	    ServiceUnit su = deployer.deploy("xbean-wsdl-ports", getServiceUnitPath("org/apache/servicemix/common/wsdl1/xbean-wsdl-ports"));
    	    fail();
    	} catch (Exception e) {
    		// "Deploy - more than one port element match"
        }            	    	    
    }
    
    public void testWsdlMultipleBindings() throws Exception {
    	
    	MyWsdl1Deployer deployer = new MyWsdl1Deployer(new MyComponent() { });
    	try {
    	    ServiceUnit su = deployer.deploy("xbean-wsdl-bindings", getServiceUnitPath("org/apache/servicemix/common/wsdl1/xbean-wsdl-bindings"));
    	    fail();
    	} catch (Exception e) {
    		// "Deploy - more than one binding element match"
        }            	    	    
    }            

    protected String getServiceUnitPath(String name) {
        URL url = getClass().getClassLoader().getResource(name + "/xbean.xml");    	
        File path = new File(url.getFile());
        path = path.getParentFile();
        return path.getAbsolutePath();
    }
    
    public class MyWsdl1Deployer extends AbstractWsdl1Deployer {
    	
    	WSDLReader reader = null;
    	
    	public MyWsdl1Deployer(ServiceMixComponent component) {
    		super(component);
    		try {
    	         reader = createWsdlReader();    	        
    		} catch (WSDLException ex) {
    			//
    		}
    	}    	    	    	
    	
    	protected MyEndpoint createEndpoint(ExtensibilityElement portElement,
                ExtensibilityElement bindingElement,
                JbiEndpoint jbiEndpoint) {
    	     return new MyEndpoint();
    	}

        protected boolean filterPortElement(ExtensibilityElement element) {
            return true;
        }

        protected boolean filterBindingElement(ExtensibilityElement element) {
            return true;	
        }

    }
    
    public class MyWsdl1EndpointDeployer extends MyWsdl1Deployer {
        WSDLReader reader = null;
    	
    	public MyWsdl1EndpointDeployer(ServiceMixComponent component) {
    		super(component);
    		try {
    	         reader = createWsdlReader();    	        
    		} catch (WSDLException ex) {
    			//
    		}
    	}    	    	    	
    	
    	protected MyEndpoint createEndpoint(ExtensibilityElement portElement,
                ExtensibilityElement bindingElement,
                JbiEndpoint jbiEndpoint) {
    	     return null;
    	}
    }
    
    protected static class MyComponent extends DefaultComponent {
        public MyComponent() {
            this.container = new Container.Smx3Container(null);
        }
    }
    
    public class MyEndpoint extends AbstractEndpoint {
    	
    	public  Role getRole() {
    		return javax.jbi.messaging.MessageExchange.Role.PROVIDER;
    	}
    	
    	public void activate() throws Exception {
    		
    	}

        public void start() throws Exception {
        	
        }

        public void stop() throws Exception {
        	
        }

        public void deactivate() throws Exception {
        
        }

        public void process(MessageExchange exchange) throws Exception {
        
        }
    	
    }
    
}
