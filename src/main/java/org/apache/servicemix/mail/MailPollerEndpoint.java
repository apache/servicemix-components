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

import javax.jbi.JBIException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.servicemix.common.endpoints.PollingEndpoint;

import org.apache.servicemix.mail.marshaler.AbstractMailMarshaler;
import org.apache.servicemix.mail.marshaler.DefaultMailMarshaler;
import org.apache.servicemix.mail.utils.MailConnectionConfiguration;
import org.apache.servicemix.mail.utils.MailUtils;

/**
 * This is the polling endpoint for the mail component.
 * 
 * @org.apache.xbean.XBean element="poller"
 * @author lhein
 */
public class MailPollerEndpoint extends PollingEndpoint implements MailEndpointType {
    private static final transient Log LOG = LogFactory.getLog(MailPollerEndpoint.class);

    private AbstractMailMarshaler marshaler = new DefaultMailMarshaler();
    private String customTrustManagers;
    private MailConnectionConfiguration config;

    private String connection;

    private int maxFetchSize = 5;

    private boolean processOnlyUnseenMessages;
    private boolean deleteProcessedMessages;
    private boolean debugMode;

    /**
     * default constructor
     */
    public MailPollerEndpoint() {
        this.processOnlyUnseenMessages = true;
        this.deleteProcessedMessages = false;
        this.debugMode = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.ConsumerEndpoint#getLocationURI()
     */
    @Override
    public String getLocationURI() {
        // return a URI that unique identify this endpoint
        return getService() + "#" + getEndpoint();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.ExchangeProcessor#process(javax.jbi.messaging.MessageExchange)
     */
    public void process(MessageExchange arg0) throws Exception {
        // Do nothing. In our case, this method should never be called
        // as we only send synchronous InOnly exchange
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.components.util.PollingComponentSupport#poll()
     */
    public void poll() throws Exception {
        LOG.debug("Polling mailfolder " + config.getFolderName() + " at host " + config.getHost() + "...");

        Store store = null;
        Folder folder = null;
        Session session = null;
        try {
            Properties props = MailUtils.getPropertiesForProtocol(this.config, this.customTrustManagers);
            props.put("mail.debug", isDebugMode() ? "true" : "false");

            // Get session
            session = Session.getInstance(props, config.getAuthenticator());

            // debug the session
            session.setDebug(this.debugMode);

            store = session.getStore(config.getProtocol());
            store.connect(config.getHost(), config.getUsername(), config.getPassword());
            folder = store.getFolder(config.getFolderName());
            if (folder == null || !folder.exists()) {
                throw new Exception("Folder not found or invalid: " + config.getFolderName());
            }
            folder.open(Folder.READ_WRITE);

            int msgCount = 0;
            // check for max fetch size
            if (this.maxFetchSize == -1) {
                // -1 means no restrictions at all - so poll all messages
                msgCount = folder.getMessageCount();
            } else {
                // poll only the set max fetch size
                msgCount = Math.min(this.maxFetchSize, folder.getMessageCount());
            }

            for (int i = 1; i <= msgCount; i++) {
                // get the message
                MimeMessage mailMsg = (MimeMessage)folder.getMessage(i);

                // check if the message may be processed
                if (isProcessOnlyUnseenMessages() && mailMsg.isSet(Flags.Flag.SEEN)) {
                    // this message should not be processed because
                    // the configuration says to process only unseen messages
                    LOG.debug("Skipped seen mail: " + mailMsg.getSubject());
                    continue;
                }

                // create a inOnly exchange
                InOnly io = getExchangeFactory().createInOnlyExchange();

                // configure the exchange target
                configureExchangeTarget(io);

                // create the in message
                NormalizedMessage normalizedMessage = io.createMessage();

                // now let the marshaller convert the mail into a normalized
                // message to send to jbi bus
                marshaler.convertMailToJBI(io, normalizedMessage, mailMsg);

                // then put the in message into the inOnly exchange
                io.setInMessage(normalizedMessage);

                // and use sendSync to deliver it
                sendSync(io);

                // now check if delivery succeeded or went wrong
                if (io.getStatus() == ExchangeStatus.ERROR) {
                    Exception e = io.getError();
                    if (e == null) {
                        e = new JBIException("Unexpected error: " + e.getMessage());
                    }
                    throw e;
                } else {
                    // then mark the mail as processed (only if no errors)
                    if (deleteProcessedMessages) {
                        // processed messages have to be marked as deleted
                        mailMsg.setFlag(Flags.Flag.DELETED, true);
                    } else {
                        // processed messages have to be marked as seen
                        mailMsg.setFlag(Flags.Flag.SEEN, true);
                    }
                }
            }
        } finally {
            // finally clean up and close the folder and store
            try {
                if (folder != null) {
                    folder.close(true);
                }
                if (store != null) {
                    store.close();
                }
            } catch (Exception ignored) {
                logger.debug(ignored);
            }
        }
    }

    /**
     * @return the deleteProcessedMessages
     */
    public boolean isDeleteProcessedMessages() {
        return this.deleteProcessedMessages;
    }

    /**
     * @param deleteProcessedMessages the deleteProcessedMessages to set
     */
    public void setDeleteProcessedMessages(boolean deleteProcessedMessages) {
        this.deleteProcessedMessages = deleteProcessedMessages;
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
     * @return the maxFetchSize
     */
    public int getMaxFetchSize() {
        return this.maxFetchSize;
    }

    /**
     * @param maxFetchSize the maxFetchSize to set
     */
    public void setMaxFetchSize(int maxFetchSize) {
        this.maxFetchSize = maxFetchSize;
    }

    /**
     * @return the processOnlyUnseenMessages
     */
    public boolean isProcessOnlyUnseenMessages() {
        return this.processOnlyUnseenMessages;
    }

    /**
     * @param processOnlyUnseenMessages the processOnlyUnseenMessages to set
     */
    public void setProcessOnlyUnseenMessages(boolean processOnlyUnseenMessages) {
        this.processOnlyUnseenMessages = processOnlyUnseenMessages;
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
}
