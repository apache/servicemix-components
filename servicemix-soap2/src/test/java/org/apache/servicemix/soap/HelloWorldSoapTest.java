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
import java.io.OutputStream;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.xml.WSDLReader;
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
import org.apache.servicemix.soap.api.model.Operation;
import org.apache.servicemix.soap.bindings.soap.Soap11;
import org.apache.servicemix.soap.bindings.soap.SoapFault;
import org.apache.servicemix.soap.bindings.soap.SoapVersion;
import org.apache.servicemix.soap.core.MessageImpl;
import org.apache.servicemix.soap.core.PhaseInterceptorChain;
import org.apache.servicemix.soap.interceptors.jbi.JbiConstants;
import org.apache.servicemix.soap.util.DomUtil;
import org.apache.servicemix.soap.wsdl.BindingFactory;
import org.apache.servicemix.soap.wsdl.WSDLUtils;
import org.apache.servicemix.soap.wsdl.validator.WSIBPValidator;
import org.apache.servicemix.tck.mock.MockExchangeFactory;
import org.apache.servicemix.tck.mock.MockMessageExchange;

public class HelloWorldSoapTest extends TestCase {
    private static transient Log log = LogFactory.getLog(HelloWorldSoapTest.class);

//    static {
//        String conf = System.getProperty("log4j.configuration");
//        System.err.println("Configuring log4j with: " + conf);
//        if (conf != null) {
//            PropertyConfigurator.configure(conf);
//        }
//    }
    
    public void testDocLitInput() throws Exception {
        ByteArrayOutputStream baos;

        Binding<?> binding = getBinding("HelloWorld-DOC.wsdl");
        PhaseInterceptorChain phaseIn = new PhaseInterceptorChain();
        phaseIn.add(binding.getInterceptors(Phase.ServerIn));
        
        Message msg = new MessageImpl();
        msg.put(Binding.class, binding);
        msg.setContent(InputStream.class, getClass().getResourceAsStream("HelloWorld-DOC-Input.xml"));
        msg.put(MessageExchangeFactory.class, new MockExchangeFactory());
        phaseIn.doIntercept(msg);

        NormalizedMessage nm = msg.getContent(NormalizedMessage.class);
        Document doc = DomUtil.parse(nm.getContent());
        baos = new ByteArrayOutputStream();
        DomUtil.getTransformerFactory().newTransformer().transform(nm.getContent(), new StreamResult(baos));
        log.info(baos.toString());
        
        // check jbi message element
        Element root = DomUtil.getFirstChildElement(doc);
        assertNotNull(root);
        assertEquals(JbiConstants.WSDL11_WRAPPER_NAMESPACE, root.getNamespaceURI()); 
        assertEquals("message", root.getLocalName());
        assertEquals("Hello", root.getAttribute("name"));

        // check body part
        Element part = DomUtil.getFirstChildElement(root);
        assertNotNull(part);
        assertEquals(JbiConstants.WSDL11_WRAPPER_NAMESPACE, part.getNamespaceURI()); 
        assertEquals("part", part.getLocalName());

        // check body element
        Element hello = DomUtil.getFirstChildElement(part);
        assertNotNull(hello);
        assertEquals("uri:HelloWorld", hello.getNamespaceURI()); 
        assertEquals("HelloRequest", hello.getLocalName());
        
        // check body content
        Element e = DomUtil.getFirstChildElement(hello);
        assertNotNull(e);
        assertEquals("uri:HelloWorld", e.getNamespaceURI()); 
        assertEquals("text", e.getLocalName());
        assertEquals("hello", e.getTextContent());

        // check header part
        Element part2 = DomUtil.getNextSiblingElement(part);
        assertNotNull(part2);
        assertEquals(JbiConstants.WSDL11_WRAPPER_NAMESPACE, part2.getNamespaceURI()); 
        assertEquals("part", part2.getLocalName());

        // check header element
        Element header = DomUtil.getFirstChildElement(part2);
        assertNotNull(header);
        assertEquals("uri:HelloWorld", header.getNamespaceURI()); 
        assertEquals("HelloHeader", header.getLocalName());
        
        // check header content
        e = DomUtil.getFirstChildElement(header);
        assertNotNull(e);
        assertEquals("uri:HelloWorld", e.getNamespaceURI()); 
        assertEquals("id", e.getLocalName());
        assertEquals("1234567890", e.getTextContent());
       
        PhaseInterceptorChain phaseOut = new PhaseInterceptorChain();
        phaseOut.add(binding.getInterceptors(Phase.ClientOut));
        baos = new ByteArrayOutputStream();

        Message msgOut = new MessageImpl();
        msgOut.put(Binding.class, binding);
        msgOut.setContent(OutputStream.class, baos);
        msgOut.setContent(MessageExchange.class, msg.getContent(MessageExchange.class));
        msgOut.setContent(NormalizedMessage.class, nm);
        msgOut.put(SoapVersion.class, msg.get(SoapVersion.class));
        phaseOut.doIntercept(msgOut);
        log.info(baos.toString());
        
        Document node = DomUtil.parse(new ByteArrayInputStream(baos.toByteArray()));
        
        Element envelope = DomUtil.getFirstChildElement(node);
        Element headers = DomUtil.getFirstChildElement(envelope);
        Element body = DomUtil.getNextSiblingElement(headers);
        
        // check body element
        hello = DomUtil.getFirstChildElement(body);
        assertNotNull(hello);
        assertEquals("uri:HelloWorld", hello.getNamespaceURI()); 
        assertEquals("HelloRequest", hello.getLocalName());
        
        // check body content
        e = DomUtil.getFirstChildElement(hello);
        assertNotNull(e);
        assertEquals("uri:HelloWorld", e.getNamespaceURI()); 
        assertEquals("text", e.getLocalName());
        assertEquals("hello", e.getTextContent());
        
        // check header
        header = DomUtil.getFirstChildElement(headers);
        assertNotNull(header);
        assertNull(DomUtil.getNextSiblingElement(header));
        assertEquals("uri:HelloWorld", header.getNamespaceURI()); 
        assertEquals("HelloHeader", header.getLocalName());
        
        // check header content
        e = DomUtil.getFirstChildElement(header);
        assertNotNull(e);
        assertEquals("uri:HelloWorld", e.getNamespaceURI()); 
        assertEquals("id", e.getLocalName());
        assertEquals("1234567890", e.getTextContent());
    }

