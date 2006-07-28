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
package org.apache.servicemix.eip;

import java.net.URL;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.Definition;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.eip.support.ExchangeTarget;
import org.apache.servicemix.locks.LockManager;
import org.apache.servicemix.locks.impl.SimpleLockManager;
import org.apache.servicemix.store.Store;
import org.apache.servicemix.store.StoreFactory;
import org.apache.servicemix.store.memory.MemoryStoreFactory;
import org.apache.servicemix.timers.TimerManager;
import org.apache.servicemix.timers.impl.TimerManagerImpl;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;

/**
 * @author gnodet
 * @version $Revision: 376451 $
 */
public abstract class EIPEndpoint extends Endpoint implements ExchangeProcessor {

    private ServiceEndpoint activated;
    private DeliveryChannel channel;
    private Resource wsdlResource;
    
    /**
     * The store to keep pending exchanges
     */
    protected Store store;
    /**
     * The store factory.
     */
    protected StoreFactory storeFactory;
    /**
     * The lock manager.
     */
    protected LockManager lockManager;
    /**
     * The timer manager.
     */
    protected TimerManager timerManager;
    /**
     * The exchange factory
     */
    protected MessageExchangeFactory exchangeFactory;
    
    /**
     * The ExchangeTarget to use to get the WSDL
     */
    protected ExchangeTarget wsdlExchangeTarget;
    
