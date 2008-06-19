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
package org.apache.servicemix.common.xbean;

import java.io.File;
import java.net.URL;

import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.ServiceMixComponent;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.xbean.classloader.JarFileClassLoader;

import junit.framework.TestCase;

public class XBeanDeployerTest extends TestCase {

    protected void setUp() throws Exception {
        
    }
    
    protected void tearDown() throws Exception {
        
    }
    
    public void testDeployWithProperties() throws Exception {
        MyXBeanDeployer deployer = new MyXBeanDeployer(new DefaultComponent());
        ServiceUnit su = deployer.deploy("xbean", getServiceUnitPath("xbean"));
        assertNotNull(su);
        assertEquals(1, su.getEndpoints().size());
        XBeanEndpoint ep = (XBeanEndpoint) su.getEndpoints().iterator().next();
        assertEquals("value", ep.getProp());
    }
    
    public void testDeployWithClasspathXml() throws Exception {
        MyXBeanDeployer deployer = new MyXBeanDeployer(new DefaultComponent() { });
        ServiceUnit su = deployer.deploy("xbean-cp", getServiceUnitPath("xbean-cp"));
        assertNotNull(su);
        ClassLoader cl = su.getConfigurationClassLoader();
        assertNotNull(cl);
        assertTrue(cl instanceof JarFileClassLoader);
        assertEquals(2, ((JarFileClassLoader) cl).getURLs().length);
        assertNotNull(cl.getResource("test.xml"));
    }
    
    public void testDeployWithInlineClasspath() throws Exception {
        MyXBeanDeployer deployer = new MyXBeanDeployer(new DefaultComponent() { });
        ServiceUnit su = deployer.deploy("xbean-inline", getServiceUnitPath("xbean-inline"));
        assertNotNull(su);
        ClassLoader cl = su.getConfigurationClassLoader();
        assertNotNull(cl);
        assertTrue(cl instanceof JarFileClassLoader);
        assertEquals(2, ((JarFileClassLoader) cl).getURLs().length);
        assertNotNull(cl.getResource("test.xml"));
    }
    
    public void testDeployWithDefaultClasspath() throws Exception {
        MyXBeanDeployer deployer = new MyXBeanDeployer(new DefaultComponent() { });
        ServiceUnit su = deployer.deploy("xbean-lib", getServiceUnitPath("xbean-lib"));
        assertNotNull(su);
        ClassLoader cl = su.getConfigurationClassLoader();
        assertNotNull(cl);
        assertTrue(cl instanceof JarFileClassLoader);
        assertEquals(2, ((JarFileClassLoader) cl).getURLs().length);
        assertNotNull(cl.getResource("test.xml"));
    }
    
    public static class MyXBeanDeployer extends AbstractXBeanDeployer {

        public MyXBeanDeployer(ServiceMixComponent component) {
            super(component);
        }
        
    }
    
    protected String getServiceUnitPath(String name) {
        URL url = getClass().getClassLoader().getResource(name + "/xbean.xml");
        File path = new File(url.getFile());
        path = path.getParentFile();
        return path.getAbsolutePath();
    }
}
