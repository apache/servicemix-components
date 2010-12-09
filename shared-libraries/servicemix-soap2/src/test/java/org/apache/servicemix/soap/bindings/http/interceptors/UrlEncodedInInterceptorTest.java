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

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.apache.servicemix.soap.bindings.http.interceptors.IriDecoderHelper.Param;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class UrlEncodedInInterceptorTest extends TestCase {

    public void testDecodeParams() {
        List<Param> p = IriDecoderHelper.decode(
                        "http://host:8192/service/392/4?name=nodet", 
                        "http://host:8192/service/{id}/{nb}", 
                        new ByteArrayInputStream("first=guillaume&age=30".getBytes()));
        assertNotNull(p);
        assertEquals(5, p.size());
        assertEquals(new Param("id", "392"), p.get(0));
        assertEquals(new Param("nb", "4"), p.get(1));
        assertEquals(new Param("name", "nodet"), p.get(2));
        assertEquals(new Param("first", "guillaume"), p.get(3));
        assertEquals(new Param("age", "30"), p.get(4));
    }
    
    public void testBuildDoc() {
        String schemaStr = 
                "<xsd:schema xmlns:xsd='http://www.w3.org/2001/XMLSchema' " +
                "            elementFormDefault='qualified'" +
                "            targetNamespace='http://example.org/Person'>" +
                "  <xsd:element name='UpdatePerson'>" +
                "    <xsd:complexType>" +
                "      <xsd:sequence>" +
                "        <xsd:element name='id' type='xsd:string'/>" +
                "        <xsd:element name='ssn' type='xsd:string' minOccurs='0'/>" +
                "        <xsd:element name='name' type='xsd:string' minOccurs='0'/>" +
                "      </xsd:sequence>" +
                "    </xsd:complexType>" +
                "  </xsd:element>" +
                "</xsd:schema>";
        XmlSchemaElement el = getElement(schemaStr, new QName("http://example.org/Person", "UpdatePerson"));
        List<Param> p = new ArrayList<Param>();
        p.add(new Param("id", "12"));
        p.add(new Param("ssn", "21"));
        p.add(new Param("name", "nodet"));
        
        Document doc = IriDecoderHelper.buildDocument(el, p);
        // The output should be
        //
        // <UpdatePerson xmlns='http://example.org/Person'>
        //   <id>12</id>
        //   <ssn>21</ssn>
        //   <name>nodet</name>
        // </UpdatePerson>
        //
        assertNotNull(doc);
        Element e = doc.getDocumentElement();
        assertNotNull(e);
        assertEquals("http://example.org/Person", e.getNamespaceURI());
        assertEquals("UpdatePerson", e.getLocalName());
        assertEquals(3, e.getChildNodes().getLength());
        Node c = e.getChildNodes().item(0);
        assertEquals("http://example.org/Person", c.getNamespaceURI());
        assertEquals("id", c.getLocalName());
        assertEquals("12", c.getFirstChild().getNodeValue());
        c = e.getChildNodes().item(1);
        assertEquals("http://example.org/Person", c.getNamespaceURI());
        assertEquals("ssn", c.getLocalName());
        assertEquals("21", c.getFirstChild().getNodeValue());
        c = e.getChildNodes().item(2);
        assertEquals("http://example.org/Person", c.getNamespaceURI());
        assertEquals("name", c.getLocalName());
        assertEquals("nodet", c.getFirstChild().getNodeValue());
    }
    
    protected XmlSchemaElement getElement(String schemaStr, QName element) {
        XmlSchemaCollection col = new XmlSchemaCollection();
        XmlSchema schema = col.read(new StreamSource(new StringReader(schemaStr)), null);
        return schema.getElementByName(element);
    }
    
}
