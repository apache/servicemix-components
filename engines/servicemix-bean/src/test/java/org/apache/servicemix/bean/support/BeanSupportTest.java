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
import javax.xml.namespace.QName;

import org.apache.servicemix.bean.AbstractBeanComponentTest;
import org.apache.servicemix.bean.BeanEndpoint;
import org.apache.servicemix.common.JbiConstants;
import org.apache.servicemix.tck.mock.MockMessageExchange;

/**
 * Test cases for {@link BeanSupport}
 */
public class BeanSupportTest extends AbstractBeanComponentTest {
    
    private static final String CORRELATION_ID = "my-correlation-id"; 
    private static final QName TARGET_SERVICE = new QName("urn:test", "new-service");
    private static final QName TARGET_INTERFACE = new QName("urn:test", "new-interface");
    private static final QName TARGET_OPERATION = new QName("urn:test", "new-operation");
    
    private BeanSupport support;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        support = new Pojo();
        BeanEndpoint endpoint = new BeanEndpoint();
        endpoint.setService(new QName("urn:test", "service"));
        endpoint.setEndpoint("endpoint");
        endpoint.setBean(support);
        
        component.addEndpoint(endpoint);
    }

    public void testCreateInOnlyExchange() throws Exception {
        MessageExchange answer = support.createInOnlyExchange(TARGET_SERVICE, null, null, null);
        assertNotNull(answer);
        assertEquals(TARGET_SERVICE, answer.getService());
        
        answer = support.createInOnlyExchange(null, TARGET_INTERFACE, TARGET_OPERATION, null);
        assertNotNull(answer);
        assertEquals(TARGET_INTERFACE, answer.getInterfaceName());
        assertEquals(TARGET_OPERATION, answer.getOperation());
    }
    
    public void testCreateInOutExchange() throws Exception {
        MessageExchange answer = support.createInOutExchange(TARGET_SERVICE, null, null, null);
        assertNotNull(answer);
        assertEquals(TARGET_SERVICE, answer.getService());
        
        answer = support.createInOutExchange(null, TARGET_INTERFACE, TARGET_OPERATION, null);
        assertNotNull(answer);
        assertEquals(TARGET_INTERFACE, answer.getInterfaceName());
        assertEquals(TARGET_OPERATION, answer.getOperation());
    }
    
    public void testCreateInOnlyExchangeFromOriginalExchange() throws Exception {
        MessageExchange answer = support.createInOnlyExchange(createMockExchange());
        assertNotNull(answer);
        assertEquals(CORRELATION_ID, answer.getProperty(JbiConstants.CORRELATION_ID));
    }
    
    public void testCreateInOnlyExchangeFromOriginalExchangeWithCorrelationId() throws Exception {
        MessageExchange exchange = createMockExchange();
        exchange.setProperty(JbiConstants.CORRELATION_ID, "my-other-correlation-id");
        MessageExchange answer = support.createInOnlyExchange(exchange);
        assertNotNull(answer);
        assertEquals("my-other-correlation-id", answer.getProperty(JbiConstants.CORRELATION_ID));
    }
    
    public void testCreateInOutExchangeFromOriginalExchange() throws Exception {
        MessageExchange answer = support.createInOutExchange(createMockExchange());
        assertNotNull(answer);
        assertEquals(CORRELATION_ID, answer.getProperty(JbiConstants.CORRELATION_ID));
    }
    
    public void testCreateRobustInOnlyExchangeFromOriginalExchange() throws Exception {
        MessageExchange answer = support.createRobustInOnlyExchange(createMockExchange());
        assertNotNull(answer);
        assertEquals(CORRELATION_ID, answer.getProperty(JbiConstants.CORRELATION_ID));
    }
    
    public void testCreateInOptionalOutExchangeFromOriginalExchange() throws Exception {
        MessageExchange answer = support.createInOptionalOutExchange(createMockExchange());
        assertNotNull(answer);
        assertEquals(CORRELATION_ID, answer.getProperty(JbiConstants.CORRELATION_ID));
    }
    
    private MessageExchange createMockExchange() {
        return new MockMessageExchange() {
            @Override
            public String getExchangeId() {
                return CORRELATION_ID;
            }
        };
    }

    private static final class Pojo extends BeanSupport {
        
    }

}
