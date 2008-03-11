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
package org.apache.servicemix.cxfbc;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.PhaseChainCache;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.MessageObserver;
import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.cxfbc.interceptors.JbiInWsdl1Interceptor;

public class CxfBcProviderMessageObserver implements MessageObserver {
    ByteArrayOutputStream response = new ByteArrayOutputStream();

    boolean written;

    String contentType;

    private MessageExchange messageExchange;

    private CxfBcProvider providerEndpoint;

    public CxfBcProviderMessageObserver(MessageExchange exchange,
            CxfBcProvider providerEndpoint) {
        this.messageExchange = exchange;
        this.providerEndpoint = providerEndpoint;
    }

    public ByteArrayOutputStream getResponseStream() throws Exception {
        synchronized (this) {
            if (!written) {
                wait(1000000000);
            }
        }
        return response;
    }

    public String getResponseContentType() {
        return contentType;
    }

    public void onMessage(Message message) {
        try {
            if (messageExchange.getStatus() != ExchangeStatus.ACTIVE) {
                return;
            }

            contentType = (String) message.get(Message.CONTENT_TYPE);
            SoapMessage soapMessage = 
                (SoapMessage) this.providerEndpoint.getCxfEndpoint().getBinding().createMessage(message);

            // create XmlStreamReader
            BindingOperationInfo boi = providerEndpoint.getEndpointInfo()
                    .getBinding().getOperation(messageExchange.getOperation());
            if (boi.getOperationInfo().isOneWay()) {
                return;
            }
            XMLStreamReader xmlStreamReader = createXMLStreamReaderFromMessage(soapMessage);
            soapMessage.setContent(XMLStreamReader.class, xmlStreamReader);
            soapMessage
                    .put(org.apache.cxf.message.Message.REQUESTOR_ROLE, true);
            Exchange cxfExchange = new ExchangeImpl();
            soapMessage.setExchange(cxfExchange);

            cxfExchange.put(BindingOperationInfo.class, boi);
            cxfExchange.put(Endpoint.class, providerEndpoint.getCxfEndpoint());
            // create Interceptor chain

            PhaseChainCache inboundChainCache = new PhaseChainCache();
            PhaseManager pm = providerEndpoint.getBus().getExtension(
                    PhaseManager.class);
            List<Interceptor> inList = new ArrayList<Interceptor>();
            
            inList.add(new JbiInWsdl1Interceptor(this.providerEndpoint.isUseJBIWrapper()));

            PhaseInterceptorChain inChain = inboundChainCache.get(pm
                    .getInPhases(), inList);
            inChain.add(providerEndpoint.getInInterceptors());
            inChain.add(providerEndpoint.getInFaultInterceptors());
            soapMessage.setInterceptorChain(inChain);
            inChain.doIntercept(soapMessage);
           
            if (boi.getOperationInfo().isOneWay()) {
                messageExchange.setStatus(ExchangeStatus.DONE);
            } else if (soapMessage.get("jbiFault") != null
                    && soapMessage.get("jbiFault").equals(true)) {
                Fault fault = messageExchange.createFault();
                fault.setContent(soapMessage.getContent(Source.class));
                messageExchange.setFault(fault);
            } else if (messageExchange instanceof InOut) {
                NormalizedMessage msg = messageExchange.createMessage();
                msg.setContent(soapMessage.getContent(Source.class));
                messageExchange.setMessage(msg, "out");
            } else if (messageExchange instanceof InOptionalOut) {
                if (soapMessage.getContent(Source.class) != null) {
                    NormalizedMessage msg = messageExchange.createMessage();
                    msg.setContent(soapMessage.getContent(Source.class));
                    messageExchange.setMessage(msg, "out");
                } else {
                    messageExchange.setStatus(ExchangeStatus.DONE);
                }
            } else {
                messageExchange.setStatus(ExchangeStatus.DONE);

            }
            boolean txSync = messageExchange.getStatus() == ExchangeStatus.ACTIVE
                    && messageExchange.isTransacted()
                    && Boolean.TRUE.equals(messageExchange
                            .getProperty(JbiConstants.SEND_SYNC));
            if (txSync) {
                providerEndpoint.getContext().getDeliveryChannel().sendSync(
                        messageExchange);
            } else {
                providerEndpoint.getContext().getDeliveryChannel().send(
                        messageExchange);
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            synchronized (this) {
                written = true;
                notifyAll();
            }
        }
    }

    private XMLStreamReader createXMLStreamReaderFromMessage(Message message) {
        XMLStreamReader xmlReader = null;
        StreamSource bodySource = new StreamSource(message
                .getContent(InputStream.class));
        xmlReader = StaxUtils.createXMLStreamReader(bodySource);
        
        findBody(message, xmlReader);
        
        return xmlReader;
    }
    
    private void findBody(Message message, XMLStreamReader xmlReader) {
        DepthXMLStreamReader reader = new DepthXMLStreamReader(xmlReader);
        try {
            int depth = reader.getDepth();
            int event = reader.getEventType();
            while (reader.getDepth() >= depth && reader.hasNext()) {
                QName name = null;
                if (event == XMLStreamReader.START_ELEMENT) {
                    name = reader.getName();
                }
                if (event == XMLStreamReader.START_ELEMENT && name.equals(((SoapMessage)message).getVersion().getBody())) {
                    reader.nextTag();
                    return;
                }
                event = reader.next();
            }
            return;
        } catch (XMLStreamException e) {
            throw new RuntimeException("Couldn't parse stream.", e);
        }
    }
}
