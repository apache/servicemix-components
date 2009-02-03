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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.components.http.InvalidStatusResponseException;
import org.apache.servicemix.http.jetty.JettyContextManager;
import org.mortbay.thread.QueuedThreadPool;

public class ServerManagerTest extends TestCase {
    private static transient Log log = LogFactory.getLog(ServerManagerTest.class);

    protected JettyContextManager server;
    protected HttpConfiguration configuration;

    protected void setUp() throws Exception {
        System.setProperty("DEBUG", "true");
        System.setProperty("java.protocol.handler.pkgs", "HTTPClient");
        configuration = new HttpConfiguration();
        server = new JettyContextManager();
        server.setConfiguration(configuration);
    }

    protected void tearDown() throws Exception {
        server.shutDown();
    }

    public void test() throws Exception {
        server.init();
        server.start();

        // Test first context
        checkFail("http://localhost:8192/Service1/echo", null);
        Object ctx1 = server.createContext("http://localhost:8192/Service1", new TestHttpProcessor());
        request("http://localhost:8192/Service1/echo", null);
        server.remove(ctx1);
        checkFail("http://localhost:8192/Service1/echo", null);

        // Test second context on the same host/port
        checkFail("http://localhost:8192/Service2/echo", null);
        Object ctx2 = server.createContext("http://localhost:8192/Service2", new TestHttpProcessor());
        request("http://localhost:8192/Service2/echo", null);
        server.remove(ctx2);
        checkFail("http://localhost:8192/Service2/echo", null);

        // Test third context on another port
        checkFail("http://localhost:8193/echo", null);
        Object ctx3 = server.createContext("http://localhost:8193", new TestHttpProcessor());
        request("http://localhost:8193/echo", null);
        server.remove(ctx3);
        checkFail("http://localhost:8193/echo", null);
    }

    public void testOverlappingPath() throws Exception {
        server.init();
        server.start();

        server.createContext("http://localhost:8192/Service1/test1", new TestHttpProcessor());

        server.createContext("http://localhost:8192/Service1/test1ex", new TestHttpProcessor());

        try {
            server.createContext("http://localhost:8192/Service1/test1", new TestHttpProcessor());
            fail("Contexts are overlapping, an exception should have been thrown");
        } catch (Exception e) {
            // ok
        }

        try {
            server.createContext("http://localhost:8192/Service1/test1/test", new TestHttpProcessor());
            fail("Contexts are overlapping, an exception should have been thrown");
        } catch (Exception e) {
            // ok
        }

        try {
            server.createContext("http://localhost:8192/Service1", new TestHttpProcessor());
            fail("Contexts are overlapping, an exception should have been thrown");
        } catch (Exception e) {
            // ok
        }
    }

    public void testSetMaxThreads() throws Exception {
        int maxThreads = 512;
        configuration.setJettyThreadPoolSize(maxThreads);
        server.init();
        assertTrue(server.getThreadPool() instanceof QueuedThreadPool);
        int threads = ((QueuedThreadPool) server.getThreadPool()).getMaxThreads();
        assertEquals("The max number of threads is incorrect!", maxThreads, threads);
    }

    protected void checkFail(String url, String content) {
        try {
            request(url, content);
            fail("Request should have failed: " + url);
        } catch (Exception e) {
            // System.out.println(e);
        }
    }

    protected String request(String url, String content) throws Exception {
        return requestWithHttpClient(url, content);
    }

    private String requestWithHttpClient(String url, String content) throws Exception {
        HttpMethod method;
        if (content != null) {
            PostMethod post = new PostMethod(url);
            post.setRequestEntity(new StringRequestEntity(content));
            method = post;
        } else {
            GetMethod get = new GetMethod(url);
            method = get;
        }
        new HttpClient().executeMethod(method);
        if (method.getStatusCode() != 200) {
            throw new InvalidStatusResponseException(method.getStatusCode());
        }
        return method.getResponseBodyAsString();
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
