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
import java.util.ArrayList;
import java.util.List;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
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
import org.apache.cxf.phase.PhaseChainCache;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.MessageObserver;
import org.apache.servicemix.common.JbiConstants;
import org.apache.servicemix.cxfbc.interceptors.JbiInWsdl1Interceptor;


public class CxfBcProviderMessageObserver implements MessageObserver {
    ByteArrayOutputStream response = new ByteArrayOutputStream();

    boolean written;

    String contentType;


    private CxfBcProvider providerEndpoint;

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
            MessageExchange messageExchange = message.getExchange().get(MessageExchange.class);
            if (messageExchange == null) {
                // probably, that's a WS-RM Response; use the messageObserver defined in exchange
                MessageObserver messageObserver = message.getExchange().get(MessageObserver.class);
                if (messageObserver != null) {
                    messageObserver.onMessage(message);
                    return;
                }
            }
            if (messageExchange != null && messageExchange.getStatus() != ExchangeStatus.ACTIVE) {
                return;
            }

                       
            contentType = (String) message.get(Message.CONTENT_TYPE);
            SoapMessage soapMessage = 
                (SoapMessage) this.providerEndpoint.getCxfEndpoint().getBinding().createMessage(message);
            
            
            soapMessage
                    .put(org.apache.cxf.message.Message.REQUESTOR_ROLE, true);
            
            // create Interceptor chain

            PhaseChainCache inboundChainCache = new PhaseChainCache();
            PhaseManager pm = providerEndpoint.getBus().getExtension(
                    PhaseManager.class);
            List<Interceptor> inList = new ArrayList<Interceptor>();
            inList.add(new ReadHeadersInterceptor(this.providerEndpoint.getBus()));
            inList.add(new MustUnderstandInterceptor());
            inList.add(new StaxInInterceptor());
            inList.add(new JbiInWsdl1Interceptor(this.providerEndpoint.isUseJBIWrapper(),
            		this.providerEndpoint.isUseSOAPEnvelope()));
            inList.add(new AttachmentInInterceptor());
            PhaseInterceptorChain inChain = inboundChainCache.get(pm
                    .getInPhases(), inList);
            inChain.add(providerEndpoint.getInInterceptors());
            soapMessage.setInterceptorChain(inChain);
            inChain.doIntercept(soapMessage);
            closeConnectionStream(soapMessage);
            if (soapMessage.getContent(Source.class) == null) {
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

}
