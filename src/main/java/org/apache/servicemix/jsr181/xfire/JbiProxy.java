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

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.Definition;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import org.codehaus.xfire.XFire;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.client.XFireProxyFactory;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.service.ServiceFactory;
import org.w3c.dom.Document;

import com.ibm.wsdl.Constants;

public class JbiProxy {
    
    protected XFire xfire;
    protected ComponentContext context;
    protected QName interfaceName;
    protected QName serviceName;
    protected String endpointName;
    protected Object proxy;
    protected Class serviceClass;
    protected Definition description;
    protected ServiceEndpoint endpoint;
    
    public static Object create(XFire xfire,
                                ComponentContext context,
                                QName interfaceName,
                                QName serviceName,
                                String endpointName,
                                Class serviceClass) throws Exception {
        JbiProxy p = new JbiProxy(xfire, context, serviceClass, interfaceName, serviceName, endpointName);
        return p.getProxy();
    }
    
    public JbiProxy(XFire xfire,
                    ComponentContext context,
                    Class serviceClass,
                    Definition description) throws Exception {
        this.xfire = xfire;
        this.context = context;
        this.serviceClass = serviceClass;
        this.description = description;
    }
    
    public JbiProxy(XFire xfire,
                    ComponentContext context,
                    Class serviceClass,
                    QName interfaceName,
                    QName serviceName,
                    String endpointName) throws Exception {
        this.xfire = xfire;
        this.context = context;
        this.interfaceName = interfaceName;
        this.serviceName = serviceName;
        this.endpointName = endpointName;
        this.serviceClass = serviceClass;
    }
    
    public Object getProxy() throws Exception {
        if (proxy == null) {
            ServiceFactory factory = ServiceFactoryHelper.findServiceFactory(xfire, serviceClass, null, null);
            Service service = factory.create(serviceClass, null, getDescription(), null);
            JBIClient client = new JBIClient(xfire, service);
            if (interfaceName != null) {
                client.getService().setProperty(JbiChannel.JBI_INTERFACE_NAME, interfaceName);
            }
            if (serviceName != null) {
                client.getService().setProperty(JbiChannel.JBI_SERVICE_NAME, serviceName);
            }
            if (endpoint != null) {
                client.getService().setProperty(JbiChannel.JBI_ENDPOINT, endpoint);
            }
            XFireProxyFactory xpf = new XFireProxyFactory(xfire);
            proxy = xpf.create(client);
        }
        return proxy;
    }
    
    public Definition getDescription() throws Exception {
        if (this.description == null) {
            ServiceEndpoint[] endpoints = getEndpoints();
            if (endpoints == null || endpoints.length == 0) {
                throw new IllegalStateException("No endpoints found for interface " + interfaceName + ", serviceName " + serviceName + " and endpoint " + endpointName);
            }
            ServiceEndpoint endpoint = chooseEndpoint(endpoints);
            if (endpoint == null) {
                throw new IllegalStateException("No suitable endpoint found");
            }
            if (serviceName != null && endpointName != null) {
                this.endpoint = endpoint;
            }
            Document doc = context.getEndpointDescriptor(endpoint);
            WSDLReader reader = WSDLFactory.newInstance().newWSDLReader(); 
            reader.setFeature(Constants.FEATURE_VERBOSE, false);
            this.description = reader.readWSDL(null, doc);
        }
        return this.description;
    }
    
    protected ServiceEndpoint[] getEndpoints() throws JBIException {
        ServiceEndpoint[] endpoints;
        if (endpointName != null && serviceName != null) {
            ServiceEndpoint endpoint = context.getEndpoint(serviceName, endpointName);
            if (endpoint == null) {
                endpoints = new ServiceEndpoint[0];
            } else {
                this.endpoint = endpoint;
                endpoints = new ServiceEndpoint[] { endpoint };
            }
        } else if (serviceName != null) {
            endpoints = context.getEndpointsForService(serviceName);
        } else if (interfaceName != null) {
            endpoints = context.getEndpoints(interfaceName);
        } else {
            throw new IllegalStateException("One of interfaceName or serviceName should be provided");
        }
        return endpoints;
    }
    
    protected ServiceEndpoint chooseEndpoint(ServiceEndpoint[] endpoints) throws JBIException {
        for (int i = 0; i < endpoints.length; i++) {
            if (context.getEndpointDescriptor(endpoints[i]) != null) {
                return endpoints[i];
            }
        }
        return null;
    }
    
    protected static class JBIClient extends Client {

        public JBIClient(XFire xfire, Service service) throws Exception {
            super(xfire.getTransportManager().getTransport(JbiTransport.JBI_BINDING),
                  service, 
                  null);
            setXFire(xfire);
        }
        
    }
}
