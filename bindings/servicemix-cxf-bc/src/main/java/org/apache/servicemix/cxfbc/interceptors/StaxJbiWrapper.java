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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.binding.soap.Soap11;
import org.apache.cxf.binding.soap.Soap12;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.model.SoapHeaderInfo;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.PartialXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.servicemix.soap.util.stax.ExtendedXMLStreamReader;
import org.apache.servicemix.soap.util.stax.FragmentStreamReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


public class StaxJbiWrapper implements XMLStreamReader {
    public static final int STATE_START_DOC = 0;
    public static final int STATE_START_ELEMENT_WRAPPER = 1;
    public static final int STATE_START_ELEMENT_PART = 2;
    public static final int STATE_RUN_PART = 3;
    public static final int STATE_END_ELEMENT_PART = 4;
    public static final int STATE_END_ELEMENT_WRAPPER = 5;
    public static final int STATE_END_DOC = 6;

    private BindingMessageInfo wsdlMessage;
    private int state = STATE_START_DOC;
    private int part = -1;
    private int reader = -1;
    private int event = START_DOCUMENT;
    private List<List<XMLStreamReader>> parts = new ArrayList<List<XMLStreamReader>>();
    private List<QName> extraPrefixes = new ArrayList<QName>();

    public StaxJbiWrapper(Message message) {
        setExtraPrefix((SoapMessage) message);
        BindingOperationInfo wsdlOperation = getOperation(message);
        wsdlMessage = !isRequestor(message) ? wsdlOperation.getInput() : wsdlOperation.getOutput();
        XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
        if (isRequestor(message)) {
            throw new UnsupportedOperationException();
        }
        List<SoapHeaderInfo> headers = wsdlMessage
            .getExtensors(SoapHeaderInfo.class);
        if (headers != null && headers.size() > 0) {
            throw new UnsupportedOperationException();
        }
        SoapBindingInfo binding = (SoapBindingInfo) message.getExchange()
            .get(Endpoint.class).getEndpointInfo().getBinding();
        String style = binding.getStyle(wsdlOperation.getOperationInfo());
        if (style == null) {
            style = binding.getStyle();
        }
        int nbBodyParts = 0;
        for (MessagePartInfo part : wsdlMessage.getMessageParts()) {
            if (nbBodyParts++ > 0) {
                throw new UnsupportedOperationException();
            }
            if ("document".equals(style)) {
                parts.add(Collections.<XMLStreamReader>singletonList(new FragmentStreamReader(xmlReader)));
            } else /* rpc-style */ {
                throw new UnsupportedOperationException();
            }
        }
    }
    
    private void setExtraPrefix(SoapMessage message) {
        Document savedEnv = (Document) message.getContent(Node.class);
        if (savedEnv != null) {
            NamedNodeMap attrs = savedEnv.getFirstChild().getAttributes();
            Map<String, String> nsMap = message.getEnvelopeNs();
            if (nsMap == null) {
                nsMap = new HashMap<String, String>();
            }
            for (int i = 0; i < attrs.getLength(); i++) {
                Node node = attrs.item(i);
                if (!node.getNodeValue().equals(Soap11.SOAP_NAMESPACE)
                        && !node.getNodeValue().equals(Soap12.SOAP_NAMESPACE)) {
                    //set extra prefix
                    nsMap.put(node.getLocalName(), node.getNodeValue());
                    if (!node.getNodeValue().equals(XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI)
                            && !node.getNodeValue().equals(XMLConstants.W3C_XML_SCHEMA_NS_URI))
                        extraPrefixes.add(new QName(node.getNodeValue(), "", node.getLocalName()));
                }
                    
            }
            if (nsMap.size() > 0) {
                message.put("soap.env.ns.map", nsMap);
            }
        }
    }


    public int getEventType() {
        return event;
    }

