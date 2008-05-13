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

import java.net.URISyntaxException;
import java.util.Map;

import org.apache.camel.Consumer;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.util.URISupport;

/**
 * Represents an {@link org.apache.camel.Endpoint} for interacting with JBI
 *
 * @version $Revision: 563665 $
 */
public class JbiEndpoint extends DefaultEndpoint<Exchange> {
    private Processor toJbiProcessor;

    private String destinationUri;

    private String mep;

    private String operation;

    private final CamelJbiComponent jbiComponent;

    public JbiEndpoint(CamelJbiComponent jbiComponent, String uri) {
        super(uri, jbiComponent);
        this.jbiComponent = jbiComponent;
        parseUri(uri);
        toJbiProcessor = new ToJbiProcessor(jbiComponent.getBinding(), jbiComponent.getComponentContext(), this);
    }

    public Producer<Exchange> createProducer() throws Exception {
        return new DefaultProducer<Exchange>(this) {
            public void process(Exchange exchange) throws Exception {
                toJbiProcessor.process(exchange);
            }
        };
    }

    private void parseUri(String uri) {
        destinationUri = uri;
        try {
            int idx = destinationUri.indexOf('?');
            if (idx > 0) {
                Map params = URISupport.parseQuery(destinationUri.substring(idx + 1));
                mep = (String) params.get("mep");
                if (mep != null && !mep.startsWith("http://www.w3.org/ns/wsdl/")) {
                    mep = "http://www.w3.org/ns/wsdl/" + mep;
                }
                operation = (String) params.get("operation");
                this.destinationUri = destinationUri.substring(0, idx);
            }
        } catch (URISyntaxException e) {
            throw new JbiException(e);
        }
    }

    public void setMep(String str) {
        mep = str;
    }

    public void setOperation(String str) {
        operation = str;
    }

    public void setDestionationUri(String str) {
        destinationUri = str;
    }

    public String getMep() {
        return mep;
    }

    public String getOperation() {
        return operation;
    }

    public String getDestinationUri() {
        return destinationUri;
    }

    public Consumer<Exchange> createConsumer(final Processor processor) throws Exception {
        return new DefaultConsumer<Exchange>(this, processor) {
            CamelJbiEndpoint jbiEndpoint;

            @Override
            protected void doStart() throws Exception {
                super.doStart();
                jbiEndpoint = jbiComponent.activateJbiEndpoint(JbiEndpoint.this, processor);
            }

            @Override
            protected void doStop() throws Exception {
                if (jbiEndpoint != null) {
                    jbiComponent.deactivateJbiEndpoint(jbiEndpoint);
                }
                super.doStop();
            }
        };
    }

    public JbiExchange createExchange() {
        return new JbiExchange(getContext(), getBinding());
    }

    public JbiBinding getBinding() {
        return jbiComponent.getBinding();
    }

    public boolean isSingleton() {
        return true;
    }
}
