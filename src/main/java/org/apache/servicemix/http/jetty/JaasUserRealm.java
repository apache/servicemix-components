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
package org.apache.servicemix.http.jetty;

import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.Map;

import javax.security.auth.Subject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.security.auth.AuthenticationService;
import org.apache.servicemix.jbi.security.auth.impl.JAASAuthenticationService;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.security.UserRealm;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

/**
 * A JAAS based implementation of a realm for jetty 6.
 * 
 * @author gnodet
 */
public class JaasUserRealm implements UserRealm {
    
    private static final Log log = LogFactory.getLog(JaasUserRealm.class);
    
    private String name = getClass().getName();
    private String domain = "servicemix-domain";
    private AuthenticationService authenticationService = new JAASAuthenticationService();
    private final Map userMap = new ConcurrentHashMap();

    /**
     * @return the authenticationService
     */
    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }

    /**
     * @param authenticationService the authenticationService to set
     */
    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * @return the domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * @param domain the domain to set
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * @return the name
     */
    public String getName() {
        return this.name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    
    public Principal authenticate(final String username, final Object credentials, Request request) {
        try {
            if ((username != null) && (!username.equals(""))) {

                JaasJettyPrincipal userPrincipal = (JaasJettyPrincipal) userMap.get(username);

                //user has been previously authenticated, but
                //re-authentication has been requested, so remove them
                if (userPrincipal != null) {
                    userMap.remove(username);
                }

                //set up the login context
                Subject subject = new Subject();
                authenticationService.authenticate(subject, domain, username, credentials);
                //login success
                userPrincipal = new JaasJettyPrincipal(username);
                userPrincipal.setSubject(subject);

                userMap.put(username, userPrincipal);

                return userPrincipal;
            } else {
                log.debug("Login Failed - null userID");
                return null;
            }

        } catch (GeneralSecurityException e) {
            log.debug("Login Failed", e);
            return null;
        }
    }

    public void disassociate(Principal user) {
    }

    public Principal getPrincipal(String username) {
        return (Principal) userMap.get(username);
    }

    public boolean isUserInRole(Principal user, String role) {
        // TODO: ?
        return false;
    }

    public void logout(Principal user) {
        JaasJettyPrincipal principal = (JaasJettyPrincipal) user;
        userMap.remove(principal.getName());
    }

    public Principal popRole(Principal user) {
        // TODO: ?
        return null;
    }

    public Principal pushRole(Principal user, String role) {
        // TODO: ?
        return null;
    }

    public boolean reauthenticate(Principal user) {
        // get the user out of the cache
        return (userMap.get(user.getName()) != null);
    }

}
