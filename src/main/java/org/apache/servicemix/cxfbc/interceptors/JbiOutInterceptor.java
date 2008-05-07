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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.model.SoapHeaderInfo;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingMessageInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.servicemix.cxfbc.WSAUtils;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.soap.interceptors.jbi.JbiConstants;
import org.apache.servicemix.soap.util.DomUtil;
import org.apache.servicemix.soap.util.QNameUtil;


public class JbiOutInterceptor extends AbstractPhaseInterceptor<Message> {

    public JbiOutInterceptor() {
        super(Phase.PRE_STREAM);
    }

    public void handleMessage(Message message) {
        MessageExchange me = message.get(MessageExchange.class);
        if (me == null) {
            me = message.getContent(MessageExchange.class);
        }
        NormalizedMessage nm = me.getMessage("in");
        fromNMSAttachments(message, nm);
        fromNMSHeaders(message, nm);
        extractHeaderFromMessagePart(message);
    }

    private void extractHeaderFromMessagePart(Message message) {
        Source source = message.getContent(Source.class);
        if (source == null) {
            return;
        }

        Element element;
        try {
            element = new SourceTransformer().toDOMElement(source);
        } catch (Exception e) {
            throw new Fault(e);
        }

        if (!JbiConstants.WSDL11_WRAPPER_NAMESPACE.equals(element
                .getNamespaceURI())
                || !JbiConstants.WSDL11_WRAPPER_MESSAGE_LOCALNAME
                        .equals(element.getLocalName())) {
            message.setContent(Source.class, new DOMSource(element));
            return;
        }

        BindingOperationInfo bop = message.getExchange().get(
                BindingOperationInfo.class);
        BindingMessageInfo msg = isRequestor(message) ? bop.getInput() : bop
                .getOutput();

        SoapBindingInfo binding = (SoapBindingInfo) message.getExchange().get(
                Endpoint.class).getEndpointInfo().getBinding();
        String style = binding.getStyle(bop.getOperationInfo());
        if (style == null) {
            style = binding.getStyle();
        }

        Element partWrapper = DomUtil.getFirstChildElement(element);
        while (partWrapper != null) {
            extractHeaderParts((SoapMessage) message, element, partWrapper, msg);
            partWrapper = DomUtil.getNextSiblingElement(partWrapper);
        }
        message.setContent(Source.class, new DOMSource(element));
    }

    private void extractHeaderParts(SoapMessage message,
            Element element, Element partWrapper, BindingMessageInfo msg) {
        List<NodeList> partsContent = new ArrayList<NodeList>();
        if (partWrapper != null) {
            if (!JbiConstants.WSDL11_WRAPPER_NAMESPACE.equals(element
                    .getNamespaceURI())
                    || !JbiConstants.WSDL11_WRAPPER_PART_LOCALNAME
                            .equals(partWrapper.getLocalName())) {
                throw new Fault(new Exception(
                        "Unexpected part wrapper element '"
                                + QNameUtil.toString(element) + "' expected '{"
                                + JbiConstants.WSDL11_WRAPPER_NAMESPACE
                                + "}part'"));
            }
            NodeList nodes = partWrapper.getChildNodes();
            partsContent.add(nodes);
        }

        List<Header> headerList = message.getHeaders();
        List<SoapHeaderInfo> headers = msg.getExtensors(SoapHeaderInfo.class);
        for (SoapHeaderInfo header : headers) {
            NodeList nl = partsContent.get(0);
            if (header.getPart().getConcreteName().getNamespaceURI().equals(
                    nl.item(0).getNamespaceURI())
                    && header.getPart().getConcreteName().getLocalPart()
                            .equals(nl.item(0).getLocalName())) {
                headerList.add(new Header(header.getPart().getConcreteName(),
                        nl.item(0)));
                partsContent.remove(0);
            }

        }

    }

    /**
     * Copy NormalizedMessage attachments to SoapMessage attachments
     */
    private void fromNMSAttachments(Message message,
            NormalizedMessage normalizedMessage) {
        Set attachmentNames = normalizedMessage.getAttachmentNames();
        Collection<Attachment> attachmentList = new ArrayList<Attachment>();
        for (Iterator it = attachmentNames.iterator(); it.hasNext();) {
            String id = (String) it.next();
            DataHandler handler = normalizedMessage.getAttachment(id);
            Attachment attachment = new AttachmentImpl(id, handler);
            attachmentList.add(attachment);
        }
        message.setAttachments(attachmentList);
        message.put(Message.CONTENT_TYPE, "application/soap+xml");
    }

    /**
     * Copy NormalizedMessage headers to SoapMessage headers
     */
    @SuppressWarnings("unchecked")
    private void fromNMSHeaders(Message message,
            NormalizedMessage normalizedMessage) {

        if (message instanceof SoapMessage) {

            Map<String, String> map = (Map<String, String>) normalizedMessage
                    .getProperty(WSAUtils.WSA_HEADERS_INBOUND);

            if (map != null) {
                AddressingProperties addressingProperties = WSAUtils
                        .getCXFAddressingPropertiesFromMap(map);
                ((SoapMessage) message).put(WSAUtils.WSA_HEADERS_INBOUND,
                        addressingProperties);
            }
        }

        if (normalizedMessage.getProperty(JbiConstants.PROTOCOL_HEADERS) != null) {
            Map<String, ?> headers = (Map<String, ?>) normalizedMessage
                    .getProperty(JbiConstants.PROTOCOL_HEADERS);
            for (Map.Entry<String, ?> entry : headers.entrySet()) {
                QName name = QNameUtil.parse(entry.getKey());
                if (name != null) {

                    Header header = new Header(name, entry.getValue());

                    if (message instanceof SoapMessage) {
                        List<Header> headerList = ((SoapMessage) message)
                                .getHeaders();
                        headerList.add(header);
                    }

                }
            }
        }
    }

    private boolean isRequestor(org.apache.cxf.message.Message message) {
        return Boolean.TRUE.equals(message
                .containsKey(org.apache.cxf.message.Message.REQUESTOR_ROLE));
    }

}
