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

/**
 * Test cases for {@link MethodInvocationFailedException}
 */
public class MethodInvocationFailedExceptionTest extends AbstractBeanComponentTest {
    
    private static final QName SERVICE = new QName("urn:test", "service");
    
    public void testReceiveMethodInvocationFailedException() throws Exception {
        BeanEndpoint endpoint = new BeanEndpoint();
        MyPojo pojo = new MyPojo();
        endpoint.setBean(pojo);
        endpoint.setService(SERVICE);
        endpoint.setEndpoint("endpoint");
        component.addEndpoint(endpoint);
        
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(SERVICE);
        client.sendSync(exchange);
        
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());
        assertTrue(exchange.getError() instanceof MethodInvocationFailedException);
        
        MethodInvocationFailedException mife = (MethodInvocationFailedException) exchange.getError();
        assertEquals(endpoint, mife.getEndpoint());
        assertEquals(pojo, mife.getPojo());
        assertTrue(mife.getCause() instanceof IllegalArgumentException);
    }
     
    public static final class MyPojo {
        
        public void handle() {
            throw new IllegalArgumentException("Hey, how can I have an IllegalArgumentException when there is no argument?");
        }
    }
}
