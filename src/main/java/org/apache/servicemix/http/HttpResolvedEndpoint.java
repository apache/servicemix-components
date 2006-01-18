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

import org.w3c.dom.DocumentFragment;

public class HttpResolvedEndpoint implements ServiceEndpoint {

    public final static String EPR_URI = "urn:servicemix:http";
    public final static String EPR_NAME = "epr";
    
    private DocumentFragment reference;
    private String epName;
    
    public HttpResolvedEndpoint(DocumentFragment epr, String epName) {
        this.reference = epr;
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
        return new QName("urn:servicemix:http", "HttpComponent");
    }
    
}