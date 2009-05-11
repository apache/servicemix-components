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
package org.apache.servicemix.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.executors.Executor;
import org.apache.servicemix.tck.mock.MockExchangeFactory;
import org.apache.servicemix.util.FileUtil;

public class FilePollerEndpointTest extends TestCase {

    private static final File DATA = new File("target/test/data");
    private static final File ARCHIVE = new File("target/test/archive");
    private final List<MessageExchange> exchanges = new LinkedList<MessageExchange>();
    private FilePollerEndpoint endpoint;

    @Override
    protected void setUp() throws Exception {
        exchanges.clear();
        endpoint = new FilePollerEndpoint() {
            {
                logger = LogFactory.getLog(this.getClass());
            }

            @Override
            protected void send(MessageExchange me) throws MessagingException {
                exchanges.add(me);
            }

            @Override
            public Executor getExecutor() {
                return new MockExecutor();
            }

            @Override
            public MessageExchangeFactory getExchangeFactory() {
                return new MockExchangeFactory() {
                    @Override
                    public InOnly createInOnlyExchange() throws MessagingException {
                        return new MockExchangeFactory.MockInOnly() {
                            private final String exchangeId = "id" + System.nanoTime();

                            @Override
                            public String getExchangeId() {
                                return exchangeId;
                            }
                        };
                    }
                };
            }
        };
        endpoint.setTargetService(new QName("urn:test", "service"));
        endpoint.setLockManager(new org.apache.servicemix.common.locks.impl.SimpleLockManager());
    }

    public void testValidateNoFile() throws Exception {
        try {
            endpoint.validate();
            fail("validate() should throw an exception when file has not been set");
        } catch (DeploymentException e) {
            // test succeeds
        }
    }

    public void testValidateArchiveNoDirectory() throws Exception {
        endpoint.setFile(DATA);
        File archive = null;
        try {
            archive = File.createTempFile("servicemix", "test");
            endpoint.setArchive(archive);
            endpoint.validate();
            fail("validate() should throw an exception when archive doesn't refer to a directory");
        } catch (DeploymentException e) {
            // test succeeds
        } finally {
            if (archive != null) {
                archive.delete();
            }
        }
    }

    public void testValidateArchiveWithoutDelete() throws Exception {
        endpoint.setFile(DATA);
        endpoint.setArchive(ARCHIVE);
        endpoint.setDeleteFile(false);
        try {
            endpoint.validate();
            fail("validate() should throw an exception when archive was set without delete");
        } catch (DeploymentException e) {
            // test succeeds
        }
    }

    public void testProcessError() throws Exception {
        createTestFile();
        endpoint.setFile(DATA);
        endpoint.pollFile(new File(DATA, "test-data.xml"));
        MessageExchange exchange = exchanges.get(0);
        exchange.setStatus(ExchangeStatus.ERROR);
        exchange.setError(new TestException());
        try {
            endpoint.process(exchange);
        } catch (TestException e) {
            // this is OK
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e);
            fail("we shouldn't be getting any other exceptions at this point");
        }
    }

    public void testProcessSuccess() throws Exception {
        createTestFile();
        endpoint.setFile(DATA);
        File file = new File(DATA, "test-data.xml");
        endpoint.pollFile(file);
        MessageExchange exchange = exchanges.get(0);
        exchange.setStatus(ExchangeStatus.DONE);
        endpoint.process(exchange);
        assertFalse(file.exists());
    }

    private void createTestFile() throws IOException {
        DATA.mkdirs();
        InputStream fis = new FileInputStream("target/test-classes/test-data.xml");
        OutputStream fos = new FileOutputStream(new File(DATA, "test-data.xml"));
        FileUtil.copyInputStream(fis, fos);
        fis.close();
        fos.close();
    }

    public void testMoveFileToNonExistentDirectory() throws Exception {
        File srcFile = File.createTempFile("poller-test-", ".tmp");
        try {
            FilePollerEndpoint.moveFile(srcFile, new File("bogus"));
            fail("moveFile() should fail when moving to non-existent directory");
        } catch (IOException ioe) {
            // test succeeds
        } finally {
            srcFile.delete();
        }
    }

    @SuppressWarnings("serial")
    private static class TestException extends Exception {
        // nothing to do here
    }

    private static class MockExecutor implements Executor {

        public int capacity() {
            return 0;
        }

        public void execute(Runnable command) {
            command.run();
        }

        public void shutdown() {
            // graciously do nothing
        }

        public int size() {
            return 0;
        }
    }
}
