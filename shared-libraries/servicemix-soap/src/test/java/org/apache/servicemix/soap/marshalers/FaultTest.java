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
package org.apache.servicemix.soap.marshalers;

import java.io.ByteArrayOutputStream;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.util.DOMUtil;
import org.apache.servicemix.soap.SoapFault;

public class FaultTest extends TestCase {
    private static transient Log log = LogFactory.getLog(FaultTest.class);

    private SourceTransformer sourceTransformer = new SourceTransformer();
    
    protected void testSoap11(boolean useDom) throws Exception {
        SoapMarshaler marshaler = new SoapMarshaler(true, useDom);
        try {
            marshaler.createReader().read(getClass().getResourceAsStream("fault-1.1-ok.xml"));
        } catch (SoapFault fault) {
            assertEquals(SoapMarshaler.SOAP_11_CODE_SERVER, fault.getCode());
            assertNull(fault.getSubcode());
            assertEquals("Server Error", fault.getReason());
            assertNotNull(fault.getDetails());
            Node node = sourceTransformer.toDOMNode(fault.getDetails());
            Element e = node instanceof Document ? ((Document) node).getDocumentElement() : (Element) node;
            assertEquals(new QName("Some-URI", "myfaultdetails"), DOMUtil.getQName(e));
        }
    }
    
    protected SoapMessage testSoap11WithMultipleDetailElements(boolean useDom) throws Exception {
        SoapMarshaler marshaler = new SoapMarshaler(true, useDom);
        try {
            SoapMessage message = marshaler.createReader().read(getClass().getResourceAsStream("fault-1.1-multiple-detail-elements.xml"));
            return message;
        } catch (SoapFault fault) {
            assertEquals(SoapMarshaler.SOAP_11_CODE_SERVER, fault.getCode());
            assertNull(fault.getSubcode());
            assertEquals("Server Error", fault.getReason());
            assertNotNull(fault.getDetails());
            Node node = sourceTransformer.toDOMNode(fault.getDetails());
            Element e = node instanceof Document ? ((Document) node).getDocumentElement() : (Element) node;
            assertEquals(new QName("Some-URI", "myfaultdetails"), DOMUtil.getQName(e));
            return null;
        }
    }

    public void testReadSoap11WithMultipleElementsUsingDom() throws Exception {
        testSoap11WithMultipleDetailElements(true);
    }

    public void testWriteSoap11WithMultipleElementsUsingDom() throws Exception {
        SoapMessage message = testSoap11WithMultipleDetailElements(true);
        message.setDocument(null);
        SoapMarshaler marshaler = new SoapMarshaler(true, true);
        SoapWriter writer = marshaler.createWriter(message);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        writer.write(baos);
        
        log.info("Resulting Fault: \n" + baos);
    }

    public void testReadSoap11UsingDom() throws Exception {
        testSoap11(true);
    }
    
    public void testReadSoap11UsingStax() throws Exception {
        testSoap11(false);
    }
    
    protected void testSoap12(boolean useDom) throws Exception {
        SoapMarshaler marshaler = new SoapMarshaler(true, useDom);
        try {
            marshaler.createReader().read(getClass().getResourceAsStream("fault-1.2-ok.xml"));
        } catch (SoapFault fault) {
            assertEquals(SoapMarshaler.SOAP_12_CODE_SENDER, fault.getCode());
            assertEquals(new QName("http://www.example.org/timeouts", "MessageTimeout"), fault.getSubcode());
            assertEquals("Sender Timeout", fault.getReason());
            assertNotNull(fault.getDetails());
            Node node = sourceTransformer.toDOMNode(fault.getDetails());
            Element e = node instanceof Document ? ((Document) node).getDocumentElement() : (Element) node;
            assertEquals(new QName("http://www.example.org/timeouts", "MaxTime"), DOMUtil.getQName(e));
        }
    }
    
    public void testReadSoap12UsingDom() throws Exception {
        testSoap12(true);
    }
    
    public void testReadSoap12UsingStax() throws Exception {
        testSoap12(false);
    }
    
    public void testWriteSoap11UsingDom() throws Exception {
        testWriteSoap11(true);
    }

    public void testWriteSoap11UsingStax() throws Exception {
        testWriteSoap11(false);
    }

