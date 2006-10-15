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
package org.apache.servicemix.bean;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ResolvedEndpoint;
import org.apache.servicemix.jbi.util.IntrospectionSupport;
import org.apache.servicemix.jbi.util.URISupport;
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
 * A JBI component for binding beans to the JBI bus which work directly off of the JBI messages
 * without requiring any SOAP Processing. If you require support for SOAP, JAX-WS, JSR-181 then you
 * should use the servicemix-jsr181 module instead.
 *
 * @org.apache.xbean.XBean element="component" description="Bean Component"
  *
  * @version $Revision: $
 */
public class BeanComponent extends DefaultComponent implements BeanFactoryAware {

    public final static String EPR_URI = "urn:servicemix:bean";
    public final static QName EPR_SERVICE = new QName(EPR_URI, "BeanComponent");
    public final static String EPR_NAME = "epr";

    private BeanEndpoint[] endpoints;
    private BeanFactory beanFactory;


    public BeanEndpoint[] getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(BeanEndpoint[] endpoints) {
        this.endpoints = endpoints;
    }

    public BeanFactory getBeanFactory() {
        return beanFactory;
    }

    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
        return ResolvedEndpoint.resolveEndpoint(epr, EPR_URI, EPR_NAME, EPR_SERVICE, "bean:");
    }

    protected List getConfiguredEndpoints() {
        return asList(getEndpoints());
    }

    protected Class[] getEndpointClasses() {
        return new Class[]{BeanEndpoint.class};
    }

    protected QName getEPRServiceName() {
        return EPR_SERVICE;
    }

    protected Endpoint getResolvedEPR(ServiceEndpoint ep) throws Exception {
        // We receive an exchange for an EPR that has not been used yet.
        // Register a provider endpoint and restart processing.
        BeanEndpoint endpoint = new BeanEndpoint(this, ep);

        // TODO
        //endpoint.setRole(MessageExchange.Role.PROVIDER);

        // lets use a URL to parse the path
        URI uri = new URI(ep.getEndpointName());

        String beanName = null;
        // lets try the host first for hierarchial URIs
        if (uri.getHost() != null) {
            // it must start bean://host/path
            beanName = uri.getHost();
        }
        else {
            // it must be an absolute URI of the form bean:name
            beanName = uri.getSchemeSpecificPart();
        }
        if (beanName != null) {
            endpoint.setBeanName(beanName);
        }
        else {
            throw new IllegalArgumentException("No bean name defined for URI: " + uri + ". Please use a URI of bean:name or bean://name?property=value");
        }

        Map map = URISupport.parseQuery(uri.getQuery());
        if (endpoint.getBean() == null) {
            endpoint.setBean(endpoint.createBean());
        }
        IntrospectionSupport.setProperties(endpoint.getBean(), map);

        endpoint.activate();
        return endpoint;
    }

}
