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

import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.BeanDefinitionStoreException;

/**
 * A simple BeanFactory containing a set of predefined beans which can be used
 * as a parent for another BeanFactory.
 *  
 * @author gnodet
 */
public class SimpleBeanFactory implements BeanFactory {
    
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
        return getBean(name, (Class) null);
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
    public Object getBean(String name, Object[] args) throws BeansException {
        if (args != null) {
            throw new BeanDefinitionStoreException("Bean is not a prototype");
        }
        return getBean(name, (Class) null);
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
    public boolean isTypeMatch(String name, Class targetType) throws NoSuchBeanDefinitionException {
        if (!beans.containsKey(name)) {
            throw new NoSuchBeanDefinitionException(name);
        }
        if (targetType == null || Object.class.equals(targetType)) {
            return true;
        }
        return targetType.isAssignableFrom(beans.get(name).getClass());
    }
    public boolean isPrototype(String name) {
        return false;
    }
    
}