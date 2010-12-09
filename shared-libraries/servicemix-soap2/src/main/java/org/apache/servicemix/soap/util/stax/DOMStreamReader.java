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
package org.apache.servicemix.soap.util.stax;

/*
 * This implementation comes from the XFire project
 * https://svn.codehaus.org/xfire/trunk/xfire/xfire-core/src/main/org/codehaus/xfire/util/stax/
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * Abstract logic for creating XMLStreamReader from DOM documents.
 * Its works using adapters for Element, Node and Attribute ( @see ElementAdapter }
 * 
 * @author <a href="mailto:tsztelak@gmail.com">Tomasz Sztelak</a>
 */
public class DOMStreamReader implements XMLStreamReader {
    public Map properties = new HashMap();

    private ArrayList<ElementFrame> frames = new ArrayList<ElementFrame>();

    private ElementFrame frame;

    private int currentEvent = XMLStreamConstants.START_DOCUMENT;

    private Node content;

    private Document document;

    private DOMNamespaceContext context;

    /**
     * @param element
     */
    public DOMStreamReader(Element element) {
        this.frame = new ElementFrame(element, null);
        frames.add(this.frame);
        newFrame(frame);
        this.document = element.getOwnerDocument();
    }

    protected ElementFrame getCurrentFrame() {
        return frame;
    }

    /* (non-Javadoc)
     * @see javax.xml.stream.XMLStreamReader#getProperty(java.lang.String)
     */
    public Object getProperty(String key) throws IllegalArgumentException {
        return properties.get(key);
    }

    /* (non-Javadoc)
     * @see javax.xml.stream.XMLStreamReader#next()
     */
    public int next() throws XMLStreamException {
        if (frame.ended) {
            frames.remove(frames.size() - 1);
            if (!frames.isEmpty()) {
                frame = (ElementFrame) frames.get(frames.size() - 1);
            } else {
                currentEvent = END_DOCUMENT;
                return currentEvent;
            }
        }

        if (!frame.started) {
            frame.started = true;
            currentEvent = START_ELEMENT;
        } else if (frame.currentAttribute < getAttributeCount() - 1) {
            frame.currentAttribute++;
            currentEvent = ATTRIBUTE;
        } else if (frame.currentNamespace < getNamespaceCount() - 1) {
            frame.currentNamespace++;
            currentEvent = NAMESPACE;
        } else if (frame.currentChild < getChildCount() - 1) {
            frame.currentChild++;

            currentEvent = moveToChild(frame.currentChild);

            if (currentEvent == START_ELEMENT) {
                ElementFrame newFrame = getChildFrame(frame.currentChild);
                newFrame.started = true;
                frame = newFrame;
                frames.add(this.frame);
                currentEvent = START_ELEMENT;

                newFrame(newFrame);
            }
        } else {
            frame.ended = true;
            currentEvent = END_ELEMENT;
            endElement();
        }
        return currentEvent;
    }

    protected void skipFrame() {
        frame.ended = true;
        currentEvent = END_ELEMENT;
    }

