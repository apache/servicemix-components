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
import java.io.InputStream;
import java.util.Properties;

import javax.jms.ConnectionFactory;

import org.apache.servicemix.jbi.security.auth.AuthenticationService;
import org.apache.servicemix.jbi.security.keystore.KeystoreManager;

/**
 * @author gnodet
 * @org.apache.xbean.XBean element="configuration"
 */
public class JmsConfiguration implements JmsConfigurationMBean {

    public static final String CONFIG_FILE = "component.properties"; 
    
    private String rootDir;
    private String componentName = "servicemix-jms";
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
     * @org.apache.xbean.Property hidden="true"
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
     * @return Returns the componentName.
     * @org.apache.xbean.Property hidden="true"
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * @param componentName The componentName to set.
     */
    public void setComponentName(String componentName) {
        this.componentName = componentName;
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
        setProperty(componentName + ".userName", userName);
        setProperty(componentName + ".password", password);
        setProperty(componentName + ".jndiInitialContextFactory", jndiInitialContextFactory);
        setProperty(componentName + ".jndiProviderUrl", jndiProviderUrl);
        setProperty(componentName + ".jndiName", jndiConnectionFactoryName);
        setProperty(componentName + ".processorName", processorName);
        setProperty(componentName + ".keystoreManagerName", keystoreManagerName);
        setProperty(componentName + ".authenticationServiceName", authenticationServiceName);
        if (rootDir != null) {
            File f = new File(rootDir, CONFIG_FILE);
            try {
                this.properties.store(new FileOutputStream(f), null);
            } catch (Exception e) {
                throw new RuntimeException("Could not store component configuration", e);
            }
        }
    }
    
    protected void setProperty(String name, String value) {
        if (value == null) {
            properties.remove(name);
        } else {
            properties.setProperty(name, value);
        }
    }
    
    public boolean load() {
        File f = null;
        InputStream in = null;
        if (rootDir != null) {
            // try to find the property file in the workspace folder
            f = new File(rootDir, CONFIG_FILE);
            if (!f.exists()) {
                f = null;
            }
        }
        if (f == null) {
            // find property file in classpath if it is not available in workspace 
            in = this.getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);
            if (in == null) {
                return false;
            }
        }

        try {
            if (f != null) {
                properties.load(new FileInputStream(f));
            } else {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load component configuration", e);
        }
        if (properties.getProperty(componentName + ".userName") != null) {
            userName = properties.getProperty("userName");
        }
        if (properties.getProperty(componentName + ".password") != null) {
            password = properties.getProperty("password");
        }
        if (properties.getProperty(componentName + ".jndiInitialContextFactory") != null) {
            jndiInitialContextFactory = properties.getProperty("jndiInitialContextFactory");
        }
        if (properties.getProperty(componentName + ".jndiProviderUrl") != null) {
            jndiProviderUrl = properties.getProperty("jndiProviderUrl");
        }
        if (properties.getProperty(componentName + ".jndiName") != null) {
            jndiConnectionFactoryName = properties.getProperty("jndiName");
        }
        if (properties.getProperty(componentName + ".processorName") != null) {
            processorName = properties.getProperty("processorName");
        }
        if (properties.getProperty(componentName + ".keystoreManagerName") != null) {
            keystoreManagerName = properties.getProperty("keystoreManagerName");
        }
        if (properties.getProperty(componentName + ".authenticationServiceName") != null) {
            authenticationServiceName = properties.getProperty("authenticationServiceName");
        }
        return true;
    }
}
