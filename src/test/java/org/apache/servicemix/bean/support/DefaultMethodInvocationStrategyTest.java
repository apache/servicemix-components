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

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.Source;

import junit.framework.TestCase;

import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.mock.MockMessageExchange;

/**
 * Test cases for {@link DefaultMethodInvocationStrategy}
 */
public class DefaultMethodInvocationStrategyTest extends TestCase {
    
    private DefaultMethodInvocationStrategy strategy;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        strategy = new DefaultMethodInvocationStrategy();
        strategy.loadDefaultRegistry();
    }
    
    public void testMessageExchangeExpression() throws Exception {
        MessageExchange exchange = new MockMessageExchange();
        NormalizedMessage message = exchange.createMessage();
        Source source = new StringSource("<my><content type='test'/></my>");
        message.setContent(source);
        exchange.setMessage(message, "in");
        
        assertSame(exchange, strategy.getDefaultParameterTypeExpression(MessageExchange.class).evaluate(exchange, message));
        assertSame(message, strategy.getDefaultParameterTypeExpression(NormalizedMessage.class).evaluate(exchange, message));
        assertSame(source, strategy.getDefaultParameterTypeExpression(Source.class).evaluate(exchange, message));
    }

}
