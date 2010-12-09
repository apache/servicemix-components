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
package org.apache.servicemix.bean;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.xml.namespace.QName;

import org.apache.servicemix.bean.beans.AnnotatedBean;
import org.springframework.beans.BeansException;
import org.springframework.context.support.StaticApplicationContext;

/**
 * Test cases for {@link BeanEndpoint}
 */
public class BeanEndpointTest extends AbstractBeanComponentTest {
    
    private static final QName SERVICE = new QName("urn:test", "service");
    private static final String BEAN_NAME = "MyBean";

    public void testExceptionWithNothingSet() throws Exception {
        BeanEndpoint endpoint = new BeanEndpoint();
        try {
            endpoint.createBean();
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            //this is what we expected
        }
    }
    
    public void testExceptionOnBeanNameWithoutApplicationContext() throws Exception {
        BeanEndpoint endpoint = new BeanEndpoint();
        try {
            endpoint.setBeanName(BEAN_NAME);
            endpoint.createBean();
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            //this is what we expected
        }        
    }
    
    public void testExceptionOnNonExistingBeanInApplicationContext() throws Exception {
        BeanEndpoint endpoint = new BeanEndpoint();
        try {
            endpoint.setApplicationContext(new StaticApplicationContext() {
                @Override
                public Object getBean(String name) throws BeansException {
                    return null;
                }
            });
            endpoint.setBeanName(BEAN_NAME);
            endpoint.createBean();
            fail("Should have thrown a NoSuchBeanException");
        } catch (NoSuchBeanException e) {
            assertEquals(BEAN_NAME, e.getBeanName());
            assertEquals(endpoint, e.getEndpoint());
        }        
    }
    
    public void testBeanClassName() throws Exception {
        BeanEndpoint endpoint = new BeanEndpoint();
        endpoint.setBeanClassName(AnnotatedBean.class.getName());
        endpoint.setService(SERVICE);
        endpoint.setEndpoint("endpoint");
        component.addEndpoint(endpoint);
        
        assertEquals("Endpoint should have found the correct bean type", AnnotatedBean.class, endpoint.getBeanType());
    }
    
    public void testExceptionOnInvalidOperationName() throws Exception {
        BeanEndpoint endpoint = new BeanEndpoint();
        endpoint.setBean(new Object());
        endpoint.setService(SERVICE);
        endpoint.setEndpoint("endpoint");
        component.addEndpoint(endpoint);

        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(SERVICE);
        exchange.setOperation(new QName("urn:test", "invalid-ops"));
        client.sendSync(exchange);
        
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
        assertTrue(exchange.getError() instanceof UnknownMessageExchangeTypeException);
        UnknownMessageExchangeTypeException umete = (UnknownMessageExchangeTypeException) exchange.getError();
        assertEquals(endpoint, umete.getEndpoint());
        assertEquals(exchange.getExchangeId(), umete.getMessageExchange().getExchangeId());
    }
}
