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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import junit.framework.TestCase;

import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.api.model.Operation;
import org.apache.servicemix.soap.bindings.soap.impl.Wsdl1SoapMessageImpl;
import org.apache.servicemix.soap.bindings.soap.impl.Wsdl1SoapOperationImpl;
import org.apache.servicemix.soap.bindings.soap.impl.Wsdl1SoapPartImpl;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapBinding;
import org.apache.servicemix.soap.core.MessageImpl;
import org.apache.servicemix.soap.util.DomUtil;
import org.apache.servicemix.soap.util.stax.StaxUtil;

public class JbiInWsdl1InterceptorTest extends TestCase {

    private final Logger logger = LoggerFactory.getLogger(JbiInWsdl1InterceptorTest.class);

    public void test() throws Exception {
        Wsdl1SoapOperationImpl wsdlOperation = new Wsdl1SoapOperationImpl();
        Wsdl1SoapMessageImpl wsdlMessage = new Wsdl1SoapMessageImpl();
        wsdlMessage.setName(new QName("urn:test", "message"));
        Wsdl1SoapPartImpl part = new Wsdl1SoapPartImpl();
        part.setBody(true);
        wsdlMessage.addPart(part);
        wsdlMessage.setMessageName("message");
        wsdlOperation.setInput(wsdlMessage);
        wsdlOperation.setStyle(Wsdl1SoapBinding.Style.DOCUMENT);

        String input = "<hello xmlns='uri:test' attr='value'>  toto  </hello>";
        
        Message message = new MessageImpl();
        message.put(Operation.class, wsdlOperation);
        XMLStreamReader reader = StaxUtil.createReader(new ByteArrayInputStream(input.getBytes()));
        reader.nextTag();
        message.setContent(XMLStreamReader.class, reader);
        
        JbiInWsdl1Interceptor interceptor = new JbiInWsdl1Interceptor(true);
        interceptor.handleMessage(message);
        Source source = message.getContent(Source.class);
        assertNotNull(source);
        Document doc = DomUtil.parse(source);
        Element root = doc.getDocumentElement();
        assertEquals(JbiConstants.WSDL11_WRAPPER_NAMESPACE, root.getNamespaceURI());
        assertEquals(JbiConstants.WSDL11_WRAPPER_MESSAGE_LOCALNAME, root.getLocalName());
        

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DomUtil.getTransformerFactory().newTransformer().transform(new DOMSource(doc), new StreamResult(baos));
        logger.info(baos.toString());
    }
    
    public void testNamespace() throws Exception {
        
        Wsdl1SoapOperationImpl wsdlOperation = new Wsdl1SoapOperationImpl();
        Wsdl1SoapMessageImpl wsdlMessage = new Wsdl1SoapMessageImpl();
        wsdlMessage.setName(new QName("urn:test", "message"));
        Wsdl1SoapPartImpl part = new Wsdl1SoapPartImpl();
        part.setBody(true);
        wsdlMessage.addPart(part);
        wsdlMessage.setMessageName("message");
        wsdlOperation.setInput(wsdlMessage);
        wsdlOperation.setStyle(Wsdl1SoapBinding.Style.DOCUMENT);

        String input = "<echo xmlns='http://www.example.org'><request xmlns=''><message>hello</message></request></echo>";
        
        Message message = new MessageImpl();
        message.put(Operation.class, wsdlOperation);
        XMLStreamReader reader = StaxUtil.createReader(new ByteArrayInputStream(input.getBytes()));
        reader.nextTag();
        message.setContent(XMLStreamReader.class, reader);
        
        JbiInWsdl1Interceptor interceptor = new JbiInWsdl1Interceptor(true);
        interceptor.handleMessage(message);
        Source source = message.getContent(Source.class);
        assertNotNull(source);
        Document doc = DomUtil.parse(source);
        Element root = doc.getDocumentElement();
        assertEquals(JbiConstants.WSDL11_WRAPPER_NAMESPACE, root.getNamespaceURI());
        assertEquals(JbiConstants.WSDL11_WRAPPER_MESSAGE_LOCALNAME, root.getLocalName());
        

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DomUtil.getTransformerFactory().newTransformer().transform(new DOMSource(doc), new StreamResult(baos));
  
        assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\"?><jbi:message xmlns:jbi=\"http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper\" xmlns:msg=\"urn:test\" name=\"message\" type=\"msg:message\" version=\"1.0\"><jbi:part><echo xmlns=\"http://www.example.org\"><request xmlns=\"\"><message>hello</message></request></echo></jbi:part></jbi:message>", baos.toString());
        logger.info(baos.toString());
    }
    
}
