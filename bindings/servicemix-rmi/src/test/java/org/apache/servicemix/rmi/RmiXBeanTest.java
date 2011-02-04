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
package org.apache.servicemix.rmi;

import java.io.File;
import java.rmi.registry.LocateRegistry;

import junit.framework.TestCase;

import org.apache.servicemix.jbi.container.JBIContainer;
import org.springframework.core.io.ClassPathResource;

/**
 * <p>
 * Test the RMI xbean deployment.
 * </p>
 * 
 * @author jbonofre
 */
public class RmiXBeanTest extends TestCase {
    
    protected JBIContainer container;
    protected RmiComponent component;
    
    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        // create and start the JBI container
        container = new JBIContainer();
        container.setRmiPort(1099);
        container.setUseMBeanServer(true);
        container.setCreateMBeanServer(true);
        container.setEmbedded(true);
        container.init();
        
        // deploy the RMI component
        component = new RmiComponent();
        // activate the RMI component into the JBI container
        container.activateComponent(component, "RMIComponent");
        
        // start the JBI container
        container.start();
    }
    
    /**
     * <p>
     * Deploy a target xbean into the JBI container.
     * </p>
     * 
     * @param xbean the xbean.xml file path.
     * @throws Exception in case of xbean deployment error.
     */
    private void deployXBean(String xbean) throws Exception {    
        // deploy the SU based on the xbean.xml
        File path = new ClassPathResource(xbean).getFile();
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("xbean", path.getAbsolutePath());
        component.getServiceUnitManager().init("xbean", path.getAbsolutePath());
        component.getServiceUnitManager().start("xbean");
    }
    
    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        // stop the JBI container
        if (container != null) {
            container.shutDown();
        }
    }
    
    /**
     * <p>
     * Test if endpoints defined in the xbean have been deployed.
     * </p>
     */
    public void testDeployment() {
        // test if the SUs have been deployed
        //assertNotNull("RMI endpoint {http://servicemix.apache.org/test}RmiTestServiceSimpleConsumer is not found in the JBI container.", container.getRegistry().getEndpoint(new QName("http://servicemix.apache.org/test", "RmiTestService"), "SimpleConsumer"));
        //assertNotNull("RMI endpoint {http://servicemix.apache.org/test}RmiTestServiceByPassConsumer is not found in the JBI container.", container.getRegistry().getEndpoint(new QName("http://servicemix.apache.org/test", "RmiTestService"), "ByPassConsumer"));
    }
    
    public void testSimpleConsumerExchange() throws Exception {
        this.deployXBean("xbean/xbean-consumer.xml");
        java.rmi.registry.Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        Echo stub = (Echo) registry.lookup("SimpleConsumer");
        String echo = stub.echo("test");
        assertEquals("Bad response from the RMI endpoint", "test", echo);
    }

    /**
     * <p>
     * Test the by pass consumer endpoint.
     * </p>
     * 
     * @throws Exception in case of exchange failure.
     */
    public void testByPassConsumerExchange() throws Exception {
        this.deployXBean("xbean/xbean-consumer.xml");
        java.rmi.registry.Registry registry = LocateRegistry.getRegistry("localhost", 1099);
        Echo stub = (Echo) registry.lookup("ByPassConsumer");
        String echo = stub.echo("test");
        assertEquals("Bad response from the RMI endpoint", "test", echo);
    }
    
    /**
     * <p>
     * Test the simple provider endpoint.
     * </p>
     * 
     * @throws Exception in case of exchange failure.
     */
    public void testSimpleProviderExchange() throws Exception {
        this.deployXBean("xbean/xbean-provider.xml");
    }

}
