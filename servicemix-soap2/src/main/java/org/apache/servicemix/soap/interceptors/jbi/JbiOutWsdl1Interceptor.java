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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.servicemix.soap.api.Fault;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.api.model.Operation;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapMessage;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapOperation;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapPart;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapBinding.Style;
import org.apache.servicemix.soap.core.AbstractInterceptor;
import org.apache.servicemix.soap.util.DomUtil;
import org.apache.servicemix.soap.util.QNameUtil;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class JbiOutWsdl1Interceptor extends AbstractInterceptor {

    private final boolean server;
    
    public JbiOutWsdl1Interceptor(boolean server) {
        this.server = server;
    }
    
    public void handleMessage(Message message) {
        // Ignore faults messages
        if (message.getContent(Exception.class) != null) {
            return;
        }
        // Check if we should not use the JBI wrapper
        if (message.get(JbiConstants.USE_JBI_WRAPPER) instanceof Boolean && ((Boolean) message.get(JbiConstants.USE_JBI_WRAPPER)) == false) {
            return;
        }
        Source source = message.getContent(Source.class);
        Element element = DomUtil.parse(source).getDocumentElement();
        if (!JbiConstants.WSDL11_WRAPPER_NAMESPACE.equals(element.getNamespaceURI()) ||
            !JbiConstants.WSDL11_WRAPPER_MESSAGE_LOCALNAME.equals(element.getLocalName())) {
            throw new Fault("Message wrapper element is '" + QNameUtil.toString(element)
                    + "' but expected '{" + JbiConstants.WSDL11_WRAPPER_NAMESPACE + "}message'");
        }
        List<NodeList> partsContent = new ArrayList<NodeList>();
        Element partWrapper = DomUtil.getFirstChildElement(element);
        while (partWrapper != null) {
            if (!JbiConstants.WSDL11_WRAPPER_NAMESPACE.equals(element.getNamespaceURI()) ||
                !JbiConstants.WSDL11_WRAPPER_PART_LOCALNAME.equals(partWrapper.getLocalName())) {
                throw new Fault("Unexpected part wrapper element '" + QNameUtil.toString(partWrapper)
                        + "' expected '{" + JbiConstants.WSDL11_WRAPPER_NAMESPACE + "}part'");
            }
            NodeList nodes = partWrapper.getChildNodes();
            partsContent.add(nodes);
            partWrapper = DomUtil.getNextSiblingElement(partWrapper);
        }
        
        Wsdl1SoapOperation wsdlOperation = getOperation(message);
        Wsdl1SoapMessage wsdlMessage = server ? wsdlOperation.getOutput() : wsdlOperation.getInput();
        Collection orderedParts = wsdlMessage.getParts();
        if (orderedParts.size() != partsContent.size()) {
            throw new Fault("Message contains " + partsContent.size() + " part(s) but expected "
                    + orderedParts.size() + " parts");
        }
        
        Document document = DomUtil.createDocument();
        Node body = null;
        if (wsdlOperation.getStyle() == Style.RPC) {
            body = DomUtil.createElement(document, wsdlMessage.getElementName());
        }
        int idxPart = 0;
        for (Wsdl1SoapPart part : wsdlMessage.getParts()) {
            NodeList nodes =  partsContent.get(idxPart++);
            if (part.isBody()) {
                if (wsdlOperation.getStyle() == Style.DOCUMENT) {
                    Element e = null;
                    for (int i = 0; i < nodes.getLength(); i++) {
                        Node n = nodes.item(i);
                        if (n instanceof Element) {
                            if (e != null) {
                                throw new Fault("Body part '" + part.getName() + "' contains more than one element; expected a single element.");
                            } else {
                                e = (Element) n;
                            }
                        }
                    }
                    if (e == null) {
                        throw new Fault("Body part '" + part.getName() + "' contains no element; expected a single element.");
                    }
                    if (!wsdlMessage.getElementName().equals(DomUtil.getQName(e))) {
                        throw new Fault("Body part '" + part.getName() + "' element '" + DomUtil.getQName(e) + " doesn't match expected element '" + QNameUtil.toString(wsdlMessage.getElementName()) + "'");
                    }
                    body = document.importNode(e, true);
                    document.appendChild(body);
                } else /* rpc-style */ {
                    for (int j = 0; j < nodes.getLength(); j++) {
                        /* note: we don't do any validation on RPC-style part value types */
                        Element e = document.createElementNS(wsdlMessage.getElementName().getNamespaceURI(), part.getName());
                        body.appendChild(e);
                        e.appendChild(document.importNode(nodes.item(j), true));
                    }
                }
            } else {
                DocumentFragment frag = document.createDocumentFragment();
                Element e = null;
                for (int i = 0; i < nodes.getLength(); i++) {
                    Node n = nodes.item(i);
                    if (n instanceof Element) {
                        if (e != null) {
                            throw new Fault("Header part '" + part.getName() + "' contains more than one element; expected a single element.");
                        } else {
                            e = (Element) n;
                        }
                    }
                }
                if (e == null) {
                    throw new Fault("Header part '" + part.getName() + "' contains no element; expected a single element.");
                }
                QName headerName = part.getElement();
                if (!headerName.equals(DomUtil.getQName(e))) {
                    throw new Fault("Header part '" + part.getName() + "' element '" + DomUtil.getQName(e) + " doesn't match expected element '" + QNameUtil.toString(headerName) + "'");
                }
                for (int j=0; j<nodes.getLength(); j++) {
                    frag.appendChild(document.importNode(nodes.item(j), true));
                }
                message.getSoapHeaders().put(headerName, frag);
            }
        }
        message.setContent(Source.class, new DOMSource(document));
    }

    protected Wsdl1SoapOperation getOperation(Message message) {
        Operation operation = message.get(Operation.class);
        if (operation == null) {
            throw new Fault("Operation not bound on this message");
        }
        if (operation instanceof Wsdl1SoapOperation == false) {
            throw new Fault("Message is not bound to a WSDL 1.1 SOAP operation");
        }
        return (Wsdl1SoapOperation) operation;
    }
    
    protected Wsdl1SoapMessage getMessage(Message message) {
        org.apache.servicemix.soap.api.model.Message msg = message.get(org.apache.servicemix.soap.api.model.Message.class);
        if (msg == null) {
            throw new Fault("Message not bound on this message");
        }
        if (msg instanceof Wsdl1SoapMessage == false) {
            throw new Fault("Message is not bound to a WSDL 1.1 SOAP operation message");
        }
        return (Wsdl1SoapMessage) msg;
    }

}
