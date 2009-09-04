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

import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

import junit.framework.TestCase;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.servicemix.tck.mock.MockMessageExchange;

/**
 * Test case for {@link JbiExchange}
 */
public class JbiExchangeTest extends TestCase {
    
    private CamelContext context;
    private JbiBinding binding;

    @Override
    protected void setUp() throws Exception {
        context = new DefaultCamelContext();
        binding = new JbiBinding(context);
    }
    
    public void testAccessJbiClasses() throws Exception {
        MessageExchange me = new MockMessageExchange();
        NormalizedMessage in = me.createMessage();
        me.setMessage(in, "in");
        NormalizedMessage out = me.createMessage();
        me.setMessage(out, "out");
        Fault fault = me.createFault();
        me.setFault(fault);
        
        JbiExchange exchange = new JbiExchange(context, binding);
        exchange.setProperty(JbiBinding.MESSAGE_EXCHANGE, me);
        
        assertSame(me, exchange.getMessageExchange());
        
        assertSame(in, exchange.getInMessage());
        assertSame(in, exchange.getIn().getNormalizedMessage());
        
        assertSame(out, exchange.getOutMessage());
        assertSame(out, exchange.getOut().getNormalizedMessage());
        
        assertSame(fault, exchange.getFaultMessage());
        assertSame(fault, exchange.getFault().getNormalizedMessage());
    }
}
