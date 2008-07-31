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
package org.apache.servicemix.common.osgi;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jbi.management.DeploymentException;

import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.id.IdGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class EndpointTracker {

    private static final Log LOGGER = LogFactory.getLog(EndpointTracker.class);

    protected DefaultComponent component;
    protected Map<EndpointWrapper, OsgiServiceUnit> endpoints = new ConcurrentHashMap<EndpointWrapper, OsgiServiceUnit>();

    public DefaultComponent getComponent() {
        return component;
    }

    public void setComponent(DefaultComponent component) {
        this.component = component;
    }

    public void register(EndpointWrapper wrapper, Map properties) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[" + component.getComponentName() + "] Endpoint registered with properties: " + properties);
        }
        Endpoint endpoint = wrapper.getEndpoint();
        if (component.isKnownEndpoint(endpoint)) {
            if (LOGGER.isDebugEnabled()) {
    	        LOGGER.debug("[" + component.getComponentName() + "] Endpoint recognized");
            }
            OsgiServiceUnit su = new OsgiServiceUnit(component, endpoint, wrapper.getClassLoader());
            endpoints.put(wrapper, su);
            component.getRegistry().registerServiceUnit(su);
            if (component.isStarted()) {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(su.getConfigurationClassLoader());
                    su.start();
                } finally {
                    Thread.currentThread().setContextClassLoader(cl);
                }
            }

        }
    }

    public void unregister(EndpointWrapper wrapper, Map properties) throws Exception {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("[" + component.getComponentName() + "] Endpoint unregistered with properties: " + properties);
        }
        // Do not access the wrapper using wrapper.getEndpoint(), has the osgi context may already be shut down
        OsgiServiceUnit su = endpoints.remove(wrapper);
        if (su != null && component.isKnownEndpoint(su.getEndpoint())) {
            if (LOGGER.isDebugEnabled()) {
    	        LOGGER.debug("[" + component.getComponentName() + "] Endpoint recognized");
            }
            try {
                ClassLoader cl = Thread.currentThread().getContextClassLoader();
                try {
                    Thread.currentThread().setContextClassLoader(su.getConfigurationClassLoader());
                    su.stop();
                } finally {
                    Thread.currentThread().setContextClassLoader(cl);
                }
            } finally {
                component.getRegistry().unregisterServiceUnit(su);
            }
        }
    }

    public static class OsgiServiceUnit extends ServiceUnit {
        private static final IdGenerator idGenerator = new IdGenerator();
        private final Endpoint endpoint;
        private final ClassLoader classLoader;
        public OsgiServiceUnit(DefaultComponent component, Endpoint endpoint, ClassLoader classLoader) throws DeploymentException {
            this.component = component;
            this.name = idGenerator.generateSanitizedId();
            this.endpoint = endpoint;
            this.classLoader = classLoader;
            this.endpoint.setServiceUnit(this);
            this.endpoint.validate();
            addEndpoint(this.endpoint);
        }
        public Endpoint getEndpoint() {
            return endpoint;
        }
        public ClassLoader  getConfigurationClassLoader() {
            if (classLoader != null) {
                return classLoader;
            } else {
                return super.getConfigurationClassLoader();
            }
        }
    }

}
