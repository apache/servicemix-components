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
package org.apache.servicemix.soap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.soap.api.InterceptorProvider.Phase;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.api.model.Binding;
import org.apache.servicemix.soap.bindings.http.HttpConstants;
import org.apache.servicemix.soap.core.MessageImpl;
import org.apache.servicemix.soap.core.PhaseInterceptorChain;
import org.apache.servicemix.soap.util.DomUtil;
import org.apache.servicemix.soap.wsdl.BindingFactory;
import org.apache.servicemix.tck.mock.MockExchangeFactory;
import org.apache.woden.WSDLException;
import org.apache.woden.WSDLFactory;
import org.apache.woden.WSDLReader;
import org.apache.woden.wsdl20.Description;
import org.apache.woden.wsdl20.Endpoint;
import org.apache.woden.wsdl20.xml.DescriptionElement;

public class PersonHttpTest extends TestCase {
    private static transient Log log = LogFactory.getLog(PersonHttpTest.class);

    private static Binding<?> binding = getBinding("Person.wsdl2");

    public void testGetPerson() throws Exception {
        PhaseInterceptorChain phaseIn = new PhaseInterceptorChain();
        phaseIn.add(binding.getInterceptors(Phase.ServerIn));
        
        Message message = new MessageImpl();
        message.put(Binding.class, binding);
        message.put(MessageExchangeFactory.class, new MockExchangeFactory());
        message.getTransportHeaders().put(HttpConstants.REQUEST_URI, "http://localhost:8192/person/312?code=abc");
        message.getTransportHeaders().put(HttpConstants.REQUEST_METHOD, HttpConstants.METHOD_GET);
        
        phaseIn.doIntercept(message);

        MessageExchange me = message.getContent(MessageExchange.class);
        NormalizedMessage nm = message.getContent(NormalizedMessage.class);
        Document doc = DomUtil.parse(nm.getContent());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DomUtil.getTransformerFactory().newTransformer().transform(nm.getContent(), new StreamResult(baos));
        log.info(baos.toString());

        assertEquals(new QName("http://example.org/Person", "getPerson"), me.getOperation());
        Element e = DomUtil.getFirstChildElement(doc);
        assertEquals("http://example.org/Person", e.getNamespaceURI()); 
        assertEquals("GetPerson", e.getLocalName());
        
        e = DomUtil.getFirstChildElement(e);
        assertEquals("http://example.org/Person", e.getNamespaceURI()); 
        assertEquals("id", e.getLocalName());
        assertEquals("312", e.getTextContent());
    }
    
    public void testUpdatePerson() throws Exception {
        PhaseInterceptorChain phaseIn = new PhaseInterceptorChain();
        phaseIn.add(binding.getInterceptors(Phase.ServerIn));
        
        Message message = new MessageImpl();
        message.put(Binding.class, binding);
        message.put(MessageExchangeFactory.class, new MockExchangeFactory());
        message.getTransportHeaders().put(HttpConstants.REQUEST_URI, "http://localhost:8192/person/312");
        message.getTransportHeaders().put(HttpConstants.REQUEST_METHOD, HttpConstants.METHOD_POST);
        message.setContent(InputStream.class, new ByteArrayInputStream("ssn=321&name=Nodet".getBytes()));
        
        phaseIn.doIntercept(message);

        MessageExchange me = message.getContent(MessageExchange.class);
        NormalizedMessage nm = message.getContent(NormalizedMessage.class);
        Document doc = DomUtil.parse(nm.getContent());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DomUtil.getTransformerFactory().newTransformer().transform(nm.getContent(), new StreamResult(baos));
        log.info(baos.toString());

        assertEquals(new QName("http://example.org/Person", "updatePerson"), me.getOperation());
        Element e = DomUtil.getFirstChildElement(doc);
        assertEquals("http://example.org/Person", e.getNamespaceURI()); 
        assertEquals("UpdatePerson", e.getLocalName());
        
        e = DomUtil.getFirstChildElement(e);
        assertEquals("http://example.org/Person", e.getNamespaceURI()); 
        assertEquals("id", e.getLocalName());
        assertEquals("312", e.getTextContent());
        
        e = DomUtil.getNextSiblingElement(e);
        assertEquals("http://example.org/Person", e.getNamespaceURI()); 
        assertEquals("ssn", e.getLocalName());
        assertEquals("321", e.getTextContent());
        
        e = DomUtil.getNextSiblingElement(e);
        assertEquals("http://example.org/Person", e.getNamespaceURI()); 
        assertEquals("name", e.getLocalName());
        assertEquals("Nodet", e.getTextContent());
    }
    
    public void testAddPerson() throws Exception {
        PhaseInterceptorChain phaseIn = new PhaseInterceptorChain();
        phaseIn.add(binding.getInterceptors(Phase.ServerIn));
        
        Message message = new MessageImpl();
        message.put(Binding.class, binding);
        message.put(MessageExchangeFactory.class, new MockExchangeFactory());
        message.getTransportHeaders().put(HttpConstants.REQUEST_URI, "http://localhost:8192/person/312");
        message.getTransportHeaders().put(HttpConstants.REQUEST_METHOD, HttpConstants.METHOD_PUT);
        message.setContent(InputStream.class, new ByteArrayInputStream("ssn=321&name=Nodet".getBytes()));
        
        phaseIn.doIntercept(message);

        MessageExchange me = message.getContent(MessageExchange.class);
        NormalizedMessage nm = message.getContent(NormalizedMessage.class);
        Document doc = DomUtil.parse(nm.getContent());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DomUtil.getTransformerFactory().newTransformer().transform(nm.getContent(), new StreamResult(baos));
        log.info(baos.toString());

        assertEquals(new QName("http://example.org/Person", "addPerson"), me.getOperation());
        Element e = DomUtil.getFirstChildElement(doc);
        assertEquals("http://example.org/Person", e.getNamespaceURI()); 
        assertEquals("AddPerson", e.getLocalName());
        
        e = DomUtil.getFirstChildElement(e);
        assertEquals("http://example.org/Person", e.getNamespaceURI()); 
        assertEquals("id", e.getLocalName());
        assertEquals("312", e.getTextContent());
        
        e = DomUtil.getNextSiblingElement(e);
        assertEquals("http://example.org/Person", e.getNamespaceURI()); 
        assertEquals("ssn", e.getLocalName());
        assertEquals("321", e.getTextContent());
        
        e = DomUtil.getNextSiblingElement(e);
        assertEquals("http://example.org/Person", e.getNamespaceURI()); 
        assertEquals("name", e.getLocalName());
        assertEquals("Nodet", e.getTextContent());
    }
    
    public void testDeletePerson() throws Exception {
        PhaseInterceptorChain phaseIn = new PhaseInterceptorChain();
        phaseIn.add(binding.getInterceptors(Phase.ServerIn));
        
        Message message = new MessageImpl();
        message.put(Binding.class, binding);
        message.put(MessageExchangeFactory.class, new MockExchangeFactory());
        message.getTransportHeaders().put(HttpConstants.REQUEST_URI, "http://localhost:8192/person/312");
        message.getTransportHeaders().put(HttpConstants.REQUEST_METHOD, HttpConstants.METHOD_DELETE);
        
        phaseIn.doIntercept(message);

        MessageExchange me = message.getContent(MessageExchange.class);
        NormalizedMessage nm = message.getContent(NormalizedMessage.class);
        Document doc = DomUtil.parse(nm.getContent());

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DomUtil.getTransformerFactory().newTransformer().transform(nm.getContent(), new StreamResult(baos));
        log.info(baos.toString());

        assertEquals(new QName("http://example.org/Person", "deletePerson"), me.getOperation());
        Element e = DomUtil.getFirstChildElement(doc);
        assertEquals("http://example.org/Person", e.getNamespaceURI()); 
        assertEquals("DeletePerson", e.getLocalName());
        
        e = DomUtil.getFirstChildElement(e);
        assertEquals("http://example.org/Person", e.getNamespaceURI()); 
        assertEquals("id", e.getLocalName());
        assertEquals("312", e.getTextContent());
        
    }

    protected static Binding<?> getBinding(String wsdlResource) {
        try {
            String url = PersonHttpTest.class.getResource(wsdlResource).toString();
            WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
            DescriptionElement descElement = reader.readWSDL(url);
            Description desc = descElement.toComponent();
            Endpoint endpoint = desc.getServices()[0].getEndpoints()[0];
            Binding<?> binding = BindingFactory.createBinding(endpoint);
            assertNotNull(binding);
            return binding;
        } catch (WSDLException e) {
            throw new RuntimeException(e);
        }
    }
    
}
