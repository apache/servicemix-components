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
package org.apache.servicemix.camel;

import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.camel.*;
import org.apache.camel.impl.DefaultComponent;
import org.apache.camel.processor.UnitOfWorkProcessor;
import org.apache.servicemix.common.util.URIResolver;
import org.apache.servicemix.id.IdGenerator;

public class JbiComponent extends DefaultComponent {

    private CamelComponent camelJbiComponent;
    private CamelContext camelContext;
    private IdGenerator idGenerator;
    private String suName;

    public JbiComponent(CamelComponent component) {
        setCamelJbiComponent(component);
    }
    
    protected JbiComponent() {
        super();
    }

    public CamelContext getCamelContext() {
        return camelContext;
    }

    public void setCamelContext(CamelContext context) {
        camelContext = context;
    }

    public final void setCamelJbiComponent(CamelComponent component) {
        this.camelJbiComponent = component;
        this.camelJbiComponent.addJbiComponent(this);
    }

    public CamelComponent getCamelJbiComponent() {
        return camelJbiComponent;
    }

    public void setSuName(String su) {
        suName = su;
    }

    public String getSuName() {
        return suName;
    }

    // Resolve Camel Endpoints
    // -------------------------------------------------------------------------
    public Endpoint createEndpoint(String uri) {
        if (uri.startsWith("jbi:")) {
            uri = uri.substring("jbi:".length());
            if (uri.startsWith("//")) {
                uri = uri.substring("//".length());
            }
            return new JbiEndpoint(this, uri);
        }
        return null;
    }


    protected CamelProviderEndpoint createJbiEndpointFromCamel(
            Endpoint camelEndpoint, AsyncProcessor processor) {
        CamelProviderEndpoint jbiEndpoint;
        String endpointUri = camelEndpoint.getEndpointUri();
        if (camelEndpoint instanceof JbiEndpoint) {
            QName service = null;
            String endpoint = null;
            if (endpointUri.startsWith("name:")) {
                endpoint = endpointUri.substring("name:".length());
                service = CamelProviderEndpoint.SERVICE_NAME;
            } else if (endpointUri.startsWith("endpoint:")) {
                String uri = endpointUri.substring("endpoint:".length());
                // lets decode "serviceNamespace sep serviceName sep
                // endpointName
                String[] parts;
                try {
                    parts = URIResolver.split3(uri);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Expected syntax jbi:endpoint:[serviceNamespace][sep][serviceName][sep][endpointName] "
                                    + "where sep = '/' or ':' depending on the serviceNamespace, but was given: "
                                    + endpointUri + ". Cause: " + e, e);
                }
                service = new QName(parts[0], parts[1]);
                endpoint = parts[2];
            } else if (endpointUri.startsWith("service:")) {
                String uri = endpointUri.substring("service:".length());
                // lets decode "serviceNamespace sep serviceName
                String[] parts;
                try {
                    parts = URIResolver.split2(uri);
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                            "Expected syntax jbi:endpoint:[serviceNamespace][sep][serviceName] "
                                    + "where sep = '/' or ':' depending on the serviceNamespace, but was given: "
                                    + endpointUri + ". Cause: " + e, e);
                }
                service = new QName(parts[0], parts[1]);
                endpoint = createEndpointName();
            } else {
                throw new IllegalArgumentException(
                        "Expected syntax jbi:endpoint:[serviceNamespace][sep][serviceName][sep][endpointName] "
                                + "or jbi:service:[serviceNamespace][sep][serviceName or jbi:name:[endpointName] but was given: "
                                + endpointUri);
            }
            jbiEndpoint = new CamelProviderEndpoint(getCamelJbiComponent()
                    .getServiceUnit(), service, endpoint, createBinding(camelEndpoint), processor);

        } else {
            jbiEndpoint = new CamelProviderEndpoint(getCamelJbiComponent()
                    .getServiceUnit(), camelEndpoint, createBinding(camelEndpoint), processor);
        }
        jbiEndpoint.setCamelEndpoint(camelEndpoint);
        return jbiEndpoint;
    }

    /*
     * Creates a binding for the given endpoint
     */
    protected JbiBinding createBinding(Endpoint camelEndpoint) {
        if (camelEndpoint instanceof JbiEndpoint) {
            return ((JbiEndpoint) camelEndpoint).createBinding();
        } else {
            return new JbiBinding(camelContext);
        }
    }

    protected String createEndpointName() {
        if (idGenerator == null) {
            idGenerator = new IdGenerator("camel");
        }
        return idGenerator.generateSanitizedId();
    }

    /**
     * Returns a JBI endpoint created for the given Camel endpoint
     */
    public CamelProviderEndpoint createJbiEndpointFromCamel(
            Endpoint camelEndpoint) {
        AsyncProcessor processor = createCamelProcessor(camelEndpoint);
        return createJbiEndpointFromCamel(camelEndpoint, processor);
    }

    protected AsyncProcessor createCamelProcessor(Endpoint camelEndpoint) {
        AsyncProcessor processor = null;
        try {
            processor = new UnitOfWorkProcessor(camelEndpoint.createProducer());
        } catch (Exception e) {
            throw new FailedToCreateProducerException(camelEndpoint, e);
        }
        return processor;
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters)
        throws Exception {
        return createEndpoint(uri);
    }
}
