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
package org.apache.servicemix.soap.ws.addressing;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import javax.xml.namespace.QName;
import javax.jbi.messaging.MessageExchange;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import org.apache.servicemix.common.util.WSAddressingConstants;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.api.Fault;
import org.apache.servicemix.soap.api.model.Binding;
import org.apache.servicemix.soap.api.model.Operation;
import org.apache.servicemix.soap.api.model.wsdl1.Wsdl1Message;
import org.apache.servicemix.soap.util.DomUtil;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapOperation;
import org.apache.servicemix.soap.bindings.soap.model.wsdl1.Wsdl1SoapMessage;
import org.apache.servicemix.soap.bindings.soap.interceptors.SoapOutInterceptor;

public class WsAddressingOutInterceptor extends AbstractWsAddressingInterceptor {

    public WsAddressingOutInterceptor(WsAddressingPolicy policy, boolean server) {
        super(policy, server);
        addBefore(SoapOutInterceptor.class.getName());
    }
    
    public void handleMessage(Message message) {
        // Clear addressing soap headers
        for (QName hdr : message.getSoapHeaders().keySet().toArray(new QName[0])) {
            if (isWSANamespace(hdr.getNamespaceURI())) {
                message.getSoapHeaders().remove(hdr);
            }
        }
        // WSA namespace to use
        String namespace = WSAddressingConstants.WSA_NAMESPACE_200508;
        // Retrieve request message
        if (isServer()) {
            Message request = (Message) message.get(Message.REQUEST_MESSAGE);
            if (request == null) {
                throw new Fault("Request message not bound on this message");
            }
            for (QName hdr : request.getSoapHeaders().keySet()) {
                if (isWSANamespace(hdr.getNamespaceURI())) {
                    // Retrieve WSA namespace in use
                    namespace = hdr.getNamespaceURI();
                    // Generate wsa:RelatesTo header
                    if (WSAddressingConstants.EL_MESSAGE_ID.equals(hdr.getLocalPart())) {
                        QName idQName = new QName(namespace, WSAddressingConstants.EL_RELATES_TO);
                        String id = DomUtil.getElementText(DomUtil.getFirstChildElement(request.getSoapHeaders().get(hdr)));
                        DocumentFragment fragment = createTextFragment(idQName, id);
                        message.getSoapHeaders().put(idQName, fragment);
                    }
                }
            }
        }
        // Generate wsa:MessageId header
        QName idQName = new QName(namespace, WSAddressingConstants.EL_MESSAGE_ID);
        String id = message.getContent(MessageExchange.class).getExchangeId();
        DocumentFragment fragment = createTextFragment(idQName, id);
        message.getSoapHeaders().put(idQName, fragment);
        // Generate wsa:Action header
        QName actionQName = new QName(namespace, WSAddressingConstants.EL_ACTION);
        Wsdl1Message msg = getMessage(message);
        Operation op = getOperation(message);
        Binding bnd = message.get(Binding.class);
        if (bnd == null) {
            throw new IllegalStateException("Binding not defined on this message");
        }
        String sep = bnd.getInterfaceName().getNamespaceURI().contains("/") ? "/" : ":";
        String action = bnd.getInterfaceName().getNamespaceURI() + sep + bnd.getInterfaceName().getLocalPart()
                            + sep + op.getName().getLocalPart() + sep + msg.getMessageName();
        fragment = createTextFragment(actionQName, action);
        message.getSoapHeaders().put(actionQName, fragment);
    }

    public Collection<URI> getRoles() {
        return Collections.emptyList();
    }

    public Collection<QName> getUnderstoodHeaders() {
        return Collections.emptyList();
    }

    protected DocumentFragment createTextFragment(QName element, String text) {
        Document doc = DomUtil.createDocument();
        DocumentFragment fragment = doc.createDocumentFragment();
        Element el = DomUtil.createElement(fragment, element);
        Text txt = doc.createTextNode(text);
        el.appendChild(txt);
        return fragment;
    }

    protected Wsdl1SoapOperation getOperation(Message message) {
        Operation operation = message.get(Operation.class);
        if (operation == null) {
            throw new Fault("Operation not bound on this message");
        }
        if (operation instanceof Wsdl1SoapOperation == false) {
            throw new Fault("Message is not bound to a WSDL 1.1 SOAP operation");
        }
        return (Wsdl1SoapOperation) operation;
    }

    protected Wsdl1SoapMessage getMessage(Message message) {
        org.apache.servicemix.soap.api.model.Message msg = message.get(org.apache.servicemix.soap.api.model.Message.class);
        if (msg == null) {
            throw new Fault("Message not bound on this message");
        }
        if (msg instanceof Wsdl1SoapMessage == false) {
            throw new Fault("Message is not bound to a WSDL 1.1 SOAP operation message");
        }
        return (Wsdl1SoapMessage) msg;
    }

}
