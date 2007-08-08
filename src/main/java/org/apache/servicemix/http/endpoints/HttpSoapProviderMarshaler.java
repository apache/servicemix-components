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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

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

/**
 * 
 * @author gnodet
 * @since 3.2
 */
public class HttpSoapProviderMarshaler implements HttpProviderMarshaler {

    private Binding<?> binding;
    private boolean useJbiWrapper = true;
    private Policy[] policies;
    private String baseUrl;

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

    public void createRequest(final MessageExchange exchange, 
                              final NormalizedMessage inMsg, 
                              final SmxHttpExchange httpExchange) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Message msg = binding.createMessage();
        msg.put(JbiConstants.USE_JBI_WRAPPER, useJbiWrapper);
        msg.setContent(MessageExchange.class, exchange);
        msg.setContent(NormalizedMessage.class, inMsg);
        msg.setContent(OutputStream.class, baos);
        exchange.setProperty(Message.class.getName(), msg);

        InterceptorChain phaseOut = getChain(Phase.ClientOut);
        phaseOut.doIntercept(msg);
        httpExchange.setMethod(HttpMethods.POST);
        httpExchange.setURL(baseUrl);
        httpExchange.setRequestContent(new ByteArrayBuffer(baos.toByteArray()));
        /*
        httpExchange.setRequestEntity(new Entity() {
            public void write(OutputStream os, Writer w) throws IOException {
                // TODO: handle http headers: Content-Type, ... 
            }
        });
        */
        // TODO: add transport headers
        // TODO: use streaming when appropriate (?)
    }

    public void handleResponse(MessageExchange exchange, SmxHttpExchange httpExchange) throws Exception {
        Message req = (Message) exchange.getProperty(Message.class.getName());
        exchange.setProperty(Message.class.getName(), null);
        Message msg = binding.createMessage(req);
        msg.put(JbiConstants.USE_JBI_WRAPPER, useJbiWrapper);
        msg.setContent(MessageExchange.class, exchange);
        msg.setContent(InputStream.class, new ByteArrayInputStream(httpExchange.getResponseData()));
        msg.put(StaxInInterceptor.ENCODING, httpExchange.getResponseEncoding());
        InterceptorChain phaseOut = getChain(Phase.ClientIn);
        phaseOut.doIntercept(msg);
        // TODO: Retrieve headers ? 
    }

    protected InterceptorChain getChain(Phase phase) {
        InterceptorChain chain = binding.getInterceptorChain(phase);
        if (policies != null) {
            for (int i = 0; i < policies.length; i++) {
                chain.add(policies[i].getInterceptors(phase));
            }
        }
        return chain;
    }

}