    public int next() throws XMLStreamException {
        switch (state) {
            case STATE_START_DOC:
                state = STATE_START_ELEMENT_WRAPPER;
                event = START_ELEMENT;
                break;
            case STATE_START_ELEMENT_WRAPPER:
                if (parts.size() > 0) {
                    state = STATE_START_ELEMENT_PART;
                    event = START_ELEMENT;
                    part = 0;
                    reader = 0;
                } else {
                    state = STATE_END_ELEMENT_WRAPPER;
                    event = END_ELEMENT;
                }
                break;
            case STATE_START_ELEMENT_PART:
                if (reader >= parts.get(part).size()) {
                    state = STATE_END_ELEMENT_PART;
                    event = END_ELEMENT;
                } else {
                    state = STATE_RUN_PART;
                    event = parts.get(part).get(reader).next();
                    if (event == START_DOCUMENT) {
                        event = parts.get(part).get(reader).next();
                    }
                }
                break;
            case STATE_RUN_PART:
                event = parts.get(part).get(reader).next();
                if (event == END_DOCUMENT) {
                    if (++reader >= parts.get(part).size()) {
                        state = STATE_END_ELEMENT_PART;
                        event = END_ELEMENT;
                    } else {
                        event = parts.get(part).get(reader).next();
                        if (event == START_DOCUMENT) {
                            event = parts.get(part).get(reader).next();
                        }
                    }
                }
                break;
            case STATE_END_ELEMENT_PART:
                if (++part >= parts.size()) {
                    state = STATE_END_ELEMENT_WRAPPER;
                    event = END_ELEMENT;
                } else {
                    state = STATE_START_ELEMENT_PART;
                    event = START_ELEMENT;
                    reader = 0;
                }
                break;
            case STATE_END_ELEMENT_WRAPPER:
            case STATE_END_DOC:
                state = STATE_END_DOC;
                event = END_DOCUMENT;
                break;
        }
        return event;
    }

    public QName getName() {
        switch (state) {
            case STATE_START_ELEMENT_WRAPPER:
            case STATE_END_ELEMENT_WRAPPER:
                return CxfJbiConstants.WSDL11_WRAPPER_MESSAGE;
            case STATE_START_ELEMENT_PART:
            case STATE_END_ELEMENT_PART:
                return CxfJbiConstants.WSDL11_WRAPPER_PART;
            case STATE_RUN_PART:
                return parts.get(part).get(reader).getName();
            default:
                throw new IllegalStateException();
        }
    }

    public String getLocalName() {
        switch (state) {
            case STATE_START_ELEMENT_WRAPPER:
            case STATE_END_ELEMENT_WRAPPER:
                return CxfJbiConstants.WSDL11_WRAPPER_MESSAGE_LOCALNAME;
            case STATE_START_ELEMENT_PART:
            case STATE_END_ELEMENT_PART:
                return CxfJbiConstants.WSDL11_WRAPPER_PART_LOCALNAME;
            case STATE_RUN_PART:
                return parts.get(part).get(reader).getLocalName();
            default:
                throw new IllegalStateException();
        }
    }

    public String getNamespaceURI() {
        switch (state) {
            case STATE_START_ELEMENT_WRAPPER:
            case STATE_END_ELEMENT_WRAPPER:
            case STATE_START_ELEMENT_PART:
            case STATE_END_ELEMENT_PART:
                return CxfJbiConstants.WSDL11_WRAPPER_NAMESPACE;
            case STATE_RUN_PART:
                return parts.get(part).get(reader).getNamespaceURI();
            default:
                throw new IllegalStateException();
        }
    }

    public String getPrefix() {
        switch (state) {
            case STATE_START_ELEMENT_WRAPPER:
            case STATE_END_ELEMENT_WRAPPER:
            case STATE_START_ELEMENT_PART:
            case STATE_END_ELEMENT_PART:
                return CxfJbiConstants.WSDL11_WRAPPER_PREFIX;
            case STATE_RUN_PART:
                String prefix = parts.get(part).get(reader).getPrefix();
                String namespaceURI;
                if (prefix != null && prefix.length() == 0 
                        && ((namespaceURI = parts.get(part).get(reader).getNamespaceURI()) != null && namespaceURI.length() > 0)) {
                    return CxfJbiConstants.WSDL11_WRAPPER_PART_LOCALNAME;
                } else {
                    return prefix;
                }
            default:
                throw new IllegalStateException();
        }
    }

