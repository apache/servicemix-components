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
package org.apache.servicemix.http;

import junit.framework.TestCase;
import org.w3c.dom.DocumentFragment;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.InstallationContext;
import javax.jbi.management.MBeanNames;
import javax.management.*;
import java.util.List;

import static org.easymock.EasyMock.*;

/**
 * Test to ensure HttpBootstrap MBean registration works correct on containers that alter the MBean ObjectName
 * (e.g. IBM WebSphere)
 */
public class HttpBootstrapTest extends TestCase {

    private HttpBootstrap bootstrap;
    private ObjectName originalName;
    private ObjectName registeredName;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        bootstrap = new HttpBootstrap();
        originalName = new ObjectName("test:Type=bootstrap");
        registeredName = new ObjectName("test:Type=bootstrap,process=process1,node=node1,cell=cell1");
    }

    public void testInitMBeanNameChanging() throws JBIException {
        bootstrap.init(new MockInstallationContext());
        assertEquals("HttpBootstrap should return the actual object name (including container-specific extra elements)",
                     registeredName, bootstrap.getExtensionMBeanName());
    }

    private final class MockInstallationContext implements InstallationContext {

        public String getComponentClassName() {
            return null;  // just a mock object - no need to implement this
        }

        public List getClassPathElements() {
            return null;  // just a mock object - no need to implement this
        }

        public String getComponentName() {
            return null;  // just a mock object - no need to implement this
        }

        public ComponentContext getContext() {
            ComponentContext context = createMock(ComponentContext.class);

            // Set up the mock MBeanNames to return the original ObjectName
            MBeanNames names = createMock(MBeanNames.class);
            expect(context.getMBeanNames()).andReturn(names);
            expect(names.createCustomComponentMBeanName("bootstrap")).andReturn(originalName);

            // Set up the mock MBeanServer to return the altered ObjectName after registration
            MBeanServer server = createMock(MBeanServer.class);
            expect(context.getMBeanServer()).andReturn(server);
            try {
                expect(server.isRegistered(originalName)).andReturn(false);
                expect(server.registerMBean(isA(HttpConfiguration.class), eq(originalName))).andReturn(new ObjectInstance(registeredName, null));
            } catch (Exception e) {
                fail("Exception occured while setting up the test: " + e.getMessage());
            }

            replay(context, names, server);

            return context;
        }

        public String getInstallRoot() {
            return null;  // just a mock object - no need to implement this
        }

        public DocumentFragment getInstallationDescriptorExtension() {
            return null;  // just a mock object - no need to implement this
        }

        public boolean isInstall() {
            return false;  // just a mock object - no need to implement this
        }

        public void setClassPathElements(List list) {
            // just a mock object - no need to implement this
        }
    }
}