    /* (non-Javadoc)
     * @see javax.xml.stream.XMLStreamReader#require(int, java.lang.String, java.lang.String)
     */
    public void require(int arg0, String arg1, String arg2) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }

    /* (non-Javadoc)
     * @see javax.xml.stream.XMLStreamReader#nextTag()
     */
    public int nextTag() throws XMLStreamException {
        while (hasNext()) {
            int e = next();
            if (e == START_ELEMENT || e == END_ELEMENT)
                return e;
        }

        return currentEvent;
    }

    /* (non-Javadoc)
     * @see javax.xml.stream.XMLStreamReader#hasNext()
     */
    public boolean hasNext() throws XMLStreamException {
        return !(frames.size() == 0 && frame.ended);

    }

    /* (non-Javadoc)
     * @see javax.xml.stream.XMLStreamReader#close()
     */
    public void close() throws XMLStreamException {
    }

    /* (non-Javadoc)
     * @see javax.xml.stream.XMLStreamReader#isStartElement()
     */
    public boolean isStartElement() {
        return (currentEvent == START_ELEMENT);
    }

    /* (non-Javadoc)
     * @see javax.xml.stream.XMLStreamReader#isEndElement()
     */
    public boolean isEndElement() {
        return (currentEvent == END_ELEMENT);
    }

    /* (non-Javadoc)
     * @see javax.xml.stream.XMLStreamReader#isCharacters()
     */
    public boolean isCharacters() {
        return (currentEvent == CHARACTERS);
    }

    /* (non-Javadoc)
     * @see javax.xml.stream.XMLStreamReader#isWhiteSpace()
     */
    public boolean isWhiteSpace() {
        return (currentEvent == SPACE);
    }

    public int getEventType() {
        return currentEvent;
    }

    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
        char[] src = getText().toCharArray();

        if (sourceStart + length >= src.length)
            length = src.length - sourceStart;

        for (int i = 0; i < length; i++) {
            target[targetStart + i] = src[i + sourceStart];
        }

        return length;
    }

    public boolean hasText() {
        return (currentEvent == CHARACTERS || currentEvent == DTD || currentEvent == ENTITY_REFERENCE
                        || currentEvent == COMMENT || currentEvent == SPACE);
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

    public boolean hasName() {
        return (currentEvent == START_ELEMENT || currentEvent == END_ELEMENT);
    }

    public String getVersion() {
        return null;
    }

    public boolean isStandalone() {
        return false;
    }

    public boolean standaloneSet() {
        // TODO Auto-generated method stub
        return false;
    }

    public String getCharacterEncodingScheme() {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * Get the document associated with this stream.
     * 
     * @return
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Find name spaces declaration in attributes and move them to separate
     * collection.
     */
    protected void newFrame(ElementFrame frame) {
        Element element = getCurrentElement();
        frame.uris = new ArrayList<String>();
        frame.prefixes = new ArrayList<String>();
        frame.attributes = new ArrayList<Node>();

        if (context == null)
            context = new DOMNamespaceContext();

        context.setElement(element);

        NamedNodeMap nodes = element.getAttributes();

        String ePrefix = element.getPrefix();
        if (ePrefix == null) {
            ePrefix = "";
        }

        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            String prefix = node.getPrefix();
            String localName = node.getLocalName();
            String value = node.getNodeValue();
            String name = node.getNodeName();

            if (prefix == null)
                prefix = "";

            if (name != null && name.equals("xmlns")) {
                frame.uris.add(value);
                frame.prefixes.add("");
            } else if (prefix.length() > 0 && prefix.equals("xmlns")) {
                frame.uris.add(value);
                frame.prefixes.add(localName);
            } else if (name.startsWith("xmlns:")) {
                prefix = name.substring(6);
                frame.uris.add(value);
                frame.prefixes.add(prefix);
            } else {
                frame.attributes.add(node);
            }
        }
    }

    protected void endElement() {
    }

    protected Element getCurrentElement() {
        return (Element) getCurrentFrame().element;
    }

    public Element skipElement() {
        Element e = (Element) getCurrentFrame().element;
        skipFrame();
        return e;
    }

    protected ElementFrame getChildFrame(int currentChild) {
        return new ElementFrame(getCurrentElement().getChildNodes().item(currentChild), getCurrentFrame());
    }

    protected int getChildCount() {
        return getCurrentElement().getChildNodes().getLength();
    }

    protected int moveToChild(int currentChild) {
        this.content = getCurrentElement().getChildNodes().item(currentChild);

        if (content instanceof Text)
            return CHARACTERS;
        else if (content instanceof Element)
            return START_ELEMENT;
        else if (content instanceof CDATASection)
            return CDATA;
        else if (content instanceof Comment)
            return CHARACTERS;
        else if (content instanceof EntityReference)
            return ENTITY_REFERENCE;

        throw new IllegalStateException();
    }

    public String getElementText() throws XMLStreamException {
        return getText();
    }

    public String getNamespaceURI(String prefix) {
        ElementFrame frame = getCurrentFrame();

        while (null != frame) {
            int index = frame.prefixes.indexOf(prefix);
            if (index != -1) {
                return (String) frame.uris.get(index);
            }

            frame = frame.parent;
        }

        return null;
    }

    public String getAttributeValue(String ns, String local) {
        if (ns == null || ns.equals(""))
            return getCurrentElement().getAttribute(local);
        else
            return getCurrentElement().getAttributeNS(ns, local);
    }

    public int getAttributeCount() {
        return getCurrentFrame().attributes.size();
    }

    Attr getAttribute(int i) {
        return (Attr) getCurrentFrame().attributes.get(i);
    }

    private String getLocalName(Attr attr) {

        String name = attr.getLocalName();
        if (name == null) {
            name = attr.getNodeName();
        }
        return name;
    }

    public QName getAttributeName(int i) {
        Attr at = getAttribute(i);

        String prefix = at.getPrefix();
        String ln = getLocalName(at);
        // at.getNodeName();
        String ns = at.getNamespaceURI();

        if (prefix == null) {
            return new QName(ns, ln);
        } else {
            return new QName(ns, ln, prefix);
        }
    }

    public String getAttributeNamespace(int i) {
        return getAttribute(i).getNamespaceURI();
    }

    public String getAttributeLocalName(int i) {
        Attr attr = getAttribute(i);
        String name = getLocalName(attr);
        return name;
    }

    public String getAttributePrefix(int i) {
        return getAttribute(i).getPrefix();
    }

    public String getAttributeType(int i) {
        return toStaxType(getAttribute(i).getNodeType());
    }

    public static String toStaxType(short jdom) {
        switch (jdom) {
        default:
            return null;
        }
    }

    public String getAttributeValue(int i) {
        return getAttribute(i).getValue();
    }

    public boolean isAttributeSpecified(int i) {
        return getAttribute(i).getValue() != null;
    }

    public int getNamespaceCount() {
        return getCurrentFrame().prefixes.size();
    }

    public String getNamespacePrefix(int i) {
        return (String) getCurrentFrame().prefixes.get(i);
    }

    public String getNamespaceURI(int i) {
        return (String) getCurrentFrame().uris.get(i);
    }

    public NamespaceContext getNamespaceContext() {
        return context;
    }

    public String getText() {
        Node node = getCurrentElement().getChildNodes().item(getCurrentFrame().currentChild);
        return node.getNodeValue();
    }

    public char[] getTextCharacters() {
        return getText().toCharArray();
    }

    public int getTextStart() {
        return 0;
    }

    public int getTextLength() {
        return getText().length();
    }

    public String getEncoding() {
        return null;
    }

    public QName getName() {
        Element el = getCurrentElement();

        String prefix = getPrefix();
        String ln = getLocalName();

        if (prefix == null) {
            return new QName(el.getNamespaceURI(), ln);
        } else {
            return new QName(el.getNamespaceURI(), ln, prefix);
        }
    }

    public String getLocalName() {
        String name = getCurrentElement().getLocalName();
        // When the element has no namespaces, null is returned
        if (name == null) {
            name = getCurrentElement().getNodeName();
        }
        return name;
    }

    public String getNamespaceURI() {
        return getCurrentElement().getNamespaceURI();
    }

    public String getPrefix() {
        String prefix = getCurrentElement().getPrefix();
        if (prefix == null) {
            prefix = "";
        }
        return prefix;
    }

    public String getPITarget() {
        throw new UnsupportedOperationException();
    }

    public String getPIData() {
        throw new UnsupportedOperationException();
    }

    public class DOMNamespaceContext implements NamespaceContext {
        private Element currentNode;

        public String getNamespaceURI(String prefix) {
            String name = prefix;
            if (name.length() == 0)
                name = "xmlns";
            else
                name = "xmlns:" + prefix;

            return getNamespaceURI(currentNode, name);
        }

        private String getNamespaceURI(Element e, String name) {
            Attr attr = e.getAttributeNode(name);
            if (attr == null) {
                Node n = e.getParentNode();
                if (n instanceof Element && n != e) {
                    return getNamespaceURI((Element) n, name);
                }
            } else {
                return attr.getValue();
            }

            return null;
        }

        public String getPrefix(String uri) {
            return getPrefix(currentNode, uri);
        }

        private String getPrefix(Element e, String uri) {
            NamedNodeMap attributes = e.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                Attr a = (Attr) attributes.item(i);

                String val = a.getValue();
                if (val != null && val.equals(uri)) {
                    String name = a.getNodeName();
                    if (name.equals("xmlns"))
                        return "";
                    else
                        return name.substring(6);
                }
            }

            Node n = e.getParentNode();
            if (n instanceof Element && n != e) {
                return getPrefix((Element) n, uri);
            }

            return null;
        }

        public Iterator<String> getPrefixes(String uri) {
            List<String> prefixes = new ArrayList<String>();

            String prefix = getPrefix(uri);
            if (prefix != null)
                prefixes.add(prefix);

            return prefixes.iterator();
        }

        public Element getElement() {
            return currentNode;
        }

        public void setElement(Element currentNode) {
            this.currentNode = currentNode;
        }
    }

    public static class ElementFrame {
        public ElementFrame(Object element, ElementFrame parent) {
            this.element = element;
            this.parent = parent;
        }

        final Object element;
        final ElementFrame parent;
        boolean started = false;
        boolean ended = false;
        int currentChild = -1;
        int currentAttribute = -1;
        int currentNamespace = -1;
        int currentElement = -1;
        List<String> uris;
        List<String> prefixes;
        List<Node> attributes;
        List allAttributes;
    }

}
