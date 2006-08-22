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
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.resource.spi.work.Work;

import org.apache.servicemix.jms.AbstractJmsProcessor;
import org.apache.servicemix.jms.JmsEndpoint;
import org.apache.servicemix.soap.Context;
import org.apache.servicemix.soap.SoapFault;
import org.apache.servicemix.soap.SoapHelper;
import org.apache.servicemix.soap.marshalers.SoapMessage;
import org.apache.servicemix.soap.marshalers.SoapWriter;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

public class StandardConsumerProcessor extends AbstractJmsProcessor {

    protected Session session;
    protected Destination destination;
    protected DeliveryChannel channel;
    protected SoapHelper soapHelper;
    protected AtomicBoolean running = new AtomicBoolean(false);

    public StandardConsumerProcessor(JmsEndpoint endpoint) {
        super(endpoint);
        this.soapHelper = new SoapHelper(endpoint);
    }

    protected void doStart(InitialContext ctx) throws Exception {
        destination = endpoint.getDestination();
        if (destination == null) {
            if (endpoint.getJndiDestinationName() != null) {
                destination = (Destination) ctx.lookup(endpoint.getJndiDestinationName());
            } else if (endpoint.getJmsProviderDestinationName() == null) {
                throw new IllegalStateException("No destination provided");
            }
        }
        channel = endpoint.getServiceUnit().getComponent().getComponentContext().getDeliveryChannel();
        synchronized (running) {
            endpoint.getServiceUnit().getComponent().getWorkManager().startWork(new Work() {
                public void release() {
                }
                public void run() {
                    StandardConsumerProcessor.this.poll();
                }
            });
            running.wait();
        }
    }

    protected void doStop() throws Exception {
        if (running.get()) {
            synchronized (running) {
                if (session != null) {
                    session.close();
                }
                running.wait();
            }
        }
        session = null;
        destination = null;
    }

    protected void poll() {
        synchronized (running) {
            running.set(true);
            running.notify();
        }
        try {
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            if (destination == null) {
                if (STYLE_QUEUE.equals(endpoint.getDestinationStyle())) {
                    destination = session.createQueue(endpoint.getJmsProviderDestinationName());
                } else {
                    destination = session.createTopic(endpoint.getJmsProviderDestinationName());
                }
            }
            MessageConsumer consumer = session.createConsumer(destination);
            while (running.get()) {
                Message message = consumer.receive();
                if (message != null) {
                    onMessage(message);
                }
            }
        } catch (Exception e) {
            log.error("", e);
        } finally {
            synchronized (running) {
                running.set(false);
                running.notify();
            }
        }
    }

    public void onMessage(final Message message) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Received jms message " + message);
            }
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
            Context context = soapHelper.createContext(soap);
            MessageExchange exchange = soapHelper.onReceive(context);
            context.setProperty(Message.class.getName(), message);
            // TODO: copy protocol messages
            //inMessage.setProperty(JbiConstants.PROTOCOL_HEADERS, getHeaders(message));
            if (!channel.sendSync(exchange)) {
                throw new IllegalStateException("Exchange has been aborted");
            }
            MessageProducer producer = null;
            Message response = null;
            try {
                if (exchange.getStatus() == ExchangeStatus.ERROR) {
                    Exception e = exchange.getError();
                    if (e == null) {
                        e = new Exception("Unkown error");
                    }
                    response = session.createObjectMessage(e);
                } else if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                    if (exchange.getFault() != null) {
                        SoapFault fault = new SoapFault(SoapFault.RECEIVER, null, null, null, exchange.getFault().getContent());
                        SoapMessage soapFault = soapHelper.onFault(context, fault);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        SoapWriter writer = soapHelper.getSoapMarshaler().createWriter(soapFault);
                        writer.write(baos);
                        response = session.createTextMessage(baos.toString());
                        response.setStringProperty(CONTENT_TYPE, writer.getContentType());
                        // TODO: Copy other properties from fault
                    } else {
                        NormalizedMessage outMsg = exchange.getMessage("out");
                        if (outMsg != null) {
                            SoapMessage out = soapHelper.onReply(context, outMsg);
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            SoapWriter writer = soapHelper.getSoapMarshaler().createWriter(out);
                            writer.write(baos);
                            response = session.createTextMessage(baos.toString());
                            response.setStringProperty(CONTENT_TYPE, writer.getContentType());
                            // TODO: Copy other properties from response
                        }
                    }
                }
                if (response != null) {
                    producer = session.createProducer(message.getJMSReplyTo());
                    response.setJMSCorrelationID(message.getJMSCorrelationID());
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
        } catch (Throwable e) {
            log.error("Error while handling jms message", e);
        }
    }

    public void process(MessageExchange exchange) throws Exception {
        throw new IllegalStateException();
    }

}
