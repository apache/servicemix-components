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
package org.apache.servicemix.mail.security;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * CustomSSLSocketFactory providing no security at all
 */
public class CustomSSLSocketFactory extends SSLSocketFactory {
    /**
     * the content of this property will be used to initialize the list
     * of trust managers for this SSL Socket Factory
     */
    public static final String PROPERTY_TRUSTMANAGERS = "custom-trust-managers";

    /**
     * this is the separator char for concatenating several managers
     */
    public static final String PROPERTY_SEPARATOR = ";";

    private static final Log LOG = LogFactory.getLog(CustomSSLSocketFactory.class);

    private SSLSocketFactory factory;

    /**
     * default constructor
     */
    public CustomSSLSocketFactory() {
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLS");
            if (System.getProperty(PROPERTY_TRUSTMANAGERS) != null
                && System.getProperty(PROPERTY_TRUSTMANAGERS).trim().length() > 0) {
                // use customized trust managers
                sslcontext.init(null, getTrustManagers(), new java.security.SecureRandom());
            } else {
                // use defaults
                sslcontext.init(null, null, null);
            }
            factory = (SSLSocketFactory)sslcontext.getSocketFactory();
        } catch (Exception ex) {
            LOG.error("Failed to create the dummy ssl socket factory.", ex);
        }
    }

    /**
     * prepares the trustmanagers to use
     * 
     * @return  an array of  trust managers
     */
    private TrustManager[] getTrustManagers() {
        List<TrustManager> managers = new ArrayList<TrustManager>();

        // look for trust managers in the system properties
        String managersString = System.getProperty(PROPERTY_TRUSTMANAGERS);
        if (managersString != null && managersString.trim().length() > 0) {
            StringTokenizer strTok = new StringTokenizer(managersString, PROPERTY_SEPARATOR);
            while (strTok.hasMoreTokens()) {
                String name = strTok.nextToken();
                try {
                    Object tm = Class.forName(name).newInstance();
                    if (tm instanceof TrustManager) {
                        managers.add((TrustManager)tm);
                    } else {
                        LOG.error("Customized trust manager " + name
                                  + " is not implementing TrustManager. Skipping...");
                    }
                } catch (IllegalAccessException iaex) {
                    LOG.error("Customized trust manager " + name + " is not accessable. Skipping...", iaex);
                } catch (InstantiationException iex) {
                    LOG.error("Customized trust manager " + name + " could not be instantiated. Skipping...",
                              iex);
                } catch (ClassNotFoundException cnfex) {
                    LOG.error("Customized trust manager " + name + " was not found. Skipping...", cnfex);
                }
            }
        }

        return managers.toArray(new TrustManager[managers.size()]);
    }

    /**
     * returns a new instance of this factory
     * 
     * @return a new factory instance
     */
    public static SocketFactory getDefault() {
        return new CustomSSLSocketFactory();
    }

    /*
     * (non-Javadoc)
     * @see javax.net.ssl.SSLSocketFactory#createSocket(java.net.Socket, java.lang.String, int, boolean)
     */
    public Socket createSocket(Socket socket, String s, int i, boolean flag) throws IOException {
        return factory.createSocket(socket, s, i, flag);
    }

    /*
     * (non-Javadoc)
     * @see javax.net.SocketFactory#createSocket()
     */
    public Socket createSocket() throws IOException {
        return factory.createSocket();
    }

    /*
     * (non-Javadoc)
     * @see javax.net.SocketFactory#createSocket(java.net.InetAddress, int, java.net.InetAddress, int)
     */
    public Socket createSocket(InetAddress inaddr, int i, InetAddress inaddr1, int j) throws IOException {
        return factory.createSocket(inaddr, i, inaddr1, j);
    }

    /*
     * (non-Javadoc)
     * @see javax.net.SocketFactory#createSocket(java.net.InetAddress, int)
     */
    public Socket createSocket(InetAddress inaddr, int i) throws IOException {
        return factory.createSocket(inaddr, i);
    }

    /*
     * (non-Javadoc)
     * @see javax.net.SocketFactory#createSocket(java.lang.String, int, java.net.InetAddress, int)
     */
    public Socket createSocket(String s, int i, InetAddress inaddr, int j) throws IOException {
        return factory.createSocket(s, i, inaddr, j);
    }

    /*
     * (non-Javadoc)
     * @see javax.net.SocketFactory#createSocket(java.lang.String, int)
     */
    public Socket createSocket(String s, int i) throws IOException {
        return factory.createSocket(s, i);
    }

    /*
     * (non-Javadoc)
     * @see javax.net.ssl.SSLSocketFactory#getDefaultCipherSuites()
     */
    public String[] getDefaultCipherSuites() {
        return factory.getSupportedCipherSuites();
    }

    /*
     * (non-Javadoc)
     * @see javax.net.ssl.SSLSocketFactory#getSupportedCipherSuites()
     */
    public String[] getSupportedCipherSuites() {
        return factory.getSupportedCipherSuites();
    }
}
