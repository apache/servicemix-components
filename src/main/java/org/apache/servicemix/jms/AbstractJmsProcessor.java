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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.soap.Context;
import org.apache.servicemix.soap.SoapFault;
import org.apache.servicemix.soap.SoapHelper;
import org.apache.servicemix.soap.marshalers.SoapMessage;
import org.apache.servicemix.soap.marshalers.SoapWriter;

public abstract class AbstractJmsProcessor implements ExchangeProcessor {

    public static final String STYLE_QUEUE = "queue";
    public static final String STYLE_TOPIC = "topic";
    
    public static final String CONTENT_TYPE = "MimeContentType";

    protected final transient Log log = LogFactory.getLog(getClass());
    
    protected JmsEndpoint endpoint;
    protected Connection connection;
    protected SoapHelper soapHelper;

    public AbstractJmsProcessor(JmsEndpoint endpoint) throws Exception {
        this.endpoint = endpoint;
        this.soapHelper = new SoapHelper(endpoint);
    }

    public void start() throws Exception {
        try {
            InitialContext ctx = getInitialContext();
            ConnectionFactory connectionFactory = null;
            connectionFactory = getConnectionFactory(ctx);
            connection = connectionFactory.createConnection();
            connection.start();
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
        Hashtable props = new Hashtable();
        if (endpoint.getInitialContextFactory() != null && endpoint.getJndiProviderURL() != null) {
            props.put(InitialContext.INITIAL_CONTEXT_FACTORY, endpoint.getInitialContextFactory());
            props.put(InitialContext.PROVIDER_URL, endpoint.getJndiProviderURL());
            return new InitialContext(props);
        } else if (endpoint.getConfiguration().getJndiInitialContextFactory() != null && 
                   endpoint.getConfiguration().getJndiProviderUrl() != null) {
            props.put(InitialContext.INITIAL_CONTEXT_FACTORY, endpoint.getConfiguration().getJndiInitialContextFactory());
            props.put(InitialContext.PROVIDER_URL, endpoint.getConfiguration().getJndiProviderUrl());
            return new InitialContext(props);
        } else {
            BaseLifeCycle lf = (BaseLifeCycle) endpoint.getServiceUnit().getComponent().getLifeCycle();
            return lf.getContext().getNamingContext();
        }
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
    
    protected void fromNMS(NormalizedMessage nm, TextMessage msg) throws Exception {
        Map headers = (Map) nm.getProperty(JbiConstants.PROTOCOL_HEADERS);
        SoapMessage soap = new SoapMessage();
        soapHelper.getJBIMarshaler().fromNMS(soap, nm);
        fromNMS(soap, msg, headers);
    }
    
    protected void fromNMS(SoapMessage soap, TextMessage msg, Map headers) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SoapWriter writer = soapHelper.getSoapMarshaler().createWriter(soap);
        writer.write(baos);
        msg.setText(baos.toString());
        if (headers != null) {
            for (Iterator it = headers.keySet().iterator(); it.hasNext();) {
                String name = (String) it.next();
                Object value = headers.get(name);
                if (shouldIncludeHeader(name, value)) {
                    msg.setObjectProperty(name, value);
                }
            }
        }
        // overwrite whatever content-type was passed on to us with the one
        // the SoapWriter constructed
        msg.setStringProperty(CONTENT_TYPE, writer.getContentType());
    }
    
    protected Context createContext() {
        return soapHelper.createContext();
    }
    
    protected MessageExchange toNMS(Message message, Context context) throws Exception {
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
        context.setInMessage(soap);
        context.setProperty(Message.class.getName(), message);
        MessageExchange exchange = soapHelper.onReceive(context);
        // TODO: copy protocol messages
        //inMessage.setProperty(JbiConstants.PROTOCOL_HEADERS, getHeaders(message));
        return exchange;
    }
    
    protected Message fromNMSResponse(MessageExchange exchange, Context context, Session session) throws Exception {
        Message response = null;
        if (exchange.getStatus() == ExchangeStatus.ERROR) {
            Exception e = exchange.getError();
            if (e == null) {
                e = new Exception("Unkown error");
            }
            response = session.createObjectMessage(e);
        } else if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            Fault jbiFault = exchange.getFault(); 
            if (jbiFault != null) {
                SoapFault fault = new SoapFault(SoapFault.RECEIVER, null, null, null, jbiFault.getContent());
                SoapMessage soapFault = soapHelper.onFault(context, fault);
                TextMessage txt = session.createTextMessage();
                fromNMS(soapFault, txt, (Map) jbiFault.getProperty(JbiConstants.PROTOCOL_HEADERS));
                response = txt;
            } else {
                NormalizedMessage outMsg = exchange.getMessage("out");
                if (outMsg != null) {
                    SoapMessage out = soapHelper.onReply(context, outMsg);
                    TextMessage txt = session.createTextMessage();
                    fromNMS(out, txt, (Map) outMsg.getProperty(JbiConstants.PROTOCOL_HEADERS));
                    response = txt;
                }
            }
        }
        return response;
    }

    private boolean shouldIncludeHeader(String name, Object value) {
        return (value instanceof String || value instanceof Number || value instanceof Date)
                        && (!endpoint.isNeedJavaIdentifiers() || isJavaIdentifier(name));
    }

    private static boolean isJavaIdentifier(String s) {
        int n = s.length();
        if (n == 0)
            return false;
        if (!Character.isJavaIdentifierStart(s.charAt(0)))
            return false;
        for (int i = 1; i < n; i++)
            if (!Character.isJavaIdentifierPart(s.charAt(i)))
                return false;
        return true;
    }
    
}
