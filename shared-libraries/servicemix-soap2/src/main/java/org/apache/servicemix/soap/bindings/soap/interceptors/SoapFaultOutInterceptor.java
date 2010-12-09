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

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;

import org.apache.servicemix.soap.api.Fault;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.bindings.soap.Soap11;
import org.apache.servicemix.soap.bindings.soap.SoapConstants;
import org.apache.servicemix.soap.bindings.soap.SoapFault;
import org.apache.servicemix.soap.bindings.soap.SoapVersion;
import org.apache.servicemix.soap.core.AbstractInterceptor;
import org.apache.servicemix.soap.util.stax.StaxUtil;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class SoapFaultOutInterceptor extends AbstractInterceptor {

    public void handleMessage(Message message) {
        Exception exception = message.getContent(Exception.class);
        XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
        SoapVersion soapVersion = message.get(SoapVersion.class);
        try {
            if (exception instanceof SoapFault) {
                SoapFault fault = (SoapFault) exception;
                if (soapVersion == null) {
                    soapVersion = Soap11.getInstance();
                }
                if (soapVersion.getVersion() == 1.1) {
                    writeSoap11Fault(writer, fault, soapVersion);
                } else if (soapVersion.getVersion() == 1.2) {
                    writeSoap12Fault(writer, fault, soapVersion);
                } else {
                    throw new IllegalStateException("Unrecognized soap version: " + soapVersion.getVersion());
                }
            }
        } catch (XMLStreamException e) {
            throw new Fault(e);
        }
    }

    private void writeSoap11Fault(XMLStreamWriter writer, SoapFault fault, SoapVersion soapVersion) throws XMLStreamException {
        fault.translateCodeTo11();

        StaxUtil.writeStartElement(writer, soapVersion.getFault());
        QName code = fault.getCode();
        if (code != null) {
            StaxUtil.writeStartElement(writer, SoapConstants.SOAP_11_FAULTCODE);
            StaxUtil.writeTextQName(writer, code);
            writer.writeEndElement();
        }
        String reason = fault.getReason();
        if (reason == null && fault.getCause() != null) {
            reason = fault.getCause().toString();
        }
        StaxUtil.writeStartElement(writer, SoapConstants.SOAP_11_FAULTSTRING);
        if (reason != null) {
            writer.writeCharacters(reason);
        }
        writer.writeEndElement();
        URI node = fault.getNode();
        if (node != null) {
            StaxUtil.writeStartElement(writer, SoapConstants.SOAP_11_FAULTACTOR);
            writer.writeCharacters(node.toString());
            writer.writeEndElement();
        }
        Source details = fault.getDetails();
        if (details != null) {
            StaxUtil.writeStartElement(writer, SoapConstants.SOAP_11_FAULTDETAIL);
            XMLStreamReader reader = StaxUtil.createReader(details);
            StaxUtil.copy(reader, writer);
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }

    private void writeSoap12Fault(XMLStreamWriter writer, SoapFault fault, SoapVersion soapVersion) throws XMLStreamException {
        fault.translateCodeTo12();
        
        StaxUtil.writeStartElement(writer, soapVersion.getFault());
        QName code = fault.getCode();
        if (code != null) {
            StaxUtil.writeStartElement(writer, SoapConstants.SOAP_12_FAULTCODE);
            StaxUtil.writeStartElement(writer, SoapConstants.SOAP_12_FAULTVALUE);
            StaxUtil.writeTextQName(writer, code);
            writer.writeEndElement();
            QName subcode = fault.getSubcode();
            if (subcode != null) {
                StaxUtil.writeStartElement(writer, SoapConstants.SOAP_12_FAULTSUBCODE);
                StaxUtil.writeStartElement(writer, SoapConstants.SOAP_12_FAULTVALUE);
                StaxUtil.writeTextQName(writer, subcode);
                writer.writeEndElement();
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
        String reason = fault.getReason();
        if (reason == null && fault.getCause() != null) {
            reason = fault.getCause().toString();
        }
        StaxUtil.writeStartElement(writer, SoapConstants.SOAP_12_FAULTREASON);
        StaxUtil.writeStartElement(writer, SoapConstants.SOAP_12_FAULTTEXT);
        writer.writeAttribute(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI, "lang", "en");
        if (reason != null) {
            writer.writeCharacters(reason);
        }
        writer.writeEndElement();
        writer.writeEndElement();
        URI node = fault.getNode();
        if (node != null) {
            StaxUtil.writeStartElement(writer, SoapConstants.SOAP_12_FAULTNODE);
            writer.writeCharacters(node.toString());
            writer.writeEndElement();
        }

        URI role = fault.getRole();
        if (role != null) {
            StaxUtil.writeStartElement(writer, SoapConstants.SOAP_12_FAULTROLE);
            writer.writeCharacters(role.toString());
            writer.writeEndElement();
        }

        Source details = fault.getDetails();
        if (details != null) {
            StaxUtil.writeStartElement(writer, SoapConstants.SOAP_12_FAULTDETAIL);
            XMLStreamReader reader = StaxUtil.createReader(details);
            StaxUtil.copy(reader, writer);
            writer.writeEndElement();
        }

        writer.writeEndElement();
    }
    
}
