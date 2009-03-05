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
package org.apache.servicemix.camel.osgi;

import org.springframework.osgi.context.BundleContextAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.apache.servicemix.camel.JbiComponent;
import org.apache.servicemix.camel.CamelComponent;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 */
public class OsgiJbiComponent extends JbiComponent implements BundleContextAware, InitializingBean, DisposableBean {

    private BundleContext bundleContext;
    private ServiceReference reference;

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void afterPropertiesSet() throws Exception {
        reference = bundleContext.getServiceReference(CamelComponent.class.getName());
        if (reference == null) {
            throw new IllegalStateException(CamelComponent.class.getName() + " not found in the OSGi registry");
        }
        CamelComponent component = (CamelComponent) bundleContext.getService(reference);
        setCamelJbiComponent(component);
    }

    public void destroy() throws Exception {
        if (reference != null) {
            bundleContext.ungetService(reference);
        }
    }
}
