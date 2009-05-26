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
package org.apache.servicemix.bean.support;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;

import junit.framework.TestCase;

import org.apache.servicemix.jbi.exception.FaultException;
import org.apache.servicemix.tck.mock.MockExchangeFactory;

/**
 * Test cases for {@link Holder}
 */
public class HolderTest extends TestCase {
    
    private final MessageExchangeFactory factory = new MockExchangeFactory(); 
    
    private Holder holder;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        holder = new Holder();
    }
    
    public void testBasicFunctionality() throws Exception {
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            public void run() {
                try {
                    assertNotNull(holder.get(1, TimeUnit.SECONDS));
                    assertTrue(holder.isDone());
                } catch (InterruptedException e) {
                    fail(e.getMessage());
                } catch (ExecutionException e) {
                    fail(e.getMessage());
                }
            }
        });
        
        MessageExchange exchange = factory.createInOutExchange();
        exchange.setStatus(ExchangeStatus.DONE);
        NormalizedMessage out = exchange.createMessage();
        exchange.setMessage(out, "out");

        //this should kick off the tests in the background thread
        holder.set(exchange);
    }
    
    public void testCancel() throws Exception {
        holder.cancel(true);
        assertTrue(holder.isCancelled());
    }
    
    public void testWrapFaults() throws Exception {
        MessageExchange exchange = factory.createInOutExchange();
        exchange.setStatus(ExchangeStatus.ACTIVE);
        exchange.setFault(exchange.createFault());
        assertExecutionExceptionOnGet(exchange, FaultException.class);
    }
    
    public void testWrapErrors() throws Exception {
        MessageExchange exchange = factory.createInOutExchange();
        exchange.setStatus(ExchangeStatus.ERROR);
        exchange.setError(new RuntimeException("This thing completely went wrong..."));
        assertExecutionExceptionOnGet(exchange, RuntimeException.class);
    }

    private void assertExecutionExceptionOnGet(MessageExchange exchange, Class<?> type) throws InterruptedException {
        try {
            holder.set(exchange);
            holder.get();
            fail("Should have thrown an ExecutionException");
        } catch (ExecutionException e) {
            assertEquals(type, e.getCause().getClass());
        }
    }

}
