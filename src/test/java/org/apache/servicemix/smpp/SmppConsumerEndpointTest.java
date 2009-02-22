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
package org.apache.servicemix.smpp;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;

import org.apache.servicemix.smpp.marshaler.DefaultSmppMarshaler;
import org.apache.servicemix.smpp.marshaler.SmppMarshalerSupport;
import org.apache.servicemix.tck.mock.MockMessageExchange;

import junit.framework.TestCase;

/**
 * JUnit test class for <code>org.apache.servicemix.smpp.SmppConsumerEndpoint</code>
 * 
 * @author mullerc
 */
public class SmppConsumerEndpointTest extends TestCase {
	
	private SmppConsumerEndpoint endpoint;

	protected void setUp() throws Exception {
		super.setUp();
		this.endpoint = new SmppConsumerEndpoint();
		this.endpoint.setTargetService(new QName("http://test", "service"));
	}

	public void testValidateWithMinAttr() throws DeploymentException {
		this.endpoint.setHost("localhost");
		this.endpoint.setSystemId("test");

		this.endpoint.validate();
		
		assertEquals("localhost", this.endpoint.getHost());
		assertEquals(2775, this.endpoint.getPort());
		assertEquals("test", this.endpoint.getSystemId());
		assertNull(this.endpoint.getPassword());
		assertEquals(50000, this.endpoint.getEnquireLinkTimer());
		assertEquals(100000, this.endpoint.getTransactionTimer());
		assertNotNull(this.endpoint.getMarshaler());
	}
	
	public void testValidateWithMaxAttr() throws DeploymentException {
		SmppMarshalerSupport marshaler = new DefaultSmppMarshaler();
		this.endpoint.setHost("localhost");
		this.endpoint.setPort(2700);
		this.endpoint.setSystemId("test");
		this.endpoint.setPassword("password");
		this.endpoint.setEnquireLinkTimer(10000);
		this.endpoint.setTransactionTimer(20000);
		this.endpoint.setMarshaler(marshaler);

		this.endpoint.validate();
		
		assertEquals("localhost", this.endpoint.getHost());
		assertEquals(2700, this.endpoint.getPort());
		assertEquals("test", this.endpoint.getSystemId());
		assertEquals("password", this.endpoint.getPassword());
		assertEquals(10000, this.endpoint.getEnquireLinkTimer());
		assertEquals(20000, this.endpoint.getTransactionTimer());
		assertSame(marshaler, this.endpoint.getMarshaler());
	}
	
	public void testValidateWithoutHost() throws DeploymentException {
		try {
			this.endpoint.validate();
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
	
	public void testValidateWithoutSystemId() throws DeploymentException {
		this.endpoint.setHost("localhost");

		try {
			this.endpoint.validate();
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
	
	public void testValidateInvalidEnquireLinkTimer() throws DeploymentException {
		this.endpoint.setHost("localhost");
		this.endpoint.setSystemId("test");
		this.endpoint.setEnquireLinkTimer(0);

		try {
			this.endpoint.validate();
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
	
	public void testValidateInvalidTransactionTimer() throws DeploymentException {
		this.endpoint.setHost("localhost");
		this.endpoint.setSystemId("test");
		this.endpoint.setTransactionTimer(0);

		try {
			this.endpoint.validate();
			fail("IllegalArgumentException expected");
		} catch (IllegalArgumentException e) {
			// expected
		}
	}
	
	public void testProcessExchangeStatusDONE() throws Exception {
		MessageExchange exchange = new MockMessageExchange();
		exchange.setStatus(ExchangeStatus.DONE);
		
		this.endpoint.process(exchange);
	}
	
	public void testProcessExchangeStatusERROR() throws Exception {
		MessageExchange exchange = new MockMessageExchange();
		exchange.setStatus(ExchangeStatus.ERROR);
		
		this.endpoint.process(exchange);
	}
	
	public void testProcessExchangeStatusACTIVE() throws Exception {
		MessageExchange exchange = new MockMessageExchange();
		exchange.setStatus(ExchangeStatus.ACTIVE);
		
		try {
			this.endpoint.process(exchange);
			fail("MessagingException expected");
		} catch (MessagingException e) {
			// expected
		}
	}
}