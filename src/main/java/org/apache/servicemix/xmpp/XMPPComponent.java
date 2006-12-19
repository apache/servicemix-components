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
package org.apache.servicemix.xmpp;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.jbi.servicedesc.ServiceEndpoint;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.jbi.util.IntrospectionSupport;
import org.apache.servicemix.jbi.util.URISupport;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;

/**
 * A JBI component for binding POJOs to the JBI bus which work directly off of the JBI messages
 * without requiring any SOAP Processing. If you require support for SOAP, JAX-WS, JSR-181 then you
 * should use the servicemix-jsr181 module instead.
 *
 * @version $Revision: $
 * @org.apache.xbean.XBean element="component" description="POJO Component"
 */
public class XMPPComponent extends DefaultComponent implements BeanFactoryAware {

    private XMPPEndpoint[] endpoints;
    private BeanFactory beanFactory;
    private String user;
    private String password;


    public XMPPEndpoint[] getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(XMPPEndpoint[] endpoints) {
        this.endpoints = endpoints;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @org.apache.xbean.Property hidden="true"
     */
    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    protected List getConfiguredEndpoints() {
        return asList(getEndpoints());
    }

    protected Class[] getEndpointClasses() {
        return new Class[]{XMPPEndpoint.class};
    }

    protected Endpoint getResolvedEPR(ServiceEndpoint ep) throws Exception {
        XMPPEndpoint endpoint = createEndpoint(ep);
        endpoint.activate();
        return endpoint;
    }

    /**
     * A factory method for creating endpoints from a service endpoint
     * which is public so that it can be easily unit tested
     */
    public XMPPEndpoint createEndpoint(ServiceEndpoint ep) throws URISyntaxException {
        URI uri = new URI(ep.getEndpointName());
        Map map = URISupport.parseQuery(uri.getQuery());

        XMPPEndpoint endpoint = null;

        // TODO how do we decide whether to use group or private chat from the URI in a nicer way?
        String room = (String) map.get("room");
        if (room != null) {
            endpoint = new GroupChatEndpoint(this, ep, room);
        }
        else {
            endpoint = new PrivateChatEndpoint(this, ep);
        }

        IntrospectionSupport.setProperties(endpoint, map);
        IntrospectionSupport.setProperties(endpoint.getMarshaler(), map, "marshal.");

        // TODO
        //endpoint.setRole(MessageExchange.Role.PROVIDER);

        endpoint.setUri(uri);

        return endpoint;
    }

}
