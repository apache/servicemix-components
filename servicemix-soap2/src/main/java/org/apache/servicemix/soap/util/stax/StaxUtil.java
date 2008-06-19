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

import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.servicemix.soap.api.Fault;
import org.apache.servicemix.soap.util.DomUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Stax related utilities.
 * 
 * Some of this code has been borrowed from the XFire project
 * 
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 * @author Dan Diephouse
 */
public class StaxUtil {

    private static XMLOutputFactory xof;
    private static XMLInputFactory xif;
    
    public static XMLStreamReader createReader(InputStream is) {
        return createReader(is, null);
    }
    
    public static XMLStreamReader createReader(InputStream is, String encoding) {
        try {
            return getXMLInputFactory().createXMLStreamReader(is, encoding);
        } catch (XMLStreamException e) {
            throw new Fault(e);
        }
    }
    
    public static XMLStreamReader createReader(Source source) {
        if (source instanceof DOMSource) {
            Node node = ((DOMSource) source).getNode();
            if (node instanceof Element) {
                return new DOMStreamReader((Element) node);
            } else {
                return new DOMStreamReader(DomUtil.getFirstChildElement(node));
            }
        } else {
            try {
                return getXMLInputFactory().createXMLStreamReader(source);
            } catch (XMLStreamException e) {
                throw new Fault(e);
            }
        }
    }
    
    public static Source createSource(XMLStreamReader reader) {
        if (reader instanceof DOMStreamReader) {
            return new DOMSource(((DOMStreamReader) reader).skipElement());
        } else {
            return new StaxSource(new FragmentStreamReader(reader));
        }
    }
    
    public static Element createElement(XMLStreamReader reader) {
        if (reader instanceof DOMStreamReader) {
            return ((DOMStreamReader) reader).skipElement();
        } else {
            Source src = new StaxSource(new FragmentStreamReader(reader));
            return DomUtil.parse(src).getDocumentElement();
        }
    }
    
    public static XMLInputFactory getXMLInputFactory() {
        if (xif == null) {
            xif = XMLInputFactory.newInstance();
        }
        return xif;
    }

    public static XMLStreamWriter createWriter(OutputStream os) {
        return createWriter(os, null);
    }
    
    public static XMLStreamWriter createWriter(OutputStream os, String encoding) {
        try {
            return getXMLOutputFactory().createXMLStreamWriter(os, encoding);
        } catch (XMLStreamException e) {
            throw new Fault(e);
        }
    }

    public static XMLOutputFactory getXMLOutputFactory() {
        if (xof == null) {
            xof = XMLOutputFactory.newInstance();
        }
        return xof;
    }
    
    /**
     * Reads a QName from the element text. Reader must be positioned at the
     * start tag.
     */
    public static QName readQName(XMLStreamReader reader) throws XMLStreamException {
        String value = reader.getElementText();
        if (value == null) {
            return null;
        }
        
        int index = value.indexOf(":");

        if (index == -1) {
            return new QName(value);
        }

        String prefix = value.substring(0, index);
        String localName = value.substring(index + 1);
        String ns = reader.getNamespaceURI(prefix);

        if (ns == null || localName == null) {
            throw new RuntimeException("Invalid QName in mapping: " + value);
        }

        return new QName(ns, localName, prefix);
    }
    
    /**
     * Copies the reader to the writer.  The start and end document
     * methods must be handled on the writer manually.
     * 
     * TODO: if the namespace on the reader has been declared previously
     * to where we are in the stream, this probably won't work.
     * 
     * @param reader
     * @param writer
     * @throws XMLStreamException
     */
    public static void copy( XMLStreamReader reader, XMLStreamWriter writer ) 
        throws XMLStreamException
    {
        int read = 0; // number of elements read in
        int event = reader.getEventType();
        
        while ( reader.hasNext() )
        {
            switch( event )
            {
                case XMLStreamConstants.START_ELEMENT:
                    read++;
                    writeStartElement( reader, writer );
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    writer.writeEndElement();
                    read--;
                    if ( read <= 0 )
                        return;
                    break;
                case XMLStreamConstants.CHARACTERS:
                    writer.writeCharacters( reader.getText() );  
                    break;
                case XMLStreamConstants.START_DOCUMENT:
                case XMLStreamConstants.END_DOCUMENT:
                case XMLStreamConstants.ATTRIBUTE:
                case XMLStreamConstants.NAMESPACE:
                    break;
                default:
                    break;
            }
            event = reader.next();
        }
    }

