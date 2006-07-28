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
package org.apache.servicemix.eip;

import java.io.File;
import java.net.URI;
import java.net.URL;

import org.apache.servicemix.jbi.container.JBIContainer;

import junit.framework.TestCase;

public class DeploymentTest extends TestCase {

    protected JBIContainer container;
    
    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        container.setEmbedded(true);
        container.init();
    }
    
    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
    }
    
    public void testDeployer() throws Exception {
        EIPComponent component = new EIPComponent();
        container.activateComponent(component, "EIPComponent");

        // Start container
        container.start();

        // Deploy SU
        URL url = getClass().getClassLoader().getResource("su/xbean.xml");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("su", path.getAbsolutePath());
        component.getServiceUnitManager().start("su");
        
        component.getServiceUnitManager().stop("su");
        component.getServiceUnitManager().shutDown("su");
        component.getServiceUnitManager().undeploy("su", path.getAbsolutePath());
    }
    
}
