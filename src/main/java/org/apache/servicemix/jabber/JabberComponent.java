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
package org.apache.servicemix.jabber;

import org.apache.activemq.util.URISupport;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ResolvedEndpoint;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.w3c.dom.DocumentFragment;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * A JBI component for binding POJOs to the JBI bus which work directly off of the JBI messages
 * without requiring any SOAP Processing. If you require support for SOAP, JAX-WS, JSR-181 then you
 * should use the servicemix-jsr181 module instead.
 *
 * @version $Revision: $
 * @org.apache.xbean.XBean element="component" description="POJO Component"
 */
public class JabberComponent extends DefaultComponent implements BeanFactoryAware {

    public final static String EPR_URI = "urn:servicemix:jabber";
    public final static QName EPR_SERVICE = new QName(EPR_URI, "JabberComponent");
    public final static String EPR_NAME = "epr";

    private JabberEndpoint[] endpoints;
    private BeanFactory beanFactory;


    public JabberEndpoint[] getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(JabberEndpoint[] endpoints) {
        this.endpoints = endpoints;
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
        return ResolvedEndpoint.resolveEndpoint(epr, EPR_URI, EPR_NAME, EPR_SERVICE, "jabber:");
    }

    protected List getConfiguredEndpoints() {
        return asList(getEndpoints());
    }

    protected Class[] getEndpointClasses() {
        return new Class[]{JabberEndpoint.class};
    }

    protected QName getEPRServiceName() {
        return EPR_SERVICE;
    }

    protected Endpoint getResolvedEPR(ServiceEndpoint ep) throws Exception {
        URI uri = new URI(ep.getEndpointName());
        Map map = URISupport.parseQuery(uri.getQuery());

        JabberEndpoint endpoint = null;
        // TODO how do we decide whether to use group or private chat from the URI??
        String room = (String) map.get("room");
        if (room != null) {
            endpoint = new GroupChatEndpoint(this, ep, room);
        }
        else {
            endpoint = new PrivateChatEndpoint(this, ep);
        }

        // TODO
        //endpoint.setRole(MessageExchange.Role.PROVIDER);

        endpoint.setUri(uri);

        // TODO
        // IntrospectionSupport.setProperties(endpoint.getBean(), map);

        endpoint.activate();
        return endpoint;
    }

}
