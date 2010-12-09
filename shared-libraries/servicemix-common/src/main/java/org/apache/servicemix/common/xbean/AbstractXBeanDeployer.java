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
package org.apache.servicemix.common.xbean;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jbi.management.DeploymentException;

import org.apache.servicemix.common.AbstractDeployer;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.EndpointComponentContext;
import org.apache.servicemix.common.ServiceMixComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.xbean.spring.context.FileSystemXmlApplicationContext;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.FileSystemResource;

public class AbstractXBeanDeployer extends AbstractDeployer {

    public AbstractXBeanDeployer(ServiceMixComponent component) {
        super(component);
    }
    
    protected String getXBeanFile() {
        return "xbean";
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.Deployer#canDeploy(java.lang.String, java.lang.String)
     */
    public boolean canDeploy(String serviceUnitName, String serviceUnitRootPath) {
        File xbean = new File(serviceUnitRootPath, getXBeanFile() + ".xml");
        if (logger.isDebugEnabled()) {
            logger.debug("Looking for " + xbean + ": " + xbean.exists());
        }
        return xbean.exists();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.Deployer#deploy(java.lang.String, java.lang.String)
     */
    public ServiceUnit deploy(String serviceUnitName, String serviceUnitRootPath) throws DeploymentException {
        AbstractXmlApplicationContext applicationContext = null;
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        try {
            // Create service unit
            XBeanServiceUnit su = new XBeanServiceUnit();
            su.setComponent(component);
            su.setName(serviceUnitName);
            su.setRootPath(serviceUnitRootPath);
            // Load configuration
            ClassLoader classLoader = component.getClass().getClassLoader();
            Thread.currentThread().setContextClassLoader(classLoader);

            File baseDir = new File(serviceUnitRootPath);
            String location = getXBeanFile();
            applicationContext = createApplicationContext(serviceUnitRootPath);

            for (Iterator iter = getBeanFactoryPostProcessors(serviceUnitRootPath).iterator(); iter.hasNext();) {
                BeanFactoryPostProcessor processor = (BeanFactoryPostProcessor) iter.next();
                applicationContext.addBeanFactoryPostProcessor(processor);
            }

            applicationContext.setValidating(false);
            applicationContext.refresh();
            su.setApplicationContext(applicationContext);
            // Use SU classloader
            Thread.currentThread().setContextClassLoader(su.getConfigurationClassLoader());
            initApplicationContext(applicationContext);

            // Retrieve endpoints
            Collection<Endpoint> endpoints = getServices(applicationContext);
            for (Endpoint endpoint : endpoints) {
                endpoint.setServiceUnit(su);
                validate(endpoint);
                su.addEndpoint(endpoint);
            }
            validate(su);
            return su;
        } catch (Throwable e) {
            logger.error(e);
            if (applicationContext != null) {
                try {
                    applicationContext.destroy();
                } catch (Exception ne) {
                    logger.error(ne);
                }
            }
            // There is a chance the thread context classloader has been changed by the xbean kernel,
            // so put back a good one
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            if (e instanceof DeploymentException) {
                throw ((DeploymentException) e);
            } else {
                throw failure("deploy", "Could not deploy xbean service unit", e);
            }
        } finally {
            Thread.currentThread().setContextClassLoader(oldCl);
        }
    }

    protected void initApplicationContext(AbstractXmlApplicationContext applicationContext) throws Exception {
    }

    protected Collection<Endpoint> getServices(AbstractXmlApplicationContext applicationContext) throws Exception {
        return applicationContext.getBeansOfType(Endpoint.class).values();
    }

    protected FileSystemXmlApplicationContext createApplicationContext(String serviceUnitRootPath) {
        File baseDir = new File(serviceUnitRootPath);
        String location = getXBeanFile();
        return new FileSystemXmlApplicationContext(
                                        new String[] { "/" + baseDir.toURI().resolve(location).getPath() + ".xml" },
                                        false,
                                        getXmlPreProcessors(serviceUnitRootPath));
    }

    protected List getXmlPreProcessors(String serviceUnitRootPath) {
        ClassLoaderXmlPreprocessor classLoaderXmlPreprocessor = new ClassLoaderXmlPreprocessor(new File(serviceUnitRootPath), component);
        return Collections.singletonList(classLoaderXmlPreprocessor);
    }
    
    protected List getBeanFactoryPostProcessors(String serviceUnitRootPath) {
        List processors = new ArrayList();
        // Property place holder
        PropertyPlaceholderConfigurer propertyPlaceholder = new PropertyPlaceholderConfigurer();
        FileSystemResource propertiesFile = new FileSystemResource(new File(serviceUnitRootPath) + "/" + getXBeanFile() + ".properties");
        if (propertiesFile.getFile().exists()) {                
            propertyPlaceholder.setLocation(propertiesFile);
            processors.add(propertyPlaceholder);
        }
        // Parent beans map
        Map beans = getParentBeansMap();
        if (beans != null) {
            processors.add(new ParentBeanFactoryPostProcessor(beans));
        }
        return processors;
    }

    protected Map getParentBeansMap() {
        Map beans = new HashMap();
        beans.put("context", new EndpointComponentContext(component.getComponentContext()));
        beans.put("component", component);
        Object smx3 = component.getSmx3Container();
        if (smx3 != null) {
            beans.put("container", smx3);
        }
        return beans;
    }
    
}
