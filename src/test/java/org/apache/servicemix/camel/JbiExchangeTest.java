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

import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

import junit.framework.TestCase;

import org.apache.camel.Exchange;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.servicemix.jbi.helper.MessageExchangePattern;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.messaging.FaultImpl;
import org.apache.servicemix.tck.mock.MockMessageExchange;
import org.apache.servicemix.tck.mock.MockNormalizedMessage;

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
    
    /*
     * Test access to the underlying NormalizedMessages
     */
    public void testAccessTheNormalizedMessages() throws Exception {
        NormalizedMessage in = new MockNormalizedMessage();
        NormalizedMessage out = new MockNormalizedMessage();
        Fault fault = new FaultImpl();
        MessageExchange mock = createMockExchange();
        mock.setMessage(in, "in");
        mock.setMessage(out, "out");
        mock.setFault(fault);
        JbiExchange exchange = new JbiExchange(new DefaultCamelContext(), new JbiBinding(), mock);
        assertSame(in, exchange.getInMessage());
        assertSame(out, exchange.getOutMessage());
        assertSame(fault, exchange.getFaultMessage());
    }
    
    public void testDetach() throws Exception {
        MessageExchange exchange = createMockExchange();
        StringSource body = new StringSource("<question>Will this still be there?</question>");
        exchange.setMessage(exchange.createMessage(), "in");
        exchange.getMessage("in").setContent(body);
        exchange.getMessage("in").setProperty("key", "value");
        JbiExchange camelExchange = new JbiExchange(new DefaultCamelContext(), new JbiBinding(), exchange);
        assertEquals(body, camelExchange.getIn().getBody());
        // now detach the Camel Exchange from the underlying JBI MessageExchange
        assertSame(exchange, camelExchange.detach());
        assertNull(camelExchange.getMessageExchange());
        // and make sure that all the data is still there
        assertEquals(body, camelExchange.getIn().getBody());
        assertEquals("value", camelExchange.getIn().getHeader("key"));
    }
    
    private MessageExchange createMockExchange() {
        return new MockMessageExchange() {
            @Override
            public URI getPattern() {
                return MessageExchangePattern.IN_OUT;
            }
            public NormalizedMessage getMessage(String name) {
                if ("fault".equalsIgnoreCase(name)) {
                    return getFault();
                }
                return super.getMessage(name);
            }
        };
    }


}
