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
package org.apache.servicemix.http;

import junit.framework.TestCase;

import org.apache.servicemix.expression.PropertyExpression;

public class ProxyParametersTest extends TestCase {

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String PROXYHOST = "hostname";
    private static final int PROXYPORT = 80;
    private PropertyExpression usernameProp;
    private PropertyExpression passwordProp;
    private ProxyParameters proxyParams;

    protected void setUp() throws Exception {
        super.setUp();
        usernameProp = new PropertyExpression(USERNAME);
        passwordProp = new PropertyExpression(PASSWORD);
    }
 
    protected void tearDown() throws Exception {
        super.tearDown();
        usernameProp = null;
        passwordProp = null;
        proxyParams = null;
    }

    public void testProxyParams() throws Exception {
        BasicAuthCredentials bac = new BasicAuthCredentials();
        bac.setUsername(usernameProp);
        bac.setPassword(passwordProp);

        // Create the Proxy Parameters
        proxyParams = new ProxyParameters();
        proxyParams.setProxyCredentials(bac);
        proxyParams.setProxyHost(PROXYHOST);
        proxyParams.setProxyPort(PROXYPORT);

        assertTrue("Proxy Parameters should have non-null authentication credentials", proxyParams.getProxyCredentials() != null);
        assertTrue("Proxy Host should be: " + PROXYHOST, proxyParams.getProxyHost().equals(PROXYHOST));
        assertTrue("Proxy Port should be: " + PROXYPORT, proxyParams.getProxyPort() == PROXYPORT);
    }

}
