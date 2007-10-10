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
package org.apache.servicemix.cxfse;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;

import org.apache.activemq.util.IdGenerator;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.jbi.JBITransportFactory;
import org.apache.servicemix.client.ClientFactory;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.container.JBIContainer;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * 
 * @author ffang
 * @org.apache.xbean.XBean element="proxy" description="A CXF proxy"
 * 
 */
public class CxfSeProxyFactoryBean implements FactoryBean, InitializingBean,
        DisposableBean {

    private String name = ClientFactory.DEFAULT_JNDI_NAME;

    private JBIContainer container;

    private ClientFactory factory;

    private ComponentContext context;

    private Class type;

    private Object proxy;

    private QName service;

    private QName interfaceName;

    private String endpoint;

    private boolean propagateSecuritySubject;

    private ServiceMixClient client;

    public Object getObject() throws Exception {
        if (proxy == null) {
            proxy = createProxy();
        }
        return proxy;
    }

    private Object createProxy() throws Exception {
        JaxWsProxyFactoryBean cf = new JaxWsProxyFactoryBean();
        cf.setServiceName(getService());
        cf.setServiceClass(type);
        cf.setAddress("jbi://" + new IdGenerator().generateSanitizedId());
        cf.setBindingId(org.apache.cxf.binding.jbi.JBIConstants.NS_JBI_BINDING);
        Bus bus = BusFactory.getDefaultBus();
        JBITransportFactory jbiTransportFactory = (JBITransportFactory) bus
                .getExtension(ConduitInitiatorManager.class)
                .getConduitInitiator(CxfSeComponent.JBI_TRANSPORT_ID);
        if (getContext() != null) { 
            DeliveryChannel dc = getContext().getDeliveryChannel();
            if (dc != null) {
                jbiTransportFactory.setDeliveryChannel(dc);
            }
        }
        return cf.create();
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
     * @param context
     *            the context to set
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
     * @param container
     *            the container to set
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
     * @param factory
     *            the factory to set
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
     * @param name
     *            the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the propagateSecuritySubject
     */
    public boolean isPropagateSecuritySubject() {
        return propagateSecuritySubject;
    }

    /**
     * @param propagateSecuritySubject
     *            the propagateSecuritySubject to set
     */
    public void setPropagateSecuritySubject(boolean propagateSecuritySubject) {
        this.propagateSecuritySubject = propagateSecuritySubject;
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
