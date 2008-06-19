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
package org.apache.servicemix.soap.util;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;

import org.apache.servicemix.soap.api.Fault;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * DOM related utilities.
 * 
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class DomUtil {

    private static DocumentBuilderFactory documentBuilderFactory;
    private static TransformerFactory transformerFactory;
    
    public static Document createDocument() {
        try {
            return getDocumentBuilderFactory().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException e) {
            throw new Fault(e);
        }
    }
    
    public static Document parse(InputStream is) {
        try {
            return getDocumentBuilderFactory().newDocumentBuilder().parse(is);
        } catch (SAXException e) {
            throw new Fault(e);
        } catch (IOException e) {
            throw new Fault(e);
        } catch (ParserConfigurationException e) {
            throw new Fault(e);
        }
    }
    
    public static Document parse(Source source) {
        try {
            Document doc = createDocument();
            DOMResult result = new DOMResult(doc);
            Transformer transformer = getTransformerFactory().newTransformer();
            transformer.transform(source, result);
            return doc;
        } catch (TransformerConfigurationException e) {
            throw new Fault(e);
        } catch (TransformerException e) {
            throw new Fault(e);
        }
    }

    public static DocumentBuilderFactory getDocumentBuilderFactory() {
        if (documentBuilderFactory == null) {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            documentBuilderFactory = f;
        }
        return documentBuilderFactory;
    }
    
    public static TransformerFactory getTransformerFactory() {
        if (transformerFactory == null) {
            transformerFactory =  TransformerFactory.newInstance();
        }
        return transformerFactory;
    }

    /**
     * Get the first child element
     * @param parent
     * @return
     */
    public static Element getFirstChildElement(Node parent) {
        NodeList childs = parent.getChildNodes();
        for (int i = 0; i < childs.getLength(); i++) {
            Node child = childs.item(i);
            if (child instanceof Element) {
                return (Element) child;
            }
        }
        return null;
    }
    
    /**
     * Returns the text of the element
     */
    public static String getElementText(Element element) {
        StringBuffer buffer = new StringBuffer();
        NodeList nodeList = element.getChildNodes();
        for (int i = 0, size = nodeList.getLength(); i < size; i++) {
            Node node = nodeList.item(i);
            if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.CDATA_SECTION_NODE) {
                buffer.append(node.getNodeValue());
            }
        }
        return buffer.toString();
    }

    /**
     * Get the next sibling element
     * @param el
     * @return
     */
    public static Element getNextSiblingElement(Element el) {
        for (Node n = el.getNextSibling(); n != null; n = n.getNextSibling()) {
            if (n instanceof Element) {
                return (Element) n;
            }
        }
        return null;
    }
    
    public static Element createElement(Node parent, QName name) {
        Document doc = parent instanceof Document ? (Document) parent : parent.getOwnerDocument();
        Element element;
        if (name.getPrefix() != null && name.getPrefix().length() > 0) {
            element = doc.createElementNS(name.getNamespaceURI(), name.getPrefix() + ":" + name.getLocalPart());
            String attr = recursiveGetAttributeValue(parent, XMLConstants.XMLNS_ATTRIBUTE + ":" + name.getPrefix());
            if (attr == null || !attr.equals(name.getNamespaceURI())) {
                element.setAttribute(XMLConstants.XMLNS_ATTRIBUTE + ":" + name.getPrefix(), 
                                     name.getNamespaceURI());
            }
        } else if (name.getNamespaceURI() != null && name.getNamespaceURI().length() > 0) {
            element = doc.createElementNS(name.getNamespaceURI(), name.getLocalPart());
            String attr = recursiveGetAttributeValue(parent, XMLConstants.XMLNS_ATTRIBUTE);
            if (attr == null || !attr.equals(name.getNamespaceURI())) {
                element.setAttribute(XMLConstants.XMLNS_ATTRIBUTE, 
                                     name.getNamespaceURI());
            }
        } else {
            element = doc.createElementNS(null, name.getLocalPart());
            String attr = recursiveGetAttributeValue(parent, XMLConstants.XMLNS_ATTRIBUTE);
            if (attr == null || attr.length() > 0) {
                element.setAttribute(XMLConstants.XMLNS_ATTRIBUTE, 
                                     "");
            }
        }
        parent.appendChild(element);
        return element;
    }
    
    /**
     * Creates a QName instance from the given namespace context for the given qualifiedName
     *
     * @param element       the element to use as the namespace context
     * @param qualifiedName the fully qualified name
     * @return the QName which matches the qualifiedName
     */
    public static QName createQName(Element element, String qualifiedName) {
        int index = qualifiedName.indexOf(':');
        if (index >= 0) {
            String prefix = qualifiedName.substring(0, index);
            String localName = qualifiedName.substring(index + 1);
            String uri = recursiveGetAttributeValue(element, XMLConstants.XMLNS_ATTRIBUTE + ":" + prefix);
            return new QName(uri, localName, prefix);
        }
        else {
            String uri = recursiveGetAttributeValue(element, XMLConstants.XMLNS_ATTRIBUTE);
            if (uri != null) {
                return new QName(uri, qualifiedName);
            }
            return new QName(qualifiedName);
        }
    }

    /**
     * Recursive method to find a given attribute value
     */
    public static String recursiveGetAttributeValue(Node parent, String attributeName) {
        if (parent instanceof Element) {
            Element element = (Element) parent;
            String answer = element.getAttribute(attributeName);
            if (answer == null || answer.length() == 0) {
                Node parentNode = element.getParentNode();
                if (parentNode instanceof Element) {
                    return recursiveGetAttributeValue((Element) parentNode, attributeName);
                }
            }
            return answer;
        } else {
            return null;
        }
    }

    protected static String getUniquePrefix(Element element) {
        int n = 1;
        while (true) {
            String nsPrefix = "ns" + n;
            if (recursiveGetAttributeValue(element, XMLConstants.XMLNS_ATTRIBUTE + ":" + nsPrefix) == null) {
                return nsPrefix;
            }
            n++;
        }
    }

    public static QName getQName(Element el) {
        if (el == null) {
            return null;
        } else if (el.getPrefix() != null) {
            return new QName(el.getNamespaceURI(), el.getLocalName(), el.getPrefix());
        } else {
            return new QName(el.getNamespaceURI(), el.getLocalName());
        }
    }

}
