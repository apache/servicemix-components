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

import java.util.Hashtable;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.common.ExchangeProcessor;

public abstract class AbstractJmsProcessor implements ExchangeProcessor {

    public static final String STYLE_QUEUE = "queue";
    public static final String STYLE_TOPIC = "topic";

    protected final transient Log log = LogFactory.getLog(getClass());
    
    protected JmsEndpoint endpoint;
    protected Connection connection;

    public AbstractJmsProcessor(JmsEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    public void start() throws Exception {
        InitialContext ctx = null;
        ConnectionFactory connectionFactory = null;
        try {
            // First check configured connectionFactory on the endpoint
            connectionFactory = getConnectionFactory();
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
        } finally {
            if (ctx != null) {
                ctx.close();
            }
        }
    }
    
    protected ConnectionFactory getConnectionFactory() throws NamingException {
        InitialContext ctx = null;
        // First check configured connectionFactory on the endpoint
        ConnectionFactory connectionFactory = endpoint.getConnectionFactory();
        // Then, check for jndi connection factory name on the endpoint
        if (connectionFactory == null && endpoint.getJndiConnectionFactoryName() != null) {
            ctx = getInitialContext();
            connectionFactory = (ConnectionFactory) ctx.lookup(endpoint.getJndiConnectionFactoryName());
        }
        // Check for a configured connectionFactory on the configuration
        if (connectionFactory == null && endpoint.getConfiguration().getConnectionFactory() != null) {
            connectionFactory = endpoint.getConfiguration().getConnectionFactory();
        }
        // Check for jndi connection factory name on the configuration
        if (connectionFactory == null && endpoint.getConfiguration().getJndiConnectionFactoryName() != null) {
            ctx = getInitialContext();
            connectionFactory = (ConnectionFactory) ctx.lookup(endpoint.getConfiguration().getJndiConnectionFactoryName());
        }
        return connectionFactory;
    }

    protected InitialContext getInitialContext() throws NamingException {
        Hashtable props = new Hashtable();
        if (endpoint.getInitialContextFactory() != null && endpoint.getJndiProviderURL() != null) {
            props.put(Context.INITIAL_CONTEXT_FACTORY, endpoint.getInitialContextFactory());
            props.put(Context.PROVIDER_URL, endpoint.getJndiProviderURL());
            return new InitialContext(props);
        } else if (endpoint.getConfiguration().getJndiInitialContextFactory() != null && 
                   endpoint.getConfiguration().getJndiProviderUrl() != null) {
            props.put(Context.INITIAL_CONTEXT_FACTORY, endpoint.getConfiguration().getJndiInitialContextFactory());
            props.put(Context.PROVIDER_URL, endpoint.getConfiguration().getJndiProviderUrl());
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

}
