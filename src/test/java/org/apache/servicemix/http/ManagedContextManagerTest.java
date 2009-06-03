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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ManagedContextManagerTest extends TestCase {

    private static transient Log log = LogFactory.getLog(ManagedContextManagerTest.class);
    private ManagedContextManager server;
    private HttpConfiguration config;

    protected void setUp() throws Exception {
        super.setUp();
        config = new HttpConfiguration();
        server = new ManagedContextManager();
        server.setConfiguration(config);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        server.shutDown();
        server = null;
        config = null;
    }

    // Test createContext with a path that does not begin with a / and
    // does not end with a /.
    public void testCreateContext() throws Exception {
        server.init();
        server.start();

        TestHttpProcessor httpProcessor = new TestHttpProcessor();
        String strUrl = "path/to/some/resource";

        String returnedPath = (String)server.createContext(strUrl, httpProcessor);
        assertTrue("Context path should have / at end and at the beginning", returnedPath.equals("/" + strUrl + "/"));
    }

    // Test getMainProcessor with an unmanaged config - should throw IllegalStateException
    public void testGetMainProcessorUnmanagedConfig() throws Exception {
        server.init();
        server.start();

        try {
            server.getMainProcessor();
            fail("getMainProcessor() should fail for unmanaged config.");
        } catch (IllegalStateException ise) {
            // test succeeds
            log.info("testGetMainProcessorUnmanagedConfig() threw the expected exception");
        }
    }

    public static class TestHttpProcessor implements HttpProcessor {
        public SslParameters getSsl() {
            return null;
        }

        public String getAuthMethod() {
            return null;
        }

        public void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
            log.info(request);
        }

    }
    
}
