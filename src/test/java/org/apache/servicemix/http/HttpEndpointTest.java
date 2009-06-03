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

import javax.jbi.management.DeploymentException;

import junit.framework.TestCase;

import org.apache.servicemix.common.DefaultServiceUnit;

public class HttpEndpointTest extends TestCase {

    private HttpEndpoint httpEndpoint;
    private MyServiceUnit httpSU;

    protected void setUp() throws Exception {
        super.setUp();
        httpEndpoint = new HttpEndpoint();
        httpSU = new MyServiceUnit();
        httpEndpoint.setServiceUnit(httpSU);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        httpEndpoint = null;
    }

    // Test validate() when getRole() returns null.
    public void testValidateRoleNull() throws Exception {
        try {
            httpEndpoint.validate();
            fail("validate() should fail when Role is null");
        } catch (DeploymentException de) {
            String errorMsg = de.getMessage();
            assertTrue("Exception should contain the correct error message string", errorMsg.contains("Endpoint must have a defined role"));
        }
    }

    // Test validate() when location URI is null.
    public void testValidateLocationUriNull() throws Exception {
        httpEndpoint.setRoleAsString("consumer");

        try {
            httpEndpoint.validate();
            fail("validate() should fail when Location URI is null");
        } catch (DeploymentException de) {
            String errorMsg = de.getMessage();
            String msg = "Exception should contain the correct error message string";
            assertTrue(msg, errorMsg.contains("Endpoint must have a defined locationURI"));
        }
    }

    // Test validate() for non-SOAP endpoint when default MEP is not set.
    public void testValidateNonSoapNoMep() throws Exception {
        httpEndpoint.setRoleAsString("consumer");
        httpEndpoint.setSoap(false);
        httpEndpoint.setLocationURI("http://webhost:8080/someService");
        httpEndpoint.setDefaultMep(null);

        try {
            httpEndpoint.validate();
            fail("validate() should fail for non-SOAP endpoint with no default MEP");
        } catch (DeploymentException de) {
            String errorMsg = de.getMessage();
            String msg = "Exception should contain the correct error message string";
            assertTrue(msg, errorMsg.contains("Non soap endpoints must have a defined defaultMep"));
        }
    }

    // Support class needed for HttpEndpoint tests.
    public class MyServiceUnit extends DefaultServiceUnit {
        public MyServiceUnit() {
            super(new HttpComponent());
        }
    }
}
