/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.servicemix.common.PersistentConfiguration;
import org.apache.servicemix.jbi.security.auth.AuthenticationService;
import org.apache.servicemix.jbi.security.keystore.KeystoreManager;
import org.mortbay.jetty.nio.SelectChannelConnector;

/**
 * 
 * @author gnodet
 * @org.apache.xbean.XBean element="configuration"
 */
public class HttpConfiguration extends PersistentConfiguration implements HttpConfigurationMBean {

    public static final String DEFAULT_JETTY_CONNECTOR_CLASS_NAME = SelectChannelConnector.class.getName();
    
    private boolean streamingEnabled = false;
    private String jettyConnectorClassName = DEFAULT_JETTY_CONNECTOR_CLASS_NAME;

    private transient KeystoreManager keystoreManager;
    private transient AuthenticationService authenticationService;
    
    /**
     * The JNDI name of the AuthenticationService object
     */
    private String authenticationServiceName = "java:comp/env/smx/AuthenticationService";
    
    /**
     * The JNDI name of the KeystoreManager object
     */
    private String keystoreManagerName = "java:comp/env/smx/KeystoreManager";

    /**
     * The maximum number of threads for the Jetty thread pool. It's set 
     * to 255 by default to match the default value in Jetty. 
     */
    private int jettyThreadPoolSize = 255;
    
    /**
     * Maximum number of concurrent requests to the same host.
     */
    private int maxConnectionsPerHost = 32;
    
    /**
     * Maximum number of concurrent requests.
     */
    private int maxTotalConnections = 256;
    
    /**
     * If true, use register jetty mbeans
     */
    private boolean jettyManagement;

    /**
     * @return the jettyManagement
     */
    public boolean isJettyManagement() {
        return jettyManagement;
    }

    /**
     * @param jettyManagement the jettyManagement to set
     */
    public void setJettyManagement(boolean jettyManagement) {
        this.jettyManagement = jettyManagement;
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
     * @return the authenticationServiceName
     */
    public String getAuthenticationServiceName() {
        return authenticationServiceName;
    }

    /**
     * @param authenticationServiceName the authenticationServiceName to set
     */
    public void setAuthenticationServiceName(String authenticationServiceName) {
        this.authenticationServiceName = authenticationServiceName;
        save();
    }

    /**
     * @return the keystoreManager
     */
    public KeystoreManager getKeystoreManager() {
        return keystoreManager;
    }

    /**
     * @param keystoreManager the keystoreManager to set
     */
    public void setKeystoreManager(KeystoreManager keystoreManager) {
        this.keystoreManager = keystoreManager;
    }

    /**
     * @return the keystoreManagerName
     */
    public String getKeystoreManagerName() {
        return keystoreManagerName;
    }

    /**
     * @param keystoreManagerName the keystoreManagerName to set
     */
    public void setKeystoreManagerName(String keystoreManagerName) {
        this.keystoreManagerName = keystoreManagerName;
        save();
    }

    public boolean isStreamingEnabled() {
        return streamingEnabled;
    }

    public void setStreamingEnabled(boolean streamingEnabled) {
        this.streamingEnabled = streamingEnabled;
        save();
    }

    public String getJettyConnectorClassName() {
        return jettyConnectorClassName;
    }

    public void setJettyConnectorClassName(String jettyConnectorClassName) {
        this.jettyConnectorClassName = jettyConnectorClassName;
        save();
    }

    public int getJettyThreadPoolSize() {
        return jettyThreadPoolSize;
    }

    public void setJettyThreadPoolSize(int jettyThreadPoolSize) {
        this.jettyThreadPoolSize = jettyThreadPoolSize;
        save();
    }
    
    public int getMaxConnectionsPerHost() {
        return maxConnectionsPerHost;
    }

    public void setMaxConnectionsPerHost(int maxConnectionsPerHost) {
        this.maxConnectionsPerHost = maxConnectionsPerHost;
        save();
    }

    public int getMaxTotalConnections() {
        return maxTotalConnections;
    }

    public void setMaxTotalConnections(int maxTotalConnections) {
        this.maxTotalConnections = maxTotalConnections;
        save();
    }
    
    public void save() {
        properties.setProperty("jettyThreadPoolSize", Integer.toString(jettyThreadPoolSize));
        properties.setProperty("jettyConnectorClassName", jettyConnectorClassName);
        properties.setProperty("streamingEnabled", Boolean.toString(streamingEnabled));
        properties.setProperty("maxConnectionsPerHost", Integer.toString(maxConnectionsPerHost));
        properties.setProperty("maxTotalConnections", Integer.toString(maxTotalConnections));
        properties.setProperty("keystoreManagerName", keystoreManagerName);
        properties.setProperty("authenticationServiceName", authenticationServiceName);
        properties.setProperty("jettyManagement", Boolean.toString(jettyManagement));
        super.save();
    }
    
    public boolean load() {
        if (super.load()) {
            if (properties.getProperty("jettyThreadPoolSize") != null) {
                jettyThreadPoolSize = Integer.parseInt(properties.getProperty("jettyThreadPoolSize"));
            }
            if (properties.getProperty("jettyConnectorClassName") != null) {
                jettyConnectorClassName = properties.getProperty("jettyConnectorClassName");
            }
            if (properties.getProperty("streamingEnabled") != null) {
                streamingEnabled = Boolean.valueOf(properties.getProperty("streamingEnabled")).booleanValue();
            }
            if (properties.getProperty("maxConnectionsPerHost") != null) {
                maxConnectionsPerHost = Integer.parseInt(properties.getProperty("maxConnectionsPerHost"));
            }
            if (properties.getProperty("maxTotalConnections") != null) {
                maxTotalConnections = Integer.parseInt(properties.getProperty("maxTotalConnections"));
            }
            if (properties.getProperty("keystoreManagerName") != null) {
                keystoreManagerName = properties.getProperty("keystoreManagerName");
            }
            if (properties.getProperty("authenticationServiceName") != null) {
                authenticationServiceName = properties.getProperty("authenticationServiceName");
            }
            if (properties.getProperty("jettyManagement") != null) {
                jettyManagement = Boolean.valueOf(properties.getProperty("jettyManagement")).booleanValue();
            }
            return true;
        } else {
            return false;
        }
    }

}
