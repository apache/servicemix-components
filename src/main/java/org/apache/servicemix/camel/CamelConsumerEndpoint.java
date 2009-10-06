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

import org.apache.camel.Exchange;
import org.apache.servicemix.common.endpoints.ConsumerEndpoint;
import org.apache.servicemix.common.util.URIResolver;
import org.apache.servicemix.id.IdGenerator;
import org.apache.servicemix.jbi.exception.FaultException;

/**
 * A consumer endpoint that will be used to send JBI exchanges
 * originating from camel.
 */
public class CamelConsumerEndpoint extends ConsumerEndpoint {

    public static final QName SERVICE_NAME = new QName("http://camel.apache.org/schema/jbi", "provider");

    private JbiBinding binding;
    
    private JbiEndpoint jbiEndpoint;    
    
    public CamelConsumerEndpoint(JbiBinding binding, JbiEndpoint jbiEndpoint) {
        setService(SERVICE_NAME);
        setEndpoint(new IdGenerator().generateId());
        this.binding = binding;
        this.jbiEndpoint = jbiEndpoint;
    }

    public void process(MessageExchange exchange) throws Exception {
        // we don't expect any asynchronous MessageExchange callbacks because we're using sendSync
        logger.error("Unexpected MessageExchange received: " + exchange);
    }

    public void process(Exchange exchange) throws Exception {
        try {
            MessageExchange messageExchange = binding.makeJbiMessageExchange(exchange, getExchangeFactory(), jbiEndpoint.getMep());

            if (jbiEndpoint.getOperation() != null) {
                messageExchange.setOperation(jbiEndpoint.getOperation());
            }

            URIResolver.configureExchange(messageExchange, getContext(), jbiEndpoint.getDestinationUri());

            sendSync(messageExchange);

            processReponse(messageExchange, exchange);

        } catch (MessagingException e) {
            exchange.setException(e);
            throw new JbiException(e);
        } 
    }
    
    private void processReponse(MessageExchange messageExchange, Exchange exchange) throws MessagingException {
        if (messageExchange.getStatus() == ExchangeStatus.ERROR) {
            exchange.setException(messageExchange.getError());
        } else if (messageExchange.getStatus() == ExchangeStatus.ACTIVE) {
            // first copy the exchange headers
	        binding.copyHeadersFromJbiToCamel(messageExchange, exchange);
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
}
