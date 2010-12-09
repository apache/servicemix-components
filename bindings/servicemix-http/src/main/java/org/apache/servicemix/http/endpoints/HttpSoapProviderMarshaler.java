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
package org.apache.servicemix.http.endpoints;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.HashMap;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.http.jetty.SmxHttpExchange;
import org.apache.servicemix.soap.api.InterceptorChain;
import org.apache.servicemix.soap.api.InterceptorProvider.Phase;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.api.Policy;
import org.apache.servicemix.soap.api.model.Binding;
import org.apache.servicemix.soap.interceptors.jbi.JbiConstants;
import org.apache.servicemix.soap.interceptors.xml.StaxInInterceptor;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.HttpHeaders;

/**
 * 
 * @author gnodet
 * @since 3.2
 */
public class HttpSoapProviderMarshaler extends AbstractHttpProviderMarshaler implements HttpProviderMarshaler {

    private Binding<?> binding;
    private boolean useJbiWrapper = true;
    private Policy[] policies;
    private String baseUrl;
    private Map<Phase, InterceptorChain> chains = new HashMap<Phase, InterceptorChain>();
    private String soapVersion = "1.2";

    public Binding<?> getBinding() {
        return binding;
    }

    public void setBinding(Binding<?> binding) {
        this.binding = binding;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public boolean isUseJbiWrapper() {
        return useJbiWrapper;
    }

    public void setUseJbiWrapper(boolean useJbiWrapper) {
        this.useJbiWrapper = useJbiWrapper;
    }

    public Policy[] getPolicies() {
        return policies;
    }

    public void setPolicies(Policy[] policies) {
        this.policies = policies;
    }
    
    public String getSoapVersion() {
        return this.soapVersion;
    }
    
    public void setSoapVersion(String soapVersion) {
        this.soapVersion = soapVersion;
    }
    
    public void createRequest(final MessageExchange exchange,
                              final NormalizedMessage inMsg, 
                              final SmxHttpExchange httpExchange) throws Exception {
        if (getContentEncoding() != null) {
            httpExchange.setRequestHeader(HttpHeaders.CONTENT_ENCODING, getContentEncoding());
        }
        if (getAcceptEncoding() != null) {
            httpExchange.setRequestHeader(HttpHeaders.ACCEPT_ENCODING, getAcceptEncoding());
        }
        // set the content type depending of the SOAP version
        if (soapVersion.equals("1.1")) {
            httpExchange.setRequestContentType("text/xml");
        } else {
            httpExchange.setRequestContentType("application/soap+xml");
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStream encodingStream = getRequestEncodingStream(getContentEncoding(), baos);
        Message msg = binding.createMessage();
        msg.put(JbiConstants.USE_JBI_WRAPPER, useJbiWrapper);
        msg.setContent(MessageExchange.class, exchange);
        msg.setContent(NormalizedMessage.class, inMsg);
        msg.setContent(OutputStream.class, encodingStream);
        exchange.setProperty(Message.class.getName(), msg);

        InterceptorChain phaseOut = getChain(Phase.ClientOut);
        phaseOut.doIntercept(msg);
        encodingStream.close();
        httpExchange.setMethod(HttpMethods.POST);
        httpExchange.setURL(baseUrl);
        httpExchange.setRequestContent(new ByteArrayBuffer(baos.toByteArray()));
        for (Map.Entry<String,String> entry : msg.getTransportHeaders().entrySet()) {
            if (!isBlackListed(entry.getKey())) {
                httpExchange.addRequestHeader(entry.getKey(), entry.getValue());
            }
        }
        /*
        httpExchange.setRequestEntity(new Entity() {
            public void write(OutputStream os, Writer w) throws IOException {
                // TODO: handle http headers: Content-Type, ... 
            }
        });
        */
        // TODO: use streaming when appropriate (?)
    }

    public void handleResponse(MessageExchange exchange, SmxHttpExchange httpExchange) throws Exception {
        Message req = (Message) exchange.getProperty(Message.class.getName());
        exchange.setProperty(Message.class.getName(), null);
        Message msg = binding.createMessage(req);
        msg.put(JbiConstants.USE_JBI_WRAPPER, useJbiWrapper);
        msg.setContent(MessageExchange.class, exchange);
        msg.setContent(InputStream.class, getResponseEncodingStream(
                    httpExchange.getResponseFields().getStringField(HttpHeaders.CONTENT_ENCODING),
                    httpExchange.getResponseStream()));
        msg.put(StaxInInterceptor.ENCODING, httpExchange.getResponseEncoding());
        InterceptorChain phaseOut = getChain(Phase.ClientIn);
        phaseOut.doIntercept(msg);
        // TODO: Retrieve headers ? 
    }

    public void handleException(MessageExchange exchange, SmxHttpExchange httpExchange, Throwable ex) {
        exchange.setError((Exception)ex);
    }


    protected InterceptorChain getChain(Phase phase) {
        InterceptorChain chain = chains.get(phase);
        if (chain == null) {
            chain = binding.getInterceptorChain(phase);
            if (policies != null) {
                for (int i = 0; i < policies.length; i++) {
                    chain.add(policies[i].getInterceptors(phase));
                }
            }
            chains.put(phase, chain);
        }
        return chain;
    }

}
