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
import org.apache.servicemix.xmpp.exceptions.XMPPListenerException;
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
import org.jivesoftware.smack.packet.RosterPacket;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import java.net.URI;

/**
 * Represents a base XMPP endpoint
 *
 * @version $Revision: $
 */
public abstract class XMPPEndpoint extends ProviderEndpoint implements PacketListener {

    private XMPPMarshalerSupport marshaler = new DefaultXMPPMarshaler();
    private XMPPConnection connection;

    protected static final int DEFAULT_XMPP_PORT = 5222;

    private String host;
    private int port = -1;
    private String user;
    private String password;
    private String resource = "ServiceMix";
    private boolean login = true;
    private PacketFilter filter;
    private boolean createAccount;
    private ConnectionConfiguration connectionConfig;

    /**
     * Validate the endpoint at either deployment time for statically defined endpoints or at runtime for dynamic endpoints
     *
     * @throws javax.jbi.management.DeploymentException
     *
     */
    @Override
    public void validate() throws DeploymentException {
        super.validate();

        if (host == null || host.trim().length() < 1) {
            // invalid host name
            throw new DeploymentException("Missing host name!");
        }
        if (port < 0) {
            port = DEFAULT_XMPP_PORT;
        }
    }

    public void stop() throws Exception {
        if (connection != null && connection.isConnected()) {
            logger.debug("Disconnecting from server " + host);
            connection.disconnect();
            connection = null;
        }
        super.stop();
    }

    public void start() throws Exception {
        super.start();

        if (this.connectionConfig == null) {
            this.connectionConfig = new ConnectionConfiguration(this.host, this.port);
//            this.connectionConfig.setCompressionEnabled(true);
//            this.connectionConfig.setReconnectionAllowed(true);
//            this.connectionConfig.setSASLAuthenticationEnabled(true);
        }

        if (this.connection == null) {
            this.connection = new XMPPConnection(this.connectionConfig);
            logger.debug("Connecting to server " + host);
            this.connection.connect();
        }

        if (this.login && !this.connection.isAuthenticated()) {
            if (this.user != null) {
                logger.debug("Logging into Jabber as user: " + user + " on connection: " + connection);
                if (password == null) {
                    logger.warn("No password configured for user: " + user);
                }

                if (createAccount) {
                    AccountManager accountManager = new AccountManager(connection);
                    accountManager.createAccount(user, password);
                }
                if (resource != null) {
                    connection.login(user, password, resource);
                } else {
                    connection.login(user, password);
                }
            } else {
                logger.debug("Logging in anonymously to Jabber on connection: " + connection);
                connection.loginAnonymously();
            }
            getConnection().addPacketListener(this, filter);

            // now lets send a presence
            connection.sendPacket(new Presence(Presence.Type.available));
        }
    }

    public void processPacket(Packet packet) {
        try {
            logger.debug("Received packet: " + packet);

            if (logger.isDebugEnabled()) {
            for (String property : packet.getPropertyNames())
                {
                logger.debug("Packet header: " + property + " value: " + packet.getProperty(property));
                }
            }

            if (packet instanceof Message) {
                Message message = (Message) packet;
                logger.debug("Received message: " + message + " with " + message.getBody());
            } else if (packet instanceof RosterPacket) {
                RosterPacket rosterPacket = (RosterPacket) packet;

                if (logger.isDebugEnabled()) {
                    logger.debug("Roster packet with : " + rosterPacket.getRosterItemCount());
                    for (RosterPacket.Item item : rosterPacket.getRosterItems()) {
                        logger.debug("Roster item: " + item);
                    }
                }
            }
            InOnly exchange = getExchangeFactory().createInOnlyExchange();
            NormalizedMessage in = exchange.createMessage();
            exchange.setInMessage(in);
            marshaler.toJBI(in, packet);
            logger.debug("Exchange: " + exchange);
            //send(exchange);
        } catch (MessagingException e) {
            throw new XMPPListenerException(e, packet);
        }
    }

    // Properties
    //-------------------------------------------------------------------------

    /**
     * Configures the endpoint from a URI
     * @param uri   the connection uri
     */
    public void setUri(URI uri) {
        setHost(uri.getHost());
        setPort(uri.getPort());
        if (uri.getUserInfo() != null) {
            setUser(uri.getUserInfo());
        }
    }

    public XMPPConnection getConnection() {
        return connection;
    }

    public void setConnection(XMPPConnection connection) {
        this.connection = connection;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public XMPPMarshalerSupport getMarshaler() {
        return marshaler;
    }

    public void setMarshaler(XMPPMarshalerSupport marshaler) {
        this.marshaler = marshaler;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getResource() {
        return resource;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public boolean isLogin() {
        return login;
    }

    public void setLogin(boolean login) {
        this.login = login;
    }

    public boolean isCreateAccount() {
        return createAccount;
    }

    public void setCreateAccount(boolean createAccount) {
        this.createAccount = createAccount;
    }

    public PacketFilter getFilter() {
        return filter;
    }

    public void setFilter(PacketFilter filter) {
        this.filter = filter;
    }
}
