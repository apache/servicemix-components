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
package org.apache.servicemix.jms.jca;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.TransactionManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.common.AsyncBaseLifeCycle;
import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.jms.AbstractJmsProcessor;
import org.apache.servicemix.jms.JmsEndpoint;
import org.apache.servicemix.soap.Context;
import org.apache.servicemix.soap.SoapFault;
import org.apache.servicemix.soap.SoapHelper;
import org.apache.servicemix.soap.marshalers.SoapMessage;
import org.apache.servicemix.soap.marshalers.SoapWriter;
import org.jencks.SingletonEndpointFactory;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

/**
 * 
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class JcaConsumerProcessor extends AbstractJmsProcessor implements MessageListener {

    private static final Log log = LogFactory.getLog(JcaConsumerProcessor.class);

    protected Map pendingMessages = new ConcurrentHashMap();
    protected DeliveryChannel channel;
    protected SoapHelper soapHelper;
    protected ResourceAdapter resourceAdapter;
    protected MessageEndpointFactory endpointFactory;
    protected ActivationSpec activationSpec;
    protected BootstrapContext bootstrapContext;
    protected TransactionManager transactionManager;
    protected ConnectionFactory connectionFactory;
    
    public JcaConsumerProcessor(JmsEndpoint endpoint) {
        super(endpoint);
        this.soapHelper = new SoapHelper(endpoint);
    }

    public void start() throws Exception {
        AsyncBaseLifeCycle lf = (AsyncBaseLifeCycle) endpoint.getServiceUnit().getComponent().getLifeCycle();
        channel = lf.getContext().getDeliveryChannel();
        transactionManager = (TransactionManager) lf.getContext().getTransactionManager();
        endpointFactory = new SingletonEndpointFactory(this, transactionManager);
        bootstrapContext = endpoint.getBootstrapContext();
        if (bootstrapContext == null) {
            throw new IllegalArgumentException("bootstrapContext not set");
        }
        connectionFactory = endpoint.getConnectionFactory();
        if (connectionFactory == null) {
            throw new IllegalArgumentException("connectionFactory not set");
        }
        activationSpec = endpoint.getActivationSpec();
        if (activationSpec == null) {
            throw new IllegalArgumentException("activationSpec not set");
        }
        resourceAdapter = endpoint.getResourceAdapter();
        if (resourceAdapter == null) {
            resourceAdapter = activationSpec.getResourceAdapter();
        } else if (activationSpec.getResourceAdapter() == null) {
            activationSpec.setResourceAdapter(resourceAdapter);
        } else {
            throw new IllegalArgumentException("resourceAdapter not set");
        }
        resourceAdapter.start(bootstrapContext);
        resourceAdapter.endpointActivation(endpointFactory, activationSpec);
    }

    public void stop() throws Exception {
        resourceAdapter.endpointDeactivation(endpointFactory, activationSpec);
        pendingMessages.clear();
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
            String contentType = message.getStringProperty("Content-Type");
            SoapMessage soap = soapHelper.getSoapMarshaler().createReader().read(is, contentType);
            Context context = soapHelper.createContext(soap);
            MessageExchange exchange = soapHelper.onReceive(context);
            if (exchange instanceof InOnly == false) {
                throw new UnsupportedOperationException("JCA consumer endpoints can only use InOnly MEP");
            }
            exchange.setProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME, transactionManager.getTransaction());
            context.setProperty(Message.class.getName(), message);
            // TODO: copy protocol messages
            //inMessage.setProperty(JbiConstants.PROTOCOL_HEADERS, getHeaders(message));
            pendingMessages.put(exchange.getExchangeId(), context);
            if (endpoint.isSynchronous()) {
                channel.sendSync(exchange);
                process(exchange);
            } else {
                BaseLifeCycle lf = (BaseLifeCycle) endpoint.getServiceUnit().getComponent().getLifeCycle();
                lf.sendConsumerExchange(exchange, JcaConsumerProcessor.this.endpoint);
            }
        } catch (Throwable e) {
            log.error("Error while handling jms message", e);
        }
    }

    public void process(MessageExchange exchange) throws Exception {
        Context context = (Context) pendingMessages.remove(exchange.getExchangeId());
        Message message = (Message) context.getProperty(Message.class.getName());
        Message response = null;
        Connection connection = null;
        try {
            if (exchange.getStatus() == ExchangeStatus.DONE) {
                return;
            }
            connection = connectionFactory.createConnection();
            Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
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
                MessageProducer producer = session.createProducer(message.getJMSReplyTo());
                response.setJMSCorrelationID(message.getJMSCorrelationID());
                producer.send(response);
            }
        } finally {
            if (connection != null) {
                connection.close();
            }
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                exchange.setStatus(ExchangeStatus.DONE);
                channel.send(exchange);
            }
        }
    }

}
