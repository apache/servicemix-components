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

import org.apache.commons.logging.Log;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.EndpointSupport;

import org.w3c.dom.Document;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.MessageExchange.Role;
import javax.wsdl.Definition;
import javax.xml.namespace.QName;

public abstract class AbstractEndpoint implements Endpoint {

    protected QName service;
    protected String endpoint;
    protected QName interfaceName;
    protected Document description;
    protected Definition definition;
    protected ServiceUnit serviceUnit;
    protected Log logger;
    private String key;

    public AbstractEndpoint() {
    }

    public AbstractEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
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
     * The name of the endpoint.
     *
     * @param endpoint a string specifiying the name of the endpoint
     * @org.apache.xbean.Property
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
     * The qualified name of the service the endpoint exposes.
     *
     * @param service a QName specifiying the name of the service
     * @org.apache.xbean.Property
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
     * Associates an XML document with the endpoint. The XML document describes the endpoint and is typically found in the service
     * unit packaging.
     *
     * @param description a <code>Document</code> describing the endpoint
     * @org.apache.xbean.Property description="an XML document describing the endpoint" hidden="true"
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
     * The qualified name of the interface exposed by the endpoint.
     *
     * @param interfaceName a QName specifiying the name of the interface
     * @org.apache.xbean.Property
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
     * Associates an endpoint with a service unit. The service unit is used by the container to manage the endpoint's lifecycle.
     *
     * @param serviceUnit a <code>ServiceUnit</code> to which the endpoint will be associated
     * @org.apache.xbean.Property description="the service unit responsible for the endpoint" hidden="true
     */
    public void setServiceUnit(ServiceUnit serviceUnit) {
        this.serviceUnit = serviceUnit;
        this.logger = serviceUnit.getComponent().getLogger();
    }

    public boolean isExchangeOkay(MessageExchange exchange) {
        // TODO: We could check the MEP here
        return true;
    }

    public void prepareExchange(MessageExchange exchange) throws MessagingException {
        getServiceUnit().getComponent().prepareExchange(exchange, this);
    }

    public abstract void activate() throws Exception;

    public abstract void start() throws Exception;

    public abstract void stop() throws Exception;

    public abstract void deactivate() throws Exception;

    public abstract void process(MessageExchange exchange) throws Exception;

    public String toString() {
        return "Endpoint[service: " + service + ", " + "endpoint: " + endpoint + ", " + "role: "
               + (getRole() == Role.PROVIDER ? "provider" : "consumer") + "]";
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

    /**
     * @param definition
     * @org.apache.xbean.Property hidden="true"
     */
    public void setDefinition(Definition definition) {
        this.definition = definition;
    }

    public String getKey() {
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
