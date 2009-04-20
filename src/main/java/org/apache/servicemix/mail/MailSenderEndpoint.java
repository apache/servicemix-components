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

import java.util.List;
import java.util.Map;
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
    private Map<String, String> customProperties;
    private List<String> ignoreMessageProperties;

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.Endpoint#validate()
     */
    public void validate() throws DeploymentException {
        super.validate();

        if (this.config == null || this.connection == null) {
            throw new DeploymentException("No valid connection uri provided.");
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.servicemix.common.endpoints.ProviderEndpoint#processInOnly
     * (javax.jbi.messaging.MessageExchange,
     * javax.jbi.messaging.NormalizedMessage)
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

                // apply the custom properties
                applyCustomProperties(props);

                // Get session
                session = Session.getInstance(props, config.getAuthenticator());

                // debug the session
                session.setDebug(this.debugMode);

                // get the transport from session
                Transport transport = session.getTransport(config.getProtocol());

                // Connect only once here
                // Transport.send() disconnects after each send
                // Usually, no username and password is required for SMTP
                transport.connect(config.getHost(), config.getPort(), config.getUsername(), config
                    .getPassword());

                // Define message
                MimeMessage msg = new MimeMessage(session);

                // handle ignore properties
                handleIgnoreProperties(in);
                
                // let the marshaler to the conversion of message to mail
                this.marshaler.convertJBIToMail(msg, exchange, in, this.sender, this.receiver);

                // Send message
                transport.sendMessage(msg, msg.getAllRecipients());

                // close transport
                transport.close();
            } catch (MessagingException mex) {
                logger.error("Error sending mail...", mex);
                throw mex;
            } finally {
                // delete all temporary allocated resources
                this.marshaler.cleanUpResources(exchange.getExchangeId());
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.servicemix.common.endpoints.ProviderEndpoint#processInOut(
     * javax.jbi.messaging.MessageExchange,
     * javax.jbi.messaging.NormalizedMessage,
     * javax.jbi.messaging.NormalizedMessage)
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

                // apply the custom properties
                applyCustomProperties(props);

                // Get session
                session = Session.getInstance(props, config.getAuthenticator());

                // debug the session
                session.setDebug(this.debugMode);

                // get the transport from session
                Transport transport = session.getTransport(config.getProtocol());

                // Connect only once here
                // Transport.send() disconnects after each send
                // Usually, no username and password is required for SMTP
                transport.connect(config.getHost(), config.getPort(), config.getUsername(), config
                    .getPassword());

                // Define message
                MimeMessage msg = new MimeMessage(session);

                // handle ignore properties
                handleIgnoreProperties(in);
                
                // let the marshaler to the conversion of message to mail
                this.marshaler.convertJBIToMail(msg, exchange, in, this.sender, this.receiver);

                // Send message
                transport.sendMessage(msg, msg.getAllRecipients());

                // close transport
                transport.close();

                // quit the exchange
                out.setContent(new StringSource("<ack />"));
            } catch (MessagingException mex) {
                logger.error("Error sending mail...", mex);
                throw mex;
            } finally {
                // delete all temporary allocated resources
                this.marshaler.cleanUpResources(exchange.getExchangeId());
            }
        }
    }

    /**
     * handles the normalized messages ignored properties, means it will set every
     * property value to null for each key inside the list of properties to ignore
     * 
     * @param in	the normalized message
     */
    private void handleIgnoreProperties(NormalizedMessage in) {
    	if (getIgnoreMessageProperties() != null && getIgnoreMessageProperties().size()>0) {
    		for (String key : getIgnoreMessageProperties()) {
    			if (in.getProperty(key) != null) {
    				in.setProperty(key, null);
    			}
    		}
        }
    }
    
    /**
     * this will apply the custom properties to the properties map used for
     * connection to mail server
     * 
     * @param props the properties to apply to
     */
    private void applyCustomProperties(Properties props) {
        // allow custom properties
        if (customProperties != null) {
            props.putAll(customProperties);
        }
    }

    public AbstractMailMarshaler getMarshaler() {
        return this.marshaler;
    }

    /**
     * <p>With this method you can specify a marshaler class which provides the
     * logic for converting a normalized message into a mail. This class has
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

    public String getSender() {
        return this.sender;
    }

    /**
     * <p>Specifies the sender address of the mail which is being sent.</p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>no-reply@localhost</b></i><br/><br/>
     * 
     * @param sender 
     * 				a <code>String</code> value containing the sender address
     */
    public void setSender(String sender) {
        this.sender = sender;
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
     * <b><u>Example:</u></b><br />
     * &nbsp;&nbsp;&nbsp;<i>smtp://lhein@myserver?password=myPass</i><br /></p>
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
            LOG.error("The configured connection uri is invalid", ex);
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

    public String getReceiver() {
        return this.receiver;
    }

    /**
     * <p>Specifies the receiver address(es) of the mail which is being sent.</p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>null</b></i><br/><br/>
     * 
     * @param receiver 
     * 				a <code>String</code> value containing the receiver address(es)
     */
    public void setReceiver(String receiver) {
        this.receiver = receiver;
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

	public List<String> getIgnoreMessageProperties() {
		return this.ignoreMessageProperties;
	}

	/**
     * <p>Specifies a <code>java.util.Map</code> which may contain additional
     * properties for the connection. <br/>
     * <br/><b><u>Example for skipping all kind of addresses from the normalized message:</u></b><br />
     * &nbsp;<i><b>value</b>: "org.apache.servicemix.mail.to"</i> <br />
     * &nbsp;<i><b>value</b>: "org.apache.servicemix.mail.cc"</i> <br />
     * &nbsp;<i><b>value</b>: "org.apache.servicemix.mail.bcc"</i> <br />
     * &nbsp;<i><b>value</b>: "org.apache.servicemix.mail.from"</i> <br />
     * &nbsp;<i><b>value</b>: "org.apache.servicemix.mail.replyto"</i> <br /></p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>null</b></i><br/><br/>
     * 
     * @param ignoreMessageProperties 
     * 					a <code>java.util.List&lt;String&gt;</code> containing keys of properties to ignore
     */
	public void setIgnoreMessageProperties(List<String> ignoreMessageProperties) {
		this.ignoreMessageProperties = ignoreMessageProperties;
	}
}
