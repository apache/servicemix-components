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
package org.apache.servicemix.camel;

import java.io.File;
import java.net.URI;
import java.net.URL;

import org.apache.camel.CamelContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TwoServicemixCamelSusTest extends NonJbiCamelEndpointsIntegrationTest {
    
    private static final transient Log LOG = LogFactory.getLog(TwoServicemixCamelSusTest.class);

    private void deploySu(CamelJbiComponent component, String suName) throws Exception {
        String serviceUnitConfiguration = suName + "-src/camel-context.xml";
        URL url = getClass().getResource(serviceUnitConfiguration);
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();

        // Deploy and start su
        component.getServiceUnitManager()
                .deploy(suName, path.getAbsolutePath());
        component.getServiceUnitManager().init(suName, path.getAbsolutePath());
        component.getServiceUnitManager().start(suName);
    }

    private void undeploySu(CamelJbiComponent component, String suName) throws Exception {
        String serviceUnitConfiguration = suName + "-src/camel-context.xml";
        URL url = getClass().getResource(serviceUnitConfiguration);
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();

        // Stop and undeploy
        component.getServiceUnitManager().stop(suName);
        component.getServiceUnitManager().shutDown(suName);
        component.getServiceUnitManager().undeploy(suName,
                path.getAbsolutePath());
    }

    public void testComponentInstallation() throws Exception {

        CamelJbiComponent component = new CamelJbiComponent();
        container.activateComponent(component, "#ServiceMixComponent#");

        // deploy two sus here
        deploySu(component, "su3");
        JbiComponent jbiComponent = component.getJbiComponent("su3");
        assertNotNull("JbiComponent should not be null ", jbiComponent);
        CamelContext su3CamelContext = jbiComponent.getCamelContext();
        assertNotNull("We should get a camel context here ", su3CamelContext);
        deploySu(component, "su6");
        jbiComponent = component.getJbiComponent("su6");
        assertNotNull("JbiComponent should not be null ", jbiComponent);
        CamelContext su6CamelContext = jbiComponent.getCamelContext();
        assertNotNull("We should get a camel context here ", su6CamelContext);
        assertTrue("Here should be two different camel contexts",
                !su3CamelContext.equals(su6CamelContext));

        // deploy two sus here
        undeploySu(component, "su3");
        undeploySu(component, "su6");

    }

}
