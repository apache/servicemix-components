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

import org.apache.cxf.binding.jbi.JBIConstants;
import org.apache.cxf.binding.jbi.JBIFault;
import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.SoapHeader;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
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
import org.apache.cxf.service.model.SchemaInfo;
import org.apache.cxf.staxutils.PartialXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.servicemix.jbi.util.QNameUtil;
import org.apache.servicemix.soap.util.DomUtil;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class JbiInWsdl1Interceptor extends AbstractSoapInterceptor {

    private boolean useJBIWrapper = true;

    public JbiInWsdl1Interceptor(boolean useJBIWrapper) {
        super(Phase.PRE_INVOKE);
        addAfter(JbiOperationInterceptor.class.getName());
        this.useJBIWrapper = useJBIWrapper;
    }

    public void handleMessage(SoapMessage message) {
        // Ignore faults messages
        if (message.getContent(Exception.class) != null) {
            return;
        }
        Document document = DomUtil.createDocument();

        if (!useJBIWrapper) {
            
            SoapVersion soapVersion = message.getVersion();
            Element soapEnv = DomUtil.createElement(document, new QName(
                    soapVersion.getEnvelope().getNamespaceURI(), soapVersion
                            .getEnvelope().getLocalPart(), soapVersion
                            .getPrefix()));
            Element soapBody = DomUtil.createElement(soapEnv, new QName(
                    soapVersion.getBody().getNamespaceURI(), soapVersion
                            .getBody().getLocalPart(), soapVersion
                            .getPrefix()));
            soapEnv.appendChild(soapBody);
            Element body = getBodyElement(message);
            
            if (body != null) {
                soapBody.appendChild(soapBody.getOwnerDocument().importNode(body,
                    true));
            }
        } else {

            BindingOperationInfo wsdlOperation = getOperation(message);
            BindingMessageInfo wsdlMessage = !isRequestor(message) ? wsdlOperation
                    .getInput()
                    : wsdlOperation.getOutput();

            document = DomUtil.createDocument();
            Element root = DomUtil.createElement(document,
                    JbiConstants.WSDL11_WRAPPER_MESSAGE);
            String typeNamespace = wsdlMessage.getMessageInfo().getName()
                    .getNamespaceURI();
            if (typeNamespace == null || typeNamespace.length() == 0) {
                throw new IllegalArgumentException(
                        "messageType namespace is null or empty");
            }
            root
                    .setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":"
                            + JbiConstants.WSDL11_WRAPPER_MESSAGE_PREFIX,
                            typeNamespace);
            String typeLocalName = wsdlMessage.getMessageInfo().getName()
                    .getLocalPart();
            if (typeLocalName == null || typeLocalName.length() == 0) {
                throw new IllegalArgumentException(
                        "messageType local name is null or empty");
            }
            root.setAttribute(JbiConstants.WSDL11_WRAPPER_TYPE,
                    JbiConstants.WSDL11_WRAPPER_MESSAGE_PREFIX + ":"
                            + typeLocalName);
            String messageName = wsdlMessage.getMessageInfo().getName()
                    .getLocalPart();
            root.setAttribute(JbiConstants.WSDL11_WRAPPER_NAME, messageName);
            root.setAttribute(JbiConstants.WSDL11_WRAPPER_VERSION, "1.0");

            SoapBindingInfo binding = (SoapBindingInfo) message.getExchange()
                    .get(Endpoint.class).getEndpointInfo().getBinding();
            String style = binding.getStyle(wsdlOperation.getOperationInfo());
            if (style == null) {
                style = binding.getStyle();
            }

            Element body = getBodyElement(message);
            if (body == null) {
                return;
            }

            if (body.getLocalName().equals("Fault")) {
                handleJBIFault(message, body);
                return;
            }
            List<SoapHeaderInfo> headers = wsdlMessage
                    .getExtensors(SoapHeaderInfo.class);
            List<Header> headerElement = message.getHeaders();
            List<Object> parts = new ArrayList<Object>();
            for (MessagePartInfo part : wsdlMessage.getMessageParts()) {
                if ("document".equals(style)) {
                    parts.add(body);
                } else /* rpc-style */ {
                    // SOAP:Body element is the operation name, children are
                    // operation parameters

                    Element param = DomUtil.getFirstChildElement(body);
                    boolean found = false;
                    while (param != null) {
                        if (part.getName().getLocalPart().equals(
                                param.getLocalName())) {
                            found = true;
                            parts.add(wrapNodeList(param.getChildNodes()));
                            break;
                        }
                        param = DomUtil.getNextSiblingElement(param);
                    }
                    if (!found) {
                        throw new Fault(new Exception("Missing part '"
                                + part.getName() + "'"));
                    }
                }
            }
            processHeader(message, headers, headerElement, parts);
            for (Object part : parts) {
                if (part instanceof Node) {
                    addPart(root, (Node) part);
                } else if (part instanceof NodeList) {
                    addPart(root, (NodeList) part);
                } else if (part instanceof SoapHeader) {
                    addPart(root, (Node) ((SoapHeader) part).getObject());
                }
            }
        }
        message.setContent(Source.class, new DOMSource(document));
    }

    private void processHeader(SoapMessage message,
            List<SoapHeaderInfo> headers, List<Header> headerElement,
            List<Object> parts) {
        if (headers != null) {
            for (SoapHeaderInfo header : headers) {
                MessagePartInfo part = header.getPart();
                Header param = findHeader(headerElement, part);
                int idx = part.getIndex();
                QName element = part.getElementQName();
                Header hdr = getHeaderElement(message, element);
                if (hdr == null) {
                    throw new Fault(new Exception(
                            "Missing required header element: "
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
    }

    private void handleJBIFault(SoapMessage message, Element soapFault) {
        Document doc = DomUtil.createDocument();
        Element jbiFault = DomUtil.createElement(doc, new QName(
                JBIConstants.NS_JBI_BINDING, JBIFault.JBI_FAULT_ROOT));
        Node jbiFaultDetail = null;
        if (message.getVersion() instanceof Soap11) {
            jbiFaultDetail = doc.importNode(soapFault.getElementsByTagName(
                "detail").item(0).getFirstChild(), true);
        } else {
            jbiFaultDetail = doc.importNode(soapFault.getElementsByTagName(
                    "soap:Detail").item(0).getFirstChild(), true);
        }
        SchemaInfo schemaInfo = 
            getOperation(message).getBinding().getService().getSchema(jbiFaultDetail.getNamespaceURI());
        if (!schemaInfo.isElementFormQualified()) {
            //that's unquailied fault
            jbiFaultDetail = addEmptyDefaultTns((Element)jbiFaultDetail);
        }
        jbiFault.appendChild(jbiFaultDetail);
        message.setContent(Source.class, new DOMSource(doc));
        message.put("jbiFault", true);
    }

    private Element addEmptyDefaultTns(Element ret) {
        
        if (!ret.hasAttribute("xmlns")) {
            ret.setAttribute("xmlns", "");
        }
        NodeList nodes = ret.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element) {
                Element ele = (Element) nodes.item(i);
                ele = addEmptyDefaultTns(ele);

            }
        }
        return ret;
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
        BindingOperationInfo operation = message.getExchange().get(
                BindingOperationInfo.class);
        if (operation == null) {
            throw new Fault(
                    new Exception("Operation not bound on this message"));
        }
        return operation;
    }

    /**
     * Extract the content as DOM element
     */
    protected Element getBodyElement(SoapMessage message) {
        try {
            XMLStreamReader xmlReader = message
                    .getContent(XMLStreamReader.class);
            XMLStreamReader filteredReader = new PartialXMLStreamReader(
                    xmlReader, message.getVersion().getBody());
            return StaxUtils.read(filteredReader).getDocumentElement();
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
                return findHeader(headerElement, mpi);
            }
        }
        return null;
    }

    /**
     * Add a jbi:part to a normalized message document
     */
    private static void addPart(Element parent, Node partValue) {
        Element element = DomUtil.createElement(parent,
                JbiConstants.WSDL11_WRAPPER_PART);
        element.appendChild(element.getOwnerDocument().importNode(partValue,
                true));
    }

    /**
     * Add a jbi:part to a normalized message document
     */
    private static void addPart(Element parent, NodeList nodes) {
        Element element = DomUtil.createElement(parent,
                JbiConstants.WSDL11_WRAPPER_PART);
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            element.appendChild(element.getOwnerDocument().importNode(node,
                    true));
        }
    }

    private static Header findHeader(List<Header> headerElement,
            MessagePartInfo mpi) {
        Header param = null;
        if (headerElement != null) {
            QName name = mpi.getConcreteName();
            for (Header header : headerElement) {
                if (header.getName().getNamespaceURI() != null
                        && header.getName().getNamespaceURI().equals(
                                name.getNamespaceURI())
                        && header.getName().getLocalPart() != null
                        && header.getName().getLocalPart().equals(
                                name.getLocalPart())) {
                    param = header;
                }
            }
        }
        return param;
    }
    
    protected boolean isRequestor(Message message) {
        return Boolean.TRUE.equals(message.get(Message.REQUESTOR_ROLE));
    }
}
