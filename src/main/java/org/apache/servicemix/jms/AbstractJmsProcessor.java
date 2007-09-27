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
package org.apache.servicemix.jms;

import java.util.Map;
import java.util.Properties;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.Session;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.common.EndpointComponentContext;
import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.soap.Context;
import org.apache.servicemix.soap.SoapFault;
import org.apache.servicemix.soap.SoapHelper;
import org.apache.servicemix.soap.marshalers.SoapMessage;
import org.apache.servicemix.store.Store;
import org.apache.servicemix.store.memory.MemoryStoreFactory;

public abstract class AbstractJmsProcessor implements ExchangeProcessor {

    public static final String STYLE_QUEUE = "queue";
    public static final String STYLE_TOPIC = "topic";
    
    public static final String CONTENT_TYPE = "MimeContentType";

    protected final transient Log log = LogFactory.getLog(getClass());
    
    protected JmsEndpoint endpoint;
    protected Connection connection;
    protected SoapHelper soapHelper;
    protected ComponentContext context;
    protected DeliveryChannel channel;

    protected Store store;

    public AbstractJmsProcessor(JmsEndpoint endpoint) throws Exception {
        this.endpoint = endpoint;
        this.soapHelper = new SoapHelper(endpoint);
        this.context = new EndpointComponentContext(endpoint);
        this.channel = context.getDeliveryChannel();
    }

    public void start() throws Exception {
        try {
            InitialContext ctx = getInitialContext();
            ConnectionFactory connectionFactory = null;
            connectionFactory = getConnectionFactory(ctx);
            connection = connectionFactory.createConnection();
            connection.start();

            // set up the Store
            if (endpoint.store != null) {
                store = endpoint.store;
            } else if (endpoint.storeFactory != null) {
                store = endpoint.storeFactory.open(endpoint.getService().toString() + endpoint.getEndpoint());
            } else {
                store = new MemoryStoreFactory().open(endpoint.getService().toString() + endpoint.getEndpoint());
            }

            doStart(ctx);
        } catch (Exception e) {
            try {
                stop();
            } catch (Exception inner) {
                // TODO: log
            }
            throw e;
        }
    }
    
    protected ConnectionFactory getConnectionFactory(InitialContext ctx) throws NamingException {
        // First check configured connectionFactory on the endpoint
        ConnectionFactory connectionFactory = endpoint.getConnectionFactory();
        // Then, check for jndi connection factory name on the endpoint
        if (connectionFactory == null && endpoint.getJndiConnectionFactoryName() != null) {
            connectionFactory = (ConnectionFactory) ctx.lookup(endpoint.getJndiConnectionFactoryName());
        }
        // Check for a configured connectionFactory on the configuration
        if (connectionFactory == null && endpoint.getConfiguration().getConnectionFactory() != null) {
            connectionFactory = endpoint.getConfiguration().getConnectionFactory();
        }
        // Check for jndi connection factory name on the configuration
        if (connectionFactory == null && endpoint.getConfiguration().getJndiConnectionFactoryName() != null) {
            connectionFactory = (ConnectionFactory) ctx.lookup(endpoint.getConfiguration().getJndiConnectionFactoryName());
        }
        return connectionFactory;
    }

    protected InitialContext getInitialContext() throws NamingException {
        Properties props = new Properties();
        if (endpoint.getInitialContextFactory() != null && endpoint.getJndiProviderURL() != null) {
            props.put(InitialContext.INITIAL_CONTEXT_FACTORY, endpoint.getInitialContextFactory());
            props.put(InitialContext.PROVIDER_URL, endpoint.getJndiProviderURL());
            return new InitialContext(props);
        } else if (endpoint.getConfiguration().getJndiInitialContextFactory() != null 
                   && endpoint.getConfiguration().getJndiProviderUrl() != null) {
            props.put(InitialContext.INITIAL_CONTEXT_FACTORY, endpoint.getConfiguration().getJndiInitialContextFactory());
            props.put(InitialContext.PROVIDER_URL, endpoint.getConfiguration().getJndiProviderUrl());
            return new InitialContext(props);
        } else {
            BaseLifeCycle lf = (BaseLifeCycle) endpoint.getServiceUnit().getComponent().getLifeCycle();
            return lf.getContext().getNamingContext();
        }
    }

    protected Store getStore() {
        return store;
    }

    protected void doStart(InitialContext ctx) throws Exception {
    }

    public void stop() throws Exception {
        try {
            doStop();
            if (connection != null) {
                connection.close();
            }
        } finally {
            connection = null;
        }
    }

    protected void doStop() throws Exception {
    }
    
    protected Context createContext() {
        return soapHelper.createContext();
    }
    
    protected Message fromNMS(NormalizedMessage nm, Session session) throws Exception {
        SoapMessage soap = new SoapMessage();
        soapHelper.getJBIMarshaler().fromNMS(soap, nm);
        Map headers = (Map) nm.getProperty(JbiConstants.PROTOCOL_HEADERS);
        return endpoint.getMarshaler().toJMS(soap, headers, session);
    }
    
    protected MessageExchange toNMS(Message message, Context ctx) throws Exception {
        SoapMessage soap = endpoint.getMarshaler().toSOAP(message);
        ctx.setInMessage(soap);
        ctx.setProperty(Message.class.getName(), message);
        MessageExchange exchange = soapHelper.onReceive(ctx);
        // TODO: copy protocol messages
        //inMessage.setProperty(JbiConstants.PROTOCOL_HEADERS, getHeaders(message));
        return exchange;
    }
    
    protected Message fromNMSResponse(MessageExchange exchange, Context ctx, Session session) throws Exception {
        Message response = null;
        if (exchange.getStatus() == ExchangeStatus.ERROR) {
            // marshal error
            Exception e = exchange.getError();
            if (e == null) {
                e = new Exception("Unkown error");
            }
            response = endpoint.getMarshaler().toJMS(e, session);
        } else if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            // check for fault
            Fault jbiFault = exchange.getFault(); 
            if (jbiFault != null) {
                // convert fault to SOAP message
                SoapFault fault = new SoapFault(SoapFault.RECEIVER, null, null, null, jbiFault.getContent());
                SoapMessage soapFault = soapHelper.onFault(ctx, fault);
                Map headers = (Map) jbiFault.getProperty(JbiConstants.PROTOCOL_HEADERS);
                response = endpoint.getMarshaler().toJMS(soapFault, headers, session);
            } else {
                NormalizedMessage outMsg = exchange.getMessage("out");
                if (outMsg != null) {
                    SoapMessage out = soapHelper.onReply(ctx, outMsg);
                    Map headers = (Map) outMsg.getProperty(JbiConstants.PROTOCOL_HEADERS);
                    response = endpoint.getMarshaler().toJMS(out, headers, session);
                }
            }
        }
        return response;
    }

}
