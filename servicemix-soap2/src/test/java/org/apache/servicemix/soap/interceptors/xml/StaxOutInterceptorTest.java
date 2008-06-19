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
package org.apache.servicemix.soap.interceptors.xml;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import javax.xml.stream.XMLStreamWriter;

import junit.framework.TestCase;

import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.core.MessageImpl;
import org.apache.servicemix.soap.core.PhaseInterceptorChain;

public class StaxOutInterceptorTest extends TestCase {

    private StaxOutInterceptor interceptor = new StaxOutInterceptor();
    
    public void testNullInput() {
        try {
            Message msg = new MessageImpl();
            interceptor.handleMessage(msg);
            fail("Interceptor should have thrown an NPE");
        } catch (NullPointerException e) {
        }
    }
    
    public void testValidInput() throws Exception {
        Message msg = new MessageImpl();
        msg.setContent(OutputStream.class, new ByteArrayOutputStream());
        PhaseInterceptorChain chain = new PhaseInterceptorChain();
        chain.add(interceptor);
        chain.doIntercept(msg);
        XMLStreamWriter writer = msg.getContent(XMLStreamWriter.class); 
        assertNotNull(writer);
    }
    
}