    protected void testWriteSoap11(boolean useDom) throws Exception {
        SoapMarshaler marshaler = new SoapMarshaler(true, useDom);
        marshaler.setSoapUri(SoapMarshaler.SOAP_11_URI);
        SoapMessage msg = new SoapMessage();
        SoapFault fault = new SoapFault(new QName("my:urn", "code"), null, "My reason", null, null, new StringSource("<ns1:hello xmlns:ns1='my:urn'>world</ns1:hello>"));
        msg.setFault(fault);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        marshaler.createWriter(msg).write(baos);
        log.info(baos.toString());

        Node node = sourceTransformer.toDOMNode(new StringSource(baos.toString()));
        Element e = ((Document) node).getDocumentElement();
        assertEquals(new QName(SoapMarshaler.SOAP_11_URI, SoapMarshaler.ENVELOPE), DOMUtil.getQName(e));
        e = DOMUtil.getFirstChildElement(e);
        assertEquals(new QName(SoapMarshaler.SOAP_11_URI, SoapMarshaler.BODY), DOMUtil.getQName(e));
        Element elFault = DOMUtil.getFirstChildElement(e);
        assertEquals(new QName(SoapMarshaler.SOAP_11_URI, SoapMarshaler.FAULT), DOMUtil.getQName(elFault));
        Element elCode = DOMUtil.getFirstChildElement(elFault);
        assertEquals(SoapMarshaler.SOAP_11_FAULTCODE, DOMUtil.getQName(elCode));
        assertEquals(new QName("my:urn", "code"), DOMUtil.createQName(elCode, DOMUtil.getElementText(elCode)));
        Element elReason = DOMUtil.getNextSiblingElement(elCode);
        assertEquals(SoapMarshaler.SOAP_11_FAULTSTRING, DOMUtil.getQName(elReason));
        Element elDetail = DOMUtil.getNextSiblingElement(elReason);
        assertEquals(SoapMarshaler.SOAP_11_FAULTDETAIL, DOMUtil.getQName(elDetail));
        e = DOMUtil.getFirstChildElement(elDetail);
        assertEquals(new QName("my:urn", "hello"), DOMUtil.getQName(e));
    }
    
    public void testWriteSoap12UsingDom() throws Exception {
        testWriteSoap12(true);
    }
    
    public void testWriteSoap12UsingStax() throws Exception {
        testWriteSoap12(false);
    }
    
    protected void testWriteSoap12(boolean useDom) throws Exception {
        SoapMarshaler marshaler = new SoapMarshaler(true, useDom);
        marshaler.setSoapUri(SoapMarshaler.SOAP_12_URI);
        SoapMessage msg = new SoapMessage();
        SoapFault fault = new SoapFault(new QName("my:urn", "code"), null, "My reason", null, null, 
                                        new StringSource("<ns1:hello xmlns:ns1='my:urn'>world</ns1:hello>"));
        msg.setFault(fault);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        marshaler.createWriter(msg).write(baos);
        log.info(baos.toString());

        Node node = sourceTransformer.toDOMNode(new StringSource(baos.toString()));
        Element e = ((Document) node).getDocumentElement();
        assertEquals(new QName(SoapMarshaler.SOAP_12_URI, SoapMarshaler.ENVELOPE), DOMUtil.getQName(e));
        e = DOMUtil.getFirstChildElement(e);
        assertEquals(new QName(SoapMarshaler.SOAP_12_URI, SoapMarshaler.BODY), DOMUtil.getQName(e));
        Element elFault = DOMUtil.getFirstChildElement(e);
        assertEquals(new QName(SoapMarshaler.SOAP_12_URI, SoapMarshaler.FAULT), DOMUtil.getQName(elFault));
        Element elCode = DOMUtil.getFirstChildElement(elFault);
        assertEquals(SoapMarshaler.SOAP_12_FAULTCODE, DOMUtil.getQName(elCode));
        e = DOMUtil.getFirstChildElement(elCode);
        assertEquals(SoapMarshaler.SOAP_12_FAULTVALUE, DOMUtil.getQName(e));
        e = DOMUtil.getNextSiblingElement(e);
        assertEquals(SoapMarshaler.SOAP_12_FAULTSUBCODE, DOMUtil.getQName(e));
        e = DOMUtil.getFirstChildElement(e);
        assertEquals(SoapMarshaler.SOAP_12_FAULTVALUE, DOMUtil.getQName(e));
        assertEquals(new QName("my:urn", "code"), DOMUtil.createQName(e, DOMUtil.getElementText(e)));
        Element elReason = DOMUtil.getNextSiblingElement(elCode);
        assertEquals(SoapMarshaler.SOAP_12_FAULTREASON, DOMUtil.getQName(elReason));
        Element elDetail = DOMUtil.getNextSiblingElement(elReason);
        assertEquals(SoapMarshaler.SOAP_12_FAULTDETAIL, DOMUtil.getQName(elDetail));
        e = DOMUtil.getFirstChildElement(elDetail);
        assertEquals(new QName("my:urn", "hello"), DOMUtil.getQName(e));
    }
    
}
