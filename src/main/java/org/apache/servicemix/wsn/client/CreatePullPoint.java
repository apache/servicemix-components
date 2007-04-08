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

import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.client.ServiceMixClientFacade;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.resolver.ServiceNameEndpointResolver;
import org.oasis_open.docs.wsn.b_2.CreatePullPointResponse;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;

public class CreatePullPoint extends AbstractWSAClient {

    public static final String WSN_URI = "http://servicemix.org/wsnotification";

    public static final String WSN_SERVICE = "CreatePullPoint";

    public static final QName NOTIFICATION_BROKER = new QName(WSN_URI, WSN_SERVICE);

    public CreatePullPoint(ComponentContext context) throws JAXBException {
        ServiceMixClientFacade client = new ServiceMixClientFacade(context);
        client.setMarshaler(new JAXBMarshaler(JAXBContext.newInstance(Subscribe.class, RegisterPublisher.class)));
        setClient(client);
        setResolver(new ServiceNameEndpointResolver(NOTIFICATION_BROKER));
    }

    public CreatePullPoint(ComponentContext context, String brokerName) throws JAXBException {
        setClient(createJaxbClient(context));
        setEndpoint(createWSA(WSN_URI + "/" + WSN_SERVICE + "/" + brokerName));
        setResolver(resolveWSA(getEndpoint()));
    }

    public CreatePullPoint(JBIContainer container) throws JBIException, JAXBException {
        setClient(createJaxbClient(container));
        setResolver(new ServiceNameEndpointResolver(NOTIFICATION_BROKER));
    }

    public CreatePullPoint(JBIContainer container, String brokerName) throws JBIException, JAXBException {
        setClient(createJaxbClient(container));
        setEndpoint(createWSA(WSN_URI + "/" + WSN_SERVICE + "/" + brokerName));
        setResolver(resolveWSA(getEndpoint()));
    }

    public CreatePullPoint(ServiceMixClient client) {
        setClient(client);
        setResolver(new ServiceNameEndpointResolver(NOTIFICATION_BROKER));
    }

    public CreatePullPoint(ServiceMixClient client, String brokerName) {
        setClient(client);
        setEndpoint(createWSA(WSN_URI + "/" + WSN_SERVICE + "/" + brokerName));
        setResolver(resolveWSA(getEndpoint()));
    }

    public PullPoint createPullPoint() throws JBIException {
        CreatePullPointResponse response = (CreatePullPointResponse) request(
                new org.oasis_open.docs.wsn.b_2.CreatePullPoint());
        return new PullPoint(response.getPullPoint(), getClient());
    }

}
