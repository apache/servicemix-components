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
package org.apache.servicemix.wsn.jbi;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.namespace.QName;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;

import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.servicemix.common.util.DOMUtil;
import org.apache.servicemix.jbi.jaxp.StaxSource;
import org.apache.servicemix.jbi.jaxp.FragmentStreamReader;

/**
 * Helper classes dealing with the WSDL 1.1 JBI wrapper
 */
public class JbiWrapperHelper {

    public static final String WSDL11_WRAPPER_NAMESPACE = "http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper";
    public static final String WSDL11_WRAPPER_PREFIX = "jbi";
    public static final String WSDL11_WRAPPER_MESSAGE_LOCALNAME = "message";
    public static final QName WSDL11_WRAPPER_MESSAGE = new QName(WSDL11_WRAPPER_NAMESPACE, WSDL11_WRAPPER_MESSAGE_LOCALNAME, WSDL11_WRAPPER_PREFIX);
    public static final String WSDL11_WRAPPER_MESSAGE_PREFIX = "msg";
    public static final String WSDL11_WRAPPER_TYPE = "type";
    public static final String WSDL11_WRAPPER_NAME = "name";
    public static final String WSDL11_WRAPPER_VERSION = "version";
    public static final String WSDL11_WRAPPER_PART_LOCALNAME = "part";
    public static final QName WSDL11_WRAPPER_PART = new QName(WSDL11_WRAPPER_NAMESPACE, WSDL11_WRAPPER_PART_LOCALNAME, WSDL11_WRAPPER_PREFIX);

    private static final SourceTransformer transformer = new SourceTransformer();

    public static Document createDocument() throws ParserConfigurationException {
        return transformer.createDocument();
    }

    public static void wrap(Document doc) {
        wrap(doc, null, null);
    }

    public static void wrap(Document doc, QName type, String name) {
        Element wrapperMsg = doc.createElementNS(WSDL11_WRAPPER_NAMESPACE, WSDL11_WRAPPER_PREFIX + ":" + WSDL11_WRAPPER_MESSAGE_LOCALNAME);
        wrapperMsg.setAttribute(WSDL11_WRAPPER_VERSION, "1.0");
        if (type != null) {
            if (!XMLConstants.NULL_NS_URI.equals(type.getNamespaceURI())) {
                wrapperMsg.setAttributeNS(XMLConstants.XMLNS_ATTRIBUTE_NS_URI, WSDL11_WRAPPER_MESSAGE_PREFIX, type.getNamespaceURI());
            }
            wrapperMsg.setAttribute(WSDL11_WRAPPER_TYPE, WSDL11_WRAPPER_MESSAGE_PREFIX + ":" + type.getLocalPart());
        }
        if (name != null) {
            wrapperMsg.setAttribute(WSDL11_WRAPPER_NAME, name);
        }
        Element wrapperPart = doc.createElementNS(WSDL11_WRAPPER_NAMESPACE, WSDL11_WRAPPER_PREFIX + ":" + WSDL11_WRAPPER_PART_LOCALNAME);
        wrapperMsg.appendChild(wrapperPart);
        Element el = doc.getDocumentElement();
        doc.replaceChild(wrapperMsg, el);
        wrapperPart.appendChild(el);
    }

    public static Source unwrap(Source source, AtomicBoolean isJbiWrapped) throws XMLStreamException, TransformerException {
        if (source instanceof DOMSource) {
            Element el = DOMUtil.getFirstChildElement(((DOMSource) source).getNode());
            if (el == null) {
                throw new IllegalStateException("Unsupported DOMSource with no element");
            }
            if (WSDL11_WRAPPER_NAMESPACE.equals(el.getNamespaceURI()) && WSDL11_WRAPPER_MESSAGE_LOCALNAME.equals(el.getLocalName())) {
                el = DOMUtil.getFirstChildElement(el);
                if (el == null) {
                    throw new IllegalStateException("JBI message has no child element");
                }
                if (!WSDL11_WRAPPER_NAMESPACE.equals(el.getNamespaceURI()) || !WSDL11_WRAPPER_PART_LOCALNAME.equals(el.getLocalName())) {
                    throw new IllegalStateException("Expected a jbi:part element");
                }
                el = DOMUtil.getFirstChildElement(el);
                if (el == null) {
                    throw new IllegalStateException("JBI part has no child element");
                }
                isJbiWrapped.set(true);
                source = new DOMSource(el);
            }
        } else {
            XMLStreamReader reader = transformer.toXMLStreamReader(source);
            reader.nextTag();
            if (!reader.isStartElement()) {
                 throw new IllegalStateException("expected an element");
            }
            QName qname = reader.getName();
            if (qname.equals(WSDL11_WRAPPER_MESSAGE)) {
                reader.nextTag();
                if (reader.getName().equals(WSDL11_WRAPPER_PART)) {
                    reader.nextTag();
                    isJbiWrapped.set(true);
                } else {
                    throw new IllegalStateException("Expected a jbi:part element");
                }
            }
            source = new StaxSource(new FragmentStreamReader(reader));
        }
        return source;
    }

    public static Source unwrap(Source source) throws TransformerException, XMLStreamException {
        XMLStreamReader reader = new SourceTransformer().toXMLStreamReader(source);
        reader.nextTag();
        if (!reader.isStartElement()) {
             throw new IllegalStateException("expected an element");
        }
        QName qname = reader.getName();
        if (qname.equals(WSDL11_WRAPPER_MESSAGE)) {
            reader.nextTag();
            if (reader.getName().equals(WSDL11_WRAPPER_PART)) {
                reader.nextTag();
            } else {
                throw new IllegalStateException("Expected a jbi:part element");
            }
        }
        source = new StaxSource(new FragmentStreamReader(reader));
        return source;
    }
}
