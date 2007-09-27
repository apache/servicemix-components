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

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.InitialContext;

import org.apache.servicemix.jms.AbstractJmsProcessor;
import org.apache.servicemix.jms.JmsEndpoint;
import org.apache.servicemix.soap.marshalers.SoapMessage;

public class MultiplexingProviderProcessor extends AbstractJmsProcessor implements MessageListener {

    protected Session session;
    protected Destination destination;
    protected Destination replyToDestination;
    protected MessageConsumer consumer;
    protected MessageProducer producer;
    protected DeliveryChannel channel;

    public MultiplexingProviderProcessor(JmsEndpoint endpoint) throws Exception {
        super(endpoint);
    }

    protected void doStart(InitialContext ctx) throws Exception {
        channel = endpoint.getServiceUnit().getComponent().getComponentContext().getDeliveryChannel();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        destination = endpoint.getDestination();
        if (destination == null) {
            if (endpoint.getJndiDestinationName() != null) {
                destination = (Destination) ctx.lookup(endpoint.getJndiDestinationName());
            } else if (endpoint.getJmsProviderDestinationName() != null) {
                if (STYLE_QUEUE.equals(endpoint.getDestinationStyle())) {
                    destination = session.createQueue(endpoint.getJmsProviderDestinationName());
                } else {
                    destination = session.createTopic(endpoint.getJmsProviderDestinationName());
                }
            } else {
                throw new IllegalStateException("No destination provided");
            }
        }
        if (endpoint.getJndiReplyToName() != null) {
            replyToDestination = (Destination) ctx.lookup(endpoint.getJndiReplyToName());
        } else if (endpoint.getJmsProviderReplyToName() != null) {
            if (destination instanceof Queue) {
                replyToDestination = session.createQueue(endpoint.getJmsProviderReplyToName());
            } else {
                replyToDestination = session.createTopic(endpoint.getJmsProviderReplyToName());
            }
        } else {
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

    protected void doStop() throws Exception {
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
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Handling jms message " + message);
                    }
                    exchange = (InOut) store.load(message.getJMSCorrelationID());
                    if (exchange == null) {
                        throw new IllegalStateException("Could not find exchange " + message.getJMSCorrelationID());
                    }
                    SoapMessage soap = endpoint.getMarshaler().toSOAP(message);
                    NormalizedMessage out = exchange.createMessage();
                    soapHelper.getJBIMarshaler().toNMS(out, soap);
                    ((InOut) exchange).setOutMessage(out);
                    channel.send(exchange);
                } catch (Exception e) {
                    log.error("Error while handling jms message", e);
                    if (exchange != null) {
                        exchange.setError(e);
                    }                    
                } catch (Throwable e) {
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
        NormalizedMessage nm = exchange.getMessage("in");
        Message msg = fromNMS(nm, session);

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
