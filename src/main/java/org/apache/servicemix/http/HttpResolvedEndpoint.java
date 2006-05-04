/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.http;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.servicemix.jbi.util.DOMUtil;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class HttpResolvedEndpoint implements ServiceEndpoint {

    public final static String EPR_URI = "urn:servicemix:http";
    public final static QName EPR_SERVICE = new QName(EPR_URI, "HttpComponent");
    public final static String EPR_NAME = "epr";
    
    private DocumentFragment reference;
    private String epName;
    
    public HttpResolvedEndpoint(DocumentFragment epr, String epName) {
        this.reference = epr;
        this.epName = epName;
    }

    public DocumentFragment getAsReference(QName operationName) {
        return reference;
    }

    public String getEndpointName() {
        return epName;
    }

    public QName[] getInterfaces() {
        return null;
    }

    public QName getServiceName() {
        return EPR_SERVICE;
    }
    
    public static ServiceEndpoint resolveEndpoint(DocumentFragment epr) {
        if (epr.getChildNodes().getLength() == 1) {
            Node child = epr.getFirstChild();
            if (child instanceof Element) {
                Element elem = (Element) child;
                String nsUri = elem.getNamespaceURI();
                String name = elem.getLocalName();
                // Check simple endpoints
                if (HttpResolvedEndpoint.EPR_URI.equals(nsUri) && HttpResolvedEndpoint.EPR_NAME.equals(name)) {
                    return new HttpResolvedEndpoint(epr, DOMUtil.getElementText(elem));
                // Check WSA endpoints
                } else {
                    NodeList nl = elem.getElementsByTagNameNS("http://www.w3.org/2005/08/addressing", "Address");
                    if (nl.getLength() == 1) {
                        Element address = (Element) nl.item(0);
                        String uri = DOMUtil.getElementText(address);
                        if (uri != null) {
                            uri = uri.trim();
                            if (uri.startsWith("http://") || uri.startsWith("https://")) {
                                return new HttpResolvedEndpoint(epr, uri);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
    
}