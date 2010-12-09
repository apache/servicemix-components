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
package org.apache.servicemix.xmpp;

import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.common.util.MessageUtil;
import org.apache.servicemix.xmpp.marshaler.XMPPMarshalerSupport;
import org.apache.servicemix.xmpp.marshaler.impl.DefaultXMPPMarshaler;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smackx.muc.MultiUserChat;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

/**
 * This endpoint sends XMPP messages from the NMR to a specified
 * target (user or room)
 *
 * @org.apache.xbean.XBean element="sender"
 *
 * @author lhein
 */
public class XMPPSenderEndpoint extends ProviderEndpoint implements XMPPEndpointType{

    private XMPPMarshalerSupport marshaler = new DefaultXMPPMarshaler();
    private XMPPConnection connection;
    private String host;
    private int port = -1;
    private String user;
    private String password;
    private String resource;
    private String room;
    private String participant;
    private String proxyHost;
    private String proxyPort;
    private String proxyUser;
    private String proxyPass;
    private String proxyType;
    private boolean login = true;
    private boolean createAccount;
    private ConnectionConfiguration connectionConfig;
    private ProxyInfo proxyInfo;
    private Chat userChat;
    private MultiUserChat chatRoom;

    @Override
    public void validate() throws DeploymentException {
        super.validate();

        if (this.host == null || this.host.trim().length() < 1) {
            // invalid host name
            throw new DeploymentException("Missing host name!");
        }
        if (this.port < 0) {
            this.port = XMPPConstants.DEFAULT_XMPP_PORT;
        }
        if ((this.participant == null && this.room == null) ||
            (this.participant != null && this.room != null)) {
            throw new DeploymentException("You have to define exactly one of the attributes 'room' and 'participant'!");
        }
    }

    @Override
    public void start() throws Exception {
        super.start();

        // create a proxy info object if needed
        if (this.proxyInfo == null && XMPPConstants.isSet(this.proxyHost)) {
            ProxyInfo.ProxyType pType = null;
            if (XMPPConstants.isSet(this.proxyType)) {
                pType = ProxyInfo.ProxyType.valueOf(this.proxyType.toUpperCase());
            }
            this.proxyInfo = new ProxyInfo(pType,
                    this.proxyHost,
                    XMPPConstants.isSet(this.proxyPort) ? Integer.parseInt(this.proxyPort) : XMPPConstants.DEFAULT_PROXY_PORT,
                    this.proxyUser,
                    this.proxyPass);
        }

        // create the connection config
        if (this.connectionConfig == null) {
            if (this.proxyInfo != null) {
                this.connectionConfig = new ConnectionConfiguration(this.host, this.port, this.proxyInfo);
            } else {
                this.connectionConfig = new ConnectionConfiguration(this.host, this.port);
            }

            this.connectionConfig.setCompressionEnabled(true);
            this.connectionConfig.setReconnectionAllowed(true);
            this.connectionConfig.setSASLAuthenticationEnabled(true);
        }

        if (this.connection == null) {
            this.connection = new XMPPConnection(this.connectionConfig);
            this.logger.debug("Connecting to server " + this.host);
            this.connection.connect();
        }

        if (this.login && !this.connection.isAuthenticated()) {
            if (this.user != null) {
                this.logger.debug("Logging into Jabber as user: " + this.user + " on connection: " + this.connection);
                if (this.password == null) {
                    this.logger.warn("No password configured for user: " + this.user);
                }

                if (this.createAccount) {
                    AccountManager accountManager = new AccountManager(this.connection);
                    accountManager.createAccount(this.user, this.password);
                }
                if (this.resource != null) {
                    this.connection.login(this.user, this.password, this.resource);
                } else {
                    this.connection.login(this.user, this.password);
                }
            } else {
                this.logger.debug("Logging in anonymously to Jabber on connection: " + this.connection);
                this.connection.loginAnonymously();
            }
            // now lets send a presence we are available
            this.connection.sendPacket(new Presence(Presence.Type.available));
        }

        // now register listener for new packets
        if (this.connection != null && this.connection.isConnected()) {
            // if the user specified a chat room to join we do this here
            if (this.room != null) {
                this.chatRoom = new MultiUserChat(this.connection, this.room);
                this.chatRoom.join(this.user);
            } else if (this.participant != null) {
                this.userChat = this.connection.getChatManager().createChat(this.participant, null);
            }
        }
    }

