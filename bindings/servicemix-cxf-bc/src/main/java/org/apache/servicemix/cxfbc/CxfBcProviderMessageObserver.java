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
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.binding.soap.interceptor.StartBodyInterceptor;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.addressing.RelatesToType;
import org.apache.cxf.ws.addressing.soap.MAPCodec;
import org.apache.servicemix.common.JbiConstants;
import org.apache.servicemix.cxfbc.interceptors.JbiInWsdl1Interceptor;
import org.apache.servicemix.cxfbc.interceptors.SchemaValidationInInterceptor;

import org.xml.sax.SAXException;

import com.sun.xml.bind.v2.runtime.reflect.ListIterator;

public class CxfBcProviderMessageObserver implements MessageObserver {
    ByteArrayOutputStream response = new ByteArrayOutputStream();

    boolean written;

    String contentType;


    private CxfBcProvider providerEndpoint;
    
    private MessageObserver sharedMessageObserver;

    public CxfBcProviderMessageObserver(CxfBcProvider providerEndpoint) {
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
            // create Interceptor chain
           
            PhaseManager pm = providerEndpoint.getBus().getExtension(
                    PhaseManager.class);
            List<Interceptor<? extends Message>> inList = new ArrayList<Interceptor<? extends Message>>();
            inList.add(new ReadHeadersInterceptor(this.providerEndpoint.getBus()));
            inList.add(new StartBodyInterceptor());
            inList.add(new MustUnderstandInterceptor());
            inList.add(new StaxInInterceptor());
            inList.add(new JbiInWsdl1Interceptor(this.providerEndpoint.isUseJBIWrapper(),
                    this.providerEndpoint.isUseSOAPEnvelope()));
            if (this.providerEndpoint.isSchemaValidationEnabled()) {
                inList.add(new SchemaValidationInInterceptor(this.providerEndpoint.isUseJBIWrapper(),
                        this.providerEndpoint.isUseSOAPEnvelope()));
            }
            inList.add(new AttachmentInInterceptor());
            PhaseInterceptorChain inChain = new PhaseInterceptorChain(pm.getInPhases());
            inChain.add(providerEndpoint.getBus().getInInterceptors());
            inChain.add(inList);
            inChain.add(providerEndpoint.getInInterceptors());
                   
            contentType = (String) message.get(Message.CONTENT_TYPE);
            SoapMessage soapMessage = 
                (SoapMessage) this.providerEndpoint.getCxfEndpoint().getBinding().createMessage(message);
                    
            soapMessage
                    .put(org.apache.cxf.message.Message.REQUESTOR_ROLE, true);
            soapMessage.setInterceptorChain(inChain);
            MessageExchange messageExchange = soapMessage.getExchange().get(MessageExchange.class);
            if (messageExchange == null) {
                // probably, that's a WS-RM Response; use the messageObserver defined in exchange
                MessageObserver messageObserver = message.getExchange().get(MessageObserver.class);
                if (messageObserver != null) {
                    messageObserver.onMessage(message);
                    return;
                } else {
                    //decoupled endpoint case we need try to restore the exchange first;
                    Exchange exchange = restoreExchange(soapMessage);
                    if (exchange != null) {
                        MessageObserver rmMessageObserver = exchange.get(MessageObserver.class);
                        if (rmMessageObserver != null) {
                            //means it createsequence messagee
                            sharedMessageObserver = rmMessageObserver;
                            rmMessageObserver.onMessage(soapMessage);
                            return;
                        }
                    } else {
                        //means it acknowlagement message
                        if (sharedMessageObserver != null) {
                            sharedMessageObserver.onMessage(soapMessage);
                            return;
                        }
                    }
                }
            }
            if (messageExchange != null && messageExchange.getStatus() != ExchangeStatus.ACTIVE) {
                return;
            }
                      
            
            
            inChain.doIntercept(soapMessage);
            closeConnectionStream(soapMessage);
            if (soapMessage.getContent(Exception.class) != null || soapMessage.getContent(Source.class) == null) {    
                Exception ex = soapMessage.getContent(Exception.class);
                if (!(soapMessage.getExchange().get(MessageExchange.class) instanceof InOnly) && ex != null) {
                    messageExchange.setStatus(ExchangeStatus.ERROR);
                    messageExchange.setError(ex);
                    providerEndpoint.getContext().getDeliveryChannel().send(
                            messageExchange);
                }
                return;
            }
          
            messageExchange = soapMessage.getExchange().get(MessageExchange.class);
            if (MessageUtils.isPartialResponse(soapMessage)) {
                //partial response for origianl channel when use decoupled endpoint
                return;
            }
            if (soapMessage.getExchange().get(BindingOperationInfo.class).getOperationInfo().isOneWay()) {
                messageExchange.setStatus(ExchangeStatus.DONE);
            } else if (soapMessage.get("jbiFault") != null
                    && soapMessage.get("jbiFault").equals(true)) {
                Fault fault = messageExchange.createFault();
                fault.setContent(soapMessage.getContent(Source.class));
                messageExchange.setFault(fault);
                if (soapMessage.get("faultstring") != null) {
                    messageExchange.setProperty("faultstring", soapMessage.get("faultstring"));
                }
                if (soapMessage.get("faultcode") != null) {
                    messageExchange.setProperty("faultcode", soapMessage.get("faultcode"));
                }
                if (soapMessage.get("hasdetail") != null) {
                    messageExchange.setProperty("hasdetail", soapMessage.get("hasdetail"));
                }

            } else if (messageExchange instanceof InOut) {
                NormalizedMessage msg = messageExchange.createMessage();
                msg.setContent(soapMessage.getContent(Source.class));
                toNMSAttachments(msg, soapMessage);
                messageExchange.setMessage(msg, "out");
            } else if (messageExchange instanceof InOptionalOut) {
                if (soapMessage.getContent(Source.class) != null) {
                    NormalizedMessage msg = messageExchange.createMessage();
                    msg.setContent(soapMessage.getContent(Source.class));
                    toNMSAttachments(msg, soapMessage);
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

    private Exchange restoreExchange(SoapMessage message) throws IOException, SAXException, JAXBException {
        InputStream is = message.getContent(InputStream.class);
        //cache the message
        CachedOutputStream bos = new CachedOutputStream();
        IOUtils.copy(is, bos);
        bos.flush();
        is.close();
        message.setContent(InputStream.class, bos.getInputStream());
        ReadHeadersInterceptor readHeadersInterceptor = 
                new ReadHeadersInterceptor(this.providerEndpoint.getBus());
        readHeadersInterceptor.handleMessage(message);
        for (Interceptor<?> interceptor : this.providerEndpoint.getBus().getOutInterceptors()) {
            if (interceptor.getClass().getName().equals("org.apache.cxf.ws.addressing.soap.MAPCodec")) {
                MAPCodec mapCodec = (MAPCodec) interceptor;
                AddressingProperties maps = mapCodec.unmarshalMAPs(message);
                if (maps != null
                    && maps.getRelatesTo() != null
                    && isRelationshipReply(maps.getRelatesTo())) { 
                    Exchange correlatedExchange =
                            mapCodec.getUncorrelatedExchanges().get(maps.getRelatesTo().getValue());
                    message.setContent(InputStream.class, bos.getInputStream());
                    bos.close();
                    XMLStreamReader xmlReader = 
                        StaxUtils.createXMLStreamReader(message.getContent(InputStream.class));
                    message.setContent(XMLStreamReader.class, xmlReader);       
                    message.setContent(InputStream.class, bos.getInputStream());
                    return correlatedExchange;
                    
                }
                
                
            }
        }
        message.setContent(InputStream.class, bos.getInputStream());
        bos.close();
        XMLStreamReader xmlReader = 
            StaxUtils.createXMLStreamReader(message.getContent(InputStream.class));
        message.setContent(XMLStreamReader.class, xmlReader);       
        message.setContent(InputStream.class, bos.getInputStream());
        return null;
    }

    private void closeConnectionStream(SoapMessage soapMessage) throws IOException {
        InputStream is = soapMessage.getContent(InputStream.class);
        if (is != null) {
            CachedOutputStream bos = new CachedOutputStream();
            IOUtils.copy(is, bos);
            bos.flush();
            is.close();
            soapMessage.setContent(InputStream.class, bos.getInputStream());
            bos.close();
        }

    }    

    private void toNMSAttachments(NormalizedMessage normalizedMessage,
            Message soapMessage) throws MessagingException {
        if (soapMessage.getAttachments() != null) {
            for (Attachment att : soapMessage.getAttachments()) {
                normalizedMessage.addAttachment(att.getId(), att
                        .getDataHandler());
            }
        }
    }

    private boolean isRelationshipReply(RelatesToType relatesTo) {
        return Names.WSA_RELATIONSHIP_REPLY.equals(relatesTo.getRelationshipType());
    }
    
}
