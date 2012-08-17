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

import org.apache.servicemix.common.security.AuthenticationService;
import org.eclipse.jetty.security.DefaultIdentityService;
import org.eclipse.jetty.security.IdentityService;
import org.eclipse.jetty.security.MappedLoginService;
import org.eclipse.jetty.server.UserIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Principal;

/**
 * A JAAS based implementation of a identity service for jetty 8.
 *
 * @author gnodet
 */
public class JaasUserRealm extends MappedLoginService {

    private final Logger logger = LoggerFactory.getLogger(JaasUserRealm.class);

    private String domain = "servicemix-domain";
    private AuthenticationService authenticationService;


    public JaasUserRealm() {
        this._identityService = new JaasJettyIdentityService();
    }

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

    @Override
    public UserIdentity login(String username, Object credentials) {
        if ((username != null) && (!username.equals(""))) {
            // user has been previously authenticated, but
            // re-authentication has been requested, so remove them if exists
            _users.remove(username);

            // set up the login context
            Subject subject = new Subject();
            try {
                if (authenticationService != null){
                    authenticationService.authenticate(subject, domain, username, credentials);
                }
            } catch (GeneralSecurityException e) {
                logger.debug("Login Failed", e);
                return null;
            }

            // login success
            return new JaasJettyPrincipal(subject, username, IdentityService.NO_ROLES);
        } else {
            logger.debug("Login Failed - null userID");
            return null;
        }
    }

    @Override
    protected UserIdentity loadUser(String username) {
        return _users.get(username);
    }

    @Override
    protected void loadUsers() throws IOException {
    }


    public static class JaasJettyIdentityService extends DefaultIdentityService {
        @Override
        public UserIdentity newUserIdentity(Subject subject, Principal userPrincipal, String[] roles) {
            return new JaasJettyPrincipal(subject, userPrincipal.getName(), roles);
        }
    }


}
