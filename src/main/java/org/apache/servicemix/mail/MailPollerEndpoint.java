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

import com.sun.mail.pop3.POP3Folder;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jbi.JBIException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;
import javax.mail.search.FlagTerm;

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

    private List<String> seenMessages = Collections.synchronizedList(new LinkedList<String>());

    private String customTrustManagers;

    private MailConnectionConfiguration config;

    private String connection;

    private int maxFetchSize = 5;

    private boolean processOnlyUnseenMessages;

    private boolean deleteProcessedMessages;

    private boolean debugMode;

    private boolean forgetTopHeaders;

    private boolean disableTop;

    private Map<String, String> customProperties;

    private List<String> foundMessagesInFolder = Collections.synchronizedList(new LinkedList<String>());

    private org.apache.servicemix.store.Store storage;

    /**
     * default constructor
     */
    public MailPollerEndpoint() {
        this.processOnlyUnseenMessages = true;
        this.deleteProcessedMessages = false;
        this.debugMode = false;
        this.forgetTopHeaders = false;
        this.disableTop = false;
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.servicemix.common.endpoints.ConsumerEndpoint#getLocationURI()
     */
    @Override
    public String getLocationURI() {
        // return a URI that unique identify this endpoint
        return getService() + "#" + getEndpoint();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.PollingEndpoint#start()
     */
    @Override
    public synchronized void start() throws Exception {
        super.start();

        if (this.storage != null) {
            String id = config.getUsername() + " @ " + config.getHost();
            try {
                // load the list of seen messages
                List<String> loadedMsg = (List<String>)this.storage.load(id);
                if (loadedMsg != null && !loadedMsg.isEmpty()) {
                    for (String uid : loadedMsg) {
                        if (!this.seenMessages.contains(uid)) {
                            this.seenMessages.add(uid);
                        }
                    }
                    loadedMsg.clear();
                }
            } catch (IOException ioex) {
                logger.error("Error loading seen messages for: " + id, ioex);
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.PollingEndpoint#stop()
     */
    @Override
    public synchronized void stop() throws Exception {
        if (this.storage != null) {
            String id = config.getUsername() + " @ " + config.getHost();
            try {
                // save the list of seen messages
                this.storage.store(id, this.seenMessages);
            } catch (IOException ioex) {
                logger.error("Error saving list of seen messages for: " + id, ioex);
            }
        }

        super.stop();
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.servicemix.common.ExchangeProcessor#process(javax.jbi.messaging
     * .MessageExchange)
     */
    public void process(MessageExchange arg0) throws Exception {
        // Do nothing. In our case, this method should never be called
        // as we only send synchronous InOnly exchange
    }

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.components.util.PollingComponentSupport#poll()
     */
    public void poll() throws Exception {
        LOG.debug("Polling mailfolder " + config.getFolderName() + " at host " + config.getHost() + "...");

        if (maxFetchSize == 0) {
            LOG.debug("The configuration is set to poll no new messages at all...skipping.");
            return;
        }

        boolean isPopProtocol = this.config.getProtocol().toLowerCase().indexOf("pop") > -1;

        // clear the list each run
        this.foundMessagesInFolder.clear();

        Store store = null;
        Folder folder = null;
        Session session = null;
        try {
            Properties props = MailUtils.getPropertiesForProtocol(this.config, this.customTrustManagers);
            props.put("mail.debug", isDebugMode() ? "true" : "false");
            props.put("mail.pop3.forgettopheaders", isForgetTopHeaders() ? "true" : "false");
            props.put("mail.pop3.disabletop", isDisableTop() ? "true" : "false");

            // apply the custom properties
            applyCustomProperties(props);

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

            Message[] messages = null;
            if (isProcessOnlyUnseenMessages() && !isPopProtocol) {
                messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            } else {
                messages = folder.getMessages();
            }

            String uid = null;

            int fetchSize = getMaxFetchSize() == -1 ? messages.length : Math.min(getMaxFetchSize(),
                                                                                 messages.length);
            int fetchedMessages = 0;

            for (int cnt = 0; cnt < messages.length; cnt++) {
                // get the message
                MimeMessage mailMsg = (MimeMessage)messages[cnt];

                if (isProcessOnlyUnseenMessages() && isPopProtocol) {
                    // POP3 doesn't support flags, so we need to check manually
                    // if message is new or not
                    if (folder instanceof POP3Folder) {
                        POP3Folder pf = (POP3Folder)folder;
                        uid = pf.getUID(mailMsg);

                        // remember each found message
                        if (uid != null) {
                            foundMessagesInFolder.add(uid);
                        }

                        // check if we already processed the message
                        if (uid != null && this.seenMessages.contains(uid)) {
                            // this message was already processed
                            uid = null;
                            continue;
                        }
                    }
                }

                // only process a message if the max message fetch size isn't
                // exceeded then
                if (fetchedMessages < fetchSize) {
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

                    // increment the fetched messages counter
                    fetchedMessages++;

                    // now check if delivery succeeded or went wrong
                    if (io.getStatus() == ExchangeStatus.ERROR) {
                        Exception e = io.getError();
                        if (e == null) {
                            e = new JBIException("Unexpected error occured...");
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
                        // remember the processed mail if needed
                        if (isProcessOnlyUnseenMessages() && isPopProtocol && uid != null) {
                            // POP3 doesn't support flags, so we need to
                            // remember processed mails
                            this.seenMessages.add(uid);
                        }
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
                // clean up the seen messages list because of maybe deleted
                // messages
                if (isProcessOnlyUnseenMessages() && isPopProtocol) {
                    cleanUpSeenMessages();
                }
            } catch (Exception ignored) {
                logger.debug(ignored);
            }
        }
    }

    /**
     * this method will check if a seen message was deleted from mail folder and
     * remove this from the list of messages already seen
     */
    private synchronized void cleanUpSeenMessages() {
        List<String> uidsToRemove = new LinkedList<String>();
        // first collect all uid's to remove
        for (String uid : seenMessages) {
            if (!foundMessagesInFolder.contains(uid)) {
                // the message was deleted from the mail folder, so delete it
                // also from the seen messages list as well
                uidsToRemove.add(uid);
            }
        }
        // now remove them
        for (String uid : uidsToRemove) {
            seenMessages.remove(uid);
        }
    }

    /**
     * applies custom properties to the used properties for mail server
     * connection
     * 
     * @param props the properties to apply to
     */
    private void applyCustomProperties(Properties props) {
        // allow custom properties
        if (customProperties != null) {
            props.putAll(customProperties);
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

    /**
     * @return the forgetTopHeaders
     */
    public boolean isForgetTopHeaders() {
        return this.forgetTopHeaders;
    }

    /**
     * @param forgetTopHeaders the forgetTopHeaders to set
     */
    public void setForgetTopHeaders(boolean forgetTopHeaders) {
        this.forgetTopHeaders = forgetTopHeaders;
    }

    /**
     * @return the disableTop
     */
    public boolean isDisableTop() {
        return this.disableTop;
    }

    /**
     * @param disableTop the disableTop to set
     */
    public void setDisableTop(boolean disableTop) {
        this.disableTop = disableTop;
    }

    /**
     * * @return Returns the customProperties.
     */
    public Map<String, String> getCustomProperties() {
        return this.customProperties;
    }

    /**
     * @param customProperties The customProperties to set.
     */
    public void setCustomProperties(Map<String, String> customProperties) {
        this.customProperties = customProperties;
    }

    /**
     * * @return Returns the storage.
     */
    public org.apache.servicemix.store.Store getStorage() {
        return this.storage;
    }

    /**
     * @param storage The storage to set.
     */
    public void setStorage(org.apache.servicemix.store.Store storage) {
        this.storage = storage;
    }
}
