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
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.apache.servicemix.components.util.DefaultFileMarshaler;
import org.apache.servicemix.ftp.FtpPollerEndpoint.FtpData;
import org.apache.servicemix.tck.MessageList;
import org.apache.servicemix.tck.ReceiverComponent;

/**
 * Tests for {@link FtpPollerEndpoint}
 */
public class FtpPollerEndpointTest extends AbstractFtpTestSupport {

	private ReceiverComponent receiver;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		ReceiverComponent rec = new MyReceiverComponent();
		rec.setService(new QName("receiver"));
		rec.setEndpoint("endpoint");
		container.activateComponent(rec, "receiver");
		receiver = rec;
	}

	// test basic polling
	public void testSimplePoll() throws Exception {
		Map<String, File> files = createTestFiles(3);
		container.activateComponent(createComponent(), "servicemix-ftp");
		MessageList messageList = receiver.getMessageList();
		messageList.assertMessagesReceived(files.size());
		assertFilesProcessed(messageList, files, new FileAssertions() {
			public void assertProcessed(File file, NormalizedMessage message) {
				assertFalse("expected <" + file + "> to be deleted", file
						.exists());
			}
		});
	}

	// test recursive polling
	public void testRecursive() throws Exception {
		Map<String, File> files = createTestFiles(1);
		files.putAll(createTestFiles(1, "dir1"));
		files.putAll(createTestFiles(1, "dir1/dir2"));

		container.activateComponent(createComponent(), "servicemix-ftp");
		MessageList messageList = receiver.getMessageList();
		messageList.assertMessagesReceived(files.size());
		assertFilesProcessed(messageList, files, new FileAssertions() {
			public void assertProcessed(File file, NormalizedMessage message) {
				assertFalse("expected <" + file + "> to be deleted", file
						.exists());
			}
		});
	}

	// test non-recursive polling
	public void testNotRecursive() throws Exception {
		Map<String, File> files = createTestFiles(3);
		createTestFiles(2, "dir1/dir2");

		FtpPollerEndpoint endpoint = createEndpoint();
		endpoint.setRecursive(false);

		container
				.activateComponent(createComponent(endpoint), "servicemix-ftp");
		MessageList messageList = receiver.getMessageList();
		messageList.assertMessagesReceived(files.size());
		assertFilesProcessed(messageList, files, new FileAssertions() {
			public void assertProcessed(File file, NormalizedMessage message) {
				assertFalse("expected <" + file + "> to be deleted", file
						.exists());
			}
		});
	}

	// test polling without deleting
	public void testNoDelete() throws Exception {
		Map<String, File> files = createTestFiles(1);

		FtpPollerEndpoint endpoint = createEndpoint();
		endpoint.setDeleteFile(false);

		container
				.activateComponent(createComponent(endpoint), "servicemix-ftp");
		MessageList messageList = receiver.getMessageList();
		messageList.assertMessagesReceived(files.size());
		assertFilesProcessed(messageList, files, new FileAssertions() {
			public void assertProcessed(File file, NormalizedMessage message) {
				assertTrue("expected <" + file + "> to exist", file.exists());
			}
		});
	}

	// test polling with archiving
	public void testArchive() throws Exception {
		Map<String, File> files = createTestFiles(3);

		FtpPollerEndpoint endpoint = createEndpoint();
		endpoint.setArchive(new URI("archived"));
		endpoint.setDeleteFile(true);
		endpoint.setRecursive(false);

		container
				.activateComponent(createComponent(endpoint), "servicemix-ftp");
		MessageList messageList = receiver.getMessageList();
		messageList.assertMessagesReceived(files.size());

		final File archiveDir = new File(TEST_DIR, "/archived");
		assertTrue("expected <" + archiveDir + "> to exist", archiveDir
				.exists());

		assertFilesProcessed(messageList, files, new FileAssertions() {
			public void assertProcessed(File file, NormalizedMessage message) {
				assertFalse("expected <" + file + "> to be deleted" + file,
						file.exists());

				final String expectedName = file.getName();
				FilenameFilter filter = new FilenameFilter() {
					public boolean accept(File parent, String name) {
						return name.endsWith(expectedName);
					}
				};
				assertEquals("unexpected number of archives for file: " + file
						+ "", 1, archiveDir.listFiles(filter).length);
			}
		});
	}

	// test polling with a filter
	public void testFilter() throws Exception {
		Map<String, File> files = createTestFiles(1);
		createTestFile("ignore-test-1.xml");
		createTestFile("ignore-test-2.xml");

		FileFilter filter = new FileFilter() {
			public boolean accept(File file) {
				return (!file.getName().startsWith("ignore"));
			}
		};

		FtpPollerEndpoint endpoint = createEndpoint();
		endpoint.setFilter(filter);

		container
				.activateComponent(createComponent(endpoint), "servicemix-ftp");

		MessageList messageList = receiver.getMessageList();
		messageList.assertMessagesReceived(files.size());
		assertFilesProcessed(messageList, files, new FileAssertions() {
			public void assertProcessed(File file, NormalizedMessage message) {
				assertFalse("expected <" + file + "> to be deleted", file
						.exists());
			}
		});

	}

	// test polling with stateless = false
	public void testNotStateless() throws Exception {
		Map<String, File> files = createTestFiles(3);

		// Replace the receiver component to check the exchange properties:
		// when 'stateless' is false the FtpData object should not be passed
		// with the exchange because it is cached by the endpoint.
		container.deleteComponent("receiver");
		ReceiverComponent newReceiver = new MyReceiverComponent() {
			@Override
			public void onMessageExchange(MessageExchange exchange)
					throws MessagingException {
				// !stateless: FtpData should not be passed with the exchange
				if (exchange.getProperty(FtpData.class.getName()) != null) {
					fail(exchange, new Exception("Unexpected property: "
							+ FtpData.class.getName()));
				} else {
					super.onMessageExchange(exchange);
				}
			}
		};
		newReceiver.setService(new QName("receiver"));
		newReceiver.setEndpoint("endpoint");
		container.activateComponent(newReceiver, "receiver");

		FtpPollerEndpoint endpoint = createEndpoint();
		endpoint.setStateless(false);
		container
				.activateComponent(createComponent(endpoint), "servicemix-ftp");
		MessageList messageList = newReceiver.getMessageList();
		messageList.assertMessagesReceived(files.size());
		assertFilesProcessed(messageList, files, new FileAssertions() {
			public void assertProcessed(File file, NormalizedMessage message) {
				assertFalse("expected <" + file + "> to be deleted", file
						.exists());
			}
		});

	}

	public void testValidateNoCwdWhenRecursive() throws URISyntaxException {
		FtpPollerEndpoint endpoint = new FtpPollerEndpoint();
		endpoint.setUri(new URI("ftp://anonymous@just.a.server/test"));
		endpoint.setTargetService(new QName("test", "service"));
		endpoint.setChangeWorkingDirectory(true);
		try {
			endpoint.validate();
			fail("validate() should throw exception when changeWorkingDirectory='true' and recursive='true'");
		} catch (DeploymentException e) {
			// this is what we expect
		}
	}

	public void testValidateUriOrHost() throws URISyntaxException,
			DeploymentException {
		FtpPollerEndpoint endpoint = new FtpPollerEndpoint();
		endpoint.setTargetService(new QName("test", "service"));

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

	public void testValidateDeleteWhenArchive() throws URISyntaxException {
		FtpPollerEndpoint endpoint = new FtpPollerEndpoint();
		endpoint.setUri(new URI("ftp://anonymous@just.a.server/test/data"));
		endpoint.setArchive(new URI("ftp://anonymous@just.a.server"));
		endpoint.setDeleteFile(false);
		try {
			endpoint.validate();
			fail("validate() should throw exception when both archive is set but delete is false");
		} catch (DeploymentException e) {
			// this is what we expect
		}
	}

	private FtpComponent createComponent() throws Exception {
		return createComponent(createEndpoint());
	}

	private FtpComponent createComponent(FtpPollerEndpoint endpoint)
			throws Exception {
		FtpComponent component = new FtpComponent();
		component.setEndpoints(new FtpPollerEndpoint[] { endpoint });
		return component;
	}

	private FtpPollerEndpoint createEndpoint() throws Exception {
		FtpPollerEndpoint endpoint = new FtpPollerEndpoint();
		endpoint.setPeriod(1000);
		endpoint.setService(new QName("ftp"));
		endpoint.setEndpoint("endpoint");
		endpoint.setTargetService(receiver.getService());
		endpoint.setUri(new URI("ftp://testuser1:password@localhost:"
				+ getListenerPort() + "/"));

		endpoint.validate();
		return endpoint;
	}

	/**
	 * Generate some number of unique test files in the FTP root directory.
	 */
	protected Map<String, File> createTestFiles(int count) throws IOException {
		return createTestFiles(count, null);
	}

	/**
	 * Generate some number of unique test files in the given dir relative to
	 * the FTP root directory.
	 */
	protected Map<String, File> createTestFiles(int count, String path)
			throws IOException {
		Map<String, File> files = new HashMap<String, File>(count);

		File targetDir = path == null ? TEST_DIR : new File(TEST_DIR, path);
		targetDir.mkdirs();

		for (int i = 0; i < count; i++) {
			File file = File.createTempFile("test-", ".xml", targetDir);
			createTestFile(file);
			files.put(file.getName(), file);
		}
		return files;
	}

	/**
	 * Generate a test file with given path relative to the FTP root directory.
	 */
	protected File createTestFile(String path) throws IOException {
		File file = new File(TEST_DIR, path);
		createTestFile(file);
		return file;
	}

	/**
	 * Check that the given message list contains a message for each file in the
	 * given file map, and that the files have been processed as expected.
	 */
	protected void assertFilesProcessed(MessageList messageList,
			Map<String, File> files, FileAssertions additionalAssertions)
			throws Exception {

		assertEquals(messageList.getMessageCount(), files.size());
		for (Iterator<NormalizedMessage> iter = messageList.getMessages()
				.iterator(); iter.hasNext();) {
			NormalizedMessage message = iter.next();

			String fileName = (String) message
					.getProperty(DefaultFileMarshaler.FILE_NAME_PROPERTY);
			File file = files.get(fileName);
			assertNotNull("invalid file name: " + fileName, file);

			File path = new File(TEST_DIR, ((String) message
					.getProperty(DefaultFileMarshaler.FILE_PATH_PROPERTY)));
			assertEquals("invalid file path", file, path);

			additionalAssertions.assertProcessed(file, message);
		}
	}

	/**
	 * Variant of ReceiverComponent that sets done on the message exchange
	 * before updating the message list. This allows the FTP Poller endpoint to
	 * finish processing (i.e. delete or archive the file) before the test
	 * results are examined.
	 */
	private static class MyReceiverComponent extends ReceiverComponent {
		public void onMessageExchange(MessageExchange exchange)
				throws MessagingException {
			NormalizedMessage inMessage = getInMessage(exchange);
			NormalizedMessage copyMessage = exchange.createMessage();
			getMessageTransformer().transform(exchange, inMessage, copyMessage);
			done(exchange);
			getMessageList().addMessage(copyMessage);
		}
	}

	private static interface FileAssertions {
		void assertProcessed(File file, NormalizedMessage message);
	}

}
