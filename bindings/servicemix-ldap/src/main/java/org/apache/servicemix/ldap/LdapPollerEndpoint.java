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
package org.apache.servicemix.ldap;

import java.net.URL;
import java.util.ArrayList;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.naming.NamingEnumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.common.endpoints.PollingEndpoint;
import org.apache.servicemix.ldap.marshaler.DefaultLdapMarshaler;
import org.apache.servicemix.ldap.marshaler.LdapMarshalerSupport;

/**
 * <p>
 * This polling endpoint periodically request the LDAP directory (using endpoint properties)
 * and sends the LDAP entries/attributes response into the NMR.
 * This poller uses the LDAP marshaler to marshal/unmarshal LDAP request result into a normalized
 * message.
 * </p>
 * 
 * @author jbonofre
 * @org.apache.xbean.XBean element="poller"
 */
public class LdapPollerEndpoint extends PollingEndpoint implements LdapEndpointType {
    
    private final static transient Log LOG = LogFactory.getLog(LdapPollerEndpoint.class);
    
    private URL url; // the LDAP server URL looks like ldap://hostname:port
    private String contextFactory = "com.sun.jndi.ldap.LdapCtxFactory"; // the LDAP context factory to use, the default is the Sun one
    private String bindDn = null; // use the Distinguish Name bindDn to bind to the LDAP directory
    private String bindPassword = null; // bind password
    private String searchBase; // use searchBase as the starting point for the search instead of the default
    private String filter = "(objectclass=*)"; // the search filter
    private boolean newOnly = false; // fetch only new entries if true, fetch all entries if false
    private ArrayList cache = new ArrayList(); // contains the cache of latest LDAP entries
    private boolean persistent = false; // keep the LDAP connection open
    private LdapMarshalerSupport marshaler = new DefaultLdapMarshaler();
    
    private LdapConnection ldapConnection;
    
    public LdapPollerEndpoint() { }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.AbstractEndpoint#process(javax.jbi.messaging.MessageExchange)
     */
    @Override
    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            LOG.debug("Received DONE for a sent message");
            return;
        }
        if (exchange.getStatus() == ExchangeStatus.ERROR) {
            LOG.warn("Received ERROR state for a sent message");
            return;
        }
        throw new MessagingException("Unsupported exchange received ...");
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.PollingEndpoint#poll()
     */
    @Override
    public void poll() throws Exception {
        if (!persistent) {
            ldapConnection.connect();
        }

        LOG.debug("Define the search filter to " + filter + " in " + searchBase);
        NamingEnumeration namingEnumeration = ldapConnection.search(searchBase, filter);
        
        // TODO compare with the cache to send the new entries
        
        // create an InOnly exchange
        LOG.debug("Create the InOnly exchange.");
        InOnly exchange = getExchangeFactory().createInOnlyExchange();
        // create the "in" normalized message
        LOG.debug("Create the in message.");
        NormalizedMessage message = exchange.createMessage();
        exchange.setInMessage(message);
        
        // marshal the LDAP naming enumeration into the in message
        marshaler.marshal(message, namingEnumeration);
        
        // send the exchange
        LOG.debug("Send the exchange.");
        send(exchange);
        
        if (!persistent) {
            ldapConnection.disconnect();
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.ConsumerEndpoint#validate()
     */
    @Override
    public void validate() throws DeploymentException {
        super.validate();
        
        // TODO add an abstract WSDL
        
        // validate properties
        if (url == null) {
            throw new DeploymentException("LDAP URL is mandatory.");
        }
        if (contextFactory == null) {
            throw new DeploymentException("LDAP Context Factory is mandatory.");
        }
        
        // create the LDAP connection
        ldapConnection = new LdapConnection(url, contextFactory, bindDn, bindPassword);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.PollingEndpoint#start()
     */
    @Override
    public void start() throws Exception {
        super.start();
        if (persistent) {
            LOG.debug("The LDAP connection is persistent, connect to the LDAP server now");
            ldapConnection.connect();
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.PollingEndpoint#stop()
     */
    @Override
    public void stop() throws Exception {
        if (persistent) {
            LOG.debug("The LDAP connection is persistent, disconnect to the LDAP server now");
            ldapConnection.disconnect();
        }
        super.stop();
    }
    
    public URL getUrl() {
        return url;
    }
    
    /**
     * <p>
     * This attribute specifies the LDAP directory server URL.<br/>
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>null</b></i>
     * 
     * @param url a <code>URL</code> value representing the LDAP directory server URL
     */
    public void setUrl(URL url) {
        this.url = url;
    }
    
    public String getBindDn() {
        return bindDn;
    }
    
    /**
     * <p>
     * This attribute specifies the Distinguish Name to bind to the LDAP directory.<br/>
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>null</b></i>
     * 
     * @param bindDn a <code>String</code> value representing the Distinguish Name to bind to the LDAP directory
     */
    public void setBindDn(String bindDn) {
        this.bindDn = bindDn;
    }
    
    public String getBindPassword() {
        return bindPassword;
    }
    
    /**
     * <p>
     * This attribute specifies the DN password to bind to the LDAP directory.<br/>
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>null</b></i>
     * 
     * @param bindPassword a <code>String</code> value representing the DN password to bind to the LDAP directory
     */
    public void setBindPassword(String bindPassword) {
        this.bindPassword = bindPassword;
    }
    
    public String getFilter() {
        return filter;
    }
    
    /**
     * <p>
     * This attribute specifies the LDAP search filter.<br/>
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>(objectclass=*)</b></i>
     * @param filter
     */
    public void setFilter(String filter) {
        this.filter = filter;
    }
    
    public boolean isNewOnly() {
        return newOnly;
    }
    
    /**
     * <p>
     * This attribute specifies if the search fetch only new entries
     * or all entries.
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>false</b></i>
     * 
     * @param newOnly a <code>boolean</code> value representing if the search fetch all entries (false) or only new (true)
     */
    public void setNewOnly(boolean newOnly) {
        this.newOnly = newOnly;
    }
    
    public boolean isPersistent() {
        return persistent;
    }
    
    /**
     * <p>
     * This attribute specifies if the LDAP directory server connection is persistent or not.
     * Persistent means that the LDAP connection is made at the endpoint start (it's the same connection
     * for all endpoint polls). If persistent is made to false, the LDAP connection is performed at each
     * poll call.
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>false</b></i>
     * 
     * @param persistent a <code>boolean</code> value representing if the LDAP connection is persitent or not
     */
    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

}
