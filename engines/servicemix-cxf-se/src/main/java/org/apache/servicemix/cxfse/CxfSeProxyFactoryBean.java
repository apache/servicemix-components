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

import java.lang.reflect.Method;
import java.util.List;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.jbi.JBITransportFactory;
import org.apache.servicemix.cxfse.interceptors.AttachmentInInterceptor;
import org.apache.servicemix.cxfse.interceptors.AttachmentOutInterceptor;
import org.apache.servicemix.id.IdGenerator;
import org.apache.servicemix.jbi.api.ClientFactory;
import org.apache.servicemix.jbi.api.Container;
import org.apache.servicemix.jbi.api.ServiceMixClient;

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
    
    private static final String[] CXF_CONFIG = new String[] {
        "META-INF/cxf/cxf.xml",
        "META-INF/cxf/cxf-extension-soap.xml",
        "META-INF/cxf/transport/jbi/cxf-transport-jbi.xml",
        "META-INF/cxf/binding/jbi/cxf-binding-jbi.xml"
    };

    private String name = ClientFactory.DEFAULT_JNDI_NAME;

    private Container container;

    private ClientFactory factory;

    private ComponentContext context;

    private Class type;

    private Object proxy;

    private QName service;

    private QName interfaceName;

    private String endpoint;

    private boolean propagateSecuritySubject;

    private ServiceMixClient client;
    
    private boolean useJBIWrapper = true;
    
    private boolean useSOAPEnvelope = true;

    private boolean mtomEnabled;
    
    private Object componentRegistry;

    private boolean clearClientResponseContext = true;
    
    public Object getObject() throws Exception {
        if (proxy == null) {
            proxy = createProxy();
        }
        
        return proxy;
    }

    private Object createProxy() throws Exception {
        JaxWsProxyFactoryBean cf = new JaxWsProxyFactoryBean();
        cf.setServiceName(getService());
        if (getEndpoint() != null) {
            cf.setEndpointName(new QName(getService().getNamespaceURI(), getEndpoint()));
        }
        cf.setServiceClass(type);
        cf.setAddress("jbi://" + new IdGenerator().generateSanitizedId());
        if (isUseJBIWrapper()) {
            cf.setBindingId(org.apache.cxf.binding.jbi.JBIConstants.NS_JBI_BINDING);
        }
        ComponentContext internalContext = getInternalContext();
       
        Bus bus = new SpringBusFactory().createBus(CXF_CONFIG);;
        JBITransportFactory jbiTransportFactory = (JBITransportFactory) bus
                .getExtension(ConduitInitiatorManager.class)
                .getConduitInitiator(JBITransportFactory.TRANSPORT_ID);
        if (internalContext != null) { 
            DeliveryChannel dc = internalContext.getDeliveryChannel();
            if (dc != null) {
                jbiTransportFactory.setDeliveryChannel(dc);
            }
        }
        cf.setBus(bus);
        Object proxy = cf.create();
        if (!isUseJBIWrapper() && !isUseSOAPEnvelope()) {
        	removeInterceptor(ClientProxy.getClient(proxy).getEndpoint().getBinding().getInInterceptors(), 
				"ReadHeadersInterceptor");
        	removeInterceptor(ClientProxy.getClient(proxy).getEndpoint().getBinding().getInFaultInterceptors(), 
        		"ReadHeadersInterceptor");
        	removeInterceptor(ClientProxy.getClient(proxy).getEndpoint().getBinding().getOutInterceptors(), 
				"SoapOutInterceptor");
        	removeInterceptor(ClientProxy.getClient(proxy).getEndpoint().getBinding().getOutFaultInterceptors(), 
				"SoapOutInterceptor");
        	removeInterceptor(ClientProxy.getClient(proxy).getEndpoint().getBinding().getOutInterceptors(), 
        		"StaxOutInterceptor");
        }
        if (isMtomEnabled()) {
            ClientProxy.getClient(proxy).getEndpoint()
                .getBinding().getInInterceptors().add(new AttachmentInInterceptor());
            ClientProxy.getClient(proxy).getEndpoint()
                .getBinding().getOutInterceptors().add(new AttachmentOutInterceptor());
        }
        if (isClearClientResponseContext()) {
            ClearClientResponseContextInterceptor clearClientResponseContextInterceptor = new ClearClientResponseContextInterceptor(ClientProxy.getClient(proxy));
            ClientProxy.getClient(proxy).getEndpoint().getBinding().getOutInterceptors().add(clearClientResponseContextInterceptor);
            ClientProxy.getClient(proxy).getEndpoint().getBinding().getOutFaultInterceptors().add(clearClientResponseContextInterceptor);
        }
            
        ClientProxy.getClient(proxy).setThreadLocalRequestContext(true);
        if (isPropagateSecuritySubject()) {
            jbiTransportFactory.setDeliveryChannel(new PropagateSecuritySubjectDeliveryChannel(jbiTransportFactory.getDeliveryChannel()));
        }
        return proxy;
    }

    private void removeInterceptor(List<Interceptor<? extends Message>> interceptors, String whichInterceptor) {
		for (Interceptor interceptor : interceptors) {
			if (interceptor.getClass().getName().endsWith(whichInterceptor)) {
				interceptors.remove(interceptor);
			}
		}
	}
    
    public Class getObjectType() {
        return type;
    }

    public boolean isSingleton() {
        return true;
    }

    protected ComponentContext getInternalContext() throws Exception {
        if (CxfSeComponent.getComponentRegistry() != null) {
            //in osgi container, use ComponentRegistry from CxfSeComponent
            Object componentRegistry = CxfSeComponent.getComponentRegistry();
            //use reflection to avoid nmr project dependency
            Method mth = componentRegistry.getClass().getMethod("createComponentContext");
            if (mth != null) {
                context = (ComponentContext) mth.invoke(componentRegistry);
            }
        } else if (getComponentRegistry() != null) {
            //in osgi container, use ComponentRegistry from Proxy directly, 
            //this won't depend on the CxfSeComponent Bundle start first
            Object componentRegistry = getComponentRegistry();
            //use reflection to avoid nmr project dependency
            Method mth = componentRegistry.getClass().getMethod("createComponentContext");
            if (mth != null) {
                context = (ComponentContext) mth.invoke(componentRegistry);
            }
        }
        
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

    /**
     * Specifies the webservice POJO type
     */
    public void setType(Class type) {
        this.type = type;
    }

    /**
     * The name of the endpoint.
     *
     * @org.apache.xbean.Property description="The name of the endpoint."
     * */
    public String getEndpoint() {
        return endpoint;
    }


    public void setEndpoint(String endpointName) {
        this.endpoint = endpointName;
    }

    /**
     * Specifies the servicemodel interface name
     *
     * @org.apache.xbean.Property description="Specifies the servicemodel interface name"
     * */
    public QName getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(QName interfaceName) {
        this.interfaceName = interfaceName;
    }

    /**
     * Specifies the servicemodel service name
     *
     * @org.apache.xbean.Property description="Specifies the servicemodel service name"
     * */
    public QName getService() {
        return service;
    }

    public void setService(QName service) {
        this.service = service;
    }

    /**
     * Allows injecting the ComponentContext
     *
     * @org.apache.xbean.Property description="Allows injecting the ComponentContext"
     * */
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

    public Container getContainer() {
        return container;
    }

    /**
     * Allows injecting a JBI Container instance (e.g. for testing purposes).
     */
    public void setContainer(Container container) {
        this.container = container;
    }

    /**
     * Allows injecting a ClientFactory
     *
     * @org.apache.xbean.Property description="Allows injecting a ClientFactory"
     * */
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

    public String getName() {
        return name;
    }

    /**
     * Specifies the JNDI name for looking up the ClientFactory. Defaults to <code>java:comp/env/jbi/ClientFactory</code>.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public boolean isPropagateSecuritySubject() {
        return propagateSecuritySubject;
    }

    /**
     * When set to <code>true</code>, the security subject is propagated along to the proxied endpoint.  Defaults to <code>false</code>.
     *
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

    /**
     * Specifies if the endpoint expects messages that are encased in the 
     * JBI wrapper used for SOAP messages. Ignore the value of useSOAPEnvelope 
     * if useJBIWrapper is true
     *
     * @org.apache.xbean.Property description="Specifies if the endpoint expects to receive the JBI wrapper in the message received from the NMR. The  default is <code>true</code>. Ignore the value of useSOAPEnvelope if useJBIWrapper is true"
     * */
    public void setUseJBIWrapper(boolean useJBIWrapper) {
        this.useJBIWrapper = useJBIWrapper;
    }

    public boolean isUseJBIWrapper() {
        return useJBIWrapper;
    }
    
    /**
     * Specifies if the endpoint expects soap messages when useJBIWrapper is false, 
     * if useJBIWrapper is true then ignore useSOAPEnvelope
     *
     * @org.apache.xbean.Property description="Specifies if the endpoint expects soap messages when useJBIWrapper is false, if useJBIWrapper is true then ignore useSOAPEnvelope. The  default is <code>true</code>.
     * */
	public void setUseSOAPEnvelope(boolean useSOAPEnvelope) {
		this.useSOAPEnvelope = useSOAPEnvelope;
	}

	public boolean isUseSOAPEnvelope() {
		return useSOAPEnvelope;
	}

    /**
     * Specifies if the endpoint can process messages with binary data.
     *
     * @param mtomEnabled a <code>boolean</code>
     * @org.apache.xbean.Property description="Specifies if the service can consume MTOM formatted binary data. The default is <code>false</code>."
     */
    public void setMtomEnabled(boolean mtomEnabled) {
        this.mtomEnabled = mtomEnabled;
    }

    public boolean isMtomEnabled() {
        return mtomEnabled;
    }

    /**
     * Allows injecting a custom component registry for looking up the proxied endpoint
     *
     * @org.apache.xbean.Property description="Allows injecting a custom component registry for looking up the proxying endpoint."
     * */
    public void setComponentRegistry(Object componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    public Object getComponentRegistry() {
        return componentRegistry;
    }

    /**
     * Specifies if the CXF client response context is cleared after each proxy invocation. Set to true if
     * caller wishes to use CXF client call to getResponseContext() to obtain response values directly, such
     * as the message exchange for the invocation.
     * 
     * @org.apache.xbean.Property description=
     *                            "Specifies if the CXF client response context is cleared after each proxy invocation. The default is
     *                            <code>true</code>."
     */
    public void setClearClientResponseContext(boolean clearClientResponseContext) {
        this.clearClientResponseContext = clearClientResponseContext;
    }

    public boolean isClearClientResponseContext() {
        return clearClientResponseContext;
    }

    public static class ClearClientResponseContextInterceptor extends AbstractPhaseInterceptor<Message> {
        private final Client client;

        public ClearClientResponseContextInterceptor(Client client) {
            super(Phase.POST_LOGICAL_ENDING);
            this.client = client;
        }

        public void handleMessage(Message message) throws Fault {
            client.getResponseContext().clear();
        }

        public void handleFault(Message message) {
            client.getResponseContext().clear();
        }
    }

    public class PropagateSecuritySubjectDeliveryChannel implements DeliveryChannel {

        private DeliveryChannel delegate;

        public PropagateSecuritySubjectDeliveryChannel(DeliveryChannel dc) {
            this.delegate = dc;
        }

        public void close() throws MessagingException {
            delegate.close();
        }

        public MessageExchangeFactory createExchangeFactory() {
            return delegate.createExchangeFactory();
        }

        public MessageExchangeFactory createExchangeFactory(QName interfaceName) {
            return delegate.createExchangeFactory(interfaceName);
        }

        public MessageExchangeFactory createExchangeFactoryForService(QName serviceName) {
            return delegate.createExchangeFactoryForService(serviceName);
        }

        public MessageExchangeFactory createExchangeFactory(ServiceEndpoint endpoint) {
            return delegate.createExchangeFactory(endpoint);
        }

        public MessageExchange accept() throws MessagingException {
            return delegate.accept();
        }

        public MessageExchange accept(long timeout) throws MessagingException {
            return delegate.accept(timeout);
        }

        public void send(MessageExchange exchange) throws MessagingException {
            propagateSubject(exchange);
            delegate.send(exchange);
        }

        public boolean sendSync(MessageExchange exchange) throws MessagingException {
            propagateSubject(exchange);
            return delegate.sendSync(exchange);
        }

        public boolean sendSync(MessageExchange exchange, long timeout) throws MessagingException {
            propagateSubject(exchange);
            return delegate.sendSync(exchange, timeout);
        }

        private void propagateSubject(MessageExchange exchange) {
            NormalizedMessage msg;
            if (exchange instanceof InOnly) {
                msg = ((InOnly) exchange).getInMessage();
            } else if (exchange instanceof InOut) {
                msg = ((InOut) exchange).getInMessage();
            } else {
                throw new RuntimeException("Unable to determine message type to propagate subject: " + exchange.getClass().getName());
            }
            if (msg.getSecuritySubject() == null && JBIContext.getMessageExchange() != null && JBIContext.getMessageExchange().getMessage("in") != null) {
                msg.setSecuritySubject(JBIContext.getMessageExchange().getMessage("in").getSecuritySubject());
            }
        }
    }
}
