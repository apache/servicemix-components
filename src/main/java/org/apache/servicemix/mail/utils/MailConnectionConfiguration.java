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
package org.apache.servicemix.mail.utils;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * a data holder class for the connection information
 * 
 * @author lhein
 */
public class MailConnectionConfiguration {
    private String host;
    private String password;
    private String protocol;
    private String username;
    private String folderName = "INBOX";

    private int port = -1;

    /**
     * @return the host
     */
    public String getHost() {
        return this.host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return this.protocol.toLowerCase();
    }

    /**
     * @param protocol the protocol to set
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the folderName
     */
    public String getFolderName() {
        return this.folderName;
    }

    /**
     * @param folderName the folderName to set
     */
    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return this.port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * flags the used protocol to be secure or not
     * 
     * @return  true if secure otherwise false
     */
    public boolean isSecureProtocol() {
        return this.protocol.equalsIgnoreCase("smtps") || this.protocol.equalsIgnoreCase("pop3s")
               || this.protocol.equalsIgnoreCase("imaps");
    }

    /**
     * returns an authenticator object for use in sessions
     * 
     * @return  the authenticator object
     */
    public Authenticator getAuthenticator() {
        return new Authenticator() {
            /*
             * (non-Javadoc)
             * 
             * @see javax.mail.Authenticator#getPasswordAuthentication()
             */
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                PasswordAuthentication pa = new PasswordAuthentication(getUsername(), getPassword());
                return pa;
            }
        };
    }
}