    private static void writeStartElement(XMLStreamReader reader, XMLStreamWriter writer) 
        throws XMLStreamException
    {
        String local = reader.getLocalName();
        String uri = reader.getNamespaceURI();
        String prefix = reader.getPrefix();
        if (prefix == null)
        {
            prefix = "";
        }
        if (uri == null)
        {
            uri = "";
        }
        
        String boundPrefix = writer.getPrefix(uri);
        boolean writeElementNS = false;
        if ( boundPrefix == null || !prefix.equals(boundPrefix) )
        {   
            writeElementNS = true;
        }
        
        // Write out the element name
        if (uri != null)
        {
            if (prefix.length() == 0) 
            { 
                
                writer.writeStartElement(local);
                writer.setDefaultNamespace(uri); 
                
            } 
            else 
            { 
                writer.writeStartElement(prefix, local, uri); 
                writer.setPrefix(prefix, uri); 
            } 
        }
        else
        {
            writer.writeStartElement( local );
        }

        // Write out the namespaces
        for ( int i = 0; i < reader.getNamespaceCount(); i++ )
        {
            String nsURI = reader.getNamespaceURI(i);
            String nsPrefix = reader.getNamespacePrefix(i);
            if (nsPrefix == null) nsPrefix = "";
            
            if ( nsPrefix.length() ==  0 )
            {
                writer.writeDefaultNamespace(nsURI);
            }
            else
            {
                writer.writeNamespace(nsPrefix, nsURI);
            }

            if (nsURI.equals(uri) && nsPrefix.equals(prefix))
            {
                writeElementNS = false;
            }
        }
        
        // Check if the namespace still needs to be written.
        // We need this check because namespace writing works 
        // different on Woodstox and the RI.
        if (writeElementNS)
        {
            if ( prefix == null || prefix.length() ==  0 )
            {
                writer.writeDefaultNamespace(uri);
            }
            else
            {
                writer.writeNamespace(prefix, uri);
            }
        }

        // Write out attributes
        for ( int i = 0; i < reader.getAttributeCount(); i++ )
        {
            String ns = reader.getAttributeNamespace(i);
            String nsPrefix = reader.getAttributePrefix(i);
            if ( ns == null || ns.length() == 0 )
            {
                writer.writeAttribute(
                        reader.getAttributeLocalName(i),
                        reader.getAttributeValue(i));
            }
            else if (nsPrefix == null || nsPrefix.length() == 0)
            {
                writer.writeAttribute(
                    reader.getAttributeNamespace(i),
                    reader.getAttributeLocalName(i),
                    reader.getAttributeValue(i));
            }
            else
            {
                writer.writeAttribute(reader.getAttributePrefix(i),
                                      reader.getAttributeNamespace(i),
                                      reader.getAttributeLocalName(i),
                                      reader.getAttributeValue(i));
            }
            
            
        }
    }
    
    /**
     * Write a start element with the specified parameters
     * @param writer
     * @param uri
     * @param local
     * @param prefix
     * @throws XMLStreamException
     */
    public static void writeStartElement( XMLStreamWriter writer, String uri, String local, String prefix ) 
        throws XMLStreamException 
    {
        if (prefix == null)
        {
            prefix = "";
        }
        if (uri == null)
        {
            uri = "";
        }
        
        String boundPrefix = writer.getPrefix(uri);
        boolean writeElementNS = false;
        if ( boundPrefix == null || !prefix.equals(boundPrefix) )
        {   
            writeElementNS = true;
        }
        
        // Write out the element name
        if (uri != null)
        {
            if (prefix.length() == 0) 
            { 
                
                writer.writeStartElement(local);
                writer.setDefaultNamespace(uri); 
                
            } 
            else 
            { 
                writer.writeStartElement(prefix, local, uri); 
                writer.setPrefix(prefix, uri); 
            } 
        }
        else
        {
            writer.writeStartElement( local );
        }

        // Check if the namespace still needs to be written.
        // We need this check because namespace writing works 
        // different on Woodstox and the RI.
        if (writeElementNS)
        {
            if ( prefix.length() ==  0 )
            {
                writer.writeDefaultNamespace(uri);
            }
            else
            {
                writer.writeNamespace(prefix, uri);
            }
        }
    }

