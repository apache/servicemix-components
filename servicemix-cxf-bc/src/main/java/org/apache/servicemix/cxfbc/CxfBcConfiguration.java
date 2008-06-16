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
package org.apache.servicemix.cxfbc;

import org.apache.servicemix.jbi.security.auth.AuthenticationService;

public class CxfBcConfiguration {

    private transient AuthenticationService authenticationService;
    
    /**
     * The JNDI name of the AuthenticationService object
     */
    private String authenticationServiceName = "java:comp/env/smx/AuthenticationService";
    
    
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

    
}