    public boolean hasName() {
        return state == STATE_RUN_PART ? parts.get(part).get(reader).isStartElement() : (event == START_ELEMENT || event == END_ELEMENT);
    }

    public Object getProperty(String s) throws IllegalArgumentException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void require(int i, String s, String s1) throws XMLStreamException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getElementText() throws XMLStreamException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public int nextTag() throws XMLStreamException {
        while (hasNext()) {
            int e = next();
            if (e == START_ELEMENT || e == END_ELEMENT)
                return e;
        }
        return event;
    }

    public boolean hasNext() throws XMLStreamException {
        return event != END_DOCUMENT;
    }

    public void close() throws XMLStreamException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getNamespaceURI(String s) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isStartElement() {
        return state == STATE_RUN_PART ? parts.get(part).get(reader).isStartElement() : event == START_ELEMENT;
    }

    public boolean isEndElement() {
        return state == STATE_RUN_PART ? parts.get(part).get(reader).isEndElement() : event == END_ELEMENT;
    }

    public boolean isCharacters() {
        return state == STATE_RUN_PART ? parts.get(part).get(reader).isCharacters() : event == CHARACTERS;
    }

    public boolean isWhiteSpace() {
        return state == STATE_RUN_PART ? parts.get(part).get(reader).isWhiteSpace() : event == SPACE;
    }

    public String getAttributeValue(String s, String s1) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public int getAttributeCount() {
        switch (state) {
            case STATE_START_ELEMENT_WRAPPER:
                return 7 + extraPrefixes.size();
            case STATE_START_ELEMENT_PART:
                return 0;
            case STATE_RUN_PART:
                return parts.get(part).get(reader).getAttributeCount();
            default:
                throw new IllegalStateException();
        }
    }

    public QName getAttributeName(int i) {
        switch (state) {
            case STATE_START_ELEMENT_WRAPPER:
                switch (i) {
                    case 0: return new QName(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                            CxfJbiConstants.WSDL11_WRAPPER_PREFIX,
                                            XMLConstants.XMLNS_ATTRIBUTE);
                    case 1: return new QName(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                            CxfJbiConstants.WSDL11_WRAPPER_MESSAGE_PREFIX,
                                             XMLConstants.XMLNS_ATTRIBUTE);
                    case 2: return new QName(CxfJbiConstants.WSDL11_WRAPPER_TYPE);
                    case 3: return new QName(CxfJbiConstants.WSDL11_WRAPPER_NAME);
                    case 4: return new QName(CxfJbiConstants.WSDL11_WRAPPER_VERSION);
                    case 5: return new QName(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                            CxfJbiConstants.WSDL11_WRAPPER_XSI_PREFIX,
                                             XMLConstants.XMLNS_ATTRIBUTE);
                    case 6: return new QName(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                            CxfJbiConstants.WSDL11_WRAPPER_XSD_PREFIX,
                                             XMLConstants.XMLNS_ATTRIBUTE);
                    default:{
                        if (i < getAttributeCount()) {
                            return new QName(XMLConstants.XMLNS_ATTRIBUTE_NS_URI,
                                    extraPrefixes.get(i - 7).getPrefix(),
                                                     XMLConstants.XMLNS_ATTRIBUTE);
                        } else {
                            throw new IllegalStateException();
                        }
                    }
                }
            case STATE_RUN_PART:
                return parts.get(part).get(reader).getAttributeName(i);
            default:
                throw new IllegalStateException();
        }
    }

