/** 
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.servicemix.jms;

public interface JmsConfigurationMBean {

    /**
     * @return Returns the password.
     */
    public String getPassword();
    /**
     * @param password The password to set.
     */
    public void setPassword(String password);
    /**
     * @return Returns the userName.
     */
    public String getUserName();
    /**
     * @param userName The userName to set.
     */
    public void setUserName(String userName);
    /**
     * @return Returns the jndiName.
     */
    public String getJndiName();
    /**
     * @param jndiName The jndiName to set.
     */
    public void setJndiName(String jndiName);
}
