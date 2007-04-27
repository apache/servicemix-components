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
package org.apache.servicemix.http;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;

/**
 * @author roehl.sioson
 * @org.apache.xbean.XBean element="basicAuthCredentials"
 * description="This class contains parameters needed to send basic authentication credentials"
 */
public class BasicAuthCredentials {

    protected String username;
    protected String password;

    public BasicAuthCredentials() {
    }

    /**
     * @return Returns the username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param ssl The username to set.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return Returns the password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param ssl The password to set.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Applies this authentication to the given method.
     *
     * @param method The method to receive authentication headers.
     */
    public void applyCredentials(HttpClient client) {
        AuthScope scope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
        Credentials credentials = new UsernamePasswordCredentials(this.username, this.password);
        client.getState().setCredentials(scope, credentials);
    }

    /**
     * Applies this authentication to the given method.
     *
     * @param method The method to receive authentication headers.
     */
    public void applyProxyCredentials(HttpClient client) {
        AuthScope scope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
        Credentials credentials = new UsernamePasswordCredentials(this.username, this.password);
        client.getState().setProxyCredentials(scope, credentials);
    }

}
