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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.naming.InitialContext;

import junit.framework.TestCase;

import org.apache.ftpserver.ConnectionConfigFactory;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.DefaultFtplet;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.FtpRequest;
import org.apache.ftpserver.ftplet.FtpSession;
import org.apache.ftpserver.ftplet.Ftplet;
import org.apache.ftpserver.ftplet.FtpletResult;
import org.apache.ftpserver.impl.DefaultFtpServer;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor;
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.util.FileUtil;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Base class for FTP tests that starts an FTP server and JBI container.
 */
public abstract class AbstractFtpTestSupport extends TestCase {

	private static final String REQUEST_LOG = "requestLog";
	public static final File TEST_DIR = new File("target/test");
	public static final File RESOURCES = new File(System.getProperty(
			"org.apache.servicemix.ftp.testResources", "target/test-classes"));

	private static final File USERS_FILE = new File(RESOURCES,
			"users.properties");

	protected DefaultFtpServer server;
	protected JBIContainer container;
	protected DefaultServiceMixClient client;

	@Override
	protected void setUp() throws Exception {
		createJbiContainer();
		createSmxClient();
		cleanDirs();
		initDirs();
		server = (DefaultFtpServer) createServerFactory().createServer();
		server.start();
	}

	@Override
	protected void tearDown() throws Exception {
		if (container != null) {
			container.stop();
			container.shutDown();
			while (!container.isShutDown()) {
				System.out.println("WAITING to shutdown...");
			}
		}
		if (server != null) {
			server.stop();
		}
		cleanDirs();
	}

	protected void createSmxClient() throws Exception {
		client = new DefaultServiceMixClient(container);
	}

	protected void createJbiContainer() throws Exception {
		container = new JBIContainer();
		configureJbiContainer();
		container.init();
		container.start();
	}

	protected void configureJbiContainer() throws Exception {
		container.setEmbedded(true);
		container.setUseMBeanServer(false);
		container.setCreateMBeanServer(false);
		container.setCreateJmxConnector(false);
		container.setMonitorInstallationDirectory(false);
		container.setNamingContext(new InitialContext());
		container.setFlowName("st");
	}

	protected void initDirs() throws Exception {
		if (!TEST_DIR.mkdirs()) {
			throw new Exception("Unable to create directory: " + TEST_DIR);
		}
	}

	protected void cleanDirs() throws Exception {
		if (!FileUtil.deleteFile(TEST_DIR)) {
			throw new Exception("Unable to delete directory: " + TEST_DIR);
		}
	}

	protected FtpServerFactory createServerFactory() {
		FtpServerFactory serverFactory = new FtpServerFactory();
		serverFactory.setConnectionConfig(new ConnectionConfigFactory()
				.createConnectionConfig());

		ListenerFactory listenerFactory = new ListenerFactory();
		listenerFactory.setPort(0);
		serverFactory.addListener("default", listenerFactory.createListener());

		PropertiesUserManagerFactory userMgrFactory = new PropertiesUserManagerFactory();
		userMgrFactory.setAdminName("admin");
		userMgrFactory.setPasswordEncryptor(new ClearTextPasswordEncryptor());
		userMgrFactory.setFile(USERS_FILE);

		serverFactory.setUserManager(userMgrFactory.createUserManager());
		serverFactory.setFtplets(getFtplets());

		return serverFactory;
	}

	protected Map<String, Ftplet> getFtplets() {
		Map<String, Ftplet> map = new HashMap<String, Ftplet>();
		map.put(REQUEST_LOG, new RequestLogFtplet());
		return map;
	}

	protected final int getListenerPort() {
		return server.getListener("default").getPort();
	}

	/*
	 * Asserts that the given file exists and contains the given string.
	 */
	protected void assertFileContains(File file, String string)
			throws IOException {

		assertTrue("file does not exist: " + file, file.exists());
		assertTrue("cannot read file: " + file, file.canRead());

		Scanner scanner = null;
		try {
			scanner = new Scanner(file);
			String result = scanner.findInLine(string);
			assertNotNull("string '" + string + "' not found in file: " + file,
					result);
		} finally {
			if (scanner != null)
				scanner.close();
		}
	}

	/**
	 * Copy test data to given file.
	 */
	protected void createTestFile(File file) throws IOException {
		file.getParentFile().mkdirs();
		InputStream fis = new FileInputStream(RESOURCES + "/test-data.xml");
		OutputStream fos = new FileOutputStream(file);
		FileUtil.copyInputStream(fis, fos);
		fis.close();
		fos.close();
	}

	/**
	 * Get the list of requests received by the FTP server.
	 */
	protected List<String> getRequestLog() {
		RequestLogFtplet ftplet = (RequestLogFtplet) server.getFtplets().get(
				REQUEST_LOG);
		return ftplet.getRequests();
	}

	/**
	 * Test assertion that the FTP server executed the given requests (in
	 * order). Any requests before or after the given sequence are ignored.
	 */
	protected void assertRequestLogContains(String[] expected) {
		List<String> requestLog = getRequestLog();
		String[] requestArray = new String[requestLog.size()];
		requestArray = requestLog.toArray(requestArray);

		String[] actual = null;

		for (int i = 0; i < requestArray.length; i++) {
			if (requestArray[i].equals(expected[0])) {
				actual = (String[]) Arrays.copyOfRange(requestArray, i, i
						+ expected.length);
			}
		}

		assertTrue("expected " + Arrays.toString(expected) + " actual "
				+ Arrays.toString(actual), Arrays.equals(expected, actual));
	}

	/**
	 * Ftplet to capture the list of requests received by the FTP server.
	 */
	protected static class RequestLogFtplet extends DefaultFtplet {

		List<String> requests = new ArrayList<String>();

		@Override
		public FtpletResult beforeCommand(FtpSession session, FtpRequest request)
				throws FtpException, IOException {
			requests.add(request.getRequestLine());
			return super.beforeCommand(session, request);
		}

		public List<String> getRequests() {
			return requests;
		}

	}

}
