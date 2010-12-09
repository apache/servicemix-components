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
package org.apache.servicemix.soap.handlers.security;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;

/**
 * Base implementation for security callback handler.
 * 
 * @author gnodet
 */
public class BaseSecurityCallbackHandler implements CallbackHandler {
    
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        if (callbacks == null || callbacks.length == 0) {
            throw new IllegalStateException("callbacks is null or empty");
        }
        for (int i = 0; i < callbacks.length; i++) {
            if (callbacks[i] instanceof WSPasswordCallback == false) {
                throw new UnsupportedCallbackException(callbacks[i]);
            }
            processCallback((WSPasswordCallback) callbacks[i]);
        }
    } 
    
    protected void processCallback(WSPasswordCallback callback) throws IOException, UnsupportedCallbackException {
        switch (callback.getUsage()) {
        case WSPasswordCallback.DECRYPT:
            processDecrypt(callback);
            break;
        case WSPasswordCallback.USERNAME_TOKEN:
            processUsernameToken(callback);
            break;
        case WSPasswordCallback.SIGNATURE:
            processSignature(callback);
            break;
        case WSPasswordCallback.KEY_NAME:
            processKeyName(callback);
            break;
        case WSPasswordCallback.USERNAME_TOKEN_UNKNOWN:
            processUsernameTokenUnkown(callback);
            break;
        default:
            throw new UnsupportedCallbackException(callback);
        }
    }

    /**
     * Need a password to get the private key of
     * this identifier (username) from    the keystore. WSS4J uses this private
     * key to decrypt the session (symmetric) key. Because the encryption
     * method uses the public key to encrypt the session key it needs no
     * password (a public key is usually not protected by a password)
     */
    protected void processDecrypt(WSPasswordCallback callback) throws IOException, UnsupportedCallbackException {
        throw new UnsupportedCallbackException(callback);
    }
    
    /** 
     * Need the password to fill in or to
     * verify a <code>UsernameToken</code>
     */
    protected void processUsernameToken(WSPasswordCallback callback) throws IOException, UnsupportedCallbackException {
        throw new UnsupportedCallbackException(callback);
    }
    
    /**
     * Need the password to get the private key of
     * this identifier (username) from    the keystore. WSS4J uses this private
     * key to produce a signature. The signature verfication uses the public
     * key to verfiy the signature
     */
    protected void processSignature(WSPasswordCallback callback) throws IOException, UnsupportedCallbackException {
        throw new UnsupportedCallbackException(callback);
    }
    
    /**
     * Need the <i>key</i>, not the password,
     * associated with the identifier. WSS4J uses this key to encrypt or
     * decrypt parts of the SOAP request. Note, the key must match the
     * symmetric encryption/decryption algorithm specified (refer to
     * {@link org.apache.ws.security.handler.WSHandlerConstants#ENC_SYM_ALGO})
     */
    protected void processKeyName(WSPasswordCallback callback) throws IOException, UnsupportedCallbackException {
        throw new UnsupportedCallbackException(callback);
    }
    
    /**
     * Either a not specified 
     * password type or a password type passwordText. In these both cases <b>only</b>
     * the password variable is <b>set</>. The callback class now may check if
     * the username and password match. If they don't match the callback class must
     * throw an exception. The exception can be a UnsupportedCallbackException or
     * an IOException.</li>
     */
    protected void processUsernameTokenUnkown(WSPasswordCallback callback) throws IOException, UnsupportedCallbackException {
        throw new UnsupportedCallbackException(callback);
    }
    
}
