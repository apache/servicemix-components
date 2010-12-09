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
package org.apache.servicemix.soap.bindings.soap.interceptors;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.bindings.soap.SoapFault;
import org.apache.servicemix.soap.bindings.soap.SoapVersion;
import org.apache.servicemix.soap.core.MessageImpl;
import org.apache.servicemix.soap.interceptors.xml.StaxInInterceptor;

public class SoapInInterceptorTest extends TestCase {
    private static transient Log log = LogFactory.getLog(SoapInInterceptorTest.class);

    private SoapInInterceptor interceptor = new SoapInInterceptor();
    
    public void testNullInput() {
        try {
            Message msg = new MessageImpl();
            interceptor.handleMessage(msg);
            fail("Interceptor should have thrown an NPE");
        } catch (NullPointerException e) {
        }
    }
    
    public void testBadNamespace() throws Exception {
        Message msg = new MessageImpl();
        InputStream is = new ByteArrayInputStream("<hello/>".getBytes());
        msg.setContent(InputStream.class, is);
        new StaxInInterceptor().handleMessage(msg);
        try {
            interceptor.handleMessage(msg);
            fail("Interceptor should have thrown a SoapFault");
        } catch (SoapFault e) {
            log.info(e.getMessage(), e);
        }
    }
    
    public void testBadLocalName() throws Exception {
        Message msg = new MessageImpl();
        InputStream is = new ByteArrayInputStream("<test xmlns='http://www.w3.org/2003/05/soap-envelope'/>".getBytes());
        msg.setContent(InputStream.class, is);
        new StaxInInterceptor().handleMessage(msg);
        try {
            interceptor.handleMessage(msg);
            fail("Interceptor should have thrown a SoapFault");
        } catch (SoapFault e) {
            log.info(e.getMessage(), e);
        }
    }
    
    public void testEmptyEnvelopeName() throws Exception {
        Message msg = new MessageImpl();
        InputStream is = new ByteArrayInputStream("<Envelope xmlns='http://www.w3.org/2003/05/soap-envelope'/>".getBytes());
        msg.setContent(InputStream.class, is);
        new StaxInInterceptor().handleMessage(msg);
        try {
            interceptor.handleMessage(msg);
            fail("Interceptor should have thrown a SoapFault");
        } catch (SoapFault e) {
            log.info(e.getMessage(), e);
        }
    }
    
    public void testValidInputWithHeadersName() throws Exception {
        String str = "<s:Envelope xmlns:s='http://www.w3.org/2003/05/soap-envelope'>" +
                     "  <s:Header>" +
                     "    <header1>one</header1>" +
                     "    <header1>two</header1>" +
                     "    <header2 xmlns='urn:y'>three</header2>" +
                     "  </s:Header>" +
                     "  <s:Body>" +
                     "    <hello>world</hello>" +
                     "  </s:Body>" +
                     "</s:Envelope>";
        Message msg = new MessageImpl();
        InputStream is = new ByteArrayInputStream(str.getBytes());
        msg.setContent(InputStream.class, is);
        new StaxInInterceptor().handleMessage(msg);
        interceptor.handleMessage(msg);
        
        assertNotNull(msg.get(SoapVersion.class));
        assertEquals("s", msg.get(SoapVersion.class).getPrefix());
        assertNotNull(msg.getSoapHeaders().get(new QName("header1")));
        assertEquals(2, msg.getSoapHeaders().get(new QName("header1")).getChildNodes().getLength());
        assertNotNull(msg.getSoapHeaders().get(new QName("urn:y", "header2")));
        assertEquals(1, msg.getSoapHeaders().get(new QName("urn:y", "header2")).getChildNodes().getLength());
        assertNotNull(msg.getContent(XMLStreamReader.class));
        assertEquals(XMLStreamConstants.START_ELEMENT, msg.getContent(XMLStreamReader.class).getEventType());
        assertEquals(new QName("hello"), msg.getContent(XMLStreamReader.class).getName());
    }
    
    public void testValidInputWithEmptyBody() throws Exception {
        String str = "<s:Envelope xmlns:s='http://www.w3.org/2003/05/soap-envelope'>" +
                     "  <s:Body>" +
                     "  </s:Body>" +
                     "</s:Envelope>";
        Message msg = new MessageImpl();
        InputStream is = new ByteArrayInputStream(str.getBytes());
        msg.setContent(InputStream.class, is);
        new StaxInInterceptor().handleMessage(msg);
        interceptor.handleMessage(msg);
        
        assertNotNull(msg.get(SoapVersion.class));
        assertEquals("s", msg.get(SoapVersion.class).getPrefix());
        assertEquals(0, msg.getSoapHeaders().size());
        assertNull(msg.getContent(XMLStreamReader.class));
        //assertNotNull(msg.getContent(XMLStreamReader.class));
        //assertEquals(XMLStreamConstants.END_ELEMENT, msg.getContent(XMLStreamReader.class).getEventType());
        //assertEquals(msg.get(SoapVersion.class).getBody(), msg.getContent(XMLStreamReader.class).getName());
    }
    
}
