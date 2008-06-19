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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import junit.framework.TestCase;

import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.core.MessageImpl;
import org.w3c.dom.Document;

public class DomInInterceptorTest extends TestCase {

    private DomInInterceptor interceptor = new DomInInterceptor();
    
    public void testNullInput() {
        Message msg = new MessageImpl();
        interceptor.handleMessage(msg);
        assertNull(msg.getContent(Document.class));
    }
    
    public void testValidInput() throws Exception {
        Message msg = new MessageImpl();
        InputStream is = new ByteArrayInputStream("<hello/>".getBytes());
        msg.setContent(InputStream.class, is);
        interceptor.handleMessage(msg);
        Document doc = msg.getContent(Document.class); 
        assertNotNull(doc);
        assertEquals("hello", doc.getDocumentElement().getLocalName());
    }
    
}