    @Override
    public void stop() throws Exception {
        if (this.connection != null && this.connection.isConnected()) {
            this.logger.debug("Disconnecting from server " + this.host);
            this.connection.disconnect();
            this.connection = null;
        }
        super.stop();
    }

    @Override
    protected void processInOnly(MessageExchange exchange, NormalizedMessage in) throws Exception {
        // Exchange is finished
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            //return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            // Exchange has been aborted with an exception
            //return;
        } else if (exchange.getFault() != null) {
            // Fault message
            exchange.setStatus(ExchangeStatus.DONE);
            getChannel().send(exchange);
        } else {
            // send the message to XMPP server
            sendMessage(exchange, in);
        }
    }

    @Override
    protected void processInOut(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws Exception {
        // Exchange is finished
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            //return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            // Exchange has been aborted with an exception
            //return;
        } else if (exchange.getFault() != null) {
            // Fault message
            exchange.setStatus(ExchangeStatus.DONE);
            getChannel().send(exchange);
        } else {
            // send the message to XMPP server
            sendMessage(exchange, in);
            // and copy the input message to the output
            MessageUtil.transferInToOut(exchange, exchange);
            // to finally send it back to the sender on success
            getChannel().send(exchange);
        }
    }

    /**
     * retrieves the message from the exchange and sends it to the defined target
     *
     * @param exchange  the message exchange received through NMR
     * @param inMsg     the normalized inMessage from the exchange
     * @throws Exception    on errors
     */
    private void sendMessage(MessageExchange exchange, NormalizedMessage inMsg) throws Exception {
        // first we create a new dummy message
        Message message = null;

        if (this.participant != null) {
            message = new Message(this.participant, Message.Type.normal);
        } else {
            message = this.chatRoom.createMessage();
            message.setTo(this.room);
        }

        // that message gets filled through the marshaler
        getMarshaler().fromJBI(message, exchange, inMsg);

        // initialize some more fields of the message
        message.setFrom(this.user);

        // distinguish between single and multi user chat
        if (this.participant != null) {
            // single chat message
            this.userChat.sendMessage(message);
        } else {
            // chat room message
            this.chatRoom.sendMessage(message);
        }
    }

    public XMPPMarshalerSupport getMarshaler() {
        return this.marshaler;
    }

    /**
     * <p>With this method you can specify a marshaler class which provides the
     * logic for converting an xmpp message into a normalized message. This class has
     * to implement the interface <code>XMPPMarshalerSupport</code> or another class which
     * implements it. If you don't specify a marshaler, the
     * <code>DefaultXMPPMarshaler</code> will be used.</p>
     *
     * @param marshaler
     * 				a class which implements <code>XMPPMarshalerSupport</code>
     */
    public void setMarshaler(XMPPMarshalerSupport marshaler) {
        this.marshaler = marshaler;
    }

    public String getHost() {
        return this.host;
    }

    /**
     * <p>With that method you can specify the host name of the XMPP server as
     * hostname or ip address.</p>
     *
     * @param host  the hostname or ip address of the XMPP server
     */
    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return this.port;
    }

    /**
     * <p>This method will set the port number for the XMPP connection. If nothing
     * is defined the default XMPP port number 5222 will be used.</p>
     *
     * @param port  the port number of the XMPP server
     */
    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return this.user;
    }

    /**
     * <p>This method if used to specify the user name to use for connecting to
     * the XMPP server. It is not required that this user already exists but if
     * not then the server should allow registration of new users and this user
     * should not already exist with another password.</p>
     *
     * @param user  the name of the user to use for connecting. for example: joe
     */
    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return this.password;
    }

    /**
     * <p>This method sets the password for connecting to the XMPP server.</p>
     *
     * @param password  the password for connecting to the XMPP server
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public String getResource() {
        return this.resource;
    }

    /**
     * <p>Specify here the resource string to submit to the XMPP server. Usually you define
     * the identifier of the XMPP client here.</p>
     *
     * @param resource  the resource identifier (for example: servicemix-xmpp)
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    public String getProxyHost() {
        return this.proxyHost;
    }

    /**
     * <p>Here you can specify the hostname or ip address of a proxy to be used to connect
     * to the XMPP server. If you don't define this no proxy is used.</p>
     *
     * @param proxyHost     the hostname or ip address of the proxy to use
     */
    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public String getProxyPort() {
        return this.proxyPort;
    }

    /**
     * <p>Here you can specify the port of the proxy server. If you do not define this the
     * default port (3128) will be used.
     *
     * @param proxyPort     the default proxy port number (default: 3128)
     */
    public void setProxyPort(String proxyPort) {
        this.proxyPort = proxyPort;
    }

    public String getProxyUser() {
        return this.proxyUser;
    }

    /**
     * <p>If your proxy needs authentication you can specify here the user name. Leave this
     * undefined if your proxy does not need authentication.</p>
     *
     * @param proxyUser     the name of the user to authenticate with the proxy
     */
    public void setProxyUser(String proxyUser) {
        this.proxyUser = proxyUser;
    }

    public String getProxyPass() {
        return this.proxyPass;
    }

    /**
     * <p>If your proxy needs authentication you can specify here the user password. Leave this
     * undefined if your proxy does not need authentication.</p>
     *
     * @param proxyPass the password of the user to authenticate with the proxy
     */
    public void setProxyPass(String proxyPass) {
        this.proxyPass = proxyPass;
    }

    public String getProxyType() {
        return this.proxyType;
    }

    /**
     * <p>Here you can specify the type of proxy you have. Possible values are:
     *   <code>NONE</code>, <code>HTTP</code>, <code>SOCKS4</code>, <code>SOCKS5</code>
     *
     * @param proxyType  the type of proxy (<code>NONE</code>, <code>HTTP</code>, <code>SOCKS4</code>, <code>SOCKS5</code>)
     */
    public void setProxyType(String proxyType) {
        this.proxyType = proxyType;
    }

    public boolean isLogin() {
        return this.login;
    }

    /**
     * <p>Here you can specify if the user should login to the server or not. Not logging in means that endpoint
     * itself will be created but it will be inactive.</p>
     *
     * @param login     a flag if the process should login to the XMPP account (true / false)
     */
    public void setLogin(boolean login) {
        this.login = login;
    }

    public boolean isCreateAccount() {
        return this.createAccount;
    }

    /**
     * <p>Specify here if you want to create an account for the user if
     * the user is currently not existing on the XMPP server.</p>
     *
     * @param createAccount     flag if an account should be created if the user doesn't exist (true / false)
     */
    public void setCreateAccount(boolean createAccount) {
        this.createAccount = createAccount;
    }

    public String getRoom() {
        return this.room;
    }

    /**
     * <p>Specify here an optional room to join. If set, the user
     * will join that room and listens to messages there.</p>
     *
     * @param room  the room to join or null if no room should be joined
     */
    public void setRoom(String room) {
        this.room = room;
    }

    public String getParticipant() {
        return participant;
    }

    /**
     * <p>Specify here an optional participant to send messages
     * to. You have to define a room or participant in order
     * to have send function working.</p>
     *
     * @param participant   the receiver of the message (for example: joe@jabber.org)
     */
    public void setParticipant(String participant) {
        this.participant = participant;
    }
}
