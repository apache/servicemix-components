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
package org.apache.servicemix.cxfbc.interceptors;


import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.model.SoapHeaderInfo;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.PartialXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.servicemix.jbi.jaxp.StaxSource;
import org.apache.servicemix.jbi.util.QNameUtil;
import org.apache.servicemix.soap.util.DomUtil;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class JbiInWsdl1Interceptor extends AbstractSoapInterceptor {

    public JbiInWsdl1Interceptor() {
        super(Phase.UNMARSHAL);
        addAfter(JbiOperationInterceptor.class.getName());
    }
    
    public void handleMessage(SoapMessage message) {
        // Ignore faults messages
        if (message.getContent(Exception.class) != null) {
            return;
        }
        // Check if we should not use the JBI wrapper
        if (message.get(JbiConstants.USE_JBI_WRAPPER) instanceof Boolean && ((Boolean) message.get(JbiConstants.USE_JBI_WRAPPER)) == false) {
            XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
            if (xmlReader != null) {
                message.setContent(Source.class, new StaxSource(xmlReader));
            }
            return;
        }
        
        BindingOperationInfo wsdlOperation = getOperation(message);
        if (wsdlOperation.getUnwrappedOperation() != null) {
            wsdlOperation = wsdlOperation.getUnwrappedOperation();
        }
        BindingMessageInfo wsdlMessage = !isRequestor(message) ? wsdlOperation.getInput() : wsdlOperation.getOutput();

        Document document = DomUtil.createDocument();
        Element root = DomUtil.createElement(document, JbiConstants.WSDL11_WRAPPER_MESSAGE);
        String typeNamespace = wsdlMessage.getMessageInfo().getName().getNamespaceURI();
        if (typeNamespace == null || typeNamespace.length() == 0) {
            throw new IllegalArgumentException("messageType namespace is null or empty");
        }
        root.setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + JbiConstants.WSDL11_WRAPPER_MESSAGE_PREFIX,
                          typeNamespace);
        String typeLocalName = wsdlMessage.getMessageInfo().getName().getLocalPart();
        if (typeLocalName == null || typeLocalName.length() == 0) {
            throw new IllegalArgumentException("messageType local name is null or empty");
        }
        root.setAttribute(JbiConstants.WSDL11_WRAPPER_TYPE, JbiConstants.WSDL11_WRAPPER_MESSAGE_PREFIX + ":" + typeLocalName);
        String messageName = wsdlMessage.getMessageInfo().getName().getLocalPart();
        root.setAttribute(JbiConstants.WSDL11_WRAPPER_NAME, messageName);
        root.setAttribute(JbiConstants.WSDL11_WRAPPER_VERSION, "1.0");
        
        SoapBindingInfo binding = (SoapBindingInfo) message.getExchange().get(Endpoint.class).getEndpointInfo().getBinding();
        String style = binding.getStyle(wsdlOperation.getOperationInfo());
        if (style == null) {
            style = binding.getStyle();
        }
        Element body = getBodyElement(message);
        List<SoapHeaderInfo> headers = wsdlMessage.getExtensors(SoapHeaderInfo.class);
        List<Header> headerElement = message.getHeaders();
        List<Object> parts = new ArrayList<Object>();
        for (MessagePartInfo part : wsdlMessage.getMessageParts()) {
            if ("document".equals(style)) {
                parts.add(body);
            } else /* rpc-style */ {
                // SOAP:Body element is the operation name, children are operation parameters
                Element param = DomUtil.getFirstChildElement(body);
                boolean found = false;
                while (param != null) {
                    if (part.getName().equals(new QName(param.getNamespaceURI(), param.getLocalName()))) {
                        found = true;
                        parts.add(wrapNodeList(param.getChildNodes()));
                        break;
                    }
                    param = DomUtil.getNextSiblingElement(param);
                }
                if (!found) {
                    throw new Fault(new Exception("Missing part '" + part.getName() + "'"));
                }
            }
        }
        if (headers != null) {
            for (SoapHeaderInfo header : headers) {
                MessagePartInfo part = header.getPart();
                Header param = findHeader(headerElement, part);
                int idx = part.getIndex();
                QName element = part.getElementQName();
                Header hdr = getHeaderElement(message, element);
                if (hdr == null) {
                    throw new Fault(new Exception("Missing required header element: "
                                + QNameUtil.toString(element)));
                }
                if (idx > parts.size()) {
                    parts.add(param);
                } else if (idx == -1) {
                    parts.add(0, param);
                } else {
                    parts.add(idx, param);
                }
            }
        }
        for (Object part : parts) {
            if (part instanceof Node) {
                addPart(root, (Node) part);
            } else if (part instanceof NodeList) {
                addPart(root, (NodeList) part);
            } else if (part instanceof SoapHeader) {
            	addPart(root, (Node)((SoapHeader)part).getObject());
            }
        }
        
        message.setContent(Source.class, new DOMSource(document));
    }

    private NodeList wrapNodeList(final NodeList childNodes) {
        return new NodeList() {
            public int getLength() {
                return childNodes.getLength();
            }
            public Node item(int index) {
                return childNodes.item(index);
            }
        };
    }

    protected BindingOperationInfo getOperation(Message message) {
        BindingOperationInfo operation = message.getExchange().get(BindingOperationInfo.class);
        if (operation == null) {
            throw new Fault(new Exception("Operation not bound on this message"));
        }
        return operation;
    }

    /**
     * Extract the content as DOM element
     */
    protected Element getBodyElement(SoapMessage message) {
        try {
            XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
            XMLStreamReader filteredReader = new PartialXMLStreamReader(xmlReader, message.getVersion().getBody());
            Document doc = StaxUtils.read(filteredReader);
            return doc.getDocumentElement();
        } catch (XMLStreamException e) {
            throw new Fault(e);
        }
    }
    
    protected Header getHeaderElement(SoapMessage message, QName name) {
        Exchange exchange = message.getExchange();
        BindingOperationInfo bop = exchange.get(BindingOperationInfo.class);
        if (bop.isUnwrapped()) {
            bop = bop.getWrappedOperation();
        }
        boolean client = isRequestor(message);
        BindingMessageInfo bmi = client ? bop.getOutput() : bop.getInput();
        if (bmi == null) {
            // one way operation.
            return null;
        }
        List<SoapHeaderInfo> headers = bmi.getExtensors(SoapHeaderInfo.class);
        if (headers == null || headers.size() == 0) {
            return null;
        }
        List<Header> headerElement = message.getHeaders();
        for (SoapHeaderInfo header : headers) {
            if (header.getPart().getElementQName().equals(name)) {
                MessagePartInfo mpi = header.getPart();
                Header param = findHeader(headerElement, mpi);
                return param;
            }
        }
        return null;
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

    private static Header findHeader(List<Header> headerElement, MessagePartInfo mpi) {
        Header param = null;
        if (headerElement != null) {
            QName name = mpi.getConcreteName();
            for (Header header : headerElement) {
                if (header.getName().getNamespaceURI() != null 
                        && header.getName().getNamespaceURI().equals(name.getNamespaceURI())
                        && header.getName().getLocalPart() != null
                        && header.getName().getLocalPart().equals(name.getLocalPart())) {
                    param = header;
                }
            }
        }
        return param;
    }
}
