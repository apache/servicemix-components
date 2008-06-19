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
package org.apache.servicemix.soap.interceptors.jbi;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.apache.servicemix.jbi.FaultException;
import org.apache.servicemix.soap.api.Fault;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.api.model.Binding;
import org.apache.servicemix.soap.api.model.Operation;
import org.apache.servicemix.soap.bindings.soap.SoapConstants;
import org.apache.servicemix.soap.bindings.soap.model.SoapOperation;
import org.apache.servicemix.soap.core.AbstractInterceptor;
import org.apache.servicemix.soap.util.QNameUtil;
import org.w3c.dom.DocumentFragment;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class JbiOutInterceptor extends AbstractInterceptor {

    private final boolean server;
    
    public JbiOutInterceptor(boolean server) {
        this.server = server;
    }
    
    public void handleMessage(Message message) {
        NormalizedMessage nm = message.getContent(NormalizedMessage.class);
        message.setContent(Source.class, nm.getContent());
        fromNMSAttachments(message, nm);
        fromNMSHeaders(message, nm);

        if (!server) {
            MessageExchange me = message.getContent(MessageExchange.class);
            Binding binding = message.get(Binding.class);
            Operation operation = binding.getOperation(me.getOperation());
            if (operation != null) {
                if (!me.getPattern().equals(operation.getMep())) {
                    throw new Fault("Received incorrect exchange mep.  Received " + me.getPattern()
                                    + " but expected " + operation.getMep() + " for operation "
                                    + operation.getName());
                }
                message.put(Operation.class, operation);
                if (operation instanceof SoapOperation<?>) {
                    String soapAction = ((SoapOperation<?>) operation).getSoapAction();
                    message.getTransportHeaders().put(SoapConstants.SOAP_ACTION_HEADER, soapAction);
                }
            }
        }
    }

    /**
     * Copy NormalizedMessage attachments to SoapMessage attachments
     */
    private void fromNMSAttachments(Message message, NormalizedMessage normalizedMessage) {
        Set attachmentNames = normalizedMessage.getAttachmentNames();
        for (Iterator it = attachmentNames.iterator(); it.hasNext();) {
            String id = (String) it.next();
            DataHandler handler = normalizedMessage.getAttachment(id);
            message.getAttachments().put(id, handler);
        }
    }

    /**
     * Copy NormalizedMessage headers to SoapMessage headers
     */
    @SuppressWarnings("unchecked")
    private void fromNMSHeaders(Message message, NormalizedMessage normalizedMessage) {
        if (normalizedMessage.getProperty(JbiConstants.PROTOCOL_HEADERS) != null) {
            Map<String, ?> headers = (Map<String, ?>) normalizedMessage.getProperty(JbiConstants.PROTOCOL_HEADERS);
            for (Map.Entry<String, ?> entry : headers.entrySet()) {
                QName name = QNameUtil.parse(entry.getKey());
                if (name != null) {
                    message.getSoapHeaders().put(name, (DocumentFragment) entry.getValue());
                } else {
                    message.getTransportHeaders().put(entry.getKey(), (String) entry.getValue());
                }
            }
        }
    }

}
