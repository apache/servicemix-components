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
import java.util.Collection;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.ServiceSupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ServiceUnit;

/**
 * Test cases to ensure that {@link org.apache.servicemix.camel.CamelContextEndpoint} correctly
 * manages the lifecycle of the underlying {@link org.apache.camel.CamelContext}
 */
public class CamelContextEndpointLifeCycleTest extends NonJbiCamelEndpointsIntegrationTest {
    private static final transient Log LOG = LogFactory.getLog(CamelContextEndpointLifeCycleTest.class);

    public void testComponentInstallation() throws Exception {
        String serviceUnitConfiguration = suName + "-src/camel-context.xml";

        CamelJbiComponent component = new CamelJbiComponent();
        container.activateComponent(component, "#ServiceMixComponent#");
        URL url = getClass().getResource(serviceUnitConfiguration);
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        try {
            // Deploy and start su
            component.getServiceUnitManager().deploy(suName, path.getAbsolutePath());
            component.getServiceUnitManager().init(suName, path.getAbsolutePath());
            component.getServiceUnitManager().start(suName);

            ServiceUnit su = component.getRegistry().getServiceUnit(suName);
            Collection<Endpoint> endpoints = su.getEndpoints();
            assertEquals("There should have one Endpoint", 1, endpoints.size());
            Endpoint endpoint = endpoints.iterator().next();
            assertTrue("It should be CamelContextEndpoint", endpoint instanceof CamelContextEndpoint);
            CamelContext camelContext = ((CamelContextEndpoint)endpoint).getCamelContext();
            // check the CamelContextEndpoint status
            assertTrue("The CamelContext should be started", ((ServiceSupport)camelContext).isStarted());
            // Stop
            component.getServiceUnitManager().stop(suName);
            // check the CamelContextEndpoint status
            assertTrue("The CamelContext should be stopped", ((ServiceSupport)camelContext).isStopped());
            // reStart
            component.getServiceUnitManager().start(suName);
            assertTrue("The CamelContext should be started", ((ServiceSupport)camelContext).isStarted());

            component.getServiceUnitManager().stop(suName);
            component.getServiceUnitManager().shutDown(suName);
            component.getServiceUnitManager().undeploy(suName, path.getAbsolutePath());
        } catch (Exception e) {
            LOG.error("Caught: " + e, e);
            throw e;
        }
    }
}
