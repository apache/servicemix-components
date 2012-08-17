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

import junit.framework.TestCase;
import org.apache.servicemix.common.security.AuthenticationService;
import org.eclipse.jetty.server.UserIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.Subject;
import java.security.GeneralSecurityException;

public class JaasUserRealmTest extends TestCase {

    private final static Logger logger = LoggerFactory.getLogger(JaasUserRealmTest.class);

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private JaasUserRealm jaasUserRealm;
    private AuthenticationService authService;

    protected void setUp() throws Exception {
        super.setUp();
        jaasUserRealm = new JaasUserRealm();
        authService = new TestAuthenticationService();
        jaasUserRealm.setAuthenticationService(authService);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        jaasUserRealm = null;
        authService = null;
    }

    // Test authenticate() when username is null
    public void testAuthenticateUsernameIsNull() throws Exception {
        assertNull("authenticate() should return null for null username", jaasUserRealm.login(null, null));
    }

    // Test authenticate() when authenticationService's authenticate throws 
    // a GeneralSecurityException
    public void testAuthenticateFails() throws Exception {
        String msg = "authenticate() should return null when authService authenticate fails";
        assertNull(msg, jaasUserRealm.login(USERNAME, null));
    }

    // Test authenticate() when authenticationService's authenticate is successful.
    public void testAuthenticateSuccess() throws Exception {
        UserIdentity userPrincipal = jaasUserRealm.login(USERNAME, PASSWORD);
        assertNotNull("authenticate() should return non-null Principal", userPrincipal);
    }

    // Test logout() when user logged in
    public void testLogoutUserLoggedIn() throws Exception {
        // authenticate the user
        UserIdentity userPrincipal = jaasUserRealm.login(USERNAME, PASSWORD);

        // now log them out
        jaasUserRealm.logout(userPrincipal);

        // verify the user is not in the map
        assertNull("logout() should have removed principal: " + USERNAME, jaasUserRealm.loadUser(USERNAME));
    }

    // Test logout() when user not logged in
    public void testLogoutUserNotLoggedIn() throws Exception {
        UserIdentity userPrincipal = new JaasJettyPrincipal(new Subject(), USERNAME, null);
        // user principal not added to the map via authenticate
        jaasUserRealm.logout(userPrincipal);
        // verify the user is not in the map
        assertNull("logout() should have removed principal: " + USERNAME, jaasUserRealm.loadUser(USERNAME));
    }

    // Mock implementation of an AuthenticationService to help with testing.
    public static class TestAuthenticationService implements AuthenticationService {
        public void authenticate(Subject subject, String domain, String user, Object credentials) 
            throws GeneralSecurityException {
            if (credentials == null) {
                throw new GeneralSecurityException("authenticate() failed due to null credentials");
            }
            logger.info("authenticate() called");
        }
    }
}  