    /**
     * @return Returns the exchangeFactory.
     */
    public MessageExchangeFactory getExchangeFactory() {
        return exchangeFactory;
    }
    /**
     * @param exchangeFactory The exchangeFactory to set.
     */
    public void setExchangeFactory(MessageExchangeFactory exchangeFactory) {
        this.exchangeFactory = exchangeFactory;
    }
    /**
     * @return Returns the store.
     */
    public Store getStore() {
        return store;
    }
    /**
     * @param store The store to set.
     */
    public void setStore(Store store) {
        this.store = store;
    }
    /**
     * @return Returns the storeFactory.
     */
    public StoreFactory getStoreFactory() {
        return storeFactory;
    }
    /**
     * @param storeFactory The storeFactory to set.
     */
    public void setStoreFactory(StoreFactory storeFactory) {
        this.storeFactory = storeFactory;
    }
    /**
     * @return the lockManager
     */
    public LockManager getLockManager() {
        return lockManager;
    }
    /**
     * @param lockManager the lockManager to set
     */
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }
    /**
     * @return the timerManager
     */
    public TimerManager getTimerManager() {
        return timerManager;
    }
    /**
     * @param timerManager the timerManager to set
     */
    public void setTimerManager(TimerManager timerManager) {
        this.timerManager = timerManager;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.Endpoint#getRole()
     */
    public Role getRole() {
        return Role.PROVIDER;
    }

    public void activate() throws Exception {
        logger = this.serviceUnit.getComponent().getLogger();
        ComponentContext ctx = getContext();
        channel = ctx.getDeliveryChannel();
        exchangeFactory = channel.createExchangeFactory();
        activated = ctx.activateEndpoint(service, endpoint);
        if (store == null) {
            if (storeFactory == null) {
                storeFactory = new MemoryStoreFactory();
            }
            store = storeFactory.open(getService().toString() + getEndpoint());
        }
        if (lockManager == null) {
            lockManager = new SimpleLockManager();
        }
        if (timerManager == null) {
            timerManager = new TimerManagerImpl();
        }
        timerManager.start();
        start();
    }

    public void deactivate() throws Exception {
        if (timerManager != null) {
            timerManager.stop();
        }
        stop();
        ServiceEndpoint ep = activated;
        activated = null;
        ComponentContext ctx = getServiceUnit().getComponent().getComponentContext();
        ctx.deactivateEndpoint(ep);
    }

    public ExchangeProcessor getProcessor() {
        return this;
    }
    
    public void validate() throws DeploymentException {
    }
    
    protected ComponentContext getContext() {
        return getServiceUnit().getComponent().getComponentContext();
    }
    
    protected void send(MessageExchange me) throws MessagingException {
        if (me.getRole() == MessageExchange.Role.CONSUMER &&
            me.getStatus() == ExchangeStatus.ACTIVE) {
            BaseLifeCycle lf = (BaseLifeCycle) getServiceUnit().getComponent().getLifeCycle();
            lf.sendConsumerExchange(me, (Endpoint) this);
        } else {
            channel.send(me);
        }
    }
    
    protected void sendSync(MessageExchange me) throws MessagingException {
        if (!channel.sendSync(me)) {
            throw new MessagingException("SendSync failed");
        }
    }
    
    protected void done(MessageExchange me) throws MessagingException {
        me.setStatus(ExchangeStatus.DONE);
        send(me);
    }
    
    protected void fail(MessageExchange me, Exception error) throws MessagingException {
        me.setError(error);
        send(me);
    }
    
    public void start() throws Exception {
    }
    
    public void stop() {
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.ExchangeProcessor#process(javax.jbi.messaging.MessageExchange)
     */
    public void process(MessageExchange exchange) throws Exception {
        boolean txSync = exchange.isTransacted() && Boolean.TRUE.equals(exchange.getProperty(JbiConstants.SEND_SYNC));
        if (txSync && exchange.getRole() == Role.PROVIDER && exchange.getStatus() == ExchangeStatus.ACTIVE) {
            processSync(exchange);
        } else {
            processAsync(exchange);
        }
    }
    
    /**
     * @return Returns the description.
     */
    public Document getDescription() {
        if( description == null ) {
            definition = getDefinition();
            if( definition!=null ) {
                try {
                    description = WSDLFactory.newInstance().newWSDLWriter().getDocument(definition);
                } catch (WSDLException e) {
                }
            }
        }
        return description;
    }
    
    /**
     * If the definition is not currently set, it tries to set it using 
     * the following sources in the order:
     * description, wsdlResource, wsdlExchangeTarget
     * 
     * @return Returns the definition.
     */
    public Definition getDefinition() {
        if( definition == null ) {
            definition = getDefinitionFromDescription();
            if( definition == null ) {
                definition = getDefinitionFromWsdlResource();
                if( definition == null ) {
                    definition = getDefinitionFromWsdlExchangeTarget();
                }
            }
        }
        return definition;
    }
    
    protected Definition getDefinitionFromDescription() {
        if( description!=null ) {
            try {
                return WSDLFactory.newInstance().newWSDLReader().readWSDL(null, description);
            } catch (WSDLException ignore) {
            }
        }
        return null;
    }

    protected Definition getDefinitionFromWsdlResource() {
        if( wsdlResource!=null ) {
            try {
                URL resource = wsdlResource.getURL();
                WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
                return reader.readWSDL(null, resource.toString());
            } catch (Throwable ignore) {
            }
        }
        return null;
    }
        
    protected Definition getDefinitionFromWsdlExchangeTarget() {
        if( wsdlExchangeTarget != null ) {
            try {
                Document description = getDescriptionForExchangeTarget(wsdlExchangeTarget);
                return WSDLFactory.newInstance().newWSDLReader().readWSDL(null, description);
            } catch (Throwable ignore) {
            }
        }
        return null;
    }
    
    /**
     * @return Returns the wsdl's Resource.
     */
    public Resource getWsdlResource() {
        return wsdlResource;
    }
    public void setWsdlResource(Resource wsdlResource) {
        this.wsdlResource = wsdlResource;
    }
    
    protected Document getDescriptionForExchangeTarget(ExchangeTarget match) throws JBIException {
        ServiceEndpoint[] endpoints = getEndpointsForExchangeTarget(match);
        if (endpoints == null || endpoints.length == 0) {
            return null;
        }
        ServiceEndpoint endpoint = chooseFirstEndpointWithDescriptor(endpoints);
        if (endpoint == null) {
            return null;
        }
        return getContext().getEndpointDescriptor(endpoint);
    }
    
    /**
     * 
     * @param match
     * @return an ServiceEndpoint[] of all the endpoints that matched.
     * @throws JBIException
     */
    protected ServiceEndpoint[] getEndpointsForExchangeTarget(ExchangeTarget match) throws JBIException {
        ServiceEndpoint[] endpoints;
        if (match.getEndpoint() != null && match.getService() != null) {
            ServiceEndpoint endpoint = getContext().getEndpoint(match.getService(), match.getEndpoint());
            if (endpoint == null) {
                endpoints = new ServiceEndpoint[0];
            } else {
                endpoints = new ServiceEndpoint[] { endpoint };
            }
        } else if (match.getService() != null) {
            endpoints = getContext().getEndpointsForService(match.getService());
        } else if (interfaceName != null) {
            endpoints = getContext().getEndpoints(interfaceName);
        } else {
            throw new IllegalStateException("One of interfaceName or serviceName should be provided");
        }
        return endpoints;
    }
    
    protected ServiceEndpoint chooseFirstEndpointWithDescriptor(ServiceEndpoint[] endpoints) throws JBIException {
        for (int i = 0; i < endpoints.length; i++) {
            if (getContext().getEndpointDescriptor(endpoints[i]) != null) {
                return endpoints[i];
            }
        }
        return null;
    }


    protected abstract void processAsync(MessageExchange exchange) throws Exception;

    protected abstract void processSync(MessageExchange exchange) throws Exception;
    
    public ExchangeTarget getWsdlExchangeTarget() {
        return wsdlExchangeTarget;
    }
    public void setWsdlExchangeTarget(ExchangeTarget wsdlExchangeTarget) {
        this.wsdlExchangeTarget = wsdlExchangeTarget;
    }
    
}
