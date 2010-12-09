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
package org.apache.servicemix.soap.bindings.http.interceptors;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import org.apache.servicemix.soap.util.DomUtil;
import org.apache.servicemix.soap.util.IoUtil;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * @author <a href=""mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class IriDecoderHelper {

    /**
     * Simple holder class for a name/value pair.
     */
    public static class Param {
        
        private final String name;
        private final String value;
        
        public Param(String name, String value) {
            this.name = name;
            this.value = value;
        }
        /**
         * @return the name
         */
        public String getName() {
            return name;
        }
        /**
         * @return the value
         */
        public String getValue() {
            return value;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "[" + name + "=" + value + "]";
        }
        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            final int PRIME = 31;
            int result = 1;
            result = PRIME * result + ((name == null) ? 0 : name.hashCode());
            result = PRIME * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }
        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (getClass() != obj.getClass())
                return false;
            final Param other = (Param) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
            return true;
        }
    }
    
    public static List<Param> decodeIri(String uri, String loc) {
        List<Param> values = new ArrayList<Param>();
        String path = getUriPath(uri);
        String locPath = getUriPath(loc);
        int idx2 = 0;
        char c;
        for (int idx1 = 0; idx1 < locPath.length(); idx1++) {
            c = locPath.charAt(idx1); 
            if (c == '{') {
                if (locPath.charAt(idx1 + 1) == '{') {
                    // double curly brace
                    expect(path, idx2++, '{');
                } else {
                    int locEnd = locPath.indexOf('}', idx1);
                    String name = locPath.substring(idx1 + 1, locEnd);
                    idx1 = locEnd;
                    int end = findPartEnd(path, idx2);
                    String value = path.substring(idx2, end);
                    idx2 = end;
                    values.add(new Param(name, value));
                }
            } else {
                expect(path, idx2++, c);
            }
        }
        if (idx2 < path.length()) {
            c = path.charAt(idx2++);
            if (c == '?') {
                int end = path.indexOf('#', idx2);
                if (end < 0) {
                    end = path.length();
                }
                addParams(path, idx2, end, values);
            }
        }
        return values;
    }
    
    public static void addParams(String input, int start, int stop, List<Param> params) {
        while (start < stop) {
            int eq = input.indexOf('=', start);
            int se = input.indexOf('&', eq);
            if (se < 0) {
                se = stop;
            }
            params.add(new Param(input.substring(start, eq), 
                                 input.substring(eq + 1, se)));
            start = se + 1;
        }
    }
    
    /**
     * 
     */
    public static int findPartEnd(String path, int c) {
        int end = path.length();
        int i = path.indexOf('/', c);
        if (i >= c && i < end) {
            end = i;
        }
        i = path.indexOf('?', c);
        if (i >= c && i < end) {
            end = i;
        }
        return end;
    }
    
    /**
     * Check that the next character is the one expected 
     * or throw an exception 
     */
    public static void expect(String path, int index, char c) {
        if (path.charAt(index) != c) {
            throw new IllegalStateException("Unexpected character '" + c + "' at index " + index);
        }
    }

    /**
     * Get the path of a given uri, removing the scheme and authority parts
     */
    public static String getUriPath(String uri) {
        int idx = uri.indexOf("://");
        int idx2 = uri.indexOf('/', idx + 3);
        return uri.substring(idx2 + 1);
    }

    public static String combine(String location, String httpLocation) {
        if (httpLocation == null) {
            return location;
        }
        if (httpLocation.indexOf("://") != -1) {
            return httpLocation;
        }
        if (location.endsWith("/")) {
            return location + httpLocation;
        } else {
            return location + "/" + httpLocation;
        }
    }

    /**
     * Create a dom document conformant with the given schema element
     * with the input parameters.
     * 
     * @param element
     * @param params
     * @return
     */
    public static Document buildDocument(XmlSchemaElement element, List<Param> params) {
        Document doc = DomUtil.createDocument();
        XmlSchemaComplexType cplxType = (XmlSchemaComplexType) element.getSchemaType();
        XmlSchemaSequence seq = (XmlSchemaSequence) cplxType.getParticle();
        Element e = doc.createElementNS(element.getQName().getNamespaceURI(), element.getQName().getLocalPart());
        e.setAttribute(XMLConstants.XMLNS_ATTRIBUTE, element.getQName().getNamespaceURI());
        doc.appendChild(e);
        for (int i = 0; i < seq.getItems().getCount(); i++) {
            XmlSchemaElement elChild = (XmlSchemaElement) seq.getItems().getItem(i);
            Param param = null;
            for (Param p : params) {
                if (p.getName().equals(elChild.getQName().getLocalPart())) {
                    param = p;
                    break;
                }
            }
            Element ec = doc.createElementNS(elChild.getQName().getNamespaceURI(), elChild.getQName().getLocalPart());
            if (!elChild.getQName().getNamespaceURI().equals(element.getQName().getNamespaceURI())) {
                ec.setAttribute(XMLConstants.XMLNS_ATTRIBUTE, elChild.getQName().getNamespaceURI());
            }
            if (param != null) {
                params.remove(param);
                ec.appendChild(doc.createTextNode(param.getValue()));
            }
            e.appendChild(ec);
        }            
        return doc;
    }
    
    public static Document interopolateParams(Document doc, XmlSchemaElement element, List<Param> params) {
        XmlSchemaComplexType cplxType = (XmlSchemaComplexType)element.getSchemaType();
        XmlSchemaSequence seq = (XmlSchemaSequence)cplxType.getParticle();
        Element root = doc.getDocumentElement();
        if (root == null) {
            root = doc.createElementNS(element.getQName().getNamespaceURI(), 
                                    element.getQName().getLocalPart());
            root.setAttribute(XMLConstants.XMLNS_ATTRIBUTE, element.getQName().getNamespaceURI());
            doc.appendChild(root);
        }
        
        for (int i = 0; i < seq.getItems().getCount(); i++) {
            XmlSchemaElement elChild = (XmlSchemaElement)seq.getItems().getItem(i);
            Param param = null;
            for (Param p : params) {
                if (p.getName().equals(elChild.getQName().getLocalPart())) {
                    param = p;
                    break;
                }
            }
            if (param == null) {
                continue;
            }
            
            Element ec = getElement(root, elChild.getQName());
            if (ec == null) {
                ec = doc.createElementNS(elChild.getQName().getNamespaceURI(), elChild.getQName()
                                         .getLocalPart());
                if (!elChild.getQName().getNamespaceURI().equals(element.getQName().getNamespaceURI())) {
                    ec.setAttribute(XMLConstants.XMLNS_ATTRIBUTE, elChild.getQName().getNamespaceURI());
                }
                
                // insert the element at the appropriate position
                Element insertBeforeEl = getIndexedElement(root, i);
                if (insertBeforeEl != null) {
                    root.insertBefore(ec, insertBeforeEl);
                } else {
                    root.appendChild(ec);
                }
            } else {
                NodeList childNodes = ec.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    Node n = childNodes.item(j);
                    ec.removeChild(n);
                }
            }
            
            if (param != null) {
                params.remove(param);
                ec.appendChild(doc.createTextNode(param.getValue()));
            }
        }
        return doc;
    }

    public static List<Param> decode(String uri, String loc, InputStream is) {
        List<Param> params = IriDecoderHelper.decodeIri(uri, loc);
        if (is != null) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IoUtil.copyStream(is, baos);
            IriDecoderHelper.addParams(baos.toString(), 0, baos.size(), params);
        }
        return params;
    }

    private static Element getIndexedElement(Element e, int i) {
        NodeList childNodes = e.getChildNodes();
        int elNum = 0;
        for (int j = 0; j < childNodes.getLength(); j++) {
            Node n = childNodes.item(j);
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (i == elNum) {
                    return (Element) n;
                }
                elNum++;
            }
        }
        return null;
    }

    private static Element getElement(Element element, QName name) {
        NodeList childNodes = element.getChildNodes();
        for (int j = 0; j < childNodes.getLength(); j++) {
            Node n = childNodes.item(j);
            if (n.getNodeType() == Node.ELEMENT_NODE
                && n.getLocalName().equals(name.getLocalPart())
                && n.getNamespaceURI().equals(name.getNamespaceURI())) {
                return (Element)n;
            }
        }
        return null;
    }

}
