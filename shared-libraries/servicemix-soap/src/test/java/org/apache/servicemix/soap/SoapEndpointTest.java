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
package org.apache.servicemix.soap;

import javax.jbi.messaging.MessageExchange.Role;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.DefaultServiceUnit;
import org.springframework.core.io.ClassPathResource;

import junit.framework.TestCase;

public class SoapEndpointTest extends TestCase {

	private SoapEndpointTestSupport mySoapEndpoint;
	
	protected void setUp() throws Exception {
		super.setUp();
		mySoapEndpoint = new SoapEndpointTestSupport();
		mySoapEndpoint.setSoap(true);
	}
	
	protected void tearDown() throws Exception {
		mySoapEndpoint = null;
		super.tearDown();
	}
	
	// test setRoleAsString for "consumer"
	public void testSetRoleAsStringConsumer() throws Exception {
		mySoapEndpoint.setRoleAsString("consumer");
		assertTrue("setRoleAsString() should succeed with role of consumer", 
				mySoapEndpoint.getRole() == Role.CONSUMER);
	}
	
	// test setRoleAsString for "provider"
	public void testSetRoleAsStringProvider() throws Exception {
		mySoapEndpoint.setRoleAsString("provider");
		assertTrue("setRoleAsString() should succeed with role of provider", 
				mySoapEndpoint.getRole() == Role.PROVIDER);
	}
	
	// test setRoleAsString with invalid role
	public void testSetRoleAsStringInvalid() throws Exception {
		try {
			mySoapEndpoint.setRoleAsString("invalid");
			fail("setRoleAsString() should fail for invalid role");
		} catch (IllegalArgumentException iae) {
			// test succeeds
		}
	}
	
	// test setRoleAsString with null role
	public void testSetRoleAsStringNull() throws Exception {
		try {
			mySoapEndpoint.setRoleAsString(null);
			fail("setRoleAsString() should fail for null role");
		} catch (IllegalArgumentException iae) {
			// test succeeds
		}
	}
	
	// test loadWsdl when wsdlResource is set and role is provider.
	public void testLoadWsdlAsProvider() throws Exception {
		MyServiceUnit httpSu = new MyServiceUnit();
		ClassPathResource cpResource = new ClassPathResource("org/apache/servicemix/soap/HelloWorld-DOC.wsdl");
		mySoapEndpoint.setServiceUnit(httpSu);
		mySoapEndpoint.setRole(Role.PROVIDER);
		mySoapEndpoint.setWsdlResource(cpResource);
		mySoapEndpoint.setLocationURI("http://localhost:8080/hello");
		mySoapEndpoint.setEndpoint("HelloPortSoap11");
		mySoapEndpoint.setService(new QName("uri:HelloWorld", "HelloService"));
		
		mySoapEndpoint.loadWsdl();
		
		assertFalse("getWsdls() should not return an empty list", mySoapEndpoint.getWsdls().isEmpty());
	}

	// Support class needed for SoapEndpoint tests.
    public class MyServiceUnit extends DefaultServiceUnit {
        public MyServiceUnit() {
            super(new DefaultComponent());
        }
    }
}
