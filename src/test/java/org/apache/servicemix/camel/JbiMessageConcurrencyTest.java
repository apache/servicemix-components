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
package org.apache.servicemix.camel;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

import junit.framework.TestCase;

import org.apache.camel.impl.DefaultCamelContext;
import org.apache.servicemix.jbi.helper.MessageExchangePattern;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.messaging.NormalizedMessageImpl;
import org.apache.servicemix.tck.mock.MockMessageExchange;

/**
 * Test cases for making sure that JbiMessage behaves properly when accessed concurrently 
 */
public class JbiMessageConcurrencyTest extends TestCase {

    private static final String MESSAGE = "<just><a>test</a></just>";
    private static final int COUNT = 500;
    
    public void testRunMessageCopyFromConcurrently() throws Exception {
        MessageExchange exchange = new MockMessageExchange() {
            @Override
            public URI getPattern() {
                return MessageExchangePattern.IN_OUT;
            }
            @Override
            public NormalizedMessage createMessage() throws MessagingException {
                // let's take a 'real' NormalizedMessageImpl to reproduce the toString() behavior
                return new NormalizedMessageImpl();
            }
        };
        exchange.setMessage(exchange.createMessage(), "in");
        exchange.getMessage("in").setContent(new StringSource(MESSAGE));
        
        final JbiExchange camelExchange = new JbiExchange(new DefaultCamelContext(), new JbiBinding(), exchange);
        ExecutorService executor = Executors.newFixedThreadPool(50);
        final CountDownLatch latch = new CountDownLatch(COUNT);
        final List<Exception> exceptions = new LinkedList<Exception>();
        for (int i = 0; i < COUNT; i++) {
            final int count = i;
            executor.execute(new Runnable() {
                public void run() {
                    try {
                        // let's set headers
                        camelExchange.getIn().setHeader("@" + count, "ok");
                        camelExchange.getOut().setHeader("@" + count, "ok");
                        
                        // and access them as well
                        assertNotNull(camelExchange.getIn().toString());
                        assertNotNull(camelExchange.getOut().toString());
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        
        // give the threads some time
        latch.await(3, TimeUnit.SECONDS);
        for (Exception e : exceptions) {
            e.printStackTrace();
        }
        assertEquals("Should not thrown any exceptions due to concurrent access", 0, exceptions.size());
    }
}
