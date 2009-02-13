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

import javax.jbi.management.DeploymentException;

import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.jbi.deployer.DeployedAssembly;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;

public class EndpointExporter implements BundleContextAware, ApplicationContextAware, InitializingBean, DisposableBean, DeployedAssembly {

    private BundleContext bundleContext;
    private ApplicationContext applicationContext;
    private Collection<Endpoint> endpoints;
    private String assemblyName;
    private Collection<ServiceRegistration> endpointRegistrations;

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

    public void deploy() {
        endpointRegistrations = new ArrayList<ServiceRegistration>();
        for (Endpoint ep : getEndpoints()) {
            EndpointWrapper wrapper = new EndpointWrapperImpl(ep, applicationContext.getClassLoader());
            Dictionary props = new Properties();
            ServiceRegistration reg = bundleContext.registerService(EndpointWrapper.class.getName(), wrapper, props);
            endpointRegistrations.add(reg);
        }
    }

    public Map<String, String> getServiceUnits() {
        if (endpointRegistrations == null) {
            throw new IllegalStateException("Service assembly has not been deployed");
        }
        Map<String, String> sus = new HashMap<String, String>();
        for (Endpoint ep : getEndpoints()) {
            if (ep.getServiceUnit() == null) {
                throw new IllegalStateException("Endpoint has not been initialized.  Check that the component is started.");
            }
            sus.put(ep.getServiceUnit().getName(), ep.getServiceUnit().getComponent().getComponentName());
        }
        return sus;
    }

    public void afterPropertiesSet() throws Exception {
        this.assemblyName = bundleContext.getBundle().getSymbolicName();
        bundleContext.registerService(DeployedAssembly.class.getName(), this, new Properties());
    }

    public void destroy() throws Exception {
        if (endpointRegistrations != null) {
            for (ServiceRegistration reg : endpointRegistrations) {
                reg.unregister();
            }
        }
    }
}
