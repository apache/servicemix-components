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
package org.apache.servicemix.jsr181;

import java.util.MissingResourceException;
import java.util.logging.Logger;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.management.MBeanNames;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.MBeanServer;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

public class EndpointComponentContext implements ComponentContext {

    private ComponentContext context;
    private DeliveryChannel channel;
    
    public EndpointComponentContext(ComponentContext context) {
        this.context = context;
    }

    public ServiceEndpoint activateEndpoint(QName serviceName, String endpointName) throws JBIException {
        throw new UnsupportedOperationException();
    }

    public void deactivateEndpoint(ServiceEndpoint endpoint) throws JBIException {
        throw new UnsupportedOperationException();
    }

    public void deregisterExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
        throw new UnsupportedOperationException();
    }

    public String getComponentName() {
        return context.getComponentName();
    }

    public DeliveryChannel getDeliveryChannel() throws MessagingException {
        if (this.channel == null) {
            this.channel = new EndpointDeliveryChannel(context.getDeliveryChannel());
        }
        return this.channel;
    }

    public ServiceEndpoint getEndpoint(QName service, String name) {
        return context.getEndpoint(service, name);
    }

    public Document getEndpointDescriptor(ServiceEndpoint endpoint) throws JBIException {
        return context.getEndpointDescriptor(endpoint);
    }

    public ServiceEndpoint[] getEndpoints(QName interfaceName) {
        return context.getEndpoints(interfaceName);
    }

    public ServiceEndpoint[] getEndpointsForService(QName serviceName) {
        return context.getEndpointsForService(serviceName);
    }

    public ServiceEndpoint[] getExternalEndpoints(QName interfaceName) {
        return context.getExternalEndpoints(interfaceName);
    }

    public ServiceEndpoint[] getExternalEndpointsForService(QName serviceName) {
        return context.getExternalEndpointsForService(serviceName);
    }

    public String getInstallRoot() {
        return context.getInstallRoot();
    }

    public Logger getLogger(String suffix, String resourceBundleName) throws MissingResourceException, JBIException {
        return context.getLogger(suffix, resourceBundleName);
    }

    public MBeanNames getMBeanNames() {
        return context.getMBeanNames();
    }

    public MBeanServer getMBeanServer() {
        return context.getMBeanServer();
    }

    public InitialContext getNamingContext() {
        return context.getNamingContext();
    }

    public Object getTransactionManager() {
        return context.getTransactionManager();
    }

    public String getWorkspaceRoot() {
        return context.getWorkspaceRoot();
    }

    public void registerExternalEndpoint(ServiceEndpoint externalEndpoint) throws JBIException {
        throw new UnsupportedOperationException();
    }

    public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
        return context.resolveEndpointReference(epr);
    }
    
}
