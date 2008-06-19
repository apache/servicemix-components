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
package org.apache.servicemix.soap.bindings.soap.interceptors;

import java.net.URI;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.bindings.soap.SoapConstants;
import org.apache.servicemix.soap.bindings.soap.SoapFault;
import org.apache.servicemix.soap.bindings.soap.SoapVersion;
import org.apache.servicemix.soap.core.AbstractInterceptor;
import org.apache.servicemix.soap.util.DomUtil;
import org.apache.servicemix.soap.util.stax.StaxUtil;
import org.w3c.dom.Element;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class SoapFaultInInterceptor extends AbstractInterceptor {

    public void handleMessage(Message message) {
        XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
        if (xmlReader == null || xmlReader.getEventType() != XMLStreamConstants.START_ELEMENT) {
            return;
        }
        SoapVersion soapVersion = message.get(SoapVersion.class);
        if (!soapVersion.getFault().equals(xmlReader.getName())) {
            return;
        }
        // Read fault as DOM
        Element el = StaxUtil.createElement(xmlReader);
        SoapFault fault = readFault(el);
        message.setContent(Exception.class, fault);
    }

    private SoapFault readFault(Element element) throws SoapFault {
        QName code = null;
        QName subcode = null;
        String reason = null;
        URI node = null;
        URI role = null;
        Source details = null;
        // Parse soap 1.1 faults
        if (SoapConstants.SOAP_11_URI.equals(element.getNamespaceURI())) {
            // Fault code
            Element child = DomUtil.getFirstChildElement(element);
            checkElementName(child, SoapConstants.SOAP_11_FAULTCODE);
            code = DomUtil.createQName(child, DomUtil.getElementText(child));
            // Fault string
            child = DomUtil.getNextSiblingElement(child);
            checkElementName(child, SoapConstants.SOAP_11_FAULTSTRING);
            reason = DomUtil.getElementText(child);
            child = DomUtil.getNextSiblingElement(child);
            QName childname = DomUtil.getQName(child);
            // Fault actor
            if (SoapConstants.SOAP_11_FAULTACTOR.equals(childname)) {
                node = URI.create(DomUtil.getElementText(child));
                child = DomUtil.getNextSiblingElement(child);
                childname = DomUtil.getQName(child);
            }
            // Fault details
            if (SoapConstants.SOAP_11_FAULTDETAIL.equals(childname)) {
                Element subchild = DomUtil.getFirstChildElement(child);
                if (subchild != null) {
                    details = new DOMSource(subchild);
                    subchild = DomUtil.getNextSiblingElement(subchild);
                    if (subchild != null) {
                        throw new SoapFault(SoapFault.RECEIVER, "Multiple elements are not supported in Detail");
                    }
                }
                child = DomUtil.getNextSiblingElement(child);
                childname = DomUtil.getQName(child);
            }
            // Nothing should be left
            if (childname != null) {
                throw new SoapFault(SoapFault.SENDER, "Unexpected element: " + childname);
            }
        // Parse soap 1.2 faults
        } else {
            // Fault code
            Element child = DomUtil.getFirstChildElement(element);
            checkElementName(child, SoapConstants.SOAP_12_FAULTCODE);
            Element subchild = DomUtil.getFirstChildElement(child);
            checkElementName(subchild, SoapConstants.SOAP_12_FAULTVALUE);
            code = DomUtil.createQName(subchild, DomUtil.getElementText(subchild));
            if (!SoapConstants.SOAP_12_CODE_DATAENCODINGUNKNOWN.equals(code) &&
                !SoapConstants.SOAP_12_CODE_MUSTUNDERSTAND.equals(code) &&
                !SoapConstants.SOAP_12_CODE_RECEIVER.equals(code) &&
                !SoapConstants.SOAP_12_CODE_SENDER.equals(code) &&
                !SoapConstants.SOAP_12_CODE_VERSIONMISMATCH.equals(code)) {
                throw new SoapFault(SoapFault.SENDER, "Unexpected fault code: " + code); 
            }
            subchild = DomUtil.getNextSiblingElement(subchild);
            if (subchild != null) {
                checkElementName(subchild, SoapConstants.SOAP_12_FAULTSUBCODE);
                Element subsubchild = DomUtil.getFirstChildElement(subchild);
                checkElementName(subsubchild, SoapConstants.SOAP_12_FAULTVALUE);
                subcode = DomUtil.createQName(subsubchild, DomUtil.getElementText(subsubchild));
                subsubchild = DomUtil.getNextSiblingElement(subsubchild);
                if (subsubchild != null) {
                    checkElementName(subsubchild, SoapConstants.SOAP_12_FAULTSUBCODE);
                    throw new SoapFault(SoapFault.RECEIVER, "Unsupported nested subcodes");
                }
            }
            // Fault reason
            child = DomUtil.getNextSiblingElement(child);
            checkElementName(child, SoapConstants.SOAP_12_FAULTREASON);
            subchild = DomUtil.getFirstChildElement(child);
            checkElementName(subchild, SoapConstants.SOAP_12_FAULTTEXT);
            reason = DomUtil.getElementText(subchild);
            subchild = DomUtil.getNextSiblingElement(subchild);
            if (subchild != null) {
                throw new SoapFault(SoapFault.RECEIVER, "Unsupported multiple reasons");
            }
            // Fault node
            child = DomUtil.getNextSiblingElement(child);
            QName childname = DomUtil.getQName(child);
            if (SoapConstants.SOAP_12_FAULTNODE.equals(childname)) {
                node = URI.create(DomUtil.getElementText(child));
                child = DomUtil.getNextSiblingElement(child);
                childname = DomUtil.getQName(child);
            }
            // Fault role
            if (SoapConstants.SOAP_12_FAULTROLE.equals(childname)) {
                role = URI.create(DomUtil.getElementText(child));
                child = DomUtil.getNextSiblingElement(child);
                childname = DomUtil.getQName(child);
            }
            // Fault details
            if (SoapConstants.SOAP_12_FAULTDETAIL.equals(childname)) {
                subchild = DomUtil.getFirstChildElement(child);
                if (subchild != null) {
                    details = new DOMSource(subchild);
                    subchild = DomUtil.getNextSiblingElement(subchild);
                    if (subchild != null) {
                        throw new SoapFault(SoapFault.RECEIVER, "Multiple elements are not supported in Detail");
                    }
                }
                child = DomUtil.getNextSiblingElement(child);
                childname = DomUtil.getQName(child);
            }
            // Nothing should be left
            if (childname != null) {
                throw new SoapFault(SoapFault.SENDER, "Unexpected element: " + childname);
            }
        }
        return new SoapFault(code, subcode, reason, node, role, details);
    }
    
    private void checkElementName(Element element, QName expected) throws SoapFault {
        QName name= DomUtil.getQName(element);
        if (!expected.equals(name)) {
            throw new SoapFault(SoapFault.SENDER, "Expected element: " + expected + " but found " + name);
        }            
    }

}
