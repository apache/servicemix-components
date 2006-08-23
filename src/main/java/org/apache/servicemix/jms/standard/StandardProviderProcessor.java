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
package org.apache.servicemix.jms.standard;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;
import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.apache.servicemix.jms.AbstractJmsProcessor;
import org.apache.servicemix.jms.JmsEndpoint;
import org.apache.servicemix.soap.marshalers.SoapMessage;

public class StandardProviderProcessor extends AbstractJmsProcessor {

    protected Destination destination;
    protected DeliveryChannel channel;
    
    public StandardProviderProcessor(JmsEndpoint endpoint) {
        super(endpoint);
    }

    protected void doStart(InitialContext ctx) throws Exception {
        channel = endpoint.getServiceUnit().getComponent().getComponentContext().getDeliveryChannel();
        destination = endpoint.getDestination();
        if (destination == null) {
            if (endpoint.getJndiDestinationName() != null) {
                destination = (Destination) ctx.lookup(endpoint.getJndiDestinationName());
            } else if (endpoint.getJmsProviderDestinationName() == null) {
                throw new IllegalStateException("No destination provided");
            }
        }
    }

    protected void doStop() throws Exception {
        destination = null;
    }

    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            return;
        }
        Session session = null;
        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            if (destination == null) {
                if (STYLE_QUEUE.equals(endpoint.getDestinationStyle())) {
                    destination = session.createQueue(endpoint.getJmsProviderDestinationName());
                } else {
                    destination = session.createTopic(endpoint.getJmsProviderDestinationName());
                }
            }
            MessageProducer producer = session.createProducer(destination);
            
            TextMessage msg = session.createTextMessage();
            NormalizedMessage nm = exchange.getMessage("in");
            fromNMS(nm, msg);
    
            if (exchange instanceof InOnly || exchange instanceof RobustInOnly) {
                producer.send(msg);
            } else if (exchange instanceof InOut) {
                Destination replyToDestination;
                if (destination instanceof Queue) {
                    replyToDestination = session.createTemporaryQueue();
                } else {
                    replyToDestination = session.createTemporaryTopic();
                }
                MessageConsumer consumer = session.createConsumer(replyToDestination);
                msg.setJMSCorrelationID(exchange.getExchangeId());
                msg.setJMSReplyTo(replyToDestination);
                producer.send(msg);
                Message message = consumer.receive();
                if (message instanceof ObjectMessage) {
                    Object o = ((ObjectMessage) message).getObject();
                    if (o instanceof Exception) {
                        exchange.setError((Exception) o);
                    } else {
                        throw new UnsupportedOperationException("Can not handle objects of type " + o.getClass().getName());
                    }
                } else {
                    InputStream is = null;
                    if (message instanceof TextMessage) {
                        is = new ByteArrayInputStream(((TextMessage) message).getText().getBytes());
                    } else if (message instanceof BytesMessage) {
                        int length = (int) ((BytesMessage) message).getBodyLength();
                        byte[] bytes = new byte[length];
                        ((BytesMessage) message).readBytes(bytes);
                        is = new ByteArrayInputStream(bytes);
                    } else {
                        throw new IllegalArgumentException("JMS message should be a text or bytes message");
                    }
                    String contentType = message.getStringProperty(CONTENT_TYPE);
                    SoapMessage soap = soapHelper.getSoapMarshaler().createReader().read(is, contentType);
                    NormalizedMessage out = exchange.createMessage();
                    soapHelper.getJBIMarshaler().toNMS(out, soap);
                    ((InOut) exchange).setOutMessage(out);
                }
                channel.send(exchange);
            } else {
                throw new IllegalStateException(exchange.getPattern() + " not implemented");
            }
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

}
