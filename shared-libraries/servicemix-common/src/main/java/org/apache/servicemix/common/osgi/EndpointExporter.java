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

import java.util.Dictionary;
import java.util.Properties;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.servicemix.common.Endpoint;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;

public class EndpointExporter implements BundleContextAware, ApplicationContextAware, InitializingBean, DisposableBean, DeployedAssembly {

    private final Logger logger = LoggerFactory.getLogger(EndpointExporter.class);

    private BundleContext bundleContext;
    private ApplicationContext applicationContext;
    private Collection<Endpoint> endpoints;
    private Set<Endpoint> deployed;
    private String assemblyName;
    private Collection<ServiceRegistration> endpointRegistrations;
    private ServiceRegistration assemblyRegistration;
    private Timer timer;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void setEndpoints(Collection<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public Collection<Endpoint> getEndpoints() {
        Collection<Endpoint> eps = this.endpoints;
        if (eps == null) {
            eps = this.applicationContext.getBeansOfType(Endpoint.class).values();
        }
        return eps;
    }

    public String getName() {
        return assemblyName;
    }

    public Map<String, String> getServiceUnits() {
        if (endpointRegistrations == null) {
            throw new IllegalStateException("Service assembly has not been deployed");
        }
        Map<String, String> sus = new HashMap<String, String>();
        for (Endpoint ep : getEndpoints()) {
            if (ep.getServiceUnit() == null) {
                // This should not happen, as we only register the SA after all endpoints have been deployed
                throw new IllegalStateException("Endpoint has not been initialized.  Check that the component is installed.");
            }
            sus.put(ep.getServiceUnit().getName(), ep.getServiceUnit().getComponent().getComponentName());
        }
        return sus;
    }

    public void undeploy(boolean restart) {
        if (endpointRegistrations != null) {
            for (ServiceRegistration reg : endpointRegistrations) {
                reg.unregister();
            }
            endpointRegistrations = null;
        }
        if (assemblyRegistration != null) {
            assemblyRegistration.unregister();
            assemblyRegistration = null;
        }
        if (restart) {
            deploy();
        }
    }

    public void deploy() {
        this.assemblyName = bundleContext.getBundle().getSymbolicName();
        endpointRegistrations = new ArrayList<ServiceRegistration>();
        deployed = new HashSet<Endpoint>();
        for (final Endpoint ep : getEndpoints()) {
            EndpointWrapper wrapper = new EndpointWrapperImpl(ep, applicationContext.getClassLoader()) {
                public void setDeployed() {
                    checkAndRegisterSA(ep);
                }
            };
            Dictionary props = new Properties();
            ServiceRegistration reg = bundleContext.registerService(EndpointWrapper.class.getName(), wrapper, props);
            if (reg != null) {
                endpointRegistrations.add(reg);
            }
        }
        if (assemblyRegistration == null) {
            logger.info("Waiting for all endpoints to be deployed before registering service assembly");
        }
    }

    protected synchronized void checkAndRegisterSA(Endpoint ep) {
        if (assemblyRegistration != null) {
            return;
        }
        if (ep != null && (ep.getServiceUnit() == null 
            || !ep.getServiceUnit().getComponent().getRegistry().isRegistered(ep.getServiceUnit()))) {
            logger.info("something wrong during register endpoint {}", ep.getKey());
            //get chance to unregister all endpoints with this EndpointExporter
            for (Endpoint e : deployed) {
                e.getServiceUnit().getComponent().getRegistry().unregisterServiceUnit(e.getServiceUnit());
            }
            return;
        }
        if (ep != null) {
            deployed.add(ep);
        }
        Collection<Endpoint> endpoints = getEndpoints();
        if (deployed.size() == endpoints.size()) {
            boolean initialized = true;
            for (Endpoint e : endpoints) {
                if (e.getServiceUnit().getComponent().getComponentContext() == null) {
                    initialized = false;
                    break;
                }
            }
            if (!initialized) {
                // Create the timer if not already done
                if (timer == null) {
                    timer = new Timer();
                    logger.info("All endpoints have been deployed but waiting for components initialization");
                }
                // Retry a bit later to allow some time for the components to be initialized
                // by the JBI container
                synchronized (this) {
                    timer.schedule(new TimerTask() {
                        public void run() {
                            checkAndRegisterSA(null);
                        }
                    }, 500);
                }
            } else {
                // Everything is ok, cancel the timer ...
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
                // ... and register the SA in OSGi
                logger.info("All endpoints have been deployed and components initialized. Registering service assembly.");
                assemblyRegistration = bundleContext.registerService(DeployedAssembly.class.getName(), this, new Properties());
            }
        }
    }

    public void afterPropertiesSet() {
        deploy();
    }

    public void destroy() throws Exception {
    }

}
