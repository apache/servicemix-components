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
package org.apache.servicemix.jms.multiplexing;

import java.io.IOException;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.naming.InitialContext;

import org.apache.servicemix.jms.AbstractJmsProcessor;
import org.apache.servicemix.jms.JmsEndpoint;
import org.apache.servicemix.soap.SoapFault;
import org.apache.servicemix.soap.marshalers.SoapMessage;

public class MultiplexingProviderProcessor extends AbstractJmsProcessor implements MessageListener {

    
    protected MessageConsumer consumer;
    protected MessageProducer producer;
//    protected DeliveryChannel channel;

    public MultiplexingProviderProcessor(JmsEndpoint endpoint) throws Exception {
        super(endpoint);
    }
   
    protected void doInit(InitialContext ctx) throws Exception {
//        channel = endpoint.getServiceUnit().getComponent().getComponentContext().getDeliveryChannel();
        commonDoStartTasks(ctx);
        //Create temp destination of no reply destination found.
        if (endpoint.getJndiReplyToName() == null && endpoint.getJmsProviderReplyToName() == null) {
            if (destination instanceof Queue) {
                replyToDestination = session.createTemporaryQueue();
            } else {    
                replyToDestination = session.createTemporaryTopic();
            }
        }
        producer = session.createProducer(destination);
        consumer = session.createConsumer(replyToDestination);
        consumer.setMessageListener(this);
    }

    protected void doShutdown() throws Exception {
        session = null;
        destination = null;
        consumer = null;
        producer = null;
        replyToDestination = null;
    }

    public void onMessage(final Message message) {
        if (log.isDebugEnabled()) {
            log.debug("Received jms message " + message);
        }
        endpoint.getServiceUnit().getComponent().getExecutor().execute(new Runnable() {
            public void run() {
                InOut exchange = null;
                if (log.isDebugEnabled()) {
                    log.debug("Handling jms message " + message);
                }
                String correlationID = null;
                try {
                    correlationID = message.getJMSCorrelationID();
                    exchange = (InOut) store.load(correlationID);
                    if (exchange == null) {
                        throw new IllegalStateException();
                    }
                } catch (Exception e) {
                    log.error("Could not find exchange " + (correlationID == null ? "" : correlationID), e);
                    return;
                }
                try {
                    SoapMessage soap = endpoint.getMarshaler().toSOAP(message);
                    SoapFault soapFault = soap.getFault();
                    if (soapFault != null) {
                        Fault fault = exchange.createFault();
                        fault.setContent(soapFault.getDetails());
                        exchange.setFault(fault);
                    } else {
                        NormalizedMessage msg = exchange.createMessage();
                        soapHelper.getJBIMarshaler().toNMS(msg, soap);
                        ((InOut) exchange).setOutMessage(msg);
                    }
                } catch (Exception e) {
                    log.error("Error while handling jms message", e);
                    exchange.setError(e);
                }
                try {
                    channel.send(exchange);
                } catch (MessagingException e) {
                    log.error("Error while handling jms message", e);
                }
            }
        });
    }

    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            return;
        }
        
        Message msg = createMessageFromExchange(session, exchange);

        if (exchange instanceof InOnly || exchange instanceof RobustInOnly) {
            synchronized (producer) {
                producer.send(msg);
            }
            exchange.setStatus(ExchangeStatus.DONE);
            channel.send(exchange);
        } else if (exchange instanceof InOut) {
            msg.setJMSCorrelationID(exchange.getExchangeId());
            msg.setJMSReplyTo(replyToDestination);
            store.store(exchange.getExchangeId(), exchange);
            try {
                synchronized (producer) {
                    producer.send(msg);
                }
            } catch (Exception e) {
                store.load(exchange.getExchangeId());
                throw e;
            }
        } else {
            throw new IllegalStateException(exchange.getPattern() + " not implemented");
        }
    }

}
