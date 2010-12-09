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
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * LDAP connection container.
 * </p>
 * 
 * @author jbonofre
 */
public class LdapConnection {
    
    private final static transient Log LOG = LogFactory.getLog(LdapConnection.class);
    
    private Hashtable<String, String> env = new Hashtable<String, String>();
    private DirContext context;
    private SearchControls searchControls;
    
    /**
     * <p>
     * Create a new LDAP connection.
     * </p>
     * 
     * @param url the LDAP URL.
     * @param contextFactory the LDAP context factory.
     * @param bindDn the LDAP bind DN (can be null).
     * @param bindPassword the LDAP bind password.
     */
    public LdapConnection(URL url, String contextFactory, String bindDn, String bindPassword) {
        LOG.debug("LDAP URL " + url);
        env.put(Context.PROVIDER_URL, url.toString());
        LOG.debug("Use LDAP initial context factory " + contextFactory);
        env.put(Context.INITIAL_CONTEXT_FACTORY, contextFactory);
        if (contextFactory.equals("")) {
            LOG.debug("IBM WebSphere initial context factory detected, set required properties.");
            env.put("com.ibm.websphere.naming.namespaceroot", "bootstraphostroot");
            env.put("com.ibm.ws.naming.ldap.config", "local");
            env.put("com.ibm.ws.naming.implementation", "WsnLdap");
            env.put("com.ibm.ws.naming.ldap.masterurl", url.toString());
        }
        if (bindDn != null) {
            LOG.debug("Use security authentication with bind DN " + bindDn);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, bindDn);
            env.put(Context.SECURITY_CREDENTIALS, bindPassword);
        }
        
        LOG.debug("Create search controls with subtree scope.");
        searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
    }
    
    /**
     * <p>
     * Connect to the LDAP directory.
     * </p>
     * 
     * @throws Exception in case of connection failure.
     */
    public void connect() throws Exception {
        if (context == null) {
            context = new InitialDirContext(env);
        }
    }
    
    /**
     * <p>
     * Disconnect from the LDAP directory.
     * </p>
     * 
     * @throws Exception in case of disconnect failure.
     */
    public void disconnect() throws Exception {
        if (context != null) {
            context.close();
            context = null;
        }
    }
    
    /**
     * <p>
     * Search entries on the LDAP directory.
     * </p>
     * 
     * @param searchBase the search base.
     * @param filter the search filter.
     * @return the NamingEnumeration containing entries.
     * @throws Exception in case of search failure.
     */
    public NamingEnumeration search(String searchBase, String filter) throws Exception {
        return context.search(searchBase, filter, searchControls);
    }

}
