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

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.servicemix.expression.Expression;

/**
 * @author roehl.sioson
 * @org.apache.xbean.XBean element="basicAuthCredentials" description=
 *                         "This class contains parameters needed to send basic authentication credentials"
 */
public class BasicAuthCredentials {

    protected Expression username;
    protected Expression password;
    protected Expression host;
    protected Expression domain;

    public BasicAuthCredentials() {
    }

    /**
     * Returns the username for authentication.
     * 
     * @return an <code>Expression</code> containing the username
     */
    public Expression getUsername() {
        return username;
    }

    /**
     * Sets the username to use for authentocation.
     * 
     * @param username am <code>Expression</code> containing the username.
     * @org.apache.xbean.Property description="the username to use for authentication"
     */
    public void setUsername(Expression username) {
        this.username = username;
    }

    /**
     * Returns the password for authentication.
     * 
     * @return an <code>Expression</code> containing the password
     */
    public Expression getPassword() {
        return password;
    }

    /**
     * Sets the password to use for authentication.
     * 
     * @param password an <code>Expression</code> containing the password.
     * @org.apache.xbean.Property description="the password to use for authentication"
     */
    public void setPassword(Expression password) {
        this.password = password;
    }
    
    /**
     * Returns the domain for NTLM authentication.
     * 
     * @return an <code>Expression</code> containing the NTLM domain.
     */
    public Expression getDomain() {
        return domain;
    }
    
    /**
     * Sets the domain to use for NTLM authentication.
     * 
     * @param domain an <code>Expression</code> containing the NTLM domain.
     * @org.apache.xbean.Property description="the domain to use for NTLM authentication"
     */
    public void setDomain(Expression domain) {
        this.domain = domain;
    }
    
    /**
     * Returns the host for NTLM authentication.
     * 
     * @return an <code>Expression</code> containing the NTLM host.
     */
    public Expression getHost() {
        return host;
    }
    
    /**
     * Sets the host to use for NTLM authentication.
     * 
     * @param host an <code>Expression</code> containing the NTLM host.
     * @org.apache.xbean.Property description="the host to use for NTLM authentication"
     */
    public void setHost(Expression host) {
        this.host = host;
    }

    /**
     * Applies this authentication to the given method.
     * 
     * @param client the client on which to set the authentication information
     * @param exchange the message exchange to be used for evaluating the expression
     * @param message the normalized message to be used for evaluating the expression
     * @throws MessagingException if the correct value for username/password cannot be determined when using an expression
     */
    public void applyCredentials(HttpClient client, MessageExchange exchange, NormalizedMessage message)
        throws MessagingException {
        AuthScope scope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
        Credentials credentials;
        if (domain != null && host != null) {
            credentials = new NTCredentials((String)this.username.evaluate(exchange, message), 
                    (String)this.password.evaluate(exchange, message), 
                    (String)this.host.evaluate(exchange, message), 
                    (String)this.domain.evaluate(exchange, message));
        } else {
            credentials = new UsernamePasswordCredentials((String)this.username.evaluate(exchange,
                                                                                                 message),
                                                                  (String)this.password.evaluate(exchange,
                                                                                                 message));
        }
        client.getState().setCredentials(scope, credentials);
        
    }

    /**
     * Applies this authentication to the given method.
     * 
     * @param client the client on which to set the authentication information
     * @param exchange the message exchange to be used for evaluating the expression
     * @param message the normalized message to be used for evaluating the expression
     * @throws MessagingException if the correct value for user name/password cannot be determined when using an expression
     */
    public void applyProxyCredentials(HttpClient client, MessageExchange exchange, NormalizedMessage message)
        throws MessagingException {
        AuthScope scope = new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT);
        Credentials credentials;
        if (domain != null && host != null) {
            credentials = new NTCredentials((String)this.username.evaluate(exchange, message), 
                    (String)this.password.evaluate(exchange, message), 
                    (String)this.host.evaluate(exchange, message), 
                    (String)this.domain.evaluate(exchange, message));
        } else {
            credentials = new UsernamePasswordCredentials((String)this.username.evaluate(exchange,message),
                                                                  (String)this.password.evaluate(exchange,message));
        }
        client.getState().setProxyCredentials(scope, credentials);
    }

}
