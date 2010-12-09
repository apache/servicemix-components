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

public interface JmsConfigurationMBean {

    /**
     * @return Returns the password.
     */
    String getPassword();
    
    /**
     * @param password The password to set.
     */
    void setPassword(String password);
    
    /**
     * @return Returns the userName.
     */
    String getUserName();
    
    /**
     * @param userName The userName to set.
     */
    void setUserName(String userName);
    
    /**
     * @return Returns the jndiConnectionFactoryName.
     */
    String getJndiConnectionFactoryName();
    
    /**
     * @param jndiConnectionFactoryName The jndiName to set.
     */
    void setJndiConnectionFactoryName(String jndiConnectionFactoryName);
    
    /**
     * @return Returns the jndiInitialContextFactory.
     */
    String getJndiInitialContextFactory();
    
    /**
     * @param jndiInitialContextFactory The jndiInitialContextFactory to set.
     */
    void setJndiInitialContextFactory(String jndiInitialContextFactory);
    
    /**
     * @return Returns the jndiProviderUrl.
     */
    String getJndiProviderUrl();
    
    /**
     * @param jndiProviderUrl The jndiProviderUrl to set.
     */
    void setJndiProviderUrl(String jndiProviderUrl);
    
    /**
     * @return Returns the processName.
     */
    String getProcessorName();
    
    /**
     * @param processorName The processorName to set.
     */
    void setProcessorName(String processorName);

    String getKeystoreManagerName();
    
    void setKeystoreManagerName(String name);
    
    String getAuthenticationServiceName();
    
    void setAuthenticationServiceName(String name);
    
}
