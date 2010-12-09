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
package org.apache.servicemix.saxon.support;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.servicemix.tck.mock.MockMessageExchange;
import org.easymock.EasyMock;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

public class ExchangeTargetTest extends TestCase {
    
    private static final String ENDPOINT = "endpoint";
    private static final QName INTERFACE = new QName("urn:test", "interface");
    private static final QName OPERATION = new QName("urn:test", "operation");
    private static final QName SERVICE = new QName("urn:test", "service");
    
    private ExchangeTarget target;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        target = new ExchangeTarget();
    }
  
    public void testMessagingExceptionWhenNoTargetSet() throws Exception {
        try {
            target.afterPropertiesSet();
            fail("Should have thrown a MessagingException");
        } catch (MessagingException e) {
            //this is OK
        }
        try {
            target.configureTarget(null, null);
            fail("Should have thrown a MessagingException");
        } catch (MessagingException e) {
            //this is OK
        }
    }
    
    public void testInterfaceAndOperation() throws Exception {
        target.setInterface(INTERFACE);
        target.setOperation(OPERATION);
        
        MessageExchange exchange = new MockMessageExchange();
        target.configureTarget(exchange, null);
        assertEquals(INTERFACE, exchange.getInterfaceName());
        assertEquals(OPERATION, exchange.getOperation());
    }
    
    public void testServiceAndEndpoint() throws Exception {
        target.setService(SERVICE);
        target.setEndpoint(ENDPOINT);
        
        ComponentContext context = EasyMock.createMock(ComponentContext.class);
        ServiceEndpoint endpoint = EasyMock.createMock(ServiceEndpoint.class);
        expect(context.getEndpoint(SERVICE, ENDPOINT)).andReturn(endpoint);
        replay(context);
                
        MessageExchange exchange = new MockMessageExchange();
        target.configureTarget(exchange, context);
        assertEquals(SERVICE, exchange.getService());
        assertEquals(endpoint, exchange.getEndpoint());
    }

}    

