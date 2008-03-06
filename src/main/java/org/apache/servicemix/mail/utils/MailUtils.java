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

import java.net.URI;
import java.util.Properties;

import javax.mail.internet.ParseException;

import org.apache.servicemix.mail.security.CustomSSLSocketFactory;

/**
 * utility class
 * 
 * @author lhein
 */
public final class MailUtils {
    public static final String SSL_FACTORY = "org.apache.servicemix.mail.security.CustomSSLSocketFactory"; // javax.net.ssl.SSLSocketFactory

    public static final int DEFAULT_PORT_SMTP = 25;
    public static final int DEFAULT_PORT_SMTPS = 465;
    public static final int DEFAULT_PORT_POP3 = 110;
    public static final int DEFAULT_PORT_POP3S = 995;
    public static final int DEFAULT_PORT_NNTP = 119;
    public static final int DEFAULT_PORT_IMAP = 143;
    public static final int DEFAULT_PORT_IMAPS = 993;

    public static final String PROTOCOL_SMTP = "smtp";
    public static final String PROTOCOL_SMTPS = "smtps";
    public static final String PROTOCOL_POP = "pop";
    public static final String PROTOCOL_POP3 = "pop3";
    public static final String PROTOCOL_POP3S = "pop3s";
    public static final String PROTOCOL_NNTP = "nntp";
    public static final String PROTOCOL_IMAP = "imap";
    public static final String PROTOCOL_IMAPS = "imaps";

    public static final String CONNECTION_TIMEOUT = "10000";

    /**
     * default constructor
     */
    private MailUtils() {
    }
    
    /**
     * returns the default port for a given protocol
     * 
     * @param protocol the protocol
     * @return the default port
     */
    public static int getDefaultPortForProtocol(final String protocol) {
        int port = DEFAULT_PORT_SMTP;
        
        if (protocol != null) {
            if (protocol.equalsIgnoreCase(PROTOCOL_IMAP)) {
                port = DEFAULT_PORT_IMAP;
            } else if (protocol.equalsIgnoreCase(PROTOCOL_IMAPS)) {
                port = DEFAULT_PORT_IMAPS;
            } else if (protocol.equalsIgnoreCase(PROTOCOL_NNTP)) {
                port = DEFAULT_PORT_NNTP;
            } else if (protocol.equalsIgnoreCase(PROTOCOL_POP) || protocol.equalsIgnoreCase(PROTOCOL_POP3)) {
                port = DEFAULT_PORT_POP3;
            } else if (protocol.equalsIgnoreCase(PROTOCOL_POP3S)) {
                port = DEFAULT_PORT_POP3S;
            } else if (protocol.equalsIgnoreCase(PROTOCOL_SMTP)) {
                port = DEFAULT_PORT_SMTP;
            } else if (protocol.equalsIgnoreCase(PROTOCOL_SMTPS)) {
                port = DEFAULT_PORT_SMTPS;
            } else {
                port = DEFAULT_PORT_SMTP;
            }
        }
        
        return port;
    }

    /**
     * parse the connection details via given uri
     * 
     * @param uriString the uri used for connection
     * @return a mail connection configuration object
     * @throws ParseException if the uri couldn't be parsed
     */
    public static MailConnectionConfiguration configure(String uriString) throws ParseException {
        MailConnectionConfiguration config = new MailConnectionConfiguration();

        if (uriString == null || uriString.length() <= 0) {
            throw new ParseException("The given connection uri (" + uriString + ") is invalid.");
        }

        URI uri = URI.create(uriString);

        String value = uri.getHost();
        if (value != null) {
            config.setHost(value);
        }

        String scheme = uri.getScheme();
        if (scheme != null) {
            config.setProtocol(scheme);
        } else {
            // set smtp as default
            config.setProtocol("smtp");
        }

        String userInfo = uri.getUserInfo();
        if (userInfo != null) {
            config.setUsername(userInfo);
        }

        int port = uri.getPort();
        if (port >= 0) {
            config.setPort(port);
        } else {
            config.setPort(getDefaultPortForProtocol(config.getProtocol()));
        }

        if (uri.getPath() != null && uri.getPath().length() > 0) {
            if (uri.getPath().startsWith("/")) {
                config.setFolderName(uri.getPath().substring(1));
            } else {
                config.setFolderName(uri.getPath());
            }
        } else {
            config.setFolderName("INBOX");
        }

        if (uri.getQuery().indexOf("password=") != -1) {
            // extract the password from query
            int beginIndex = uri.getQuery().indexOf("password=") + "password=".length();
            int endIndex = uri.getQuery().indexOf(';', beginIndex + 1) != -1 ? uri.getQuery()
                .indexOf(';', beginIndex + 1) : uri.getQuery().length();
            config.setPassword(uri.getQuery().substring(beginIndex, endIndex));
        } else {
            // maybe no password required
            config.setPassword("");
        }

        if (userInfo == null) {
            // alternative way of specifying the user name
            if (uri.getQuery().indexOf("user=") != -1) {
                // extract the password from query
                int beginIndex = uri.getQuery().indexOf("user=") + "user=".length();
                int endIndex = uri.getQuery().indexOf(';', beginIndex + 1) != -1 ? uri.getQuery()
                    .indexOf(';', beginIndex + 1) : uri.getQuery().length();
                config.setUsername(uri.getQuery().substring(beginIndex, endIndex));
            } else {
                // maybe no password required
                config.setUsername("");
            }
        }

        return config;
    }

    /**
     * returns the connection properties for use in endpoints
     *
     * @param config the connection configuration
     * @param customTrustManagers the custom trust manager(s)
     * @return the connection properties
     */
    public static Properties getPropertiesForProtocol(final MailConnectionConfiguration config,
                                                      final String customTrustManagers) {
        // Get system properties clone
        Properties mailConnectionProperties = (Properties)System.getProperties().clone();

        mailConnectionProperties.put("mail." + config.getProtocol() + ".connectiontimeout",
                                     CONNECTION_TIMEOUT);
        mailConnectionProperties.put("mail." + config.getProtocol() + ".timeout", CONNECTION_TIMEOUT);
        mailConnectionProperties.put("mail." + config.getProtocol() + ".host", config.getHost());
        mailConnectionProperties.put("mail." + config.getProtocol() + ".port", "" + config.getPort());
        mailConnectionProperties.put("mail." + config.getProtocol() + ".user", config.getUsername());
        mailConnectionProperties.put("mail." + config.getProtocol() + ".rsetbeforequit", "true");
        mailConnectionProperties.put("mail." + config.getProtocol() + ".auth", "true");
        mailConnectionProperties.put("mail.transport.protocol", config.getProtocol());
        mailConnectionProperties.put("mail.store.protocol", config.getProtocol());
        mailConnectionProperties.put("mail.host", config.getHost());
        mailConnectionProperties.put("mail.user", config.getUsername());

        if (customTrustManagers != null && customTrustManagers.trim().length() > 0
            && config.isSecureProtocol()) {
            // set java mail properties
            mailConnectionProperties
                .put("mail." + config.getProtocol() + ".socketFactory.class", SSL_FACTORY);
            mailConnectionProperties.put("mail." + config.getProtocol() + ".socketFactory.fallback", "false");
            mailConnectionProperties.put("mail." + config.getProtocol() + ".socketFactory.port",
                                         "" + config.getPort());

            // set the properties for the custom ssl socket factory
            System.getProperties().put(CustomSSLSocketFactory.PROPERTY_TRUSTMANAGERS, customTrustManagers);
        }

        return mailConnectionProperties;
    }
}
