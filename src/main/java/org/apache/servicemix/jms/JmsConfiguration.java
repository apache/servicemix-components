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
package org.apache.servicemix.jms;

import org.apache.servicemix.common.PersistentConfiguration;

public class JmsConfiguration extends PersistentConfiguration implements JmsConfigurationMBean {

    private String userName;
    private String password;
    private String jndiInitialContextFactory;
    private String jndiProviderUrl;
    private String jndiConnectionFactoryName;
    private String processorName = "multiplexing";
    
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
    
    public void save() {
        properties.setProperty("userName", userName);
        properties.setProperty("password", password);
        properties.setProperty("jndiInitialContextFactory", jndiInitialContextFactory);
        properties.setProperty("jndiProviderUrl", jndiProviderUrl);
        properties.setProperty("jndiName", jndiConnectionFactoryName);
        properties.setProperty("processorName", processorName);
        super.save();
    }
    
    public boolean load() {
        if (super.load()) {
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
            return true;
        } else {
            return false;
        }
    }

}