    public void testDocLitOutput() throws Exception {
        ByteArrayOutputStream baos;

        Binding<?> binding = getBinding("HelloWorld-DOC.wsdl");
        PhaseInterceptorChain phaseIn = new PhaseInterceptorChain();
        phaseIn.add(binding.getInterceptors(Phase.ClientIn));
        
        Message msg = new MessageImpl();
        MessageExchange me = new MockMessageExchange();
        msg.put(Binding.class, binding);
        msg.put(Operation.class, binding.getOperations().iterator().next());
        msg.setContent(InputStream.class, getClass().getResourceAsStream("HelloWorld-DOC-Output.xml"));
        msg.setContent(MessageExchange.class, me);
        phaseIn.doIntercept(msg);

        NormalizedMessage nm = msg.getContent(NormalizedMessage.class);
        Document doc = DomUtil.parse(nm.getContent());
        baos = new ByteArrayOutputStream();
        DomUtil.getTransformerFactory().newTransformer().transform(nm.getContent(), new StreamResult(baos));
        log.info(baos.toString());
        
        // check jbi message element
        Element root = DomUtil.getFirstChildElement(doc);
        assertNotNull(msg);
        assertEquals(JbiConstants.WSDL11_WRAPPER_NAMESPACE, root.getNamespaceURI()); 
        assertEquals("message", root.getLocalName());
        assertEquals("HelloResponse", root.getAttribute("name"));

        // check body part
        Element part = DomUtil.getFirstChildElement(root);
        assertNotNull(part);
        assertEquals(JbiConstants.WSDL11_WRAPPER_NAMESPACE, part.getNamespaceURI()); 
        assertEquals("part", part.getLocalName());

        // check body element
        Element hello = DomUtil.getFirstChildElement(part);
        assertNotNull(hello);
        assertEquals("uri:HelloWorld", hello.getNamespaceURI()); 
        assertEquals("HelloResponse", hello.getLocalName());
        
        // check body content
        Element e = DomUtil.getFirstChildElement(hello);
        assertNotNull(e);
        assertEquals("uri:HelloWorld", e.getNamespaceURI()); 
        assertEquals("text", e.getLocalName());
        assertEquals("hello", e.getTextContent());

        PhaseInterceptorChain phaseOut = new PhaseInterceptorChain();
        phaseOut.add(binding.getInterceptors(Phase.ServerOut));

        baos = new ByteArrayOutputStream();
        Message msgOut = new MessageImpl();
        msgOut.put(Binding.class, binding);
        msgOut.put(Operation.class, binding.getOperations().iterator().next());
        msgOut.setContent(OutputStream.class, baos);
        msgOut.setContent(MessageExchange.class, msg.getContent(MessageExchange.class));
        msgOut.setContent(NormalizedMessage.class, nm);
        msgOut.put(SoapVersion.class, msg.get(SoapVersion.class));
        phaseOut.doIntercept(msgOut);
        log.info(baos.toString());
        
        Document node = DomUtil.parse(new ByteArrayInputStream(baos.toByteArray()));
        Element envelope = DomUtil.getFirstChildElement(node);
        Element body = DomUtil.getFirstChildElement(envelope);
        
        // check body element
        hello = DomUtil.getFirstChildElement(body);
        assertNotNull(hello);
        assertEquals("uri:HelloWorld", hello.getNamespaceURI()); 
        assertEquals("HelloResponse", hello.getLocalName());
        
        // check body content
        e = DomUtil.getFirstChildElement(hello);
        assertNotNull(e);
        assertEquals("uri:HelloWorld", e.getNamespaceURI()); 
        assertEquals("text", e.getLocalName());
        assertEquals("hello", e.getTextContent());
    }
    