    public String getAttributeNamespace(int i) {
        switch (state) {
            case STATE_START_ELEMENT_WRAPPER:
                switch (i) {
                    case 0:
                    case 1: return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
                    case 2:
                    case 3:
                    case 4: return XMLConstants.NULL_NS_URI;
                    case 5: return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
                    case 6: return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
                    default: {
                        if (i < getAttributeCount()) {
                            return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
                        } else {
                            throw new IllegalStateException();
                        }
                    }
                }
            case STATE_RUN_PART:
                return parts.get(part).get(reader).getAttributeNamespace(i);
            default:
                throw new IllegalStateException();
        }
    }

    public String getAttributeLocalName(int i) {
        switch (state) {
            case STATE_START_ELEMENT_WRAPPER:
                switch (i) {
                    case 0: return CxfJbiConstants.WSDL11_WRAPPER_PREFIX; 
                    case 1: return CxfJbiConstants.WSDL11_WRAPPER_MESSAGE_PREFIX;
                    case 2: return CxfJbiConstants.WSDL11_WRAPPER_TYPE;
                    case 3: return CxfJbiConstants.WSDL11_WRAPPER_NAME;
                    case 4: return CxfJbiConstants.WSDL11_WRAPPER_VERSION;
                    case 5: return CxfJbiConstants.WSDL11_WRAPPER_XSI_PREFIX;
                    case 6: return CxfJbiConstants.WSDL11_WRAPPER_XSD_PREFIX;
                    default: {
                        if (i < getAttributeCount()) {
                            return extraPrefixes.get(i -7).getPrefix();
                        } else {
                            throw new IllegalStateException();
                        }
                    }
                }
            case STATE_RUN_PART:
                return parts.get(part).get(reader).getAttributeLocalName(i);
            default:
                throw new IllegalStateException();
        }
    }

    public String getAttributePrefix(int i) {
        switch (state) {
            case STATE_START_ELEMENT_WRAPPER:
                switch (i) {
                    case 0: return XMLConstants.XMLNS_ATTRIBUTE;
                    case 1: return XMLConstants.XMLNS_ATTRIBUTE;
                    case 2:
                    case 3:
                    case 4: return XMLConstants.DEFAULT_NS_PREFIX;
                    case 5: return XMLConstants.XMLNS_ATTRIBUTE;
                    case 6: return XMLConstants.XMLNS_ATTRIBUTE;
                    default: {
                        if (i < getAttributeCount()) {
                            return XMLConstants.XMLNS_ATTRIBUTE;
                        } else {
                            throw new IllegalStateException();
                        }
                    }
                }
            case STATE_RUN_PART:
                return parts.get(part).get(reader).getAttributePrefix(i);
            default:
                throw new IllegalStateException();
        }
    }

    public String getAttributeType(int i) {
        return "CDATA";
    }

    public String getAttributeValue(int i) {
        switch (state) {
            case STATE_START_ELEMENT_WRAPPER:
                switch (i) {
                    case 0:
                    {
                        return CxfJbiConstants.WSDL11_WRAPPER_NAMESPACE;
                    }
                    case 1:
                    {
                        String typeNamespace = wsdlMessage.getMessageInfo().getName().getNamespaceURI();
                        if (typeNamespace == null || typeNamespace.length() == 0) {
                            throw new IllegalArgumentException("messageType namespace is null or empty");
                        }
                        return typeNamespace;
                    }
                    case 2:
                    {
                        String typeLocalName = wsdlMessage.getMessageInfo().getName().getLocalPart();
                        if (typeLocalName == null || typeLocalName.length() == 0) {
                            throw new IllegalArgumentException("messageType local name is null or empty");
                        }
                        return CxfJbiConstants.WSDL11_WRAPPER_MESSAGE_PREFIX + ":" + typeLocalName;
                    }
                    case 3:
                        return wsdlMessage.getMessageInfo().getName().getLocalPart().toString();
                    case 4:
                        return "1.0";
                    case 5:
                        return XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
                    case 6:
                        return XMLConstants.W3C_XML_SCHEMA_NS_URI;
                    default: {
                        if (i < getAttributeCount()) {
                            return extraPrefixes.get(i -7).getNamespaceURI();
                        } else {
                            throw new IllegalStateException();
                        }
                    }
                }
            case STATE_RUN_PART:
                return parts.get(part).get(reader).getAttributeValue(i);
            default:
                throw new IllegalStateException();
        }
    }

