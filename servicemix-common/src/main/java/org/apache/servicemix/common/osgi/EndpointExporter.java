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

import org.apache.servicemix.common.Endpoint;
import org.osgi.framework.BundleContext;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;

public class EndpointExporter implements BundleContextAware, ApplicationContextAware, InitializingBean {

    private BundleContext bundleContext;
    private ApplicationContext applicationContext;
    private Collection<Endpoint> endpoints;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void setEndpoints(Collection<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public void afterPropertiesSet() throws Exception {
        Collection<Endpoint> eps = this.endpoints;
        if (eps == null) {
            eps = this.applicationContext.getBeansOfType(Endpoint.class).values();
        }
        for (Endpoint ep : eps) {
            EndpointWrapper wrapper = new EndpointWrapperImpl(ep, applicationContext.getClassLoader());
            Dictionary props = new Properties();
            bundleContext.registerService(EndpointWrapper.class.getName(), wrapper, props);
        }
    }

}
