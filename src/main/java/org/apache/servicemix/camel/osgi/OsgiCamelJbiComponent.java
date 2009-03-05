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

import java.util.List;
import java.util.Collections;
import java.util.ArrayList;
import java.io.File;
import java.net.URL;

import org.apache.servicemix.camel.CamelJbiComponent;
import org.apache.servicemix.camel.CamelSpringDeployer;
import org.apache.servicemix.common.BaseServiceUnitManager;
import org.apache.servicemix.common.Deployer;
import org.apache.servicemix.common.ServiceMixComponent;
import org.apache.servicemix.common.xbean.ClassLoaderXmlPreprocessor;
import org.apache.xbean.spring.context.SpringApplicationContext;
import org.apache.xbean.classloader.JarFileClassLoader;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.util.BundleDelegatingClassLoader;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;

/**
 * When deploying a JBI packaged SU to camel component, camel-spring can not be found
 * by Spring/XBean, thus leading to an exception about the spring namespace not being
 * found.  We need to hack the clasloader for SUs to force a reference to camel-spring
 * in the SU classloader parents.
 *
 * This does not completely solve the problem, as converters can not be found because
 * camel-core ResolverUtil does not work well
 */
public class OsgiCamelJbiComponent extends CamelJbiComponent implements BundleContextAware {

    private BundleContext bundleContext;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public BaseServiceUnitManager createServiceUnitManager() {
        CamelSpringDeployer deployer = new OsgiCamelSpringDeployer(this);
        return new BaseServiceUnitManager(this, new Deployer[] {deployer});
    }

    public class OsgiCamelSpringDeployer extends CamelSpringDeployer {
        public OsgiCamelSpringDeployer(CamelJbiComponent component) {
            super(component);
        }
        protected List getXmlPreProcessors(String serviceUnitRootPath) {
            ClassLoaderXmlPreprocessor classLoaderXmlPreprocessor =
                    new OsgiClassLoaderXmlPreprocessor(new File(serviceUnitRootPath),
                                                       component);
            return Collections.singletonList(classLoaderXmlPreprocessor);
        }
    }

    public class OsgiClassLoaderXmlPreprocessor extends ClassLoaderXmlPreprocessor {
        public OsgiClassLoaderXmlPreprocessor(File root, ServiceMixComponent component) {
            super(root, component);
        }

        protected ClassLoader getParentClassLoader(SpringApplicationContext applicationContext) {
            List<ClassLoader> parents = new ArrayList<ClassLoader>();
            parents.add(super.getParentClassLoader(applicationContext));
            for (Bundle bundle : bundleContext.getBundles()) {
                try {
                    if (bundle.getSymbolicName().contains("camel-spring")) {
                        parents.add(BundleDelegatingClassLoader.createBundleClassLoaderFor(bundle));
                    }
                } catch (Throwable e) {
                    // Do nothing
                }
            }
            return new JarFileClassLoader("SU parent class loader",
                                          new URL[0],
                                          parents.toArray(new ClassLoader[parents.size()]));
        }
    }

}
