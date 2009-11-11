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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.camel.CamelJbiComponent;
import org.apache.servicemix.camel.CamelSpringDeployer;
import org.apache.servicemix.common.BaseServiceUnitManager;
import org.apache.servicemix.common.Deployer;
import org.apache.servicemix.common.ServiceMixComponent;
import org.apache.servicemix.common.xbean.ClassLoaderXmlPreprocessor;
import org.apache.xbean.spring.context.SpringApplicationContext;
import org.apache.xbean.classloader.JarFileClassLoader;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.osgi.context.BundleContextAware;
import org.springframework.osgi.util.BundleDelegatingClassLoader;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Bundle;

/**
 * When deploying a JBI packaged SU to camel component, camel-spring and camel-osgi 
 * can not be found by Spring/XBean, thus leading to an exception about the spring 
 * and osgi namespaces not being found.  We need to hack the classloader for SUs to 
 * force a reference to camel-spring, camel-osgi and camel-cxf in the SU classloader parents.
 *
 * We also need to inject the bundleContext into the CamelContextFactoryBean to 
 * make sure the CamelContextFactoryBean will replace the regular ResolverUtils 
 * with OSGi ResolverUtils
 */
public class OsgiCamelJbiComponent extends CamelJbiComponent implements BundleContextAware {
    
    private static final Log LOG = LogFactory.getLog(OsgiCamelJbiComponent.class);
    private BundleContext bundleContext;

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
    
    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public BaseServiceUnitManager createServiceUnitManager() {
        CamelSpringDeployer deployer = new OsgiCamelSpringDeployer(this);
        return new BaseServiceUnitManager(this, new Deployer[] {deployer});
    }

    @SuppressWarnings("unchecked")
    public class OsgiCamelSpringDeployer extends CamelSpringDeployer {
        
        public OsgiCamelSpringDeployer(OsgiCamelJbiComponent component) {
            super(component);
        }
        
        protected List getXmlPreProcessors(String serviceUnitRootPath) {
            ClassLoaderXmlPreprocessor classLoaderXmlPreprocessor =
                    new OsgiClassLoaderXmlPreprocessor(new File(serviceUnitRootPath),
                                                       component);
            return Collections.singletonList(classLoaderXmlPreprocessor);
        }
        
        protected List getBeanFactoryPostProcessors(String serviceUnitRootPath) {
            List processors = super.getBeanFactoryPostProcessors(serviceUnitRootPath);
            // add the post processors to deal with camel context
            // Parent beans map
            
            processors.add(new OsgiBundleContextPostprocessor());
            
            return processors;
        }
    }
    
    private class OsgiBundleContextPostprocessor implements BeanFactoryPostProcessor {

        public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
            beanFactory.addBeanPostProcessor(new BeanPostProcessor() {

                public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
                    // do nothing here
                    return bean;
                }

                public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                    if (bean instanceof BundleContextAware) {
                        BundleContextAware bundleContextAware = (BundleContextAware)bean;
                        if (bundleContext == null) {
                            LOG.warn("No bundle defined yet so cannot inject into: " + bean);
                        } else {
                            bundleContextAware.setBundleContext(bundleContext);
                        }
                    }
                    return bean;
                }
            });
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
                    String symbolicName = bundle.getSymbolicName();
                    if (symbolicName.contains("camel-spring") || symbolicName.contains("camel-osgi") || symbolicName.contains("camel-cxf")) {
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
