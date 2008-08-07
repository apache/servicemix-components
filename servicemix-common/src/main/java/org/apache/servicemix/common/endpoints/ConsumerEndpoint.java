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
package org.apache.servicemix.common.endpoints;

import javax.jbi.component.ComponentContext;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ExternalEndpoint;
import org.apache.servicemix.common.ServiceMixComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.util.URIResolver;

public abstract class ConsumerEndpoint extends SimpleEndpoint {

    private ServiceEndpoint activated;
    private QName targetInterface;
    private QName targetService;
    private String targetEndpoint;
    private QName targetOperation;
    private String targetUri;

    public ConsumerEndpoint() {
    }

    public ConsumerEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    public ConsumerEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component.getServiceUnit(), endpoint.getServiceName(), endpoint.getEndpointName());
    }

    public Role getRole() {
        return Role.CONSUMER;
    }

    public synchronized void start() throws Exception {
        ServiceMixComponent component = getServiceUnit().getComponent();
        ComponentContext ctx = component.getComponentContext();
        activated = new ExternalEndpoint(component.getEPRElementName(), getLocationURI(), getService(),
                                         getEndpoint(), getInterfaceName());
        ctx.registerExternalEndpoint(activated);
    }

    public synchronized void stop() throws Exception {
        ServiceMixComponent component = getServiceUnit().getComponent();
        ComponentContext ctx = component.getComponentContext();
        if (activated != null) {
            ServiceEndpoint se = activated;
            activated = null;
            ctx.deregisterExternalEndpoint(se);
        }
    }

    /**
     * Return the URI identifying this external endpoint. This must be overriden so that endpoint resolution can work correctly.
     * 
     * @return the URI identifying this external endpoint
     */
    public String getLocationURI() {
        return null;
    }

    /**
     * @return the targetEndpoint
     */
    public String getTargetEndpoint() {
        return targetEndpoint;
    }

    /**
     * Sets the endpoint name of the target endpoint.
     * 
     * @param targetEndpoint a string specifiying the name of the target endpoint
     * @org.apache.xbean.Property description="the name of the endpoint to which requests are sent"
     */
    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }

    /**
     * @return the targetInterface
     */
    public QName getTargetInterface() {
        return targetInterface;
    }

    /**
     * Sets the name of the target interface.
     * 
     * @param targetInterface a QName specifiying the name of the target interface
     * @org.apache.xbean.Property description="the QName of the interface to which requests are sent"
     */
    public void setTargetInterface(QName targetInterface) {
        this.targetInterface = targetInterface;
    }

    /**
     * @return the targetService
     */
    public QName getTargetService() {
        return targetService;
    }

    /**
     * Sets the name of the target service.
     * 
     * @param targetService a QName specifiying the name of the target interface
     * @org.apache.xbean.Property description="the QName of the service to which requests are sent"
     */
    public void setTargetService(QName targetService) {
        this.targetService = targetService;
    }

    /**
     * @return the targetOperation
     */
    public QName getTargetOperation() {
        return targetOperation;
    }

    /**
     * Sets the name of the target operation.
     * 
     * @param targetOperation a QName specifiying the name of the target operation
     * @org.apache.xbean.Property description="the QName of the operation to which requests are sent"
     */
    public void setTargetOperation(QName targetOperation) {
        this.targetOperation = targetOperation;
    }

    /**
     * @return the targetUri
     */
    public String getTargetUri() {
        return targetUri;
    }

    /**
     * @param targetUri the targetUri to set
     */
    public void setTargetUri(String targetUri) {
        this.targetUri = targetUri;
    }

    protected void configureExchangeTarget(MessageExchange exchange) {
        if (targetUri != null) {
            URIResolver.configureExchange(exchange, getContext(), targetUri);
        }
        if (exchange.getInterfaceName() == null && targetInterface != null) {
            exchange.setInterfaceName(targetInterface);
        }
        if (exchange.getOperation() == null && targetOperation != null) {
            exchange.setOperation(targetOperation);
        }
        if (exchange.getService() == null && targetService != null) {
            exchange.setService(targetService);
            if (targetEndpoint != null) {
                ServiceEndpoint se = getContext().getEndpoint(targetService, targetEndpoint);
                if (se != null) {
                    exchange.setEndpoint(se);
                } else {
                    logger.warn("Target service (" + targetService + ") and endpoint (" + targetEndpoint + ")"
                              + " specified, but no matching endpoint found.  Only the service will be used for routing.");
                }
            }
        }
    }

    public void validate() throws DeploymentException {
        super.validate();
        if (targetInterface == null && targetService == null && targetUri == null) {
            throw new DeploymentException("targetInterface, targetService or targetUri should be specified");
        }
    }
}
