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
package org.apache.servicemix.cxfbc.interceptors;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.model.SoapBodyInfo;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.URIMappingInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;

public class JbiOperationInterceptor extends AbstractPhaseInterceptor<Message> {

    public JbiOperationInterceptor() {
        super();
        setPhase(Phase.UNMARSHAL);
        addAfter(URIMappingInterceptor.class.getName());
    }

    public void handleMessage(Message message) {
        if (isGET(message)) {
            return;
        }
        if (message.getExchange().get(BindingOperationInfo.class) != null) {
            return;
        }
        DepthXMLStreamReader xmlReader = getXMLStreamReader(message);
        BindingOperationInfo operation = null;
        if (!StaxUtils.toNextElement(xmlReader)) {
            message.setContent(Exception.class, new RuntimeException("There must be a method name element."));
        }
        String opName = xmlReader.getLocalName();
        if (isRequestor(message) && opName.endsWith("Response")) {
            opName = opName.substring(0, opName.length() - 8);
        }
        QName opQName = new QName(xmlReader.getNamespaceURI(), opName);
        SoapBindingInfo binding = (SoapBindingInfo) message.getExchange().get(Endpoint.class).getEndpointInfo().getBinding();
        for (BindingOperationInfo op : binding.getOperations()) {
            String style = binding.getStyle(op.getOperationInfo());
            if (style == null) {
                style = binding.getStyle();
            }
            if ("document".equals(style)) {
                BindingMessageInfo msg = !isRequestor(message) ? op.getInput() : op.getOutput();
                if (opQName.equals(msg.getExtensor(SoapBodyInfo.class).getParts().get(0).getElementQName())) {
                    operation = op;
                    break;
                }
            } else {
                if (opQName.equals(op.getName())) {
                    operation = op;
                    break;
                }
            }
        }
        if (operation != null) {
            message.getExchange().put(BindingOperationInfo.class, operation);
            message.getExchange().put(OperationInfo.class, operation.getOperationInfo());
        }
    }

    protected DepthXMLStreamReader getXMLStreamReader(Message message) {
        XMLStreamReader xr = message.getContent(XMLStreamReader.class);
        if (xr instanceof DepthXMLStreamReader) {
            return (DepthXMLStreamReader) xr;
        }
        DepthXMLStreamReader dr = new DepthXMLStreamReader(xr);
        message.setContent(XMLStreamReader.class, dr);
        return dr;
    }


    protected BindingOperationInfo getOperation(Message message, QName opName) {
        BindingOperationInfo op = ServiceModelUtil.getOperation(message.getExchange(), opName);
        if (op == null) {
            throw new Fault(new Exception("Unrecognized operation"));
        }
        return op;
    }

    protected boolean isRequestor(Message message) {
        return Boolean.TRUE.equals(message.get(Message.REQUESTOR_ROLE));
    }

}
