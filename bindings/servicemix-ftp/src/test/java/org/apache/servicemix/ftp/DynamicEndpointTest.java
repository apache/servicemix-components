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
package org.apache.servicemix.ftp;

import java.io.File;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;

import org.apache.servicemix.components.util.DefaultFileMarshaler;
import org.apache.servicemix.jbi.jaxp.StringSource;

/**
 * Test cases for dynamically creating {@link FtpSenderEndpoint}s.
 */
public class DynamicEndpointTest extends AbstractFtpTestSupport {

	private static final String FILE_CONTENT = "<hello>world</hello>";
	private static final String FILE_NAME = "test.xml";

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		container.activateComponent(new FtpComponent(), "servicemix-ftp");
	}

	// test simple send to a dynamic endpoint
	public void testSendingToDynamicEndpoint() throws Exception {
		String uri = "ftp://testuser1:password@localhost:" + getListenerPort()
				+ "/dynamic";

		InOnly exchange = createExchange(uri);
		client.sendSync(exchange);

		assertEquals("exchange status", ExchangeStatus.DONE, exchange
				.getStatus());
		assertFileContains(new File(TEST_DIR, "dynamic/" + FILE_NAME),
				FILE_CONTENT);
	}

	// test send to a dynamic endpoint with additional URI parameters
	public void testSendingToDynamicEndpointWithParameters() throws Exception {

		// create file to be replaced
		File file = new File(TEST_DIR, "dynamic/" + FILE_NAME);
		createTestFile(file);

		String uri = "ftp://testuser1:password@localhost:" + getListenerPort()
				+ "/dynamic?overwrite=true";

		InOnly exchange = createExchange(uri);
		String newContent = "<new>overwritten</new>";
		exchange.getInMessage().setContent(new StringSource(newContent));

		client.sendSync(exchange);

		assertEquals("exchange status", ExchangeStatus.DONE, exchange
				.getStatus());
		assertFileContains(new File(TEST_DIR, "dynamic/" + FILE_NAME),
				newContent);
	}

	// test send fails with invalid login
	public void testLoginFailure() throws Exception {
		String uri = "ftp://testuser1:wrong_password@localhost:"
				+ getListenerPort() + "/dynamic";

		InOnly exchange = createExchange(uri);
		exchange.getInMessage().setContent(new StringSource(FILE_CONTENT));
		client.sendSync(exchange);

		assertEquals("exchange status", ExchangeStatus.ERROR, exchange
				.getStatus());
	}

	private InOnly createExchange(String uri) throws MessagingException {
		ServiceEndpoint endpoint = client.resolveEndpointReference(uri);
		assertNotNull("expected endpoint for uri: " + uri, endpoint);

		InOnly exchange = client.createInOnlyExchange();
		exchange.setEndpoint(endpoint);
		exchange.getInMessage().setProperty(
				DefaultFileMarshaler.FILE_NAME_PROPERTY, FILE_NAME);
		exchange.getInMessage().setContent(new StringSource(FILE_CONTENT));

		return exchange;
	}

}
