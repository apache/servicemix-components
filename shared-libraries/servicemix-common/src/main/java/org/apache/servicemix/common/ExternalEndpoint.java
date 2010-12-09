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
package org.apache.servicemix.common;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

public class ExternalEndpoint implements ServiceEndpoint {

    private QName eprElement;
    private String locationUri;
    private QName serviceName;
    private String endpointName;
    private QName[] interfaces = null;
    
    public ExternalEndpoint(QName eprElement, 
                            String locationUri,
                            QName serviceName, 
                            String epName, 
                            QName[] interfaces) {
        this.eprElement = eprElement;
        this.locationUri = locationUri;
        this.endpointName = epName;
        this.serviceName = serviceName;
        this.interfaces = interfaces;
    }

    public ExternalEndpoint(QName eprElement, 
                            String locationUri,
                            QName serviceName, 
                            String epName, 
                            QName interfaceName) {
        this.eprElement = eprElement;
        this.locationUri = locationUri;
        this.endpointName = epName;
        this.serviceName = serviceName;
        if (interfaceName != null) {
            this.interfaces = new QName[] { interfaceName };
        }
    }

    public DocumentFragment getAsReference(QName operationName) {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            Document doc = dbf.newDocumentBuilder().newDocument();
            DocumentFragment df = doc.createDocumentFragment();
            Element e = doc.createElementNS(eprElement.getNamespaceURI(), eprElement.getLocalPart());
            Text t = doc.createTextNode(locationUri);
            e.appendChild(t);
            df.appendChild(e);
            return df;
        } catch (Exception e) {
            throw new RuntimeException("Could not create reference", e);
        }
    }

    public QName getServiceName() {
        return serviceName;
    }

    public String getEndpointName() {
        return endpointName;
    }

    public QName[] getInterfaces() {
        return interfaces;
    }

}
