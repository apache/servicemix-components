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

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.servicemix.soap.api.Fault;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.api.model.Operation;
import org.apache.servicemix.soap.bindings.soap.SoapFault;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapBinding;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapMessage;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapOperation;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapPart;
import org.apache.servicemix.soap.core.AbstractInterceptor;
import org.apache.servicemix.soap.util.DomUtil;
import org.apache.servicemix.soap.util.QNameUtil;
import org.apache.servicemix.soap.util.stax.StaxUtil;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class JbiInWsdl1Interceptor extends AbstractInterceptor {

    private final boolean server;
    
    public JbiInWsdl1Interceptor(boolean server) {
        this.server = server;
    }
    
    public void handleMessage(Message message) {
        // Ignore faults messages
        if (message.getContent(Exception.class) != null) {
            return;
        }
        // Check if we should not use the JBI wrapper
        if (message.get(JbiConstants.USE_JBI_WRAPPER) instanceof Boolean && ((Boolean) message.get(JbiConstants.USE_JBI_WRAPPER)) == false) {
            XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
            if (xmlReader != null) {
                message.setContent(Source.class, StaxUtil.createSource(xmlReader));
            }
            return;
        }
        
        Wsdl1SoapOperation wsdlOperation = getOperation(message);
        Wsdl1SoapMessage wsdlMessage = server ? wsdlOperation.getInput() : wsdlOperation.getOutput();

        Document document = DomUtil.createDocument();
        Element root = DomUtil.createElement(document, JbiConstants.WSDL11_WRAPPER_MESSAGE);
        String typeNamespace = wsdlMessage.getName().getNamespaceURI();
        if (typeNamespace == null || typeNamespace.length() == 0) {
            throw new IllegalArgumentException("messageType namespace is null or empty");
        }
        root.setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + JbiConstants.WSDL11_WRAPPER_MESSAGE_PREFIX,
                          typeNamespace);
        String typeLocalName = wsdlMessage.getName().getLocalPart();
        if (typeLocalName == null || typeLocalName.length() == 0) {
            throw new IllegalArgumentException("messageType local name is null or empty");
        }
        root.setAttribute(JbiConstants.WSDL11_WRAPPER_TYPE, JbiConstants.WSDL11_WRAPPER_MESSAGE_PREFIX + ":" + typeLocalName);
        String messageName = wsdlMessage.getMessageName();
        root.setAttribute(JbiConstants.WSDL11_WRAPPER_NAME, messageName);
        root.setAttribute(JbiConstants.WSDL11_WRAPPER_VERSION, "1.0");
        
        Element body = getBodyElement(message);
        for (Wsdl1SoapPart part : wsdlMessage.getParts()) {
            if (part.isBody()) {
                if (wsdlOperation.getStyle() == Wsdl1SoapBinding.Style.DOCUMENT) {
                    addPart(root, body);
                } else /* rpc-style */ {
                    // SOAP:Body element is the operation name, children are operation parameters
                    Element param = DomUtil.getFirstChildElement(body);
                    boolean found = false;
                    while (param != null) {
                        if (part.getName().equals(param.getLocalName())) {
                            found = true;
                            addPart(root, param.getChildNodes());
                        }
                        param = DomUtil.getNextSiblingElement(param);
                    }
                    if (!found) {
                        throw new SoapFault(SoapFault.SENDER, "Missing part '" + part.getName() + "'");
                    }
                }
            } else {
                QName element = part.getElement();
                DocumentFragment header = message.getSoapHeaders().remove(element);
                if (header == null) {
                    throw new SoapFault(SoapFault.SENDER, "Missing required header element: "
                                + QNameUtil.toString(element));
                }
                addPart(root, header.getChildNodes());
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

    /**
     * Extract the content as a jaxp Source
     */
    protected Element getBodyElement(Message message) {
        XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
        return xmlReader != null ? StaxUtil.createElement(xmlReader) : null; 
    }
    
    /**
     * Add a jbi:part to a normalized message document
     */
    private static void addPart(Element parent, Node partValue) {
        Element element = DomUtil.createElement(parent, JbiConstants.WSDL11_WRAPPER_PART);
        element.appendChild(element.getOwnerDocument().importNode(partValue, true));
    }

    /**
     * Add a jbi:part to a normalized message document
     */
    private static void addPart(Element parent, NodeList nodes) {
        Element element = DomUtil.createElement(parent, JbiConstants.WSDL11_WRAPPER_PART);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            element.appendChild(element.getOwnerDocument().importNode(node, true));
        }
    }

}
