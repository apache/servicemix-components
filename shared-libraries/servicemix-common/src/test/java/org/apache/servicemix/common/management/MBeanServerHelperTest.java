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
package org.apache.servicemix.common.management;

import org.junit.Before;
import org.junit.Test;

import javax.jbi.JBIException;
import javax.management.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Test cases for {@link MBeanServerHelper}
 */
public class MBeanServerHelperTest {

    private MBeanServer server = MBeanServerFactory.createMBeanServer();
    private ObjectName defaultObjectName;
    private ObjectName alteredObjectName;

    @Before
    public void setUp() throws Exception {
        defaultObjectName = new ObjectName("test:Type=bean");
        alteredObjectName = new ObjectName("test:Type=bean,node=node1,process=process1,cell=cell1");
    }

    @Test(expected = JBIException.class)
    public void testRegisterNullServerThrowsJBIException() throws NotCompliantMBeanException, MalformedObjectNameException, JBIException {
        SampleMBean mbean = new SampleMBeanImpl();
        Object object = new StandardMBean(mbean, SampleMBean.class);

        MBeanServerHelper.register(null, defaultObjectName, object);
    }

    @Test(expected = JBIException.class)
    public void testUnregisterNullServerThrowsJBIException() throws Exception {
        MBeanServerHelper.unregister(null, defaultObjectName);
    }

    public void testUnregisterNullNameWithoutException() throws Exception {
        MBeanServerHelper.unregister(server, null);
    }

    @Test
    public void testRegisterWithDefaultName() throws Exception {
        SampleMBean mbean = new SampleMBeanImpl();
        Object object = new StandardMBean(mbean, SampleMBean.class);

        assertEquals(defaultObjectName, MBeanServerHelper.register(server, defaultObjectName, object));

        // let's try a subsequent registration as well - this should not be throwing an exception
        assertEquals(defaultObjectName, MBeanServerHelper.register(server, defaultObjectName, object));

        MBeanServerHelper.unregister(server, defaultObjectName);
        assertFalse(server.isRegistered(defaultObjectName));
    }

    @Test
    public void testRegisterWithContainerDefinedName() throws Exception {
        SampleMBean mbean = new SampleMBeanImpl();
        Object object = new RenamingStandardMBean(mbean, SampleMBean.class);

        assertEquals(alteredObjectName, MBeanServerHelper.register(server, defaultObjectName, object));

        // let's try a subsequent registration as well - this should not be throwing an exception
        assertEquals(alteredObjectName, MBeanServerHelper.register(server, defaultObjectName, object));

        MBeanServerHelper.unregister(server, alteredObjectName);
        assertFalse(server.isRegistered(alteredObjectName));
    }

    /*
    * MBean interface definition used for testing
    */
    public static interface SampleMBean {

        void doSomething();

    }

    /*
     * MBean implementation
     */
    public static final class SampleMBeanImpl implements SampleMBean {

        public void doSomething() {
            // graciously do nothing here
        }
    }

    /*
    * {@link StandardMBean} implementation that will provide an alternate MBean name upon registration
    * (similar to what e.g. WebSphere does when it appends cell/node/process information)
    */
    public final class RenamingStandardMBean extends StandardMBean implements MBeanRegistration {


        public <T> RenamingStandardMBean(T object, Class<T> type) throws NotCompliantMBeanException {
            super(object, type);
        }


        public ObjectName preRegister(MBeanServer mBeanServer, ObjectName objectName) throws Exception {
            return alteredObjectName;
        }

        public void postRegister(Boolean aBoolean) {
            // graciously do nothing here
        }

        public void preDeregister() throws Exception {
            // graciously do nothing here
        }

        public void postDeregister() {
            // graciously do nothing here
        }
    }
}
