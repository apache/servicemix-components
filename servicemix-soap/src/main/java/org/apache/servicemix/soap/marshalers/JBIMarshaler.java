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
package org.apache.servicemix.soap.marshalers;

import java.net.URI;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.soap.SoapFault;
import org.w3c.dom.DocumentFragment;

/**
 * 
 * @author Guillaume Nodet
 * @version $Revision: 1.5 $
 * @since 3.0
 */
public class JBIMarshaler {

    public static final String SOAP_FAULT_CODE = "org.apache.servicemix.soap.fault.code";
    public static final String SOAP_FAULT_SUBCODE = "org.apache.servicemix.soap.fault.subcode";
    public static final String SOAP_FAULT_REASON = "org.apache.servicemix.soap.fault.reason";
    public static final String SOAP_FAULT_NODE = "org.apache.servicemix.soap.fault.node";
    public static final String SOAP_FAULT_ROLE = "org.apache.servicemix.soap.fault.role";
    
	public void toNMS(NormalizedMessage normalizedMessage, SoapMessage soapMessage) throws Exception {
    	if (soapMessage.hasHeaders()) {
    		normalizedMessage.setProperty(JbiConstants.SOAP_HEADERS, soapMessage.getHeaders());
    	}
        if (soapMessage.hasAttachments()) {
        	Map attachments = soapMessage.getAttachments();
        	for (Iterator it = attachments.entrySet().iterator(); it.hasNext();) {
        		Map.Entry entry = (Map.Entry) it.next();
        		normalizedMessage.addAttachment((String) entry.getKey(), 
        										(DataHandler) entry.getValue());
        	}
        }
        normalizedMessage.setSecuritySubject(soapMessage.getSubject());
        if (soapMessage.getFault() != null) {
            if (normalizedMessage instanceof Fault == false) {
                throw new IllegalStateException("The soap message is a fault but the jbi message is not");
            }
            SoapFault fault = soapMessage.getFault();
            normalizedMessage.setProperty(SOAP_FAULT_CODE, fault.getCode());
            normalizedMessage.setProperty(SOAP_FAULT_SUBCODE, fault.getSubcode());
            normalizedMessage.setProperty(SOAP_FAULT_REASON, fault.getReason());
            normalizedMessage.setProperty(SOAP_FAULT_NODE, fault.getNode());
            normalizedMessage.setProperty(SOAP_FAULT_ROLE, fault.getRole());
            normalizedMessage.setContent(fault.getDetails());
        } else {
            normalizedMessage.setContent(soapMessage.getSource());
        }
	}
	
	public void fromNMS(SoapMessage soapMessage, NormalizedMessage normalizedMessage) {
		if (normalizedMessage.getProperty(JbiConstants.SOAP_HEADERS) != null) {
			Map headers = (Map) normalizedMessage.getProperty(JbiConstants.SOAP_HEADERS);
        	for (Iterator it = headers.entrySet().iterator(); it.hasNext();) {
        		Map.Entry entry = (Map.Entry) it.next();
        		soapMessage.addHeader((QName) entry.getKey(), (DocumentFragment) entry.getValue());
        	}
		}
		Set attachmentNames = normalizedMessage.getAttachmentNames();
		for (Iterator it = attachmentNames.iterator(); it.hasNext();) {
			String id = (String) it.next();
			DataHandler handler = normalizedMessage.getAttachment(id);
			soapMessage.addAttachment(id, handler);
		}
        soapMessage.setSubject(normalizedMessage.getSecuritySubject());
        if (normalizedMessage instanceof Fault) {
            QName code = (QName) normalizedMessage.getProperty(SOAP_FAULT_CODE);
            QName subcode = (QName) normalizedMessage.getProperty(SOAP_FAULT_SUBCODE);
            String reason = (String) normalizedMessage.getProperty(SOAP_FAULT_REASON);
            URI node = (URI) normalizedMessage.getProperty(SOAP_FAULT_NODE);
            URI role = (URI) normalizedMessage.getProperty(SOAP_FAULT_ROLE);
            SoapFault fault = new SoapFault(code, subcode, reason, node, role, normalizedMessage.getContent());
            soapMessage.setFault(fault);
        } else {
            soapMessage.setSource(normalizedMessage.getContent());
        }
	}

}
