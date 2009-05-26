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

import java.util.Currency;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.servicemix.bean.beans.ParameterAnnotationsBean;
import org.apache.servicemix.jbi.jaxp.StringSource;

/**
 * Test cases for handling method parameter binding on method calls in servicemix-bean
 */
public class ParameterAnnotationsTest extends AbstractBeanComponentTest {
    
    private static final QName SERVICE = new QName("urn:test", "service");
    
    private Pojo pojo;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        pojo = new Pojo();
        BeanEndpoint endpoint = new BeanEndpoint();
        endpoint.setBean(pojo);
        endpoint.setService(SERVICE);
        endpoint.setEndpoint("endpoint");
        
        component.addEndpoint(endpoint);
    }
    
    public void testDefaultParameterMapping() throws Exception {
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(SERVICE);
        exchange.setOperation(new QName("handle"));
        
        NormalizedMessage message = exchange.getInMessage();
        Source content = new StringSource("<request xmlns='urn:test'><type>important!</type></request>");
        message.setContent(content);
        
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.DONE, exchange.getStatus());
        
        assertNotNullAndType(pojo.parameters[0], MessageExchange.class);
        assertNotNullAndType(pojo.parameters[1], NormalizedMessage.class);
        assertSame(message, pojo.parameters[1]);
        assertNotNullAndType(pojo.parameters[2], Source.class);
        assertSame(content, pojo.parameters[2]);
    }
    
    public void testAnnotatedParameterMapping() throws Exception {
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(SERVICE);
        exchange.setOperation(new QName("annotations"));
        
        Currency value = Currency.getInstance("EUR");
        NormalizedMessage message = exchange.getInMessage();
        message.setProperty("key", value);
        Source content = new StringSource("<ns:request xmlns:ns=\"urn:test\"><ns:type>important!</ns:type></ns:request>");
        message.setContent(content);
        
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.DONE, exchange.getStatus());
        
        assertNotNullAndType(pojo.parameters[0], Currency.class);
        assertSame(value, pojo.parameters[0]);
        assertNotNullAndType(pojo.parameters[1], String.class);
    }

    private void assertNotNullAndType(Object object, Class<?> type) {
        assertNotNull(object);
        assertTrue(type.isAssignableFrom(object.getClass()));
    }
    
    public static final class Pojo extends ParameterAnnotationsBean {
        
        private Object[] parameters;
        
        @Override
        public void handle(MessageExchange exchange, NormalizedMessage message, Source source) throws TransformerException {
            saveParameters(exchange, message, source);
        }
        
        @Override
        public void annotations(@Property(name = "key") Object value, 
                                @XPath(uri = "urn:test", prefix = "ns", xpath = "/request/type") String type) {
            saveParameters(value, type);
        }
        
        private void saveParameters(Object... parms) {
            parameters = parms;
        }
    }
}
