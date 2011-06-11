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
package org.apache.servicemix.http.exception;

import junit.framework.TestCase;
import org.apache.servicemix.tck.mock.MockExchangeFactory;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;

/**
 * Test cases for {@link HttpTimeoutException}
 */
public class HttpTimeoutExceptionTest extends TestCase {

    public void testMessageContainsExchangeId() throws MessagingException {
        MockExchangeFactory factory = new MockExchangeFactory();
        MessageExchange exchange = factory.createInOnlyExchange();

        HttpTimeoutException exception = new HttpTimeoutException(exchange);
        assertTrue("Exception message refers to the exchange id",
                   exception.getMessage().contains(exchange.getExchangeId()));
    }

    public void testMessageWithNullExchange() throws MessagingException {
        HttpTimeoutException exception = new HttpTimeoutException(null);
        assertNotNull("We still expect some kind of message", exception.getMessage());
    }

}
