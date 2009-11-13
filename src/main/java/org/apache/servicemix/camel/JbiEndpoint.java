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
import java.util.concurrent.Callable;

import javax.xml.namespace.QName;

import org.apache.camel.Consumer;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultConsumer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.impl.DefaultProducer;
import org.apache.camel.spi.HeaderFilterStrategy;
import org.apache.camel.spi.HeaderFilterStrategyAware;
import org.apache.camel.spi.Registry; 
import org.apache.camel.util.URISupport;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

/**
 * Represents an {@link org.apache.camel.Endpoint} for interacting with JBI
 *
 * @version $Revision: 563665 $
 */
public class JbiEndpoint extends DefaultEndpoint implements HeaderFilterStrategyAware {

    private static final String STRICT_SERIALIZATION = "strict";

    private String destinationUri;

    private String mep;

    private QName operation;

    private JbiProducer producer;
    
    private boolean convertExceptions;
    
    private boolean strictSerialization;

    private HeaderFilterStrategy headerFilterStrategy;    
    
    private final JbiComponent jbiComponent;

    private final JbiBinding binding;

    public JbiEndpoint(JbiComponent jbiComponent, String uri) {
        super(uri, jbiComponent);
        this.jbiComponent = jbiComponent;
        parseUri(uri);

        //now create the binding based on the information read from the URI
        this.binding = createBinding();
    }
    
    public JbiBinding createBinding() {
        JbiBinding result = new JbiBinding(this.getCamelContext(), strictSerialization);
        result.setConvertExceptions(convertExceptions);
        result.addHeaderFilterStrategy(headerFilterStrategy);
        return result;
    }

    public synchronized Producer createProducer() throws Exception {
        if (producer == null) {
            producer = new JbiProducer(this);
        }
        return producer;
    }

    protected class JbiProducer extends DefaultProducer {
        
        private final Log log = LogFactory.getLog(JbiProducer.class);

        private CamelConsumerEndpoint consumer;

        public JbiProducer(Endpoint exchangeEndpoint) {
            super(exchangeEndpoint);
        }

        @Override
        public void start() throws Exception {
            consumer = new CamelConsumerEndpoint(binding, JbiEndpoint.this);
            jbiComponent.getCamelJbiComponent().addEndpoint(consumer);
            super.start();
        }
        
        @Override
        public void stop() throws Exception {
            if (isStopped()) {
                log.debug("Camel producer for " + super.getEndpoint() + " has already been stopped");
            } else {
                log.debug("Stopping Camel producer for " + super.getEndpoint());
                jbiComponent.getCamelJbiComponent().removeEndpoint(consumer);
                super.stop();
            }
        }

        public void process(final Exchange exchange) throws Exception {
            binding.runWithCamelContextClassLoader(new Callable<Object>() {                
                public Object call() throws Exception {
                    consumer.process(exchange);
                    return null;
                }
            });
        }
        
        /*
         * Access the underlying JBI Consumer endpoint
         */
        protected CamelConsumerEndpoint getCamelConsumerEndpoint() {
            return consumer;
        }
    }

    @SuppressWarnings("unchecked")
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
                String oper = (String) params.get("operation");
                if (StringUtils.hasLength(oper)) {
                    operation = QName.valueOf(oper);
                }
                this.destinationUri = destinationUri.substring(0, idx);
                
                String filter = (String) params.get("headerFilterStrategy");
                if (StringUtils.hasLength(filter)) {
                	Registry registry = jbiComponent.getCamelContext().getRegistry();
                	if((idx = filter.indexOf('#')) != -1) {
                		filter = filter.substring(1);
                	}
                	Object object = registry.lookup(filter);
                	if (object instanceof HeaderFilterStrategy) {
                		headerFilterStrategy = (HeaderFilterStrategy)object;
                	}
                	params.remove("headerFilterStrategy");
                }
                String convertExceptions = (String) params.get("convertExceptions");
                if (StringUtils.hasLength(convertExceptions)) {
                    this.setConvertExceptions(Boolean.valueOf(convertExceptions));
                	params.remove("convertExceptions");
                }
                String serialization = (String) params.get("serialization");
                if (StringUtils.hasLength(serialization)) {
                	this.setStrictSerialization(STRICT_SERIALIZATION.equalsIgnoreCase(serialization));
                	params.remove("serialization");
                }
                String endpointUri = this.destinationUri + URISupport.createQueryString(params);
                this.setEndpointUri(endpointUri);
            }
        } catch (URISyntaxException e) {
            throw new JbiException(e);
        }
    }

    public void setMep(String str) {
        mep = str;
    }

    public void setOperation(QName operation) {
        this.operation = operation;
    }

    public void setDestionationUri(String str) {
        destinationUri = str;
    }

    public String getMep() {
        return mep;
    }

    public QName getOperation() {
        return operation;
    }

    public String getDestinationUri() {
        return destinationUri;
    }

    public Consumer createConsumer(final Processor processor) throws Exception {
        return new DefaultConsumer(this, processor) {
            private CamelProviderEndpoint jbiEndpoint;

            @Override
            protected void doStart() throws Exception {
                super.doStart();
                jbiEndpoint = jbiComponent.createJbiEndpointFromCamel(JbiEndpoint.this, processor);
                jbiComponent.getCamelJbiComponent().activateJbiEndpoint(jbiEndpoint);
            }

            @Override
            protected void doStop() throws Exception {
                if (jbiEndpoint != null) {
                    jbiComponent.getCamelJbiComponent().deactivateJbiEndpoint(jbiEndpoint);
                }
                super.doStop();
            }
        };
    }

    public boolean isSingleton() {
        return true;
    }
    
    public HeaderFilterStrategy getHeaderFilterStrategy() {
        return headerFilterStrategy;
    }

    public void setHeaderFilterStrategy(HeaderFilterStrategy strategy) {
        this.headerFilterStrategy = strategy;
    }

    public void setConvertExceptions(boolean convertExceptions) {
        this.convertExceptions = convertExceptions;
    }

    public boolean isConvertExceptions() {
        return convertExceptions;
    }

    public void setStrictSerialization(boolean strictSerialization) {
        this.strictSerialization = strictSerialization;
    }

    public boolean isStrictSerialization() {
        return strictSerialization;
    }
}
