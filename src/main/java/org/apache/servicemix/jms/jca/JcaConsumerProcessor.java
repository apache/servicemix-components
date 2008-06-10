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

import java.util.Map;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.transaction.TransactionManager;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jms.AbstractJmsProcessor;
import org.apache.servicemix.jms.JmsEndpoint;
import org.apache.servicemix.soap.Context;
import org.jencks.SingletonEndpointFactory;

/**
 * 
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class JcaConsumerProcessor extends AbstractJmsProcessor implements MessageListener {

    private static final Log LOG = LogFactory.getLog(JcaConsumerProcessor.class);

    protected Map pendingMessages = new ConcurrentHashMap();
    protected ResourceAdapter resourceAdapter;
    protected MessageEndpointFactory endpointFactory;
    protected ActivationSpec activationSpec;
    protected BootstrapContext bootstrapContext;
    protected TransactionManager transactionManager;
    protected ConnectionFactory connectionFactory;
    
    public JcaConsumerProcessor(JmsEndpoint endpoint) throws Exception {
        super(endpoint);
    }

    public void start() throws Exception {
        transactionManager = (TransactionManager) context.getTransactionManager();
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
            if (resourceAdapter == null) {
                throw new IllegalArgumentException("resourceAdapter not set");
            }
        } else if (activationSpec.getResourceAdapter() == null) {
            activationSpec.setResourceAdapter(resourceAdapter);
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
            if (LOG.isDebugEnabled()) {
                LOG.debug("Received jms message " + message);
            }
            Context context = createContext();
            MessageExchange exchange = toNMS(message, context);
            if (!(exchange instanceof InOnly)) {
                throw new UnsupportedOperationException("JCA consumer endpoints can only use InOnly MEP");
            }
            exchange.setProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME, transactionManager.getTransaction());
            pendingMessages.put(exchange.getExchangeId(), context);
            if (endpoint.isSynchronous()) {
                channel.sendSync(exchange);
                process(exchange);
            } else {
                endpoint.getServiceUnit().getComponent().prepareExchange(exchange, endpoint);
                channel.send(exchange);
            }
        } catch (Throwable e) {
            LOG.error("Error while handling jms message", e);
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
            } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
                if (endpoint.isRollbackOnError()) {
                    TransactionManager tm = 
                        (TransactionManager) endpoint.getServiceUnit().getComponent().getComponentContext().getTransactionManager();
                    tm.setRollbackOnly();
                    return;
                } else if (exchange instanceof InOnly) {
                    LOG.info("Exchange in error: " + exchange, exchange.getError());
                    return;
                } else {
                    connection = connectionFactory.createConnection();
                    Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
                    Exception error = exchange.getError();
                    if (error == null) {
                        error = new Exception("Exchange in error");
                    }
                    response = session.createObjectMessage(error);
                    MessageProducer producer = session.createProducer(message.getJMSReplyTo());
                    if (endpoint.isUseMsgIdInResponse()) {
                        response.setJMSCorrelationID(message.getJMSMessageID());
                    } else {
                        response.setJMSCorrelationID(message.getJMSCorrelationID());
                    }
                    producer.send(response);
                }
            } else {
                connection = connectionFactory.createConnection();
                Session session = connection.createSession(true, Session.SESSION_TRANSACTED);
                response = fromNMSResponse(exchange, context, session);
                if (response != null) {
                    MessageProducer producer = session.createProducer(message.getJMSReplyTo());
                    if (endpoint.isUseMsgIdInResponse()) {
                        response.setJMSCorrelationID(message.getJMSMessageID());
                    } else {
                        response.setJMSCorrelationID(message.getJMSCorrelationID());
                    }
                    producer.send(response);
                }
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
