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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.jms.AbstractJmsProcessor;
import org.apache.servicemix.jms.JmsEndpoint;
import org.apache.servicemix.soap.Context;
import org.apache.servicemix.soap.SoapFault;
import org.apache.servicemix.soap.SoapHelper;
import org.apache.servicemix.soap.marshalers.SoapMessage;
import org.apache.servicemix.soap.marshalers.SoapWriter;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

public class MultiplexingConsumerProcessor extends AbstractJmsProcessor implements MessageListener {

    private static final Log log = LogFactory.getLog(MultiplexingConsumerProcessor.class);
    
    protected Session session;
    protected Destination destination;
    protected MessageConsumer consumer;
    protected Map pendingMessages = new ConcurrentHashMap();
    protected DeliveryChannel channel;
    protected SoapHelper soapHelper;
    
    public MultiplexingConsumerProcessor(JmsEndpoint endpoint) {
        super(endpoint);
        this.soapHelper = new SoapHelper(endpoint);
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
        channel = endpoint.getServiceUnit().getComponent().getComponentContext().getDeliveryChannel();
    }


    protected void doStop() throws Exception {
        session = null;
        destination = null;
        consumer = null;
        pendingMessages.clear();
    }

    public void onMessage(final Message message) {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Received jms message " + message);
            }
            endpoint.getServiceUnit().getComponent().getWorkManager().scheduleWork(new Work() {
                public void release() {
                }
                public void run() {
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("Handling jms message " + message);
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
                        String contentType = message.getStringProperty("Content-Type");
                        SoapMessage soap = soapHelper.getSoapMarshaler().createReader().read(is, contentType);
                        Context context = soapHelper.createContext(soap);
                        MessageExchange exchange = soapHelper.onReceive(context);
                        context.setProperty(Message.class.getName(), message);
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
        } catch (WorkException e) {
            log.error("Error while handling jms message", e);
        }
    }

    public void process(MessageExchange exchange) throws Exception {
        Context context = (Context) pendingMessages.remove(exchange.getExchangeId());
        Message message = (Message) context.getProperty(Message.class.getName());
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
                    response.setStringProperty("Content-Type", writer.getContentType());
                    // TODO: Copy other properties from fault
                } else {
                    NormalizedMessage outMsg = exchange.getMessage("out");
                    if (outMsg != null) {
                        SoapMessage out = soapHelper.onReply(context, outMsg);
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        SoapWriter writer = soapHelper.getSoapMarshaler().createWriter(out);
                        writer.write(baos);
                        response = session.createTextMessage(baos.toString());
                        response.setStringProperty("Content-Type", writer.getContentType());
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
    }

}
