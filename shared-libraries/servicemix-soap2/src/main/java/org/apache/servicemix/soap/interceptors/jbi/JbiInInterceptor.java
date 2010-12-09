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

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;
import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.security.auth.Subject;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.apache.servicemix.soap.api.Fault;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.api.model.Operation;
import org.apache.servicemix.soap.bindings.soap.SoapFault;
import org.apache.servicemix.soap.core.AbstractInterceptor;
import org.apache.servicemix.soap.util.QNameUtil;
import org.w3c.dom.DocumentFragment;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class JbiInInterceptor extends AbstractInterceptor {

    public static final String OPERATION_MEP = "MEP";
    
    private final boolean server;
    
    public JbiInInterceptor(boolean server) {
        this.server = server;
    }
    
    public void handleMessage(Message message) {
        try {
            Operation operation = message.get(Operation.class);
            MessageExchange exchange;
            NormalizedMessage nm;
            // Create message
            if (server) {
                exchange = createExchange(message);
                if (operation != null) {
                    exchange.setOperation(operation.getName());
                }
                nm = exchange.createMessage();
                exchange.setMessage(nm, "in");
                message.setContent(MessageExchange.class, exchange);
            } else {
                exchange = message.getContent(MessageExchange.class);
                if (exchange == null) {
                    throw new IllegalStateException("Content of type " + MessageExchange.class + " not found on message");
                }
                if (message.getContent(Exception.class) == null) {
                    nm = exchange.getMessage("out");
                    if (nm == null) {
                        nm = exchange.createMessage();
                        exchange.setMessage(nm, "out");
                    }
                } else {
                    exchange.setFault(exchange.createFault());
                    nm = exchange.getFault();
                }
            }
            // Put headers
            toNMSHeaders(nm, message);
            // Put attachments
            toNMSAttachments(nm, message);
            // Put subject
            nm.setSecuritySubject(message.get(Subject.class));
            // Put main source
            getContent(nm, message);
            // Register new content
            message.setContent(NormalizedMessage.class, nm);
        } catch (JBIException e) {
            throw new Fault(e);
        }
    }

    /**
     * Create the JBI exchange
     */
    private MessageExchange createExchange(Message message) throws JBIException {
        URI mep;
        Operation operation = message.get(Operation.class);
        if (operation != null) {
            mep = operation.getMep();
        } else {
            mep = (URI) message.get(OPERATION_MEP);
        }
        if (mep == null) {
            throw new NullPointerException("MEP not found");
        }
        MessageExchangeFactory mef = message.get(MessageExchangeFactory.class);
        if (mef == null) {
            DeliveryChannel dv = message.get(DeliveryChannel.class);
            if (dv == null) {
                ComponentContext cc = message.get(ComponentContext.class);
                if (cc == null) {
                    throw new NullPointerException("MessageExchangeFactory or DeliveryChannel or ComponentContext not found");
                }
                dv = cc.getDeliveryChannel();
            }
            mef = dv.createExchangeFactory();
        }
        return mef.createExchange(mep);
    }

    /**
     * Convert SoapMessage headers to NormalizedMessage headers
     */
    private void toNMSHeaders(NormalizedMessage normalizedMessage, Message soapMessage) {
        Map<String, Object> headers = new HashMap<String, Object>();
        for (Map.Entry<QName, DocumentFragment> entry : soapMessage.getSoapHeaders().entrySet()) {
            headers.put(QNameUtil.toString(entry.getKey()), entry.getValue());
        }
        headers.putAll(soapMessage.getTransportHeaders());
        normalizedMessage.setProperty(JbiConstants.PROTOCOL_HEADERS, headers);
    }

    /**
     * Convert SoapMessage attachments to NormalizedMessage attachments
     */
    private void toNMSAttachments(NormalizedMessage normalizedMessage, Message soapMessage) throws MessagingException {
        for (Map.Entry<String, DataHandler> entry : soapMessage.getAttachments().entrySet()) {
            normalizedMessage.addAttachment(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Extract the content as a jaxp Source
     */
    private void getContent(NormalizedMessage nm, Message message) throws MessagingException {
        Exception e = message.getContent(Exception.class);
        if (e == null) {
            nm.setContent(message.getContent(Source.class));
        } else if (e instanceof SoapFault) {
            SoapFault fault = (SoapFault) e;
            nm.setContent(fault.getDetails());
            nm.setProperty(JbiConstants.SOAP_FAULT_CODE, fault.getCode());
            nm.setProperty(JbiConstants.SOAP_FAULT_NODE, fault.getNode());
            nm.setProperty(JbiConstants.SOAP_FAULT_REASON, fault.getReason());
            nm.setProperty(JbiConstants.SOAP_FAULT_ROLE, fault.getRole());
            nm.setProperty(JbiConstants.SOAP_FAULT_SUBCODE, fault.getSubcode());
        }
    }

}
