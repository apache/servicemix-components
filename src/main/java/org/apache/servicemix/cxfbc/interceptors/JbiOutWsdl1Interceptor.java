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


import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.model.SoapHeaderInfo;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.NSStack;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.util.QNameUtil;
import org.apache.servicemix.soap.interceptors.jbi.JbiConstants;
import org.apache.servicemix.soap.util.DomUtil;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class JbiOutWsdl1Interceptor extends AbstractSoapInterceptor {

    public JbiOutWsdl1Interceptor() {
        setPhase(Phase.MARSHAL);
    }
    
    public void handleMessage(SoapMessage message) {
        try {
            Source source = message.getContent(Source.class);
            Element element = new SourceTransformer().toDOMElement(source);
            if (!JbiConstants.WSDL11_WRAPPER_NAMESPACE.equals(element.getNamespaceURI()) ||
                !JbiConstants.WSDL11_WRAPPER_MESSAGE_LOCALNAME.equals(element.getLocalName())) {
                throw new Fault(new Exception("Message wrapper element is '" + QNameUtil.toString(element)
                        + "' but expected '{" + JbiConstants.WSDL11_WRAPPER_NAMESPACE + "}message'"));
            }
            List<NodeList> partsContent = new ArrayList<NodeList>();
            Element partWrapper = DomUtil.getFirstChildElement(element);
            while (partWrapper != null) {
                if (!JbiConstants.WSDL11_WRAPPER_NAMESPACE.equals(element.getNamespaceURI()) ||
                    !JbiConstants.WSDL11_WRAPPER_PART_LOCALNAME.equals(partWrapper.getLocalName())) {
                    throw new Fault(new Exception("Unexpected part wrapper element '" + QNameUtil.toString(element)
                            + "' expected '{" + JbiConstants.WSDL11_WRAPPER_NAMESPACE + "}part'"));
                }
                NodeList nodes = partWrapper.getChildNodes();
                partsContent.add(nodes);
                partWrapper = DomUtil.getNextSiblingElement(partWrapper);
            }
            
            BindingOperationInfo bop = message.getExchange().get(BindingOperationInfo.class);
            BindingMessageInfo msg = isRequestor(message) ? bop.getInput() : bop.getOutput();
            
            XMLStreamWriter xmlWriter = message.getContent(XMLStreamWriter.class);
    
            SoapBindingInfo binding = (SoapBindingInfo) message.getExchange().get(Endpoint.class).getEndpointInfo().getBinding();
            String style = binding.getStyle(bop.getOperationInfo());
            if (style == null) {
                style = binding.getStyle();
            }
            List<SoapHeaderInfo> headers = msg.getExtensors(SoapHeaderInfo.class);
            for (SoapHeaderInfo header : headers) {
                NodeList nl = partsContent.get(header.getPart().getIndex());
                Element headerElement = message.getHeaders(Element.class);
                for (int i = 0; i < nl.getLength(); i++) {
                    headerElement.appendChild(nl.item(i));
                }
            }
    
            if ("rpc".equals(style)) {
                addOperationNode(message, xmlWriter);
            }
            for (NodeList nl : partsContent) {
                for (int i = 0; i < nl.getLength(); i++) {
                    Node n = nl.item(i);
                    StaxUtils.writeNode(n, xmlWriter, false);
                }
            }
            if ("rpc".equals(style)) {
                xmlWriter.writeEndElement();
            }
        } catch (Fault e) {
            throw e;
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    protected String addOperationNode(Message message, 
                                      XMLStreamWriter xmlWriter) throws XMLStreamException {
        String responseSuffix = !isRequestor(message) ? "Response" : "";
        BindingOperationInfo boi = message.getExchange().get(BindingOperationInfo.class);
        String ns = boi.getName().getNamespaceURI();
        NSStack nsStack = new NSStack();
        nsStack.push();
        nsStack.add(ns);
        String prefix = nsStack.getPrefix(ns);
        StaxUtils.writeStartElement(xmlWriter, prefix, boi.getName().getLocalPart() + responseSuffix, ns);
        return ns;
    }

}
