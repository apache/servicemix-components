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

import javax.xml.namespace.QName;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.FailedToCreateProducerException;
import org.apache.camel.Processor;
import org.apache.servicemix.common.util.URIResolver;
import org.apache.servicemix.id.IdGenerator;

public class JbiComponent implements Component<Exchange> {
    private final CamelJbiComponent camelJbiComponent;
    private JbiBinding binding;
    private CamelContext camelContext;
    private IdGenerator idGenerator;
    private String suName;

    public JbiComponent(CamelJbiComponent component) {
        camelJbiComponent = component;
        camelJbiComponent.addJbiComponent(this);
    }

    public CamelContext getCamelContext() {
        return camelContext;
    }

    public void setCamelContext(CamelContext context) {
        camelContext = context;
    }

    public CamelJbiComponent getCamelJbiComponent() {
        return camelJbiComponent;
    }

    public void setSuName(String su) {
        suName = su;
    }

    public String getSuName() {
        return suName;
    }

    /**
     * @return the binding
     */
    public JbiBinding getBinding() {
        if (binding == null) {
            binding = new JbiBinding();
        }
        return binding;
    }

    /**
     * @param binding
     *            the binding to set
     */
    public void setBinding(JbiBinding binding) {
        this.binding = binding;
    }

    // Resolve Camel Endpoints
    // -------------------------------------------------------------------------
    public Endpoint<Exchange> createEndpoint(String uri) {
        if (uri.startsWith("jbi:")) {
            uri = uri.substring("jbi:".length());
            return new JbiEndpoint(this, uri);
        }
        return null;
    }


    protected CamelProviderEndpoint createJbiEndpointFromCamel(
            Endpoint camelEndpoint, Processor processor) {
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
                    .getServiceUnit(), service, endpoint, camelEndpoint,
                    getBinding(), processor);
        } else {
            jbiEndpoint = new CamelProviderEndpoint(getCamelJbiComponent()
                    .getServiceUnit(), camelEndpoint, getBinding(), processor);
        }
        return jbiEndpoint;
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
        Processor processor = createCamelProcessor(camelEndpoint);
        return createJbiEndpointFromCamel(camelEndpoint, processor);
    }

    protected Processor createCamelProcessor(Endpoint camelEndpoint) {
        Processor processor = null;
        try {
            processor = camelEndpoint.createProducer();
        } catch (Exception e) {
            throw new FailedToCreateProducerException(camelEndpoint, e);
        }
        return processor;
    }



}