    /**
     * Write a start element with the given QName.
     * However, if a namespace has already been bound to a prefix,
     * use the existing one, else default to the prefix
     * in the QName (if specified).  Else, a prefix is generated.
     * 
     * @param writer
     * @param name
     * @throws XMLStreamException
     */
    public static void writeStartElement(XMLStreamWriter writer, QName name) throws XMLStreamException {
        String prefix = choosePrefix(writer, name, false);
        writeStartElement(writer, name.getNamespaceURI(), name.getLocalPart(), prefix);
    }

    /**
     * 
     * @param out
     * @param name
     * @throws XMLStreamException
     */
    public static void writeTextQName(XMLStreamWriter out, QName name) throws XMLStreamException {
        String prefix = choosePrefix(out, name, true);
        if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix)) {
            out.writeCharacters(name.getLocalPart());
        } else {
            out.writeCharacters(prefix + ":" + name.getLocalPart());
        }
    }
    
    protected static String choosePrefix(XMLStreamWriter out, QName name, boolean declare) throws XMLStreamException {
        String uri = name.getNamespaceURI();
        // If no namespace
        if (uri == null || XMLConstants.NULL_NS_URI.equals(uri)) {
            if (!XMLConstants.NULL_NS_URI.equals(out.getNamespaceContext().getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX))) {
                out.setPrefix(XMLConstants.DEFAULT_NS_PREFIX, XMLConstants.NULL_NS_URI);
            }
            return XMLConstants.DEFAULT_NS_PREFIX;
        // Need to write a prefix
        } else {
            String defPrefix = name.getPrefix();
            // A prefix is specified
            if (defPrefix != null && !XMLConstants.DEFAULT_NS_PREFIX.equals(defPrefix)) {
                // if the uri is bound to the specified prefix, good, else
                if (!uri.equals(out.getNamespaceContext().getNamespaceURI(defPrefix))) {
                    // if there is a prefix bound to the uri, use it
                    if (out.getNamespaceContext().getPrefix(uri) != null) {
                        defPrefix = out.getNamespaceContext().getPrefix(uri);
                    // get prefix from the writer
                    } else if (out.getPrefix(uri) != null) {
                        defPrefix = out.getPrefix(uri);
                    // we need to bind the prefix
                    } else if (declare) {
                        out.setPrefix(defPrefix, uri);
                        out.writeNamespace(defPrefix, uri);
                    }
                }
            // No prefix specified
            } else {
                // if there is a prefix bound to the uri, use it
                if (out.getNamespaceContext().getPrefix(uri) != null) {
                    defPrefix = out.getNamespaceContext().getPrefix(uri);
                // get prefix from the writer
                } else if (out.getPrefix(uri) != null) {
                    defPrefix = out.getPrefix(uri);
                // we need to generate a prefix
                } else {
                    defPrefix = getUniquePrefix(out); 
                    if (declare) {
                        out.setPrefix(defPrefix, uri);
                        out.writeNamespace(defPrefix, uri);
                    }
                }
            }
            return defPrefix;
        }
    }
    
    protected static String getUniquePrefix(XMLStreamWriter writer) {
        int n = 1;
        while (true) {
            String nsPrefix = "ns" + n;
            if (writer.getNamespaceContext().getNamespaceURI(nsPrefix) == null) {
                return nsPrefix;
            }
            n++;
        }
    }

}
