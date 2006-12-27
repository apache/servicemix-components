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
package org.apache.servicemix.jsr181.xfire;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.jbi.component.ComponentContext;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.client.ClientFactory;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jsr181.Jsr181Component;
import org.codehaus.xfire.XFire;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;


/**
 * 
 * @author chirino
 * @version $Revision: 407481 $
 * @org.apache.xbean.XBean element="proxy"
 *                  description="A jsr181 proxy"
 * 
 */
public class JbiProxyFactoryBean implements FactoryBean, InitializingBean, DisposableBean {
    
    private static final Log logger = LogFactory.getLog(JbiProxyFactoryBean.class);

    private String name = ClientFactory.DEFAULT_JNDI_NAME;
    private JBIContainer container;
    private ClientFactory factory;
    private ComponentContext context;
    private Class type;
    private Object proxy;
    private InvocationHandler jbiInvocationHandler;
    private QName service;
    private QName interfaceName;
    private String endpoint;
    
    private ServiceMixClient client;
    
    public Object getObject() throws Exception {
        if( proxy == null ) {
            proxy = createProxy();
        }
        return proxy;
    }

    private Object createProxy() throws Exception {
        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{type}, new InvocationHandler(){
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                InvocationHandler next = getJBIInvocationHandler();
                return next.invoke(proxy, method, args);
            }
        });
    }
    
    synchronized private InvocationHandler getJBIInvocationHandler() throws Exception {
        if( jbiInvocationHandler == null ) {
            XFire xfire = Jsr181Component.createXFire(getInternalContext());
            Object o = JbiProxy.create(xfire, context, interfaceName, service, endpoint, type);
            jbiInvocationHandler = Proxy.getInvocationHandler(o);
        }
        return jbiInvocationHandler;
    }

    public Class getObjectType() {
        return type;
    }

    public boolean isSingleton() {
        return true;
    }
    
    protected ComponentContext getInternalContext() throws Exception {
        if (context == null) {
            if (factory == null) {
                if (container != null) {
                    factory = container.getClientFactory();
                } else {
                    factory = (ClientFactory) new InitialContext().lookup(name);
                }
            }
            client = factory.createClient();
            context = client.getContext();
        }
        return context;
    }

    public Class getType() {
        return type;
    }

    public void setType(Class type) {
        this.type = type;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpointName) {
        this.endpoint = endpointName;
    }

    public QName getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(QName interfaceName) {
        this.interfaceName = interfaceName;
    }

    public QName getService() {
        return service;
    }

    public void setService(QName service) {
        this.service = service;
    }

    /**
     * @return the context
     */
    public ComponentContext getContext() {
        return context;
    }

    /**
     * @param context the context to set
     */
    public void setContext(ComponentContext context) {
        this.context = context;
    }

    /**
     * @return the container
     */
    public JBIContainer getContainer() {
        return container;
    }

    /**
     * @param container the container to set
     */
    public void setContainer(JBIContainer container) {
        this.container = container;
    }

    /**
     * @return the factory
     */
    public ClientFactory getFactory() {
        return factory;
    }

    /**
     * @param factory the factory to set
     */
    public void setFactory(ClientFactory factory) {
        this.factory = factory;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public void afterPropertiesSet() throws Exception {
        if (type == null) {
            throw new IllegalArgumentException("type must be set");
        }
    }

    public void destroy() throws Exception {
        if (client != null) {
            client.close();
            client = null;
        }
    }

}
