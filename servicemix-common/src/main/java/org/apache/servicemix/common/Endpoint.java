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
import org.w3c.dom.Document;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.MessageExchange.Role;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;

public abstract class Endpoint {

    protected QName service;
    protected String endpoint;
    protected QName interfaceName;
    protected Document description;
    protected Definition definition;
    protected ServiceUnit serviceUnit;
    protected Log logger;
    private String key;
    
    public Endpoint() {
    }
    
    public Endpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        this.serviceUnit = serviceUnit;
        this.logger = serviceUnit.getComponent().getLogger();
        this.service = service;
        this.endpoint = endpoint;
    }
    
    /**
     * @return Returns the endpoint.
     */
    public String getEndpoint() {
        return endpoint;
    }
    /**
     * @param endpoint The endpoint to set.
     */
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        this.key = null;
    }
    /**
     * @return Returns the service.
     */
    public QName getService() {
        return service;
    }
    /**
     * @param service The service to set.
     */
    public void setService(QName service) {
        this.service = service;
        this.key = null;
    }
    /**
     * @return Returns the role.
     */
    public abstract Role getRole();
    /**
     * @return Returns the description.
     */
    public Document getDescription() {
        return description;
    }
    /**
     * @param description The description to set.
     */
    public void setDescription(Document description) {
        this.description = description;
    }
    /**
     * @return Returns the interfaceName.
     */
    public QName getInterfaceName() {
        return interfaceName;
    }
    /**
     * @param interfaceName The interfaceName to set.
     */
    public void setInterfaceName(QName interfaceName) {
        this.interfaceName = interfaceName;
    }
    /**
     * @return Returns the serviceUnit.
     */
    public ServiceUnit getServiceUnit() {
        return serviceUnit;
    }

    /**
     * @param serviceUnit The serviceUnit to set.
     */
    public void setServiceUnit(ServiceUnit serviceUnit) {
        this.serviceUnit = serviceUnit;
        this.logger = serviceUnit.component.getLogger();
    }

    public boolean isExchangeOkay(MessageExchange exchange) {
        // TODO: We could check the MEP here 
        return true;
    }

    public void prepareExchange(MessageExchange exchange) throws MessagingException {
        getServiceUnit().getComponent().prepareExchange(exchange, this);
    }

    public abstract void activate() throws Exception;
    
    public abstract void deactivate() throws Exception;

    public abstract ExchangeProcessor getProcessor();
    
    public String toString() {
        return "Endpoint[service: " + service + ", " + 
                        "endpoint: " + endpoint + ", " +
                        "role: " + (getRole() == Role.PROVIDER ? "provider" : "consumer") + "]";
    }

    /**
     * Validate the endpoint at either deployment time for statically defined endpoints or at runtime for dynamic endpoints
     * 
     * @throws DeploymentException
     */
    public void validate() throws DeploymentException {
    }
    
    public Definition getDefinition() {
        return definition;
    }

    public void setDefinition(Definition definition) {
        this.definition = definition;
    }
    
    String getKey() {
        if (key == null) {
            if (service == null) {
                throw new IllegalArgumentException("Endpoint: " + this + " has no service name defined");
            }
            if (endpoint == null) {
                throw new IllegalArgumentException("Endpoint: " + this + " has no endpoint name defined");
            }
            key = EndpointSupport.getKey(service, endpoint);
        }
        return key;
    }

}
