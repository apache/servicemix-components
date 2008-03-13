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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.jbi.servicedesc.ServiceEndpoint;

import org.apache.servicemix.common.BaseServiceUnitManager;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.Deployer;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.jbi.util.IntrospectionSupport;
import org.apache.servicemix.jbi.util.URISupport;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * A JBI component for binding beans to the JBI bus which work directly off of the JBI messages
 * without requiring any SOAP Processing. If you require support for SOAP, JAX-WS, JSR-181 then you
 * should use the servicemix-jsr181 module instead.
 *
 * @version $Revision: $
 * @org.apache.xbean.XBean element="component" description="Bean Component"
 */
public class BeanComponent extends DefaultComponent implements ApplicationContextAware {

    private BeanEndpoint[] endpoints;
    private String[] searchPackages;
    private ApplicationContext applicationContext;

    /* (non-Javadoc)
     * @see org.servicemix.common.BaseComponent#createServiceUnitManager()
     */
    @Override
    public BaseServiceUnitManager createServiceUnitManager() {
        Deployer[] deployers = new Deployer[] {new BeanXBeanDeployer(this) };
        return new BaseServiceUnitManager(this, deployers);
    }

    public BeanEndpoint[] getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(BeanEndpoint[] endpoints) {
        this.endpoints = endpoints;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public String[] getSearchPackages() {
        return searchPackages;
    }

    public void setSearchPackages(String[] searchPackages) {
        this.searchPackages = searchPackages;
    }

    @SuppressWarnings("unchecked")
    protected List getConfiguredEndpoints() {
        List list = new ArrayList(asList(getEndpoints()));
        if (searchPackages != null) {
            EndpointFinder finder = new EndpointFinder(this);
            finder.addEndpoints(list);
        }
        return list;
    }

    protected Class[] getEndpointClasses() {
        return new Class[]{BeanEndpoint.class};
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
        } else {
            // it must be an absolute URI of the form bean:name
            beanName = uri.getSchemeSpecificPart();
        }
        if (beanName != null) {
            endpoint.setBeanName(beanName);
        } else {
            throw new IllegalArgumentException("No bean name defined for URI: "
                    + uri + ". Please use a URI of bean:name or bean://name?property=value");
        }

        Map map = URISupport.parseQuery(uri.getQuery());
        if (endpoint.getBean() == null) {
            endpoint.setBean(endpoint.createBean());
        }
        IntrospectionSupport.setProperties(endpoint.getBean(), map);

        endpoint.activate();
        return endpoint;
    }

    /**
     * Adds a new component dynamically
     */
    public void addEndpoint(BeanEndpoint endpoint) throws Exception {
        super.addEndpoint(endpoint);
    }
}
