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

import java.util.Map;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.InitialContext;

import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.jms.AbstractJmsProcessor;
import org.apache.servicemix.jms.JmsEndpoint;
import org.apache.servicemix.soap.Context;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

public class MultiplexingConsumerProcessor extends AbstractJmsProcessor implements MessageListener {

    protected Session session;
    protected Destination destination;
    protected MessageConsumer consumer;
    protected Map pendingMessages;
    protected DeliveryChannel channel;

    public MultiplexingConsumerProcessor(JmsEndpoint endpoint) throws Exception {
        super(endpoint);
    }

    protected void doStart(InitialContext ctx) throws Exception {
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
        consumer = session.createConsumer(destination);
        consumer.setMessageListener(this);
        pendingMessages = new ConcurrentHashMap();
        channel = endpoint.getServiceUnit().getComponent().getComponentContext().getDeliveryChannel();
    }

    protected void doStop() throws Exception {
        session = null;
        destination = null;
        consumer = null;
        pendingMessages.clear();
        pendingMessages = null;
    }

    public void onMessage(final Message message) {
        if (log.isDebugEnabled()) {
            log.debug("Received jms message " + message);
        }
        endpoint.getServiceUnit().getComponent().getExecutor().execute(new Runnable() {
            public void run() {
                try {
                    if (log.isDebugEnabled()) {
                        log.debug("Handling jms message " + message);
                    }
                    Context context = createContext();
                    MessageExchange exchange = toNMS(message, context);
                    // TODO: copy protocol messages
                    //inMessage.setProperty(JbiConstants.PROTOCOL_HEADERS, getHeaders(message));
                    pendingMessages.put(exchange.getExchangeId(), context);
                    BaseLifeCycle lf = (BaseLifeCycle) endpoint.getServiceUnit().getComponent().getLifeCycle();
                    lf.sendConsumerExchange(exchange, MultiplexingConsumerProcessor.this.endpoint);
                } catch (Throwable e) {
                    log.error("Error while handling jms message", e);
                }
            }
        });
    }

    public void process(MessageExchange exchange) throws Exception {
        Context context = (Context) pendingMessages.remove(exchange.getExchangeId());
        // if context is null we lost it after a redeploy
        // SM-782 : If exchange is InOnly and status = done > do nothing
        if (exchange instanceof InOnly && exchange.getStatus() == ExchangeStatus.DONE) {
            return;
        } else {
            Message message = (Message) context.getProperty(Message.class.getName());
            MessageProducer producer = null;
            Message response = null;
            try {
                response = fromNMSResponse(exchange, context, session);
                if (response != null) {
                    producer = session.createProducer(message.getJMSReplyTo());
                    if (endpoint.isUseMsgIdInResponse()) {
                        response.setJMSCorrelationID(message.getJMSMessageID());
                    } else {
                        response.setJMSCorrelationID(message.getJMSCorrelationID());
                    }
                    producer.send(response);
                }
            } finally {
                if (producer != null) {
                    producer.close();
                }
                if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                    exchange.setStatus(ExchangeStatus.DONE);
                    channel.send(exchange);
                }
            }
        }
    }

}
