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

import java.lang.reflect.Method;
import java.util.Map;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;

import junit.framework.TestCase;

import org.apache.servicemix.bean.BeanEndpoint;
import org.apache.servicemix.tck.mock.MockMessageExchange;

/**
 * Test cases for {@link Request}
 */
public class RequestTest extends TestCase {
    
    public void testIsFinishedOnStatus() throws Exception {
        MessageExchange exchange = createMockExchange("my-exchange-id");
        Request request = new Request("my-correlation-id", new Object(), exchange);
        assertFalse(request.isFinished());
        exchange.setStatus(ExchangeStatus.DONE);
        assertTrue(request.isFinished());
    }
    
    public void testIsFinishedWhenAllExchangesDoneOrError() throws Exception {
        MessageExchange exchange = createMockExchange("my-exchange-id");
        Request request = new Request("my-correlation-id", new Object(), exchange);
        assertFalse(request.isFinished());
        
        MessageExchange second = createMockExchange("my-second-id");
        request.addExchange(second);
        exchange.setStatus(ExchangeStatus.DONE);
        assertFalse(request.isFinished());
        
        second.setStatus(ExchangeStatus.ERROR);
        assertTrue(request.isFinished());
    }
    
    public void testAddExchangeSetsCorrelationId() throws Exception {
        MessageExchange exchange = createMockExchange("my-exchange-id");
        Request request = new Request("my-correlation-id", new Object(), exchange);

        MessageExchange second = createMockExchange("my-second-id");
        request.addExchange(second);
        assertEquals("my-correlation-id", second.getProperty(BeanEndpoint.CORRELATION_ID));
    }
    
    public void testNoSentExchangeForCorrelationId() throws Exception {
        MessageExchange exchange = createMockExchange("my-exchange-id");
        Request request = new Request("my-correlation-id", new Object(), exchange);
        request.addExchange(exchange);
        assertEquals("We shouldn't have duplicate MessageExchange instances", 1, request.getExchanges().size());
    }
    
    public void testLazyCreateCallbacks() throws Exception {
        MessageExchange exchange = createMockExchange("my-exchange-id");
        Request request = new Request("my-correlation-id", new Object(), exchange);
        Map<Method, Boolean> callbacks = request.getCallbacks();
        assertNotNull(callbacks);
        assertSame(callbacks, request.getCallbacks());
    }
    
    private MessageExchange createMockExchange(String id) {
        MockMessageExchange exchange = new MockMessageExchange();
        exchange.setExchangeId(id);
        exchange.setStatus(ExchangeStatus.ACTIVE);
        return exchange;
    }
}
