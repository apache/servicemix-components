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

import org.apache.servicemix.common.endpoints.ConsumerEndpoint;
import org.apache.servicemix.xmpp.marshaler.XMPPMarshalerSupport;
import org.apache.servicemix.xmpp.marshaler.impl.DefaultXMPPMarshaler;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smackx.muc.MultiUserChat;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

/**
 * This endpoint receives XMPP messages and events and sends them as xml message into
 * the NMR to the defined target
 *
 * @org.apache.xbean.XBean element="receiver"
 *
 * @author lhein
 */
public class XMPPReceiverEndpoint extends ConsumerEndpoint implements XMPPEndpointType, PacketListener {

    private XMPPMarshalerSupport marshaler = new DefaultXMPPMarshaler();
    private XMPPConnection connection;
    private String host;
    private int port = -1;
    private String user;
    private String password;
    private String resource;
    private String room;
    private String proxyHost;
    private String proxyPort;
    private String proxyUser;
    private String proxyPass;
    private String proxyType;
    private boolean login = true;
    private PacketFilter filter;
    private boolean createAccount;
    private ConnectionConfiguration connectionConfig;
    private ProxyInfo proxyInfo;

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
                MultiUserChat chatRoom = new MultiUserChat(this.connection, this.room);
                chatRoom.join(this.user);
            }
            this.connection.addPacketListener(this, this.filter);
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
    public void process(MessageExchange exchange) throws Exception {
        // only DONE and ERROR states will be received here and this
        // endpoint is not interested in such messages at all
    }

    public void processPacket(Packet packet) {
        if (packet instanceof Message == false ||
            ((Message)packet).getBody() == null) {
            // we do only process messages for now, so skip it
            return;
        }

        try {
            InOnly exchange = getExchangeFactory().createInOnlyExchange();
            NormalizedMessage in = exchange.createMessage();
            configureExchangeTarget(exchange);
            exchange.setInMessage(in);
            this.marshaler.toJBI(in, packet);
            getChannel().send(exchange);
        } catch (MessagingException e) {
            this.logger.error("Unable to send exchange for packet " + packet.toXML(), e);
        }
    }

    public XMPPMarshalerSupport getMarshaler() {
        return marshaler;
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
        return host;
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
        return port;
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
        return user;
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
        return password;
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
        return resource;
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
        return proxyHost;
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
        return proxyPort;
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
        return proxyUser;
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
        return proxyPass;
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
        return proxyType;
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
        return login;
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

    public PacketFilter getFilter() {
        return filter;
    }

    /**
     * <p>Here you can define a <code>PacketFilter</code> to use for filtering XMPP packets.
     *
     * @param filter    a <code>PacketFilter</code> to use for filtering XMPP packets
     */
    public void setFilter(PacketFilter filter) {
        this.filter = filter;
    }

    public boolean isCreateAccount() {
        return createAccount;
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
        return room;
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
}
