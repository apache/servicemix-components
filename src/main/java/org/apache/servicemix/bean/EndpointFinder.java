/**
 *
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 *
 **/
package org.apache.servicemix.bean;

import org.apache.servicemix.bean.support.ResolverUtil;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.xml.namespace.QName;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Used to find POJOs on the classpath to be auto-exposed as endpoints
 *
 * @version $Revision$
 */
public class EndpointFinder implements ApplicationContextAware {
    private ApplicationContext applicationContext;
    private ResolverUtil resolver = new ResolverUtil();
    private String[] packages = {};
    private BeanComponent beanComponent;

    public EndpointFinder(BeanComponent beanComponent) {
        this.beanComponent = beanComponent;
        this.packages = beanComponent.getSearchPackages();
        this.applicationContext = beanComponent.getApplicationContext();
    }

    public String[] getPackages() {
        return packages;
    }

    public void setPackages(String[] packages) {
        this.packages = packages;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    public void addEndpoints(List list) {
        resolver.findAnnotated(Endpoint.class, packages);
        Set<Class> classes = resolver.getClasses();
        for (Class aClass : classes) {
            if (shouldIgnoreBean(aClass)) {
                continue;
            }
            if (isClient(aClass)) {
                registerClient(aClass);
            }
            else if (!Modifier.isAbstract(aClass.getModifiers())) {
                list.add(createBeanEndpoint(aClass));
            }
        }
    }

    public void destroy() throws Exception {
    }

    /**
     * Should the bean be ignored?
     */
    protected boolean shouldIgnoreBean(Class type) {
        Map beans = applicationContext.getBeansOfType(type, true, true);
        if (beans == null || beans.isEmpty()) {
            return false;
        }
        // TODO apply some filter?
        return true;
    }

    /**
     * Returns true if the interface is a client side interface
     */
    protected boolean isClient(Class type) {
        return type.isInterface() || Modifier.isAbstract(type.getModifiers());
    }

    protected void registerClient(Class type) {
        /** TODO */
    }


    protected BeanEndpoint createBeanEndpoint(Class serviceType) {
        Endpoint endpointAnnotation = (Endpoint) serviceType.getAnnotation(Endpoint.class);
        if (endpointAnnotation == null) {
            throw new IllegalArgumentException("Could not find endpoint annotation on type: " + serviceType);
        }
        BeanEndpoint answer = new BeanEndpoint();
        answer.setBeanType(serviceType);
        answer.setEndpoint(endpointAnnotation.name());
        QName service = createQName(endpointAnnotation.serviceName(), endpointAnnotation.targetNamespace());
        if (service == null) {
            service = BeanComponent.EPR_SERVICE;
        }
        answer.setService(service);
        return answer;
    }

    protected QName createQName(String localName, String uri) {
        if (isNotNullOrBlank(localName)) {
            if (isNotNullOrBlank(uri)) {
                return new QName(uri, localName);
            }
            return new QName(localName);
        }
        return null;
    }


    protected boolean isNotNullOrBlank(String text) {
        return text != null && text.trim().length() > 0;
    }
}
