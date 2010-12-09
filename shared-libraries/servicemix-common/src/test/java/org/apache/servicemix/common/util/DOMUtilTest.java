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
package org.apache.servicemix.common.util;

import java.io.File;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import junit.framework.TestCase;

public class DOMUtilTest extends TestCase {
	
	static String fileName = "org/apache/servicemix/common/util/employee.xml";

	
	public void testCreateDocument() throws Exception {		
		Document doc = DOMUtil.newDocument();
	    Element element = doc.createElementNS("http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper", "organisation");
	    
	    Element el = doc.createElement("Paul");	    
	    DOMUtil.addChildElement(element, "employee", "Jack");	    
	    DOMUtil.addChildElement(element, "employee", "Rose");
	    DOMUtil.addChildElement(element, "employee", null);
	    DOMUtil.moveContent(element, el);	
	    el.setAttribute("xmlns:number", "http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper");
	    doc.appendChild(el);
	    assertNull(DOMUtil.getQName(null));
	    DOMUtil.createQName(el, "number:One");
        assertEquals("organisation", DOMUtil.getQName(element).getLocalPart());
        
        Element elem = doc.createElementNS(null, "org");
        Element childElem = doc.createElementNS(null, "org");
        elem.appendChild(childElem);
	    DOMUtil.createQName(childElem, "letter");
	    assertEquals("org", DOMUtil.getQName(childElem).getLocalPart());
	    	    
	    DOMUtil.createQName(el, "letter:One");
	    assertEquals("organisation", DOMUtil.getQName(element).getLocalPart());	    
	    element.setPrefix("jbi");
	    assertEquals("organisation", DOMUtil.getQName(element).getLocalPart());
	    
	    NodeList nodeLst = doc.getElementsByTagName("employee");
	    Node fstNode = nodeLst.item(0);
    	if (fstNode.getNodeType() == Node.ELEMENT_NODE) {	    	    
            assertEquals("Jack" , fstNode.getFirstChild().getNodeValue());
    	}
    	fstNode = nodeLst.item(1);
    	if (fstNode.getNodeType() == Node.ELEMENT_NODE) {	    	    
            assertEquals("Rose" , fstNode.getFirstChild().getNodeValue());
    	}
    	fstNode = nodeLst.item(2);
    	if (fstNode.getNodeType() == Node.ELEMENT_NODE) {	    	    
            assertNull("" , fstNode.getFirstChild());
    	}	    
	}
	
    public void testParseDocument() throws Exception {    	    	
    	
    	try {
    	    File file = getFile(fileName);    	    
    	    DocumentBuilder db = DOMUtil.getBuilder();    	    
    	    Document doc = db.parse(file);
    	    doc.getDocumentElement().normalize();
    	    assertEquals("organization", doc.getDocumentElement().getNodeName());    	    
    	    NodeList nodeLst = doc.getElementsByTagName("employee");    	    

    	    Node fstNode = nodeLst.item(0);
    	    String log = DOMUtil.asXML(fstNode);
    	    assertNotNull(log);
    	    String indentlog = DOMUtil.asIndentedXML(fstNode);
    	    assertNotNull(indentlog);
    	    Element fstElmnt = (Element) fstNode;      
    	    	            
                Element fstNmElmnt = DOMUtil.getFirstChildElement(fstElmnt);
                assertEquals("Jack", DOMUtil.getElementText(fstNmElmnt));
                assertEquals("letters", DOMUtil.recursiveGetAttributeValue(fstNmElmnt, "type"));
                NodeList lstNmElmntLst = fstElmnt.getElementsByTagName("lastname");
                Element lstNmElmnt = (Element) lstNmElmntLst.item(0);
                assertEquals("", DOMUtil.recursiveGetAttributeValue(lstNmElmnt, "type"));
                DOMUtil.copyAttributes(fstNmElmnt, lstNmElmnt);
                assertEquals("Rose", DOMUtil.getElementText(lstNmElmnt));
                assertEquals("letters", lstNmElmnt.getAttribute("type").toString());
                fstElmnt = DOMUtil.getNextSiblingElement(fstElmnt);                
                assertNull(DOMUtil.getFirstChildElement(fstNmElmnt));
                
                fstNmElmnt = DOMUtil.getFirstChildElement(fstElmnt);
                assertEquals("Paul", DOMUtil.getElementText(fstNmElmnt));
                assertEquals("letters", DOMUtil.recursiveGetAttributeValue(fstNmElmnt, "type"));
                lstNmElmntLst = fstElmnt.getElementsByTagName("lastname");
                lstNmElmnt = (Element) lstNmElmntLst.item(0);
                assertEquals("", DOMUtil.recursiveGetAttributeValue(lstNmElmnt, "type"));                
                assertEquals("McNealy", DOMUtil.getElementText(lstNmElmnt));                
                fstElmnt = DOMUtil.getNextSiblingElement(fstElmnt);
                
                fstNmElmnt = DOMUtil.getFirstChildElement(fstElmnt);
                assertEquals("Joe", DOMUtil.getElementText(fstNmElmnt));
                assertEquals("letters", DOMUtil.recursiveGetAttributeValue(fstNmElmnt, "type"));
                lstNmElmntLst = fstElmnt.getElementsByTagName("lastname");
                lstNmElmnt = (Element) lstNmElmntLst.item(0);
                assertEquals("", DOMUtil.recursiveGetAttributeValue(lstNmElmnt, "type"));                
                assertEquals("Bloggs", DOMUtil.getElementText(lstNmElmnt));                
    	        	    
    	} catch (Exception e) {
    	    e.printStackTrace();
    	}
    	
    } 
    
    protected File getFile(String name) {
        URL url = getClass().getClassLoader().getResource(name);    	
        return new File(url.getFile());        
    }

}
