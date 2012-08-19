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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jbi.JBIException;
import javax.management.*;

/**
 * Helper methods to register and unregister objects with a JMX MBeanServer
 */
public class MBeanServerHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(MBeanServerHelper.class);

    /**
     * Registers an object with the MBean server using the specified object name.  If another object is already registered,
     * this method will first unregister it.  The method also returns the final object name after registration, because
     * some MBean servers will alter the name during the registration process
     *
     * @param server the MBean server
     * @param name the object name
     * @param object the object to be registered
     * @return the final object name after registration
     * @throws JBIException if the MBean server is null or if a problem occurs during MBean registration
     */
    public static ObjectName register(MBeanServer server, ObjectName name, Object object) throws JBIException {
        if (server == null) {
            throw new JBIException("MBeanServer is null when registering MBean " + name);
        }

        try {
            // unregister a previously existing MBean first
            doUnregister(server, name);

            LOGGER.debug("Registering MBean {}", name);
            ObjectInstance instance = doRegister(server, name, object);

            ObjectName result = instance.getObjectName();
            LOGGER.debug("Successfully registered MBean {}", result);

            return result;
        } catch (JMException e) {
            throw new JBIException("Exception occured while registering JMX MBean " + name, e);
        }
    }

    private static ObjectInstance doRegister(MBeanServer server, ObjectName name, Object object) throws JMException, JBIException {
        try {
            return server.registerMBean(object, name);
        } catch (InstanceAlreadyExistsException e) {
            ObjectName existing = new ObjectName(e.getMessage());

            if (existing.equals(name)) {
                throw e;
            } else {
                // if the server has another instance registered under a different name,
                // let's try unregistering that alternative name and retry registration afterwards
                LOGGER.debug("Existing MBean {} matches {} - unregistering it before continuing", existing, name);
                doUnregister(server, existing);

                return doRegister(server, name, object);
            }
        }
    }

    /**
     * Unregister the object with the specified name from the MBean server
     *
     * @param server the mbean server
     * @param name the object name
     *
     * @throws JBIException if the MBean server is null or if there is a problem unregistering the MBean
     */
    public static void unregister(MBeanServer server, ObjectName name) throws JBIException {
        if (name != null) {
            if (server == null) {
                throw new JBIException("MBeanServer is null when registering MBean " + name);
            }

            try {
                doUnregister(server, name);
            } catch (JMException e) {
                throw new JBIException("Unable to unregister object with name " + name, e);
            }
        }
    }

    private static void doUnregister(MBeanServer server, ObjectName name) throws JMException {
        if (server.isRegistered(name)) {
            LOGGER.debug("Unregistering MBean {}", name);
            server.unregisterMBean(name);
        }
    }
}
