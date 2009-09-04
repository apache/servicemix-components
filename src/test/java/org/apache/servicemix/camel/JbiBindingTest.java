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

import java.util.ArrayList;
import java.util.HashMap;

import javax.activation.DataHandler;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.security.auth.Subject;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import junit.framework.TestCase;

import org.apache.camel.Exchange;
import org.apache.camel.ExchangePattern;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultMessage;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.mock.MockExchangeFactory;
import org.apache.servicemix.tck.mock.MockNormalizedMessage;

public class JbiBindingTest extends TestCase {

    private static final QName OPERATION = new QName("urn:test", "operation");
    private static final Source CONTENT = new StringSource("<my>content</my>");
    private static final String KEY = "key";
    private static final Object VALUE = "value";
    private static final DataHandler DATA = new DataHandler(new Object(), "application/dummy");
    private static final String ID = "id";
    private static final Subject SUBJECT = new Subject();
    
    private MessageExchangeFactory factory;
    private JbiBinding binding;
    
    @Override
    protected void setUp() throws Exception {
        factory = new MockExchangeFactory();
        binding = new JbiBinding(new DefaultCamelContext());
    }
    
    public void testCreateExchangeWithOperation() throws Exception {
        MessageExchange me = factory.createInOnlyExchange();
        me.setOperation(OPERATION);
        
        Exchange exchange = binding.createExchange(me);
        assertNotNull(exchange);
        assertSame("JBI MessageExchange is available as a property",
                   me, exchange.getProperty(JbiBinding.MESSAGE_EXCHANGE));
        assertEquals("JBI operation is available as a property",
                     OPERATION, exchange.getProperty(JbiBinding.OPERATION));
        assertEquals("JBI operation is available as a property (String value)",
                     OPERATION.toString(), exchange.getProperty(JbiBinding.OPERATION_STRING));
        assertEquals("Camel Exchange uses the same MEP",
                     ExchangePattern.InOnly, exchange.getPattern());
    }

    public void testCreateExchange() throws Exception {
        MessageExchange me = factory.createRobustInOnlyExchange();
        
        Exchange exchange = binding.createExchange(me);
        assertNotNull(exchange);
        assertSame("JBI MessageExchange is available as a property",
                   me, exchange.getProperty(JbiBinding.MESSAGE_EXCHANGE));
        assertEquals("Camel Exchange uses the same MEP",
                ExchangePattern.RobustInOnly, exchange.getPattern());
    }
    
    public void testCreateExchangeWithInContentAndHeaders() throws Exception {
        MessageExchange me = factory.createInOptionalOutExchange();
        MockNormalizedMessage nm = new MockNormalizedMessage();
        nm.setContent(CONTENT);
        nm.setProperty(KEY, VALUE);
        me.setMessage(nm, "in");
        
        Exchange exchange = binding.createExchange(me);
        assertNotNull(exchange);
        assertSame("JBI MessageExchange is available as a property",
                   me, exchange.getProperty(JbiBinding.MESSAGE_EXCHANGE));
        assertEquals("JBI NormalizedMessage content is available in the Camel Message",
                     CONTENT, exchange.getIn().getBody());
        assertEquals("JBI NormalizedMessage headers are available in the Camel Message",
                     VALUE, exchange.getIn().getHeader(KEY));
        assertEquals("Camel Exchange uses the same MEP",
                     ExchangePattern.InOptionalOut, exchange.getPattern());
    }
    
    public void testCreateExchangeWithInContentAndAttachment() throws Exception {
        MessageExchange me = factory.createInOutExchange();
        MockNormalizedMessage nm = new MockNormalizedMessage();
        nm.setContent(CONTENT);
        nm.addAttachment(ID, DATA);
        me.setMessage(nm, "in");
        
        Exchange exchange = binding.createExchange(me);
        assertNotNull(exchange);
        assertSame("JBI MessageExchange is available as a property",
                   me, exchange.getProperty(JbiBinding.MESSAGE_EXCHANGE));
        assertEquals("JBI NormalizedMessage content is available in the Camel Message",
                     CONTENT, exchange.getIn().getBody());
        assertEquals("JBI NormalizedMessage attachments are available in the Camel Message",
                     DATA, exchange.getIn().getAttachment(ID));
        assertEquals("Camel Exchange uses the same MEP",
                     ExchangePattern.InOut, exchange.getPattern());
    }
    
    public void testCreateExchangeWithSecuritySubject() throws Exception {
        MessageExchange me = factory.createInOutExchange();
        MockNormalizedMessage nm = new MockNormalizedMessage();
        nm.setSecuritySubject(SUBJECT);
        me.setMessage(nm, "in");
        
        Exchange exchange = binding.createExchange(me);
        assertNotNull(exchange);
        assertSame("JBI MessageExchange is available as a property",
                   me, exchange.getProperty(JbiBinding.MESSAGE_EXCHANGE));
        assertSame("JBI NormalizedMessage SecuritySubject is available as a property",
                   SUBJECT, exchange.getIn().getHeader(JbiBinding.SECURITY_SUBJECT));
    }
    
    public void testCopyCamelToJbiAssertSerializableProperties() throws Exception {
        NormalizedMessage to = new MockNormalizedMessage();
        
        Message from = new DefaultMessage();
        from.setBody(CONTENT);
        from.setHeader(KEY, VALUE);
        from.setHeader("binding", binding);
        from.setHeader("map", new HashMap());
        from.setHeader("collection", new HashMap());

        binding.copyFromCamelToJbi(from, to);
        assertEquals("Should only have copied one property (ignore non-serializable properties)", 
                     1, to.getPropertyNames().size());
    }
    
    public void testCopyFromCamelToJbiWithSecuritySubject() throws Exception {
        Exchange from = new DefaultExchange(new DefaultCamelContext());
        from.getIn().setHeader(JbiBinding.SECURITY_SUBJECT, SUBJECT);
        
        NormalizedMessage to = new MockNormalizedMessage();
        binding.copyFromCamelToJbi(from.getIn(), to);
        assertSame("JBI SecuritySubject should have been set from the Camel Message header",
                   SUBJECT, to.getSecuritySubject());
    }
    
    public void testIsSerializable() throws Exception {
        assertTrue("A String is serializable", binding.isSerializable("test"));
        assertFalse("JbiBinding is not serializable", binding.isSerializable(binding));
        assertFalse("Maps can contain non-serializable data", binding.isSerializable(new HashMap()));
        assertFalse("Collections can contain non-serializable data", binding.isSerializable(new ArrayList()));
    }
}
