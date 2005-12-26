/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.jms;

import java.util.Map;

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
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import org.apache.servicemix.jms.wsdl.JmsAddress;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

public class MultiplexingProviderProcessor extends AbstractJmsProcessor implements MessageListener {

    protected Session session;
    protected Destination destination;
    protected Destination replyToDestination;
    protected MessageConsumer consumer;
    protected MessageProducer producer;
    protected JmsMarshaler marshaler = new JmsMarshaler();
    protected Map pendingExchanges = new ConcurrentHashMap();
    protected DeliveryChannel channel;
    
    public MultiplexingProviderProcessor(JmsEndpoint endpoint) {
        super(endpoint);
    }

    protected void doStart(InitialContext ctx) throws Exception {
        channel = endpoint.getServiceUnit().getComponent().getComponentContext().getDeliveryChannel();
        JmsAddress address = endpoint.getAddress();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        if (address.getJndiDestinationName() != null) {
            destination = (Destination) ctx.lookup(address.getJndiDestinationName());
        } else if (address.getJmsProviderDestinationName() != null) {
            if (STYLE_QUEUE.equals(address.getDestinationStyle())) {
                destination = session.createQueue(address.getJmsProviderDestinationName());
            } else {
                destination = session.createTopic(address.getJmsProviderDestinationName());
            }
        } else {
            throw new IllegalStateException("No destination provided");
        }
        if (destination instanceof Queue) {
            replyToDestination = session.createTemporaryQueue();
        } else {
            replyToDestination = session.createTemporaryTopic();
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
        try {
            endpoint.getServiceUnit().getComponent().getWorkManager().scheduleWork(new Work() {
                public void release() {
                }
                public void run() {
                    try {
                        InOut exchange = (InOut) pendingExchanges.remove(message.getJMSCorrelationID());
                        if (exchange == null) {
                            throw new IllegalStateException("Could not find exchange " + message.getJMSCorrelationID());
                        }
                        NormalizedMessage out = exchange.createMessage();
                        marshaler.toNMS(out, message);
                        exchange.setOutMessage(out);
                        channel.send(exchange);
                    } catch (Exception e) {
                        // TODO: log exception
                        e.printStackTrace();
                    }
                }
            });
        } catch (WorkException e) {
            // TODO: log exception
            e.printStackTrace();
        }
    }

    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            exchange.setStatus(ExchangeStatus.DONE);
            channel.send(exchange);
            return;
        }
        if (endpoint.isSoap()) {
            throw new IllegalStateException("soap not implemented");
        }
        if (exchange instanceof InOnly || exchange instanceof RobustInOnly) {
            Message msg = marshaler.createMessage(((InOnly) exchange).getInMessage(), session);
            synchronized (producer) {
                producer.send(msg);
            }
        } else if (exchange instanceof InOut) {
            Message msg = marshaler.createMessage(((InOut) exchange).getInMessage(), session);
            msg.setJMSCorrelationID(exchange.getExchangeId());
            msg.setJMSReplyTo(replyToDestination);
            pendingExchanges.put(exchange.getExchangeId(), exchange);
            try {
                synchronized (producer) {
                    producer.send(msg);
                }
            } catch (Exception e) {
                pendingExchanges.remove(exchange.getExchangeId());
                throw e;
            }
        } else {
            throw new IllegalStateException(exchange.getPattern() + " not implemented");
        }
    }

    
}