    public boolean isAttributeSpecified(int i) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public int getNamespaceCount() {
        switch (state) {
        case STATE_START_ELEMENT_WRAPPER:
        case STATE_END_ELEMENT_WRAPPER:
        case STATE_START_ELEMENT_PART:
        case STATE_END_ELEMENT_PART:
            return 0;
        case STATE_RUN_PART:
            return parts.get(part).get(reader).getNamespaceCount();
        default:
            throw new IllegalStateException();
    }
    }

    public String getNamespacePrefix(int i) {
        String prefix = parts.get(part).get(reader).getNamespacePrefix(i);
        if (prefix != null && prefix.length() == 0) {
            String uri = parts.get(part).get(reader).getNamespaceURI();
            if (uri != null && uri.length() > 0) {
                return CxfJbiConstants.WSDL11_WRAPPER_PART_LOCALNAME;
            }
        }
        return prefix;
    }

    public String getNamespaceURI(int i) {
        return parts.get(part).get(reader).getNamespaceURI(i);
    }

    public NamespaceContext getNamespaceContext() {
        if (state == STATE_RUN_PART) {
            return parts.get(part).get(reader).getNamespaceContext();
        } else {
            return new ExtendedXMLStreamReader.SimpleNamespaceContext();
        }
    }

    public String getText() {
        if (state == STATE_RUN_PART) {
            return parts.get(part).get(reader).getText();
        } else {
            throw new IllegalStateException();
        }
    }

    public char[] getTextCharacters() {
        if (state == STATE_RUN_PART) {
            return parts.get(part).get(reader).getTextCharacters();
        } else {
            throw new IllegalStateException();
        }
    }

    public int getTextCharacters(int i, char[] chars, int i1, int i2) throws XMLStreamException {
        if (state == STATE_RUN_PART) {
            return parts.get(part).get(reader).getTextCharacters(i, chars, i1, i2);
        } else {
            throw new IllegalStateException();
        }
    }

    public int getTextStart() {
        if (state == STATE_RUN_PART) {
            return parts.get(part).get(reader).getTextStart();
        } else {
            throw new IllegalStateException();
        }
    }

    public int getTextLength() {
        if (state == STATE_RUN_PART) {
            return parts.get(part).get(reader).getTextLength();
        } else {
            throw new IllegalStateException();
        }
    }

    public String getEncoding() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean hasText() {
        if (state == STATE_RUN_PART) {
            return parts.get(part).get(reader).hasText();
        } else {
            return false;
        }
    }

    public Location getLocation() {
        return new Location() {
            public int getCharacterOffset() {
                return 0;
            }
            public int getColumnNumber() {
                return 0;
            }
            public int getLineNumber() {
                return 0;
            }
            public String getPublicId() {
                return null;
            }
            public String getSystemId() {
                return null;
            }
        };
    }

    public String getVersion() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean isStandalone() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public boolean standaloneSet() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getCharacterEncodingScheme() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getPITarget() {
        throw new UnsupportedOperationException("Not implemented");
    }

    public String getPIData() {
        throw new UnsupportedOperationException("Not implemented");
    }
    
    private BindingOperationInfo getOperation(Message message) {
        BindingOperationInfo operation = message.getExchange().get(
                BindingOperationInfo.class);
        if (operation == null) {
            throw new Fault(
                    new Exception("Operation not bound on this message"));
        }
        return operation;
    }
    
    private boolean isRequestor(Message message) {
        return Boolean.TRUE.equals(message.get(Message.REQUESTOR_ROLE));
    }
}