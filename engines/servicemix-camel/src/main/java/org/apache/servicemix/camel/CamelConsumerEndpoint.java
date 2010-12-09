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

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;

import org.apache.camel.AsyncCallback;
import org.apache.camel.AsyncProcessor;
import org.apache.camel.Exchange;
import org.apache.servicemix.common.endpoints.ConsumerEndpoint;
import org.apache.servicemix.common.util.URIResolver;
import org.apache.servicemix.id.IdGenerator;
import org.apache.servicemix.jbi.exception.FaultException;

import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A consumer endpoint that will be used to send JBI exchanges
 * originating from camel.
 */
public class CamelConsumerEndpoint extends ConsumerEndpoint implements AsyncProcessor {

    public static final QName SERVICE_NAME = new QName("http://camel.apache.org/schema/jbi", "provider");

    private JbiBinding binding;
    
    private JbiEndpoint jbiEndpoint;

    private Map<String, ContinuationData> continuations = new ConcurrentHashMap<String, ContinuationData>();

    public CamelConsumerEndpoint(JbiBinding binding, JbiEndpoint jbiEndpoint) {
        setService(SERVICE_NAME);
        setEndpoint(new IdGenerator().generateId());
        this.binding = binding;
        this.jbiEndpoint = jbiEndpoint;
    }

    /**
     * Process the JBI MessageExchange that is being delivered asynchronously as a response to what was sent by
     * the {@link #process(Exchange, AsyncCallback)} method
     */
    public void process(final MessageExchange messageExchange) throws Exception {
        final ContinuationData data = continuations.remove(messageExchange.getExchangeId());

        if (data == null) {
            logger.error("Unexpected MessageExchange received: " + messageExchange);
        } else {
            binding.runWithCamelContextClassLoader(new Callable<Object>() {
                public Object call() throws Exception {
                    processReponse(messageExchange, data.exchange);
                    data.callback.done(false);
                    return null;
                }
            });            
        }
    }

    /**
     * Process the Camel Exchange by sending/receiving a JBI MessageExchange synchronously
     */
    public void process(Exchange exchange) throws Exception {
        try {
            MessageExchange messageExchange = prepareMessageExchange(exchange);

            sendSync(messageExchange);

            processReponse(messageExchange, exchange);

        } catch (MessagingException e) {
            exchange.setException(e);
            throw new JbiException(e);
        }
    }

    /**
     * Process the Camel Exchange by sending a JBI MessageExchange asynchronously, where the response
     * will be handled by the {@link #process(javax.jbi.messaging.MessageExchange)} method
     */
    public boolean process(Exchange exchange, AsyncCallback asyncCallback) {
        MessageExchange messageExchange = null;
        try {
            messageExchange = prepareMessageExchange(exchange);

            continuations.put(messageExchange.getExchangeId(),
                              new ContinuationData(exchange, asyncCallback));

            send(messageExchange);

            return false;
        } catch (Exception e) {
            if (messageExchange != null) {
                continuations.remove(messageExchange.getExchangeId());
            }

            exchange.setException(e);
            asyncCallback.done(true);
            return true;
        }
    }

    /*
     * Create and configure a JBI MessageExchange for a given Camel Exchange
     */
    private MessageExchange prepareMessageExchange(Exchange exchange) throws MessagingException, URISyntaxException {
        MessageExchange messageExchange = binding.makeJbiMessageExchange(exchange, getExchangeFactory(), jbiEndpoint.getMep());

        if (jbiEndpoint.getOperation() != null) {
            messageExchange.setOperation(jbiEndpoint.getOperation());
        }

        URIResolver.configureExchange(messageExchange, getContext(), jbiEndpoint.getDestinationUri());
        return messageExchange;
    }    

    /*
     * Process a JBI response message by updating the corresponding Camel exchange.  
     */
    private void processReponse(MessageExchange messageExchange, Exchange exchange) throws MessagingException {
        if (messageExchange.getStatus() == ExchangeStatus.ERROR) {
            exchange.setException(messageExchange.getError());
        } else if (messageExchange.getStatus() == ExchangeStatus.ACTIVE) {
            // first copy the exchange headers
            binding.copyPropertiesFromJbiToCamel(messageExchange, exchange);
            // then copy the out/fault message
            if (messageExchange.getFault() != null) {
                binding.copyFromJbiToCamel(messageExchange.getMessage("fault"), exchange.getOut());
                exchange.getOut().setBody(new FaultException("Fault occured for " + exchange.getPattern() + " exchange", 
                        messageExchange, messageExchange.getFault()));
                exchange.getOut().setFault(true);
            } else if (messageExchange.getMessage("out") != null) {
                binding.copyFromJbiToCamel(messageExchange.getMessage("out"), exchange.getOut());
            }
            done(messageExchange);
        }
    }

    @Override
    public void validate() throws DeploymentException {
        // No validation required
    }

    /**
     * Provides read-only access to the underlying map of continuation data 
     */
    protected Map<String, ContinuationData> getContinuationData() {
        return Collections.unmodifiableMap(continuations);
    }

    /**
     * Access the underlying Camel {@link org.apache.camel.Endpoint}
     */
    protected JbiEndpoint getJbiEndpoint() {
        return jbiEndpoint;
    }

    /**
     * Encapsulates all the data necessary to continue processing the Camel Exchange
     */
    private static final class ContinuationData {

        private final Exchange exchange;
        private final AsyncCallback callback;

        private ContinuationData(Exchange exchange, AsyncCallback callback) {
            super();
            this.exchange = exchange;
            this.callback = callback;
        }
    }
}
