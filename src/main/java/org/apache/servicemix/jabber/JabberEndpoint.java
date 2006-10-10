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
package org.apache.servicemix.jabber;

import org.apache.servicemix.common.ProviderEndpoint;
import org.apache.servicemix.common.ServiceUnit;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterPacket;
import org.jivesoftware.smack.packet.Message;

import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import java.net.URI;
import java.util.Iterator;

/**
 * Represents a base Jabber endpoint
 *
 * @version $Revision: $
 */
public abstract class JabberEndpoint extends ProviderEndpoint implements PacketListener {
    private JabberMarshaler marshaler = new JabberMarshaler();
    private XMPPConnection connection;
    private String host;
    private int port;
    private String user;
    private String password;
    private String resource = "ServiceMix";
    private boolean login = true;
    private PacketFilter filter;
    private boolean createAccount;

    public JabberEndpoint() {
    }

    public JabberEndpoint(JabberComponent component, ServiceEndpoint serviceEndpoint) {
        super(component, serviceEndpoint);
        init(component);
    }

    public void stop() throws Exception {
        super.stop();
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }

    public void start() throws Exception {
        super.start();
        if (connection == null) {
            if (port > 0) {
                connection = new XMPPConnection(host, port);
            }
            else {
                connection = new XMPPConnection(host);
            }
        }
        getConnection().addPacketListener(this, filter);
        if (login && !connection.isAuthenticated()) {
            if (user != null) {
                logger.info("Logging in to Jabber as user: " + user + " on connection: " + connection);
                if (password == null) {
                    logger.warn("No password configured for user: " + user);
                }

                if (createAccount) {
                    AccountManager accountManager = new AccountManager(connection);
                    accountManager.createAccount(user, password);
                }
                if (resource != null) {
                    connection.login(user, password, resource);
                }
                else {
                    connection.login(user, password);
                }
            }
            else {
                logger.info("Logging in anonymously to Jabber on connection: " + connection);
                connection.loginAnonymously();
            }

            // now lets send a presence

            connection.sendPacket(new Presence(Presence.Type.AVAILABLE));
        }
    }


    public void setServiceUnit(ServiceUnit serviceUnit) {
        super.setServiceUnit(serviceUnit);
        init((JabberComponent) serviceUnit.getComponent());
    }

    public void processPacket(Packet packet) {
        try {
            System.out.println("Received packet: " + packet);
            Iterator iter = packet.getPropertyNames();
            while (iter.hasNext()) {
                String property = (String) iter.next();
                System.out.println("Packet header: " + property + " value: " + packet.getProperty(property));
            }
            if (packet instanceof Message) {
                Message message = (Message) packet;
                System.out.println("Received message: " + message + " with " + message.getBody());

            }
            else if (packet instanceof RosterPacket) {
                RosterPacket rosterPacket = (RosterPacket) packet;
                System.out.println("Roster packet with : " + rosterPacket.getRosterItemCount());
                Iterator rosterItems = rosterPacket.getRosterItems();
                while (rosterItems.hasNext()) {
                    Object item = rosterItems.next();
                    System.out.println("Roster item: " + item);
                }

            }
            InOnly exchange = getExchangeFactory().createInOnlyExchange();
            NormalizedMessage in = exchange.createMessage();
            exchange.setInMessage(in);
            marshaler.toNMS(in, packet);
            System.out.println("Exchange: " + exchange);
            //send(exchange);
        }
        catch (MessagingException e) {
            throw new JabberListenerException(e, packet);
        }
    }

    // Properties
    //-------------------------------------------------------------------------

    /**
     * Configures the endpoint from a URI
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

    public JabberMarshaler getMarshaler() {
        return marshaler;
    }

    public void setMarshaler(JabberMarshaler marshaler) {
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


    protected void init(JabberComponent component) {
        if (user == null) {
            user = component.getUser();
        }
        if (password == null) {
            password = component.getPassword();
        }
    }

}
