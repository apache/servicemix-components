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

import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.servicemix.soap.api.Fault;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.bindings.soap.SoapConstants;
import org.apache.servicemix.soap.bindings.soap.SoapFault;
import org.apache.servicemix.soap.bindings.soap.SoapVersion;
import org.apache.servicemix.soap.bindings.soap.SoapVersionFactory;
import org.apache.servicemix.soap.core.AbstractInterceptor;
import org.apache.servicemix.soap.util.stax.StaxUtil;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class SoapInInterceptor extends AbstractInterceptor {
    
    private final SoapVersion soapVersion;
    
    public SoapInInterceptor() {
        this(null);
    }
    
    public SoapInInterceptor(SoapVersion soapVersion) {
        this.soapVersion = soapVersion;
    }
    
    public void handleMessage(Message message) {
        XMLStreamReader xmlReader = message.getContent(XMLStreamReader.class);
        if (xmlReader == null) {
            throw new NullPointerException("No xml reader found");
        }
        try {
            QName name = xmlReader.getName();
            SoapVersion soapVersion = this.soapVersion;
            if (soapVersion == null) {
                soapVersion = message.get(SoapVersion.class);
            }
            if (soapVersion == null) {
                soapVersion = SoapVersionFactory.getInstance().getSoapVersion(name);
            } else {
                soapVersion = soapVersion.getDerivedVersion(name.getPrefix());
            }
            if (soapVersion == null) {
                throw new SoapFault(SoapConstants.SOAP_12_CODE_VERSIONMISMATCH, "Unrecognized namespace: "
                                + xmlReader.getNamespaceURI() + " at ["
                                + xmlReader.getLocation().getLineNumber() + ","
                                + xmlReader.getLocation().getColumnNumber()
                                + "]. Expecting a Soap 1.1 or 1.2 namespace.");
            }
            message.put(SoapVersion.class, soapVersion);
            if (!name.equals(soapVersion.getEnvelope())) {
                if (name.getLocalPart().equals(soapVersion.getEnvelope().getLocalPart())) {
                    throw new SoapFault(SoapConstants.SOAP_12_CODE_VERSIONMISMATCH, 
                          "Expected a SOAP " + soapVersion.getVersion() + " request");
                }
                throw new SoapFault(SoapConstants.SOAP_12_CODE_VERSIONMISMATCH, "Unrecognized element: "
                                + xmlReader.getName() + " at ["
                                + xmlReader.getLocation().getLineNumber() + ","
                                + xmlReader.getLocation().getColumnNumber()
                                + "]. Expecting 'Envelope'.");
            }
            xmlReader.nextTag();
            if (xmlReader.getName().equals(soapVersion.getHeader())) {
                Map<QName, DocumentFragment> headers = message.getSoapHeaders();
                while (xmlReader.nextTag() != XMLStreamConstants.END_ELEMENT) {
                    QName hn = xmlReader.getName();
                    Element e = StaxUtil.createElement(xmlReader);
                    DocumentFragment df = headers.get(hn);
                    if (df == null) {
                        df = e.getOwnerDocument().createDocumentFragment();
                    }
                    e = (Element) df.getOwnerDocument().importNode(e, true);
                    df.appendChild(e);
                    headers.put(hn, df);
                }
                xmlReader.nextTag();
            }
            if (!xmlReader.getName().equals(soapVersion.getBody())) {
                throw new SoapFault(SoapFault.SENDER, "Unrecognized element: "
                                + xmlReader.getName() + ". Expecting 'Body'.");
            }
            if (xmlReader.nextTag() == XMLStreamConstants.END_ELEMENT) {
                // Empty body
                message.setContent(XMLStreamReader.class, null);
            }
        } catch (XMLStreamException e) {
            throw new Fault(e);
        }
    }
    
}
