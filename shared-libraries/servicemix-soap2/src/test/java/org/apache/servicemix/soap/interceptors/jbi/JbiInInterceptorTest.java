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
package org.apache.servicemix.soap.interceptors.jbi;

import java.io.StringReader;
import java.util.Map;

import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.apache.servicemix.soap.api.Interceptor;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.core.MessageImpl;
import org.apache.servicemix.tck.mock.MockExchangeFactory;

public class JbiInInterceptorTest extends TestCase {

    public void test() throws Exception {
        Interceptor interceptor = new JbiInInterceptor(true);
        Message message = new MessageImpl();
        message.put(JbiInInterceptor.OPERATION_MEP, JbiConstants.IN_ONLY);
        message.put(MessageExchangeFactory.class, new MockExchangeFactory());
        message.setContent(Source.class, new StreamSource(new StringReader("<hello/>")));
        message.getTransportHeaders().put("Content-Type", "text/xml");
        
        interceptor.handleMessage(message);
        
        MessageExchange me = message.getContent(MessageExchange.class);
        assertNotNull(me);
        assertTrue(me instanceof InOnly);
        NormalizedMessage nm = me.getMessage("in");
        assertNotNull(nm);
        assertNotNull(nm.getContent());
        Map headers = (Map) nm.getProperty(JbiConstants.PROTOCOL_HEADERS);
        assertNotNull(headers);
        assertEquals("text/xml", headers.get("Content-Type"));
    }
    
}
