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

import org.apache.servicemix.common.endpoints.PollingEndpoint;
import org.apache.servicemix.mail.marshaler.AbstractMailMarshaler;
import org.apache.servicemix.mail.marshaler.DefaultMailMarshaler;
import org.apache.servicemix.mail.utils.MailConnectionConfiguration;
import org.apache.servicemix.mail.utils.MailUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the polling endpoint for the mail component.
 * 
 * @org.apache.xbean.XBean element="poller"
 * @author lhein
 */
public class MailPollerEndpoint extends PollingEndpoint implements MailEndpointType {

    private final Logger logger = LoggerFactory.getLogger(MailPollerEndpoint.class);

    private AbstractMailMarshaler marshaler = new DefaultMailMarshaler();

    private final List<String> seenMessages = Collections.synchronizedList(new LinkedList<String>());

    private String customTrustManagers;

    private MailConnectionConfiguration config;

    private String connection;

    private int maxFetchSize = -1;

    private boolean processOnlyUnseenMessages;

    private boolean deleteProcessedMessages;

    private boolean debugMode;

    private Map<String, String> customProperties;

    private final List<String> foundMessagesInFolder = Collections.synchronizedList(new LinkedList<String>());

    private org.apache.servicemix.store.Store storage;

    /**
     * default constructor
     */
    public MailPollerEndpoint() {
    	super();
    	
        this.processOnlyUnseenMessages = true;
        this.deleteProcessedMessages = false;
        this.debugMode = false;
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
    @SuppressWarnings("unchecked")
    public synchronized void start() throws Exception {
        super.start();

        if (this.storage == null) {
        	return;
        }
        	
        String id = config.getUsername() + " @ " + config.getHost();
        try {
        	// load the list of seen messages
        	List<String> loadedMsg = (List<String>)this.storage.load(id);
        	if (loadedMsg == null || loadedMsg.isEmpty()) {
        		return;
        	}
        		
        	for (String uid : loadedMsg) {
        		if (!this.seenMessages.contains(uid)) {
        			this.seenMessages.add(uid);
        		}
        	}
        	loadedMsg.clear();
        } catch (IOException ioex) {
        	logger.error("Error loading seen messages for: " + id, ioex);
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
    public void process(MessageExchange exchange) throws Exception {
        // Do nothing. In our case, this method should never be called
        // as we only send synchronous InOnly exchange
    }

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.components.util.PollingComponentSupport#poll()
     */
    public void poll() throws Exception {
        logger.debug("Polling mailfolder " + config.getFolderName() + " at host " + config.getHost() + "...");

        if (maxFetchSize == 0) {
            logger.debug("The configuration is set to poll no new messages at all...skipping.");
            return;
        }

        boolean isPopProtocol = this.config.getProtocol().toLowerCase().indexOf("pop") > -1;

        // clear the list each run
        this.foundMessagesInFolder.clear();

        Store store = null;
        Folder folder = null;
        Session session;
        try {
            Properties props = MailUtils.getPropertiesForProtocol(this.config, this.customTrustManagers);
            props.put("mail.debug", isDebugMode() ? "true" : "false");

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

            Message[] messages;
            if (isProcessOnlyUnseenMessages() && !isPopProtocol) {
                messages = folder.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));
            } else {
                messages = folder.getMessages();
            }

            int fetchSize = getMaxFetchSize() == -1 ? messages.length : Math.min(getMaxFetchSize(),
                                                                                 messages.length);
            int fetchedMessages = 0;
            String uid;
            
            for (Message msg : messages) {
                uid = null;
                
                // get the message
                MimeMessage mailMsg = (MimeMessage)msg;

                if (isProcessOnlyUnseenMessages() && isPopProtocol) {
                    // POP3 doesn't support flags, so we need to check manually
                    // if message is new or not
                    try {
                        Object ouid = folder.getClass().getMethod("getUID", Message.class).invoke(folder, mailMsg);
                        
                        // remember each found message
                        if (ouid != null) {
                            uid = (String)ouid;
                            foundMessagesInFolder.add(uid);
                        }

                        // check if we already processed the message
                        if (uid != null && this.seenMessages.contains(uid)) {
                            // this message was already processed
                            continue;
                        }
                    } catch (Exception ex) {
                        // this folder doesn't provide UIDs for messages
                        logger.warn(getEndpoint() + ": Unable to determine unique id of mail.", ex);
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
                        // to ensure reprocessing of the mail we set it to UNSEEN even if we
                        // did not mark it seen before (seems there are some mail systems out there
                        // which do set somehow automatically)
                        mailMsg.setFlag(Flags.Flag.SEEN, false);

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
                logger.debug("", ignored);
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

    public boolean isDeleteProcessedMessages() {
        return this.deleteProcessedMessages;
    }

    /**
     * <p>This flag is used to indicate what happens to a processed mail polled
     * from a mail folder. If it is set to <code>true</code> the mail will
     * be deleted after it was sent into the bus successfully. If set to
     * <code>false</code> the mail will reside inside the mail folder but will
     * be marked as already seen.<br/> 
     * If the sending of the mail results in an error, the mail will not be
     * deleted / marked and reprocessed on next run of the polling cycle.<p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>false</b></i> 
     * 
     * @param deleteProcessedMessages 
     * 				a <code>boolean</code> value as flag
     */
    public void setDeleteProcessedMessages(boolean deleteProcessedMessages) {
        this.deleteProcessedMessages = deleteProcessedMessages;
    }

    public AbstractMailMarshaler getMarshaler() {
        return this.marshaler;
    }

    /**
     * <p>With this method you can specify a marshaler class which provides the
     * logic for converting a mail into a normalized message. This class has
     * to extend the abstract class <code>AbstractMailMarshaler</code> or an
     * extending class. If you don't specify a marshaler, the 
     * <code>DefaultMailMarshaler</code> will be used.</p>
     * 
     * @param marshaler 
     * 				a class which extends <code>AbstractMailMarshaler</code>
     */
    public void setMarshaler(AbstractMailMarshaler marshaler) {
        this.marshaler = marshaler;
    }

    public int getMaxFetchSize() {
        return this.maxFetchSize;
    }

    /**
     * <p>This sets the maximum amount of mails to process within one polling cycle.
     * If the maximum amount is reached all other mails in "unseen" state will 
     * be skipped.</p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>-1 (unlimited)</b></i><br/><br/>
     * 
     * @param maxFetchSize 
     * 				a <code>int</code> value for maximum to be polled messages
     */
    public void setMaxFetchSize(int maxFetchSize) {
        this.maxFetchSize = maxFetchSize;
    }

    public boolean isProcessOnlyUnseenMessages() {
        return this.processOnlyUnseenMessages;
    }

    /**
     * <p>This flag is used to indicate whether all mails are polled from a 
     * mail folder or only the unseen mails are processed.<br/><br />
     * If it is set to <b><code>true</code></b> only the unseen mails will be 
     * processed.<br /> 
     * If it is set to <b><code>false</code></b> all mails will be processed.<br/></p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>true</b></i><br/><br/>
     * 
     * @param processOnlyUnseenMessages 
     * 				a <code>boolean</code> value as flag
     */
    public void setProcessOnlyUnseenMessages(boolean processOnlyUnseenMessages) {
        this.processOnlyUnseenMessages = processOnlyUnseenMessages;
    }

    public String getConnection() {
        return this.connection;
    }

    /**
     * <p>Specifies the connection URI used to connect to a mail server.
     * <br /><br />
     * <b><u>Templates:</u></b> <br />
     *     &nbsp;&nbsp;&nbsp;<i>&lt;protocol&gt;://&lt;user&gt;@&lt;host&gt;[:&lt;port&gt;][/&lt;folder&gt;]?password=&lt;password&gt;</i>
     *     <br /><b>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;OR</b><br/>
     *     &nbsp;&nbsp;&nbsp;<i>&lt;protocol&gt;://&lt;host&gt;[:&lt;port&gt;][/&lt;folder&gt;]?user=&lt;user&gt;;password=&lt;password&gt;</i>
     * <br /><br />
     * <b><u>Details:</u></b><br /><br/>
     * <table border="0" cellpadding="0" cellspacing="0">
     * <tr>
     * 		<td width="40%" align="left"><b><u>Name</u></b></td>
     * 		<td width="60%" align="left"><b><u>Description</u></b></td>
     * </tr>
     * <tr>
     * 		<td>protocol</td>
     * 		<td>the protocol to use (example: pop3 or imap)</td>
     * </tr>
     * <tr>
     * 		<td>user</td>
     * 		<td>the user name used to log into an account</td>
     * </tr>
     * <tr>
     * 		<td>host</td>
     * 		<td>the name or ip address of the mail server</td>
     * </tr>
     * <tr>
     * 		<td>port</td>
     * 		<td>the port number to use (optional)</td>
     * </tr>
     * <tr>
     * 		<td>folder</td>
     * 		<td>the folder to poll from (optional)</td>
     * </tr>
     * <tr>
     * 		<td>password</td>
     * 		<td>the password for the login</td>
     * </tr>
     * </table>
     * <br/>
     * <b><u>Examples:</u></b><br />
     * &nbsp;&nbsp;&nbsp;<i>imap://lhein@imapserver:143/INBOX?password=mypass</i><br />
     * &nbsp;&nbsp;&nbsp;<i>pop3://pop3server/INBOX?user=me@myhome.org;password=mypass</i></p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>null</b></i><br/><br/>
     * 
     * @param connection 
     * 				a <code>String</code> value containing the connection details
     */
    public void setConnection(String connection) {
        this.connection = connection;
        try {
            this.config = MailUtils.configure(this.connection);
        } catch (ParseException ex) {
            logger.error("The configured connection uri is invalid", ex);
        }
    }

    public boolean isDebugMode() {
        return this.debugMode;
    }

    /**
     * <p>Specifies if the JavaMail is run in <code>DEBUG</code> mode. This means
     * that while connecting to server and processing mails a detailed log
     * is written to debug output. <br />
     * This mode is very handy if you are experiencing problems with your
     * mail server connection and you want to find out what is going wrong
     * in communication with the server.
     * <br /><br />
     * &nbsp;&nbsp;&nbsp;<b>true</b> - <i>the debug mode is <b>enabled</b></i>
     * <br />
     * &nbsp;&nbsp;&nbsp;<b>false</b> - <i>the debug mode is <b>disabled</b></i></p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>false</b></i><br/><br/>
     * 
     * @param debugMode 
     * 				a <code>boolean</code> value for debug mode
     */
    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public String getCustomTrustManagers() {
        return this.customTrustManagers;
    }

    /**
     * <p>Specifies one or more trust manager classes separated by a semicolon (<b>;</b>).<br/>
     * These classes have to implement the <code>Trustmanager</code> interface and need to provide
     * an empty default constructor to be valid.<br/><br />
     * If you want to accept all security certificates without a check you may 
     * consider using the <code>DummyTrustManager</code> class. It is actually only
     * an empty stub without any checking logic. <br/><b>But be aware that this will be
     * a security risk in production environments. </b></p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>null</b></i><br/><br/>
     * 
     * @param customTrustManagers 
     * 					a <code>String</code> value containing one or more full class names separated by <b>;</b> char
     */
    public void setCustomTrustManagers(String customTrustManagers) {
        this.customTrustManagers = customTrustManagers;
    }

    public Map<String, String> getCustomProperties() {
        return this.customProperties;
    }

    /**
     * <p>Specifies a <code>java.util.Map</code> which may contain additional
     * properties for the connection. <br/>
     * <br/><b><u>Example for disabling TOP for POP3 headers:</u></b><br />
     * &nbsp;<i><b>key</b>: "mail.pop3.disabletop"</i> <br />
     * &nbsp;<i><b>value</b>: "true"</i></p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>null</b></i><br/><br/>
     * 
     * @param customProperties 
     * 					a <code>java.util.Map&lt;String, String&gt;</code> containing connection properties
     */
    public void setCustomProperties(Map<String, String> customProperties) {
        this.customProperties = customProperties;
    }

    public org.apache.servicemix.store.Store getStorage() {
        return this.storage;
    }

    /**
     * <p>Specifies a <code>org.apache.servicemix.store.Store</code> object which 
     * will be used for storing the identifications of already processed messages.<br/>
     * <b>This store is only used with the POP3 protocol and if unseen mails are 
     * processed only.</b></p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>null</b></i><br/><br/>
     * 
     * @param storage 
     * 					a <code>org.apache.servicemix.store.Store</code> object for storing seen message idents
     */
    public void setStorage(org.apache.servicemix.store.Store storage) {
        this.storage = storage;
    }
}
