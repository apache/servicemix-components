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
import java.util.List;
import java.util.ArrayList;

import javax.xml.namespace.QName;
import javax.jbi.component.ComponentContext;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.Definition;
import javax.wsdl.Service;
import javax.wsdl.Port;
import javax.wsdl.PortType;

import org.w3c.dom.Document;

import org.apache.servicemix.common.util.WSAddressingConstants;
import org.apache.servicemix.common.util.URIResolver;
import org.apache.servicemix.common.tools.wsdl.PortTypeDecorator;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.api.model.Binding;
import org.apache.servicemix.soap.api.model.Operation;
import org.apache.servicemix.soap.bindings.soap.SoapFault;
import org.apache.servicemix.soap.wsdl.BindingFactory;
import org.apache.servicemix.soap.interceptors.wsdl.WsdlOperationInInterceptor;

public class WsAddressingInOperationInterceptor extends AbstractWsAddressingInterceptor {

    public WsAddressingInOperationInterceptor(WsAddressingPolicy policy, boolean server) {
        super(policy, server);
        addBefore(WsdlOperationInInterceptor.class.getName());
    }

    public void handleMessage(Message message) {
        QName itf = null;
        QName op = null;
        QName svc = null;
        String ep = null;
        String nsUri = null;
        for (QName qname : message.getSoapHeaders().keySet()) {
            if (isWSANamespace(qname.getNamespaceURI())) {
                if (nsUri == null) {
                    nsUri = qname.getNamespaceURI();
                } else if (!nsUri.equals(qname.getNamespaceURI())) {
                    throw new SoapFault(SoapFault.SENDER, "Inconsistent use of wsa namespaces");
                }
                if (WSAddressingConstants.EL_ACTION.equals(qname.getLocalPart())) {
                    String action = getHeaderText(message.getSoapHeaders().get(qname));
                    String[] parts = URIResolver.split3(action);
                    itf = new QName(parts[0], parts[1]);
                    op = new QName(parts[0], parts[2]);
                } else if (WSAddressingConstants.EL_TO.equals(qname.getLocalPart())) {
                    String to = getHeaderText(message.getSoapHeaders().get(qname));
                    String[] parts = URIResolver.split3(to);
                    svc = new QName(parts[0], parts[1]);
                    ep = parts[2];
                } else {
                    // TODO: what ?
                }
            }
        }
        if (svc != null && ep != null) {
            try {
                ComponentContext ctx = message.get(ComponentContext.class);
                ServiceEndpoint se = ctx.getEndpoint(svc, ep);
                Document doc = ctx.getEndpointDescriptor(se);
                Definition def = WSDLFactory.newInstance().newWSDLReader().readWSDL(null, doc);
                Service wsdlSvc = def.getService(svc);
                if (wsdlSvc == null) {
                    if (def.getServices().size() == 0 && def.getPortTypes().size() == 1) {
                        PortType portType = (PortType) def.getPortTypes().values().iterator().next();
                        Definition newDef = PortTypeDecorator.createImportDef(def, svc.getNamespaceURI(), "urn:import");
                        PortTypeDecorator.decorate(newDef, portType, "jbi:" + svc.toString() + ":" + ep,
                                                   portType.getQName().getLocalPart() + "JBI",
                                                   svc.getLocalPart(), ep, "1.1");
                        wsdlSvc = newDef.getService(svc);
                    }
                }
                Port wsdlPort = wsdlSvc.getPort(ep);
                Binding b = BindingFactory.createBinding(wsdlPort);
                message.put(Binding.class, b);
                message.put(ServiceEndpoint.class, se);
            } catch (Exception e) {
                throw new SoapFault(e);
            }
        }
        if (itf != null && op != null) {
            Binding<?> binding = message.get(Binding.class);
            List<Operation<?>> matching = new ArrayList<Operation<?>>();
            for (Operation<?> operation : binding.getOperations()) {
                if (operation.getName().equals(op)) {
                    matching.add(operation);
                }
            }
            if (matching.size() == 1) {
                Operation operation = matching.get(0);
                message.put(Operation.class, operation);
                message.put(org.apache.servicemix.soap.api.model.Message.class,
                            isServer() ? operation.getOutput() : operation.getInput());
            }
        }
    }

    public Collection<URI> getRoles() {
        return Collections.emptyList();
    }

    public Collection<QName> getUnderstoodHeaders() {
        List<QName> h = new ArrayList<QName>();
        h.add(new QName(WSAddressingConstants.WSA_NAMESPACE_200303, WSAddressingConstants.EL_ACTION));
        h.add(new QName(WSAddressingConstants.WSA_NAMESPACE_200303, WSAddressingConstants.EL_TO));
        h.add(new QName(WSAddressingConstants.WSA_NAMESPACE_200403, WSAddressingConstants.EL_ACTION));
        h.add(new QName(WSAddressingConstants.WSA_NAMESPACE_200403, WSAddressingConstants.EL_TO));
        h.add(new QName(WSAddressingConstants.WSA_NAMESPACE_200408, WSAddressingConstants.EL_ACTION));
        h.add(new QName(WSAddressingConstants.WSA_NAMESPACE_200408, WSAddressingConstants.EL_TO));
        h.add(new QName(WSAddressingConstants.WSA_NAMESPACE_200508, WSAddressingConstants.EL_ACTION));
        h.add(new QName(WSAddressingConstants.WSA_NAMESPACE_200508, WSAddressingConstants.EL_TO));
        return h;
    }

}
