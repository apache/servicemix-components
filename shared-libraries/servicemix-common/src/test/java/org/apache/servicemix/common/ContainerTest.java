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
package org.apache.servicemix.common;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;

import junit.framework.TestCase;
import org.apache.servicemix.common.osgi.Configuration;
import org.apache.servicemix.executors.impl.ExecutorFactoryImpl;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.easymock.EasyMock;

/**
 * Test cases for {@link Container}
 */
public class ContainerTest extends TestCase {
    
    public void testServiceMix3() throws JBIException {
        JBIContainer jbi = new JBIContainer();
        jbi.init();
        jbi.activateComponent(new EPRTest.DummyComponent(), "dummy");

        Container container = new Container.Smx3Container(jbi.getComponent("dummy").getContext());
        assertSame("Should use the ServiceMix 3 container executor factory",
                   jbi.getExecutorFactory(), container.getExecutorFactory());

        jbi.shutDown();
    }

    public void testServiceMix4() {
        final Configuration config = Configuration.getInstance();
        config.setCorePoolSize(10);
        config.setMaximumPoolSize(50);

        Container container = new Container.Smx4Container(EasyMock.createNiceMock(ComponentContext.class));
        final ExecutorFactoryImpl factory = (ExecutorFactoryImpl) container.getExecutorFactory();
        assertNotNull(factory);

        assertEquals("Executor config should have been copied from OSGi Configuration instance",
                     new Integer(10), factory.getDefaultConfig().getCorePoolSize());
        assertEquals("Executor config should have been copied from OSGi Configuration instance",
                     new Integer(50), factory.getDefaultConfig().getMaximumPoolSize());
    }

    public void testUnknown() {
        Container container = new Container.UnknownContainer(EasyMock.createNiceMock(ComponentContext.class));
        assertNotNull("Ensure that even an unknown container provides an executor factory",
                      container.getExecutorFactory());
    }
}
