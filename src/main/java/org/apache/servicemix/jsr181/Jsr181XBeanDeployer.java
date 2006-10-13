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
package org.apache.servicemix.jsr181;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jbi.management.DeploymentException;

import org.apache.servicemix.common.BaseComponent;
import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.EndpointComponentContext;
import org.apache.servicemix.common.xbean.AbstractXBeanDeployer;
import org.apache.servicemix.common.xbean.XBeanServiceUnit;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

public class Jsr181XBeanDeployer extends AbstractXBeanDeployer {

    public Jsr181XBeanDeployer(BaseComponent component) {
        super(component);
    }

    protected boolean validate(Endpoint endpoint) throws DeploymentException {
        if (endpoint instanceof Jsr181Endpoint == false) {
            throw failure("deploy", "Endpoint should be a Jsr181 endpoint", null);
        }
        Jsr181Endpoint ep = (Jsr181Endpoint) endpoint;
        if (ep.getPojo() == null && ep.getPojoClass() == null) {
            throw failure("deploy", "Endpoint must have a non-null pojo or a pojoClass", null);
        }
        try {
            ep.registerService();
        } catch (Exception e) {
            throw failure("deploy", "Could not register endpoint", e);
        }
        return true;
    }

    protected List getBeanFactoryPostProcessors(String serviceUnitRootPath) {
        List processors = new ArrayList(super.getBeanFactoryPostProcessors(serviceUnitRootPath));
        processors.add(new BeanFactoryPostProcessor() {
            public void postProcessBeanFactory(ConfigurableListableBeanFactory factory) throws BeansException {
                Map beans = new HashMap();
                beans.put("context", new EndpointComponentContext(((BaseLifeCycle) component.getLifeCycle()).getContext()));
                BeanFactory parent = new SimpleBeanFactory(beans);
                factory.setParentBeanFactory(parent);
            }
        });
        return processors;
    }
    
    private static class SimpleBeanFactory implements BeanFactory {
        private final Map beans;
        public SimpleBeanFactory(Map beans) {
            this.beans = beans;
        }
        public boolean containsBean(String name) {
            return beans.containsKey(name);
        }
        public String[] getAliases(String name) throws NoSuchBeanDefinitionException {
            Object bean = beans.get(name);
            if (bean == null) {
                throw new NoSuchBeanDefinitionException(name);
            }
            return new String[0];
        }
        public Object getBean(String name) throws BeansException {
            return getBean(name, null);
        }
        public Object getBean(String name, Class requiredType) throws BeansException {
            Object bean = beans.get(name);
            if (bean == null) {
                throw new NoSuchBeanDefinitionException(name);
            }
            if (requiredType != null && !requiredType.isInstance(bean)) {
                throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
            }
            return bean;
        }
        public Class getType(String name) throws NoSuchBeanDefinitionException {
            Object bean = beans.get(name);
            if (bean == null) {
                throw new NoSuchBeanDefinitionException(name);
            }
            return bean.getClass();
        }
        public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
            Object bean = beans.get(name);
            if (bean == null) {
                throw new NoSuchBeanDefinitionException(name);
            }
            return true;
        }
    }

}
