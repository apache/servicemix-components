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
package org.apache.servicemix.jms;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import javax.jms.ConnectionFactory;

import org.apache.servicemix.common.PersistentConfiguration;
import org.apache.servicemix.jbi.security.auth.AuthenticationService;
import org.apache.servicemix.jbi.security.keystore.KeystoreManager;

/**
 * @author gnodet
 *
 */
public class JmsConfiguration implements JmsConfigurationMBean {

    public final static String CONFIG_FILE = "component.properties"; 
    
    private String rootDir;
    private Properties properties = new Properties();
    private String userName;
    private String password;
    private String jndiInitialContextFactory;
    private String jndiProviderUrl;
    private String jndiConnectionFactoryName;
    private String processorName = "multiplexing";
    private transient ConnectionFactory connectionFactory;
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
     * @return Returns the rootDir.
     */
    public String getRootDir() {
        return rootDir;
    }

    /**
     * @param rootDir The rootDir to set.
     */
    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
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
        save();
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
    /**
     * @return Returns the password.
     */
    public String getPassword() {
        return password;
    }
    /**
     * @param password The password to set.
     */
    public void setPassword(String password) {
        this.password = password;
        save();
    }
    /**
     * @return Returns the userName.
     */
    public String getUserName() {
        return userName;
    }
    /**
     * @param userName The userName to set.
     */
    public void setUserName(String userName) {
        this.userName = userName;
        save();
    }
    /**
     * @return Returns the jndiName.
     */
    public String getJndiConnectionFactoryName() {
        return jndiConnectionFactoryName;
    }
    /**
     * @param jndiName The jndiName to set.
     */
    public void setJndiConnectionFactoryName(String jndiName) {
        this.jndiConnectionFactoryName = jndiName;
        save();
    }
    /**
     * @return Returns the jndiInitialContextFactory.
     */
    public String getJndiInitialContextFactory() {
        return jndiInitialContextFactory;
    }
    /**
     * @param jndiInitialContextFactory The jndiInitialContextFactory to set.
     */
    public void setJndiInitialContextFactory(String jndiInitialContextFactory) {
        this.jndiInitialContextFactory = jndiInitialContextFactory;
        save();
    }
    /**
     * @return Returns the jndiProviderUrl.
     */
    public String getJndiProviderUrl() {
        return jndiProviderUrl;
    }
    /**
     * @param jndiProviderUrl The jndiProviderUrl to set.
     */
    public void setJndiProviderUrl(String jndiProviderUrl) {
        this.jndiProviderUrl = jndiProviderUrl;
        save();
    }
    /**
     * @return Returns the processorName.
     */
    public String getProcessorName() {
        return processorName;
    }
    /**
     * @param processorName The processorName to set.
     */
    public void setProcessorName(String processorName) {
        this.processorName = processorName;
        save();
    }
    /**
     * @return Returns the connectionFactory.
     */
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }
    /**
     * Default ConnectionFactory to use in a spring configuration.
     * @param connectionFactory the connectionFactory to set.
     */
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    
    public void save() {
        properties.setProperty("userName", userName);
        properties.setProperty("password", password);
        properties.setProperty("jndiInitialContextFactory", jndiInitialContextFactory);
        properties.setProperty("jndiProviderUrl", jndiProviderUrl);
        properties.setProperty("jndiName", jndiConnectionFactoryName);
        properties.setProperty("processorName", processorName);
        properties.setProperty("keystoreManagerName", keystoreManagerName);
        properties.setProperty("authenticationServiceName", authenticationServiceName);
        if (rootDir != null) {
            File f = new File(rootDir, CONFIG_FILE);
            try {
                this.properties.store(new FileOutputStream(f), null);
            } catch (Exception e) {
                throw new RuntimeException("Could not store component configuration", e);
            }
        }
    }
    
    public boolean load() {
        if (rootDir == null) {
            return false;
        }
        File f = new File(rootDir, CONFIG_FILE);
        if (!f.exists()) {
            return false;
        }
        try {
            properties.load(new FileInputStream(f));
        } catch (IOException e) {
            throw new RuntimeException("Could not load component configuration", e);
        }
        if (properties.getProperty("userName") != null) {
            userName = properties.getProperty("userName");
        }
        if (properties.getProperty("password") != null) {
            password = properties.getProperty("password");
        }
        if (properties.getProperty("jndiInitialContextFactory") != null) {
            jndiInitialContextFactory = properties.getProperty("jndiInitialContextFactory");
        }
        if (properties.getProperty("jndiProviderUrl") != null) {
            jndiProviderUrl = properties.getProperty("jndiProviderUrl");
        }
        if (properties.getProperty("jndiName") != null) {
            jndiConnectionFactoryName = properties.getProperty("jndiName");
        }
        if (properties.getProperty("processorName") != null) {
            processorName = properties.getProperty("processorName");
        }
        if (properties.getProperty("keystoreManagerName") != null) {
            keystoreManagerName = properties.getProperty("keystoreManagerName");
        }
        if (properties.getProperty("authenticationServiceName") != null) {
            authenticationServiceName = properties.getProperty("authenticationServiceName");
        }
        return true;
    }
}
