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

import javax.jbi.component.Component;
import javax.xml.namespace.QName;

import org.apache.servicemix.tck.MessageList;
import org.apache.servicemix.tck.ReceiverComponent;

/**
 * Test cases for {@link FTPClientPool}.
 */
public class FtpClientPoolTest extends AbstractFtpTestSupport {

	private ReceiverComponent receiver;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		ReceiverComponent rec = new ReceiverComponent();
		rec.setService(new QName("receiver"));
		rec.setEndpoint("endpoint");
		container.activateComponent(rec, "receiver");
		receiver = rec;

		File file = new File(TEST_DIR, "test.xml");
		createTestFile(file);
	}

	// test using a client pool to set passive mode
	public void testPassive() throws Exception {
		FTPClientPool pool = createClientPool();
		pool.setPassiveMode(true);
		pool.afterPropertiesSet();
		FtpPollerEndpoint endpoint = createEndpoint(pool);

		container
				.activateComponent(createComponent(endpoint), "servicemix-ftp");
		MessageList messageList = receiver.getMessageList();
		messageList.assertMessagesReceived(1);

		assertRequestLogContains(new String[] { "PASV" });
	}

	// test using a client pool to set binary mode
	public void testBinary() throws Exception {
		FTPClientPool pool = createClientPool();
		pool.setBinaryMode(true);
		pool.afterPropertiesSet();
		FtpPollerEndpoint endpoint = createEndpoint(pool);

		container
				.activateComponent(createComponent(endpoint), "servicemix-ftp");

		assertRequestLogContains(new String[] { "TYPE I" });
	}

	// test using a client pool to disable binary mode
	public void testNotBinary() throws Exception {
		FTPClientPool pool = createClientPool();
		pool.setBinaryMode(false);
		pool.afterPropertiesSet();
		FtpPollerEndpoint endpoint = createEndpoint(pool);

		container
				.activateComponent(createComponent(endpoint), "servicemix-ftp");

		assertFalse("request not expected: TYPE I", getRequestLog().contains(
				"TYPE I"));
	}

	private Component createComponent(FtpPollerEndpoint endpoint) {
		FtpComponent component = new FtpComponent();
		component.setEndpoints(new FtpPollerEndpoint[] { endpoint });
		return component;
	}

	private FtpPollerEndpoint createEndpoint(FTPClientPool clientPool)
			throws Exception {
		FtpPollerEndpoint endpoint = new FtpPollerEndpoint();
		endpoint.setPeriod(1000);
		endpoint.setService(new QName("ftp"));
		endpoint.setEndpoint("endpoint");
		endpoint.setTargetService(receiver.getService());
		endpoint.setClientPool(clientPool);
		return endpoint;
	}

	private FTPClientPool createClientPool() {
		FTPClientPool pool = new FTPClientPool();
        pool.setHost("localhost");
        pool.setPort(getListenerPort());
		pool.setUsername("testuser2");
		pool.setPassword("password");
		return pool;
	}

}
