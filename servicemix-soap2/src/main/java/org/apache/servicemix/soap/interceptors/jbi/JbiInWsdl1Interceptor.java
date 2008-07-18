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

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.Location;
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
import org.apache.servicemix.soap.util.stax.StaxSource;
import org.apache.servicemix.soap.util.stax.DOMStreamReader;
import org.apache.servicemix.soap.util.stax.FragmentStreamReader;
import org.apache.servicemix.soap.util.stax.ExtendedXMLStreamReader;

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

        try {
            XMLStreamReader reader = new StaxJbiWrapper(message);
            message.setContent(Source.class, new StaxSource(reader));
            //throw new UnsupportedOperationException();
        } catch (UnsupportedOperationException e) {
            Document document = createDomWrapper(message);
            message.setContent(Source.class, new DOMSource(document));
        }
    }

    protected Document createDomWrapper(Message message) {
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
        return document;
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

    protected class StaxJbiWrapper implements XMLStreamReader {
        public static final int STATE_START_DOC = 0;
        public static final int STATE_START_ELEMENT_WRAPPER = 1;
        public static final int STATE_START_ELEMENT_PART = 2;
        public static final int STATE_RUN_PART = 3;
        public static final int STATE_END_ELEMENT_PART = 4;
        public static final int STATE_END_ELEMENT_WRAPPER = 5;
        public static final int STATE_END_DOC = 6;

        private Message message;
        private Wsdl1SoapMessage wsdlMessage;
        private int state = STATE_START_DOC;
        private int part = -1;
        private int reader = -1;
        private int event = START_DOCUMENT;
        private List<List<XMLStreamReader>> parts = new ArrayList<List<XMLStreamReader>>();

        public StaxJbiWrapper(Message message) {
            this.message = message;
            Wsdl1SoapOperation wsdlOperation = getOperation(message);
            wsdlMessage = server ? wsdlOperation.getInput() : wsdlOperation.getOutput();
            XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
            int nbBodyParts = 0;
            for (Wsdl1SoapPart part : wsdlMessage.getParts()) {
                if (part.isBody()) {
                    if (nbBodyParts++ > 0) {
                        throw new UnsupportedOperationException();
                    }
                    if (wsdlOperation.getStyle() == Wsdl1SoapBinding.Style.DOCUMENT) {
                        parts.add(Collections.<XMLStreamReader>singletonList(new FragmentStreamReader(xmlReader)));
                    } else /* rpc-style */ {
                        throw new UnsupportedOperationException();
                    }
                } else {
                    QName element = part.getElement();
                    DocumentFragment header = message.getSoapHeaders().get(element);
                    if (header == null) {
                        throw new SoapFault(SoapFault.SENDER, "Missing required header element: "
                                    + QNameUtil.toString(element));
                    }
                    List<XMLStreamReader> readers = new ArrayList<XMLStreamReader>();
                    NodeList l = header.getChildNodes();
                    for (int i = 0; i < l.getLength(); i++) {
                        Node n = l.item(i);
                        if (n instanceof Element) {
                            readers.add(new DOMStreamReader((Element) n));
                        }
                    }
                    parts.add(readers);
                }
            }
            // Now that we know we can handle the message, remove header parts that are included in the jbi wrapper
            for (Wsdl1SoapPart part : wsdlMessage.getParts()) {
                if (!part.isBody()) {
                    message.getSoapHeaders().remove(part.getElement());
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
                    return JbiConstants.WSDL11_WRAPPER_MESSAGE;
                case STATE_START_ELEMENT_PART:
                case STATE_END_ELEMENT_PART:
                    return JbiConstants.WSDL11_WRAPPER_PART;
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
                    return JbiConstants.WSDL11_WRAPPER_MESSAGE_LOCALNAME;
                case STATE_START_ELEMENT_PART:
                case STATE_END_ELEMENT_PART:
                    return JbiConstants.WSDL11_WRAPPER_PART_LOCALNAME;
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
                    return JbiConstants.WSDL11_WRAPPER_NAMESPACE;
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
                    return JbiConstants.WSDL11_WRAPPER_PREFIX;
                case STATE_RUN_PART:
                    return parts.get(part).get(reader).getPrefix();
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
                    return 4;
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
                                                 JbiConstants.WSDL11_WRAPPER_MESSAGE_PREFIX,
                                                 XMLConstants.XMLNS_ATTRIBUTE);
                        case 1: return new QName(JbiConstants.WSDL11_WRAPPER_TYPE);
                        case 2: return new QName(JbiConstants.WSDL11_WRAPPER_NAME);
                        case 3: return new QName(JbiConstants.WSDL11_WRAPPER_VERSION);
                        default: throw new IllegalStateException();
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
                        case 0: return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
                        case 1:
                        case 2:
                        case 3: return XMLConstants.NULL_NS_URI;
                        default: throw new IllegalStateException();
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
                        case 0: return JbiConstants.WSDL11_WRAPPER_MESSAGE_PREFIX;
                        case 1: return JbiConstants.WSDL11_WRAPPER_TYPE;
                        case 2: return JbiConstants.WSDL11_WRAPPER_NAME;
                        case 3: return JbiConstants.WSDL11_WRAPPER_VERSION;
                        default: throw new IllegalStateException();
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
                        case 1:
                        case 2:
                        case 3: return XMLConstants.DEFAULT_NS_PREFIX;
                        default: throw new IllegalStateException();
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
                            String typeNamespace = wsdlMessage.getName().getNamespaceURI();
                            if (typeNamespace == null || typeNamespace.length() == 0) {
                                throw new IllegalArgumentException("messageType namespace is null or empty");
                            }
                            return typeNamespace;
                        }
                        case 1:
                        {
                            //root.setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + JbiConstants.WSDL11_WRAPPER_MESSAGE_PREFIX,
                            //                  typeNamespace);
                            String typeLocalName = wsdlMessage.getName().getLocalPart();
                            if (typeLocalName == null || typeLocalName.length() == 0) {
                                throw new IllegalArgumentException("messageType local name is null or empty");
                            }
                            return JbiConstants.WSDL11_WRAPPER_MESSAGE_PREFIX + ":" + typeLocalName;
                        }
                        case 2:
                            return wsdlMessage.getMessageName();
                        case 3:
                            return "1.0";
                        default: throw new IllegalStateException();
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
            return 0;
        }

        public String getNamespacePrefix(int i) {
            throw new UnsupportedOperationException("Not implemented");
        }

        public String getNamespaceURI(int i) {
            throw new UnsupportedOperationException("Not implemented");
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
    }
}