    public void testDocLitFault() throws Exception {
        ByteArrayOutputStream baos;

        Binding<?> binding = getBinding("HelloWorld-DOC.wsdl");
        PhaseInterceptorChain phaseIn = new PhaseInterceptorChain();
        phaseIn.add(binding.getInterceptors(Phase.ClientIn));
        
        Message msg = new MessageImpl();
        msg.put(Binding.class, binding);
        msg.put(Operation.class, binding.getOperations().iterator().next());
        msg.setContent(InputStream.class, getClass().getResourceAsStream("HelloWorld-DOC-Fault.xml"));
        msg.setContent(MessageExchange.class, new MockMessageExchange());
        phaseIn.doIntercept(msg);

        NormalizedMessage nm = msg.getContent(NormalizedMessage.class);
        Document doc = DomUtil.parse(nm.getContent());
        baos = new ByteArrayOutputStream();
        DomUtil.getTransformerFactory().newTransformer().transform(nm.getContent(), new StreamResult(baos));
        log.info(baos.toString());
        
        Element root = DomUtil.getFirstChildElement(doc);
        assertNotNull(root);
        assertEquals("uri:HelloWorld", root.getNamespaceURI()); 
        assertEquals("HelloFault", root.getLocalName());

        PhaseInterceptorChain phaseOut = new PhaseInterceptorChain();
        phaseOut.add(binding.getInterceptors(Phase.ServerOut));

        baos = new ByteArrayOutputStream();
        Message msgOut = new MessageImpl();
        msgOut.put(Binding.class, binding);
        msgOut.setContent(OutputStream.class, baos);
        msgOut.setContent(MessageExchange.class, msg.getContent(MessageExchange.class));
        msgOut.setContent(NormalizedMessage.class, nm);
        msgOut.put(SoapVersion.class, msg.get(SoapVersion.class));
        
        try {
            phaseOut.doIntercept(msgOut);
            fail("A soap fault should have been thrown");
        } catch (SoapFault fault) {
            PhaseInterceptorChain phaseOutFault = new PhaseInterceptorChain();
            phaseOutFault.add(binding.getInterceptors(Phase.ServerOutFault));
            phaseOutFault.doIntercept(msgOut);
        }

        log.info(baos.toString());
        Document node = DomUtil.parse(new ByteArrayInputStream(baos.toByteArray()));
        
        Element envelope = DomUtil.getFirstChildElement(node);
        assertEquals(Soap11.getInstance().getEnvelope(), DomUtil.getQName(envelope));
        Element body = DomUtil.getFirstChildElement(envelope);
        assertEquals(Soap11.getInstance().getBody(), DomUtil.getQName(body));
        Element fault = DomUtil.getFirstChildElement(body);
        assertEquals(Soap11.getInstance().getFault(), DomUtil.getQName(fault));
        
        Element faultCode = DomUtil.getFirstChildElement(fault);
        Element faultString = DomUtil.getNextSiblingElement(faultCode);
        Element faultDetail = DomUtil.getNextSiblingElement(faultString);
        Element child = DomUtil.getFirstChildElement(faultDetail);
        assertEquals(new QName("uri:HelloWorld", "HelloFault"), DomUtil.getQName(child));
    }
    
    public void testRpcLitInput() throws Exception {
        ByteArrayOutputStream baos;

        Binding<?> binding = getBinding("HelloWorld-RPC.wsdl");
        PhaseInterceptorChain phaseIn = new PhaseInterceptorChain();
        phaseIn.add(binding.getInterceptors(Phase.ServerIn));
        
        Message msg = new MessageImpl();
        msg.put(Binding.class, binding);
        msg.setContent(InputStream.class, getClass().getResourceAsStream("HelloWorld-RPC-Input.xml"));
        msg.put(MessageExchangeFactory.class, new MockExchangeFactory());
        phaseIn.doIntercept(msg);
        
        NormalizedMessage nm = msg.getContent(NormalizedMessage.class);
        Document doc = DomUtil.parse(nm.getContent());

        baos = new ByteArrayOutputStream();
        DomUtil.getTransformerFactory().newTransformer().transform(nm.getContent(), new StreamResult(baos));
        log.info(baos.toString());

        Element root = DomUtil.getFirstChildElement(doc);
        assertNotNull(msg);
        assertEquals(JbiConstants.WSDL11_WRAPPER_NAMESPACE, root.getNamespaceURI()); 
        assertEquals("message", root.getLocalName());
        assertEquals("Hello", root.getAttribute("name"));

        // check part "header1"
        Element wrapper = DomUtil.getFirstChildElement(root);
        assertNotNull(wrapper);
        assertEquals(JbiConstants.WSDL11_WRAPPER_NAMESPACE, wrapper.getNamespaceURI()); 
        assertEquals("part", wrapper.getLocalName());

        Element header1 = DomUtil.getFirstChildElement(wrapper);
        assertNotNull(header1);
        assertEquals("uri:HelloWorld", header1.getNamespaceURI()); 
        assertEquals("HelloHeader1", header1.getLocalName());

        Element id1 = DomUtil.getFirstChildElement(header1);
        assertNotNull(id1);
        assertEquals("uri:HelloWorld", id1.getNamespaceURI()); 
        assertEquals("id1", id1.getLocalName());
        assertEquals("abcdefghij", id1.getTextContent());

        // check part "header2"
        wrapper = DomUtil.getNextSiblingElement(wrapper);
        assertNotNull(wrapper);
        assertEquals(JbiConstants.WSDL11_WRAPPER_NAMESPACE, wrapper.getNamespaceURI()); 
        assertEquals("part", wrapper.getLocalName());

        Element header2 = DomUtil.getFirstChildElement(wrapper);
        assertNotNull(header2);
        assertEquals("uri:HelloWorld", header2.getNamespaceURI()); 
        assertEquals("HelloHeader2", header2.getLocalName());

        Element id2 = DomUtil.getFirstChildElement(header2);
        assertNotNull(id2);
        assertEquals("uri:HelloWorld", id2.getNamespaceURI()); 
        assertEquals("id2", id2.getLocalName());
        assertEquals("1234567890", id2.getTextContent());

        // check part "param1"
        wrapper = DomUtil.getNextSiblingElement(wrapper);
        assertNotNull(wrapper);
        assertEquals(JbiConstants.WSDL11_WRAPPER_NAMESPACE, wrapper.getNamespaceURI()); 
        assertEquals("part", wrapper.getLocalName());

        assertNull(DomUtil.getFirstChildElement(wrapper));
        assertEquals("foo", wrapper.getTextContent());

        // check part "param2"
        wrapper = DomUtil.getNextSiblingElement(wrapper);
        assertNotNull(wrapper);
        assertEquals(JbiConstants.WSDL11_WRAPPER_NAMESPACE, wrapper.getNamespaceURI()); 
        assertEquals("part", wrapper.getLocalName());

        assertNull(DomUtil.getFirstChildElement(wrapper));
        assertEquals("bar", wrapper.getTextContent());

        PhaseInterceptorChain phaseOut = new PhaseInterceptorChain();
        phaseOut.add(binding.getInterceptors(Phase.ClientOut));

        baos = new ByteArrayOutputStream();
        Message msgOut = new MessageImpl();
        msgOut.put(Binding.class, binding);
        msgOut.setContent(OutputStream.class, baos);
        msgOut.setContent(MessageExchange.class, msg.getContent(MessageExchange.class));
        msgOut.setContent(NormalizedMessage.class, nm);
        msgOut.put(SoapVersion.class, msg.get(SoapVersion.class));
        phaseOut.doIntercept(msgOut);
        log.info(baos.toString());
        
        Document node = DomUtil.parse(new ByteArrayInputStream(baos.toByteArray()));
        Element envelope = DomUtil.getFirstChildElement(node);
        Element headers = DomUtil.getFirstChildElement(envelope);
        Element body = DomUtil.getNextSiblingElement(headers);

        // check operation wrapper element
        wrapper = DomUtil.getFirstChildElement(body);
        assertNotNull(wrapper);
        assertEquals("uri:HelloWorld", wrapper.getNamespaceURI()); 
        assertEquals("Hello", wrapper.getLocalName());
        
        // check "param1" part
        Element param1 = DomUtil.getFirstChildElement(wrapper);
        assertNotNull(param1);
        assertEquals("uri:HelloWorld", param1.getNamespaceURI()); 
        assertEquals("param1", param1.getLocalName());
        assertNull(DomUtil.getFirstChildElement(param1));
        assertEquals("foo", param1.getTextContent() );

        // check "param2" part
        Element param2 = DomUtil.getNextSiblingElement(param1);
        assertNotNull(param2);
        assertEquals("uri:HelloWorld", param1.getNamespaceURI()); 
        assertEquals("param2", param2.getLocalName());
        assertNull(DomUtil.getFirstChildElement(param2));
        assertEquals("bar", param2.getTextContent() );
    }
    
    public void testRpcLitOutput() throws Exception {
        ByteArrayOutputStream baos;

        Binding<?> binding = getBinding("HelloWorld-RPC.wsdl");
        PhaseInterceptorChain phaseIn = new PhaseInterceptorChain();
        phaseIn.add(binding.getInterceptors(Phase.ClientIn));
        
        Message msg = new MessageImpl();
        MessageExchange me = new MockMessageExchange();
        msg.put(Binding.class, binding);
        msg.put(Operation.class, binding.getOperations().iterator().next());
        msg.setContent(InputStream.class, getClass().getResourceAsStream("HelloWorld-RPC-Output.xml"));
        msg.setContent(MessageExchange.class, me);
        phaseIn.doIntercept(msg);

        NormalizedMessage nm = msg.getContent(NormalizedMessage.class);
        Document doc = DomUtil.parse(nm.getContent());

        baos = new ByteArrayOutputStream();
        DomUtil.getTransformerFactory().newTransformer().transform(nm.getContent(), new StreamResult(baos));
        log.info(baos.toString());
        
        // check jbi message element
        Element root = DomUtil.getFirstChildElement(doc);
        assertNotNull(root);
        assertEquals(JbiConstants.WSDL11_WRAPPER_NAMESPACE, root.getNamespaceURI()); 
        assertEquals("message", root.getLocalName());
        assertEquals("HelloResponse", root.getAttribute("name"));

        // check part wrapper
        Element part = DomUtil.getFirstChildElement(root);
        assertNotNull(part);
        assertEquals(JbiConstants.WSDL11_WRAPPER_NAMESPACE, part.getNamespaceURI()); 
        assertEquals("part", part.getLocalName());

        // check part content
        assertTrue( "Unexpected part element", DomUtil.getFirstChildElement(part) == null);
        assertEquals("hello", part.getTextContent());

        PhaseInterceptorChain phaseOut = new PhaseInterceptorChain();
        phaseOut.add(binding.getInterceptors(Phase.ServerOut));

        baos = new ByteArrayOutputStream();
        Message msgOut = new MessageImpl();
        msgOut.put(Binding.class, binding);
        msgOut.put(Operation.class, binding.getOperations().iterator().next());
        msgOut.setContent(OutputStream.class, baos);
        msgOut.setContent(MessageExchange.class, msg.getContent(MessageExchange.class));
        msgOut.setContent(NormalizedMessage.class, nm);
        msgOut.put(SoapVersion.class, msg.get(SoapVersion.class));
        phaseOut.doIntercept(msgOut);
        log.info(baos.toString());
        
        Document node = DomUtil.parse(new ByteArrayInputStream(baos.toByteArray()));
        Element envelope = DomUtil.getFirstChildElement(node);
        Element body = DomUtil.getFirstChildElement(envelope);
        
        // check body element
        Element hello = DomUtil.getFirstChildElement(body);
        assertNotNull(hello);
        assertEquals("uri:HelloWorld", hello.getNamespaceURI()); 
        assertEquals("HelloResponse", hello.getLocalName());
        
        // check body content
        Element e = DomUtil.getFirstChildElement(hello);
        assertNotNull(e);
        assertEquals("uri:HelloWorld", e.getNamespaceURI()); 
        assertEquals("text", e.getLocalName());
        assertEquals("hello", e.getTextContent());
    }
    
    protected Binding<?> getBinding(String wsdlResource) throws Exception {
        String url = getClass().getResource(wsdlResource).toString();
        WSDLReader reader = WSDLUtils.createWSDL11Reader(); 
        Definition def = reader.readWSDL(url);
        WSIBPValidator validator = new WSIBPValidator(def);
        if (!validator.isValid()) {
            for (String err : validator.getErrors()) {
                log.info(err);
            }
        }
        Service svc = (Service) def.getServices().values().iterator().next();
        Port port = (Port) svc.getPorts().values().iterator().next();
        Binding<?> binding = BindingFactory.createBinding(port);
        return binding;
    }
    
}
