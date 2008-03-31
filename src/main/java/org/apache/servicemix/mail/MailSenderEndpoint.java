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
package org.apache.servicemix.mail;

import java.util.Properties;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.mail.marshaler.AbstractMailMarshaler;
import org.apache.servicemix.mail.marshaler.DefaultMailMarshaler;
import org.apache.servicemix.mail.utils.MailConnectionConfiguration;
import org.apache.servicemix.mail.utils.MailUtils;

/**
 * this is the sending endpoint for the mail component
 * 
 * @org.apache.xbean.XBean element="sender"
 * @author lhein
 */
public class MailSenderEndpoint extends ProviderEndpoint implements MailEndpointType {
    private static final transient Log LOG = LogFactory.getLog(MailSenderEndpoint.class);

    private AbstractMailMarshaler marshaler = new DefaultMailMarshaler();
    private MailConnectionConfiguration config;
    private String customTrustManagers;
    private String connection;
    private String sender;
    private String receiver;
    private boolean debugMode;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.Endpoint#validate()
     */
    public void validate() throws DeploymentException {
        super.validate();

        if (this.config == null || this.connection == null) {
            throw new DeploymentException("No valid connection uri provided.");
        }
        if (this.sender == null) {
            this.sender = this.marshaler != null
                ? this.marshaler.getDefaultSenderForOutgoingMails() : AbstractMailMarshaler.DEFAULT_SENDER;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#processInOnly(javax.jbi.messaging.MessageExchange,
     *      javax.jbi.messaging.NormalizedMessage)
     */
    @Override
    protected void processInOnly(MessageExchange exchange, NormalizedMessage in) throws Exception {
        // Exchange is finished
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            // Exchange has been aborted with an exception
            return;
        } else if (exchange.getFault() != null) {
            // Fault message
            exchange.setStatus(ExchangeStatus.DONE);
            getChannel().send(exchange);
        } else {
            Session session = null;
            try {
                Properties props = MailUtils.getPropertiesForProtocol(this.config, this.customTrustManagers);
                props.put("mail.debug", isDebugMode() ? "true" : "false");

                // Get session
                session = Session.getInstance(props, config.getAuthenticator());

                // debug the session
                session.setDebug(this.debugMode);

                // Define message
                MimeMessage msg = new MimeMessage(session);

                // let the marshaler to the conversion of message to mail
                this.marshaler.convertJBIToMail(msg, exchange, in, this.sender, this.receiver);

                // Send message
                Transport.send(msg);
            } catch (MessagingException mex) {
                logger.error("Error sending mail...", mex);
                throw mex;
            } finally {
                // delete all temporary allocated resources
                this.marshaler.cleanUpResources();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#processInOut(javax.jbi.messaging.MessageExchange,
     *      javax.jbi.messaging.NormalizedMessage, javax.jbi.messaging.NormalizedMessage)
     */
    @Override
    protected void processInOut(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out)
        throws Exception {
        // Exchange is finished
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            // Exchange has been aborted with an exception
            return;
        } else if (exchange.getFault() != null) {
            // Fault message
            exchange.setStatus(ExchangeStatus.DONE);
            getChannel().send(exchange);
        } else {
            Session session = null;
            try {
                Properties props = MailUtils.getPropertiesForProtocol(this.config, this.customTrustManagers);
                props.put("mail.debug", isDebugMode() ? "true" : "false");

                // Get session
                session = Session.getInstance(props, config.getAuthenticator());

                // debug the session
                session.setDebug(this.debugMode);

                // Define message
                MimeMessage msg = new MimeMessage(session);

                // let the marshaler to the conversion of message to mail
                this.marshaler.convertJBIToMail(msg, exchange, in, this.sender, this.receiver);

                // Send message
                Transport.send(msg);

                // quit the exchange
                out.setContent(new StringSource("<ack />"));
            } catch (MessagingException mex) {
                logger.error("Error sending mail...", mex);
                throw mex;
            } finally {
                // delete all temporary allocated resources
                this.marshaler.cleanUpResources();
            }
        }
    }

    /**
     * @return the marshaler
     */
    public AbstractMailMarshaler getMarshaler() {
        return this.marshaler;
    }

    /**
     * @param marshaler the marshaler to set
     */
    public void setMarshaler(AbstractMailMarshaler marshaler) {
        this.marshaler = marshaler;
    }

    /**
     * @return the sender
     */
    public String getSender() {
        return this.sender;
    }

    /**
     * @param sender the sender to set
     */
    public void setSender(String sender) {
        this.sender = sender;
    }

    /**
     * returns the connection uri used for this poller endpoint
     * 
     * @return Returns the connection.
     */
    public String getConnection() {
        return this.connection;
    }

    /**
     * sets the connection uri
     * 
     * @param connection The connection to set.
     */
    public void setConnection(String connection) {
        this.connection = connection;
        try {
            this.config = MailUtils.configure(this.connection);
        } catch (ParseException ex) {
            LOG.error("The configured connection uri is invalid", ex);
        }
    }

    /**
     * @return the debugMode
     */
    public boolean isDebugMode() {
        return this.debugMode;
    }

    /**
     * @param debugMode the debugMode to set
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
     * @return the customTrustManagers
     */
    public String getCustomTrustManagers() {
        return this.customTrustManagers;
    }

    /**
     * @param customTrustManagers the customTrustManagers to set
     */
    public void setCustomTrustManagers(String customTrustManagers) {
        this.customTrustManagers = customTrustManagers;
    }

    /** 
     * @return Returns the receiver.
     */
    public String getReceiver() {
        return this.receiver;
    }

    /**
     * @param receiver The receiver to set.
     */
    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }
}
