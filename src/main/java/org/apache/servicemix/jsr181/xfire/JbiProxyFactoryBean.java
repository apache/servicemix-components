/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import javax.xml.namespace.QName;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jsr181.Jsr181LifeCycle;
import org.codehaus.xfire.XFire;
import org.springframework.beans.factory.FactoryBean;


/**
 * 
 * @author chirino
 * @version $Revision: 407481 $
 * @org.apache.xbean.XBean element="proxy"
 *                  description="A jsr181 proxy"
 * 
 */
public class JbiProxyFactoryBean implements FactoryBean {

    private JBIContainer container;
    private Class type;
    private Object proxy;
    private InvocationHandler jbiInvocationHandler;
    private QName service;
    private QName interfaceName;
    private String endpoint;
    
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
            DefaultServiceMixClient client = new DefaultServiceMixClient(container);
            ComponentContext context = client.getContext();
            XFire xfire = Jsr181LifeCycle.createXFire(context);
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

    public JBIContainer getContainer() {
        return container;
    }

    public void setContainer(JBIContainer container) {
        this.container = container;
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

}
