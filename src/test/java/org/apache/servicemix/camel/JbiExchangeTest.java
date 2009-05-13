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

import javax.jbi.messaging.MessageExchange;

import junit.framework.TestCase;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.servicemix.jbi.messaging.MessageExchangeSupport;
import org.apache.servicemix.tck.mock.MockMessageExchange;

/**
 * Test case for {@link JbiExchange}
 */
public class JbiExchangeTest extends TestCase {
    
    private static final String KEY = "org.apache.servicemix.camel.TEST_KEY";
    private static final String VALUE = "TEST_VALUE";
    
    /*
     * Test setting the property on both the Camel Exchange and the underlying JBI MessageExchange
     */
    public void testSetPropertyOnMessageExchange() throws Exception {
        MessageExchange jbiExchange = createMockExchange();
        Exchange camelExchange = new JbiExchange(new DefaultCamelContext(), new JbiBinding(), jbiExchange); 
        camelExchange.setProperty(KEY, VALUE);
        assertEquals(VALUE, camelExchange.getProperty(KEY));
        assertEquals(VALUE, jbiExchange.getProperty(KEY));
    }
    
    /*
     * Test setting the property without overriding the existing JBI MessageExchange one
     */
    public void testSetPropertyNoOverrideMessageExchange() throws Exception {
        MessageExchange jbiExchange = createMockExchange();
        jbiExchange.setProperty(KEY, VALUE);
        Exchange camelExchange = new JbiExchange(new DefaultCamelContext(), new JbiBinding(), jbiExchange); 
        camelExchange.setProperty(KEY, "OVERRIDE_TEST_VALUE");
        assertEquals(VALUE, jbiExchange.getProperty(KEY));
    }
    
    /*
     * Test setting a property without having a MessageExchange in there
     */
    public void testSetPropertyWithoutMessageExchange() throws Exception {
        Exchange camelExchange = new JbiExchange(new DefaultCamelContext(), new JbiBinding()); 
        camelExchange.setProperty(KEY, VALUE);
        assertEquals(VALUE, camelExchange.getProperty(KEY));
    }
    
    private MessageExchange createMockExchange() {
        return new MockMessageExchange() {
            @Override
            public URI getPattern() {
                return MessageExchangeSupport.IN_OUT;
            }
        };
    }


}
