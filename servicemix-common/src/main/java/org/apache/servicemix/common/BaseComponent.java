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
package org.apache.servicemix.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.executors.Executor;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

import javax.jbi.component.ComponentContext;
import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.component.ServiceUnitManager;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

/**
 * Base class for a component.
 * 
 * @author Guillaume Nodet
 * @version $Revision$
 * @since 3.0
 */
@Deprecated
public abstract class BaseComponent implements ServiceMixComponent {

    protected final transient Log logger = LogFactory.getLog(getClass());
    
    protected BaseLifeCycle lifeCycle;
    protected Registry registry;
    protected BaseServiceUnitManager serviceUnitManager;
    
    public BaseComponent() {
        lifeCycle = createLifeCycle();
        registry = createRegistry();
        serviceUnitManager = createServiceUnitManager();
    }
    
    /* (non-Javadoc)
     * @see javax.jbi.component.Component#getLifeCycle()
     */
    public ComponentLifeCycle getLifeCycle() {
        return lifeCycle;
    }

    /* (non-Javadoc)
     * @see javax.jbi.component.Component#getServiceUnitManager()
     */
    public ServiceUnitManager getServiceUnitManager() {
        return serviceUnitManager;
    }

    /* (non-Javadoc)
     * @see javax.jbi.component.Component#getServiceDescription(javax.jbi.servicedesc.ServiceEndpoint)
     */
    public Document getServiceDescription(ServiceEndpoint endpoint) {
        if (logger.isDebugEnabled()) {
            logger.debug("Querying service description for " + endpoint);
        }
        String key = EndpointSupport.getKey(endpoint);
        Endpoint ep = this.registry.getEndpoint(key);
        if (ep != null) {
            Document doc = ep.getDescription();
            if (doc == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("No description found for " + key);
                }
            }
            return doc;
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("No endpoint found for " + key);
            }
            return null;
        }
    }

    /* (non-Javadoc)
     * @see javax.jbi.component.Component#isExchangeWithConsumerOkay(javax.jbi.servicedesc.ServiceEndpoint, javax.jbi.messaging.MessageExchange)
     */
    public boolean isExchangeWithConsumerOkay(ServiceEndpoint endpoint, MessageExchange exchange) {
        String key = EndpointSupport.getKey(endpoint);
        Endpoint ep = this.registry.getEndpoint(key);
        if (ep != null) {
            if (ep.getRole() != Role.PROVIDER) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Endpoint " + key + " is a consumer. Refusing exchange with consumer.");
                }
                return false;
            } else {
                return ep.isExchangeOkay(exchange);
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("No endpoint found for " + key + ". Refusing exchange with consumer.");
            }
            return false;
        }
    }

    /* (non-Javadoc)
     * @see javax.jbi.component.Component#isExchangeWithProviderOkay(javax.jbi.servicedesc.ServiceEndpoint, javax.jbi.messaging.MessageExchange)
     */
    public boolean isExchangeWithProviderOkay(ServiceEndpoint endpoint, MessageExchange exchange) {
        // TODO: check if the selected endpoint is good for us
        return true;
    }

    /* (non-Javadoc)
     * @see javax.jbi.component.Component#resolveEndpointReference(org.w3c.dom.DocumentFragment)
     */
    public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
        return null;
    }
    
    /**
     * Create the life cycle object.
     * Derived classes should override this method to be able to
     * use a custom life cycle implementation.
     * 
     * @return a life cycle object
     */
    protected BaseLifeCycle createLifeCycle() {
        return new BaseLifeCycle(this);
    }

    /**
     * Create the service unit manager.
     * Derived classes should override this method and return a 
     * BaseServiceUnitManager so that the component is able to 
     * handle service unit deployment.
     * 
     * @return a service unit manager
     */
    protected BaseServiceUnitManager createServiceUnitManager() {
        return null;
    }

    protected Registry createRegistry() {
        return new Registry(this);
    }

    public ComponentContext getComponentContext() {
        return lifeCycle.getContext();
    }

    public String getComponentName() {
        if (getComponentContext() == null) {
            return "Component (" + getClass().getName() + ") not yet initialized";
        }
        return getComponentContext().getComponentName();
    }

    /**
     * @return Returns the logger.
     */
    public Log getLogger() {
        return logger;
    }

    /**
     * @return Returns the registry.
     */
    public Registry getRegistry() {
        return registry;
    }

    /**
     * Shortcut to retrieve this component's executor.
     * 
     * @return the executor for this component
     */
    public Executor getExecutor() {
        return lifeCycle.getExecutor();
    }

    public Object getSmx3Container() {
        return lifeCycle.getSmx3Container();
    }

    public void prepareExchange(MessageExchange exchange, Endpoint endpoint) throws MessagingException {
        lifeCycle.prepareExchange(exchange, endpoint);
    }

    public QName getEPRElementName() {
        return null;
    }

    @Deprecated
    public void prepareConsumerExchange(MessageExchange exchange, Endpoint endpoint) throws MessagingException {
        lifeCycle.prepareConsumerExchange(exchange, endpoint);
    }

    @Deprecated
    public void sendConsumerExchange(MessageExchange exchange, Endpoint endpoint) throws MessagingException {
        lifeCycle.sendConsumerExchange(exchange, endpoint);
    }
}
