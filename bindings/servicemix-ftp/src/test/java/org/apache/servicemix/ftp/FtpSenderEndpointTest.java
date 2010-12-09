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
import java.net.URI;
import java.net.URISyntaxException;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.apache.servicemix.components.util.DefaultFileMarshaler;
import org.apache.servicemix.jbi.jaxp.StringSource;

/**
 * Test cases for {@link FtpSenderEndpoint}
 */
public class FtpSenderEndpointTest extends AbstractFtpTestSupport {

	private static final String DEFAULT_CONTENT = "<hello>world</hello>";
	private static final String DEFAULT_FILENAME = "test.xml";

	public void testAutoCreateDir() throws Exception {

		// test auto-create: false
		FtpSenderEndpoint endpoint = createEndpoint();
		endpoint.setAutoCreateDirectory(false);
		endpoint.setUri(new URI("ftp://testuser1:password@localhost:"
				+ getListenerPort() + "/user1"));
		container
				.activateComponent(createComponent(endpoint), "servicemix-ftp");

		File dir = new File(TEST_DIR, "user1");
		assertFalse("dir should not have been created: " + dir, dir.exists());

		container.deactivateComponent("servicemix-ftp");

		// test auto-create: true
		endpoint = createEndpoint();
		endpoint.setAutoCreateDirectory(true);
		endpoint.setUri(new URI("ftp://testuser2:password@localhost:"
				+ getListenerPort() + "/user2"));

		container
				.activateComponent(createComponent(endpoint), "servicemix-ftp");

		dir = new File(TEST_DIR, "user2");
		assertTrue("expected dir to be created: " + dir, dir.exists());

		// test default
		endpoint = new FtpSenderEndpoint();
		assertEquals("unexpected default for 'autoCreateDirectory", true,
				endpoint.isAutoCreateDirectory());
	}
	
	// test simple upload
	public void testSimpleUpload() throws Exception {
		container.activateComponent(createComponent(), "servicemix-ftp");

		MessageExchange exchange = createExchange(DEFAULT_FILENAME,
				DEFAULT_CONTENT);
		client.sendSync(exchange);

		assertEquals(ExchangeStatus.DONE, exchange.getStatus());
		File file = new File(TEST_DIR, DEFAULT_FILENAME);
		assertFileContains(file, DEFAULT_CONTENT);
	}

	// test upload fails to overwrite existing file
	public void testUploadNoOverwrite() throws Exception {
		container.activateComponent(createComponent(), "servicemix-ftp");

		MessageExchange exchange = createExchange(DEFAULT_FILENAME,
				DEFAULT_CONTENT);
		client.sendSync(exchange);

		assertEquals(ExchangeStatus.DONE, exchange.getStatus());
		File file = new File(TEST_DIR, DEFAULT_FILENAME);
		assertFileContains(file, DEFAULT_CONTENT);

		exchange = createExchange(DEFAULT_FILENAME, "<new>overwritten</new>");
		client.sendSync(exchange);

		// should fail and file should remain as before
		assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
		assertFileContains(file, DEFAULT_CONTENT);
	}

	// test upload that overwrites an existing file
	public void testUploadAndOverwrite() throws Exception {
		FtpSenderEndpoint endpoint = createEndpoint();
		endpoint.setOverwrite(true);
		container
				.activateComponent(createComponent(endpoint), "servicemix-ftp");

		MessageExchange exchange = createExchange(DEFAULT_FILENAME,
				DEFAULT_CONTENT);
		client.sendSync(exchange);

		assertEquals(ExchangeStatus.DONE, exchange.getStatus());
		File file = new File(TEST_DIR, DEFAULT_FILENAME);
		assertFileContains(file, DEFAULT_CONTENT);

		exchange = createExchange(DEFAULT_FILENAME, "<new>overwritten</new>");
		client.sendSync(exchange);

		assertEquals(ExchangeStatus.DONE, exchange.getStatus());
		assertFileContains(file, "<new>overwritten</new>");
	}

	// test upload with a specific temporary file name
	public void testUploadWithTempName() throws Exception {
		container.activateComponent(createComponent(), "servicemix-ftp");

		MessageExchange exchange = createExchange(DEFAULT_FILENAME,
				DEFAULT_CONTENT);
		exchange.setProperty(DefaultFileMarshaler.TEMP_FILE_NAME_PROPERTY,
				"upload.tmp");
		client.sendSync(exchange);

		assertEquals(ExchangeStatus.DONE, exchange.getStatus());
		File file = new File(TEST_DIR, DEFAULT_FILENAME);
		assertFileContains(file, DEFAULT_CONTENT);

		assertRequestLogContains(new String[] { "STOR upload.tmp",
				"RNFR upload.tmp", "RNTO test.xml" });
	}

	public void testValidateUriOrHost() throws URISyntaxException,
			DeploymentException {
		FtpSenderEndpoint endpoint = new FtpSenderEndpoint();

		try {
			endpoint.validate();
			fail("validate() should throw exception when neither URI nor clientPool.host is set");
		} catch (DeploymentException e) {
			// this is what we expect
		}

		endpoint.setUri(new URI("ftp://anonymous@just.a.server/test"));
		endpoint.validate(); // should not throw

		FTPClientPool clientPool = new FTPClientPool();
		clientPool.setHost("just.a.server");
		endpoint.setClientPool(clientPool);
		try {
			endpoint.validate();
			fail("validate() should throw exception when both URI and clientPool.host are set");
		} catch (DeploymentException e) {
			// this is what we expect
		}

		endpoint.setUri(null);
		endpoint.validate(); // should not throw
	}

	private FtpSenderEndpoint createEndpoint() throws Exception {
		FtpSenderEndpoint endpoint = new FtpSenderEndpoint();
		endpoint.setService(new QName("ftp"));
		endpoint.setEndpoint("endpoint");
		endpoint.setUri(new URI("ftp://testuser1:password@localhost:"
				+ getListenerPort() + "/"));
		return endpoint;
	}

	private FtpComponent createComponent() throws Exception {
		return createComponent(createEndpoint());
	}

	private FtpComponent createComponent(FtpSenderEndpoint endpoint)
			throws Exception {
		FtpComponent component = new FtpComponent();
		component.setEndpoints(new FtpSenderEndpoint[] { endpoint });
		return component;
	}

	private MessageExchange createExchange(String filename, String content)
			throws MessagingException {
		InOnly me = client.createInOnlyExchange();
		NormalizedMessage inMessage = me.getInMessage();
		inMessage.setContent(new StringSource(content));
		me.setService(new QName("ftp"));
		me.setProperty(DefaultFileMarshaler.FILE_NAME_PROPERTY, filename);
		return me;
	}

}
