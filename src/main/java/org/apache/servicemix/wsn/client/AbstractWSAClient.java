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
package org.apache.servicemix.wsn.client;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.client.ServiceMixClientFacade;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.resolver.EndpointResolver;
import org.apache.servicemix.jbi.resolver.ServiceAndEndpointNameResolver;
import org.apache.servicemix.jbi.resolver.URIResolver;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;
import org.w3._2005._08.addressing.AttributedURIType;
import org.w3._2005._08.addressing.EndpointReferenceType;

public abstract class AbstractWSAClient {

    private EndpointReferenceType endpoint;

    private EndpointResolver resolver;

    private ServiceMixClient client;

    public AbstractWSAClient() {
    }

    public AbstractWSAClient(EndpointReferenceType endpoint, ServiceMixClient client) {
        this.endpoint = endpoint;
        this.resolver = resolveWSA(endpoint);
        this.client = client;
    }

    public static EndpointReferenceType createWSA(String address) {
        EndpointReferenceType epr = new EndpointReferenceType();
        AttributedURIType attUri = new AttributedURIType();
        attUri.setValue(address);
        epr.setAddress(attUri);
        return epr;
    }

    public static ServiceMixClient createJaxbClient(JBIContainer container) throws JBIException, JAXBException {
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        client.setMarshaler(new JAXBMarshaler(JAXBContext.newInstance(Subscribe.class, RegisterPublisher.class)));
        return client;
    }

    public static ServiceMixClient createJaxbClient(ComponentContext context) throws JAXBException {
        ServiceMixClientFacade client = new ServiceMixClientFacade(context);
        client.setMarshaler(new JAXBMarshaler(JAXBContext.newInstance(Subscribe.class, RegisterPublisher.class)));
        return client;
    }

    public static EndpointResolver resolveWSA(EndpointReferenceType ref) {
        String[] parts = URIResolver.split3(ref.getAddress().getValue());
        return new ServiceAndEndpointNameResolver(new QName(parts[0], parts[1]), parts[2]);
    }

    public EndpointReferenceType getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(EndpointReferenceType endpoint) {
        this.endpoint = endpoint;
    }

    public EndpointResolver getResolver() {
        return resolver;
    }

    public void setResolver(EndpointResolver resolver) {
        this.resolver = resolver;
    }

    public ServiceMixClient getClient() {
        return client;
    }

    public void setClient(ServiceMixClient client) {
        this.client = client;
    }

    protected Object request(Object request) throws JBIException {
        return client.request(resolver, null, null, request);
    }

    protected void send(Object request) throws JBIException {
        client.sendSync(resolver, null, null, request);
    }

}
