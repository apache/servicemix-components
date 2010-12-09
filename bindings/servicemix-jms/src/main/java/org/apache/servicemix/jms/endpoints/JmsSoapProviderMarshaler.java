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
package org.apache.servicemix.jms.endpoints;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.servicemix.soap.api.InterceptorChain;
import org.apache.servicemix.soap.api.InterceptorProvider.Phase;
import org.apache.servicemix.soap.api.Policy;
import org.apache.servicemix.soap.api.model.Binding;
import org.apache.servicemix.soap.interceptors.jbi.JbiConstants;

public class JmsSoapProviderMarshaler implements JmsProviderMarshaler {

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

    public Message createMessage(MessageExchange exchange, NormalizedMessage in, Session session) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        org.apache.servicemix.soap.api.Message msg = binding.createMessage();
        exchange.setProperty(org.apache.servicemix.soap.api.Message.class.getName(), null);
        msg.put(JbiConstants.USE_JBI_WRAPPER, useJbiWrapper);
        msg.setContent(MessageExchange.class, exchange);
        msg.setContent(NormalizedMessage.class, in);
        msg.setContent(OutputStream.class, baos);
        exchange.setProperty(Message.class.getName(), msg);

        InterceptorChain phaseOut = getChain(Phase.ClientOut);
        phaseOut.doIntercept(msg);
        TextMessage jmsMessage = session.createTextMessage();
        jmsMessage.setText(baos.toString());
        return jmsMessage;
    }

    public void populateMessage(Message message, MessageExchange exchange, NormalizedMessage normalizedMessage) throws Exception {
        org.apache.servicemix.soap.api.Message req = (org.apache.servicemix.soap.api.Message) exchange.getProperty(Message.class.getName());
        exchange.setProperty(org.apache.servicemix.soap.api.Message.class.getName(), null);
        org.apache.servicemix.soap.api.Message msg = binding.createMessage(req);
        msg.put(JbiConstants.USE_JBI_WRAPPER, useJbiWrapper);
        msg.setContent(MessageExchange.class, exchange);
        msg.setContent(NormalizedMessage.class, normalizedMessage);
        String str = ((TextMessage) message).getText();
        msg.setContent(InputStream.class, new ByteArrayInputStream(str.getBytes()));

        InterceptorChain phaseIn = getChain(Phase.ClientIn);
        phaseIn.doIntercept(msg);
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
