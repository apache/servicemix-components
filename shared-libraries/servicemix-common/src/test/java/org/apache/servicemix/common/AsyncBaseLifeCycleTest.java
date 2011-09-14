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
package org.apache.servicemix.common;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.jbi.messaging.MessageExchange;
import javax.xml.namespace.QName;

import junit.framework.TestCase;
import org.apache.servicemix.common.endpoints.SimpleEndpoint;
import org.apache.servicemix.tck.mock.MockMessageExchange;

/**
 * Test cases for {@link org.apache.servicemix.common.AsyncBaseLifeCycle}
 */
public class AsyncBaseLifeCycleTest extends TestCase {

    private static final long TIMEOUT = 2000;

    private AsyncBaseLifeCycle lifecycle;
    private ExecutorService executor;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        lifecycle = new AsyncBaseLifeCycle();
        lifecycle.setExecutorFactory(lifecycle.createExecutorFactory());
        lifecycle.setComponent(new DefaultComponent());

        executor = Executors.newSingleThreadExecutor();
    }

    public void testPrepareShutdown() throws InterruptedException {
        final Endpoint endpoint = new MockEndpoint() ;

        MockMessageExchange exchange1 = createMockExchange();
        MockMessageExchange exchange2 = createMockExchange();

        // adding 2 known exchanges
        lifecycle.handleExchange(endpoint, exchange1, true);
        lifecycle.handleExchange(endpoint, exchange2, true);

        final CountDownLatch done = new CountDownLatch(1);

        executor.submit(new Callable() {
            public Object call() throws Exception {
                try {
                    lifecycle.prepareShutdown(endpoint);
                } finally {
                    done.countDown();
                }
                return null;
            }
        });

        assertFalse("Should be waiting for prepareShutdown to complete (2 exchanges pending)",
                    done.await(1, TimeUnit.SECONDS));

        lifecycle.handleExchange(endpoint, exchange1, false);

        assertFalse("Should be waiting for prepareShutdown to complete (1 exchange pending)",
                    done.await(1, TimeUnit.SECONDS));

        lifecycle.handleExchange(endpoint, exchange2, false);

        assertTrue("prepareShutdown is now done", done.await(100, TimeUnit.SECONDS));
    }

    private MockMessageExchange createMockExchange() {
        MockMessageExchange exchange = new MockMessageExchange();
        exchange.setExchangeId(UUID.randomUUID().toString());
        return exchange;
    }

    public void testPrepareShutdownWithTimeout() throws InterruptedException, ExecutionException, TimeoutException {
        final Endpoint endpoint = new MockEndpoint() ;

        MockMessageExchange exchange = new MockMessageExchange();
        exchange.setExchangeId(UUID.randomUUID().toString());

        // adding a known exchange
        lifecycle.handleExchange(endpoint, exchange, true);

        final CountDownLatch done = new CountDownLatch(1);

        Future<Long> time = executor.submit(new Callable<Long>() {
            public Long call() throws Exception {
                long start = System.currentTimeMillis();
                lifecycle.prepareShutdown(endpoint, TIMEOUT);
                return (System.currentTimeMillis() - start);
            }
        });

        assertEquals("Should be waiting for prepareShutdown to complete",
                     1, done.getCount());

        Long shutdown = time.get(2 * TIMEOUT, TimeUnit.MILLISECONDS);

        assertTrue("prepareShutdown should have timed out after " + TIMEOUT + "ms (was " + shutdown + "ms)",
                   shutdown >= TIMEOUT);
    }

    public static class MockEndpoint extends SimpleEndpoint {

        public MockEndpoint() {
            super();
            setService(new QName("urn:test", "service"));
            setEndpoint("endpoint");
        }

        @Override
        public MessageExchange.Role getRole() {
            return null;
        }

        @Override
        public void process(MessageExchange exchange) throws Exception {
            // graciously do nothing
        }
    }
}
