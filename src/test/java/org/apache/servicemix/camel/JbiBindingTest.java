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

import javax.jbi.messaging.NormalizedMessage;

import junit.framework.TestCase;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultExchange;
import org.apache.camel.impl.DefaultMessage;
import org.apache.servicemix.tck.mock.MockNormalizedMessage;

public class JbiBindingTest extends TestCase {

    private JbiBinding binding;
    
    @Override
    protected void setUp() throws Exception {
        binding = new JbiBinding();
    }
    
    public void testGetJbiInContentForString() {
        CamelContext context = new DefaultCamelContext();
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setBody("<hello>world!</hello>");
        assertNotNull(binding.getJbiInContent(exchange));
    }
    
    public void testGetNormalizedMessageForDefaultCamelMessage() {
        Message message = new DefaultMessage();
        assertNull(binding.getNormalizedMessage(message));
    }
    
    public void testGetNormalizedMessageForJbiCamelMessage() {
        JbiMessage camelMessage = new JbiMessage();
        assertNull(binding.getNormalizedMessage(camelMessage));
        
        NormalizedMessage jbiMessage = new MockNormalizedMessage();
        camelMessage.setNormalizedMessage(jbiMessage);
        assertSame(jbiMessage, binding.getNormalizedMessage(camelMessage));
    }

}
