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

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.namespace.QName;

import org.apache.servicemix.soap.api.InterceptorChain;
import org.apache.servicemix.soap.api.InterceptorProvider.Phase;
import org.apache.servicemix.soap.api.Policy;
import org.apache.servicemix.soap.api.model.Binding;
import org.apache.servicemix.soap.bindings.soap.SoapFault;
import org.apache.servicemix.soap.bindings.soap.SoapVersion;
import org.apache.servicemix.soap.interceptors.jbi.JbiConstants;

public class JmsSoapConsumerMarshaler implements JmsConsumerMarshaler {

    private Binding<?> binding;
    private boolean useJbiWrapper = true;
    private Policy[] policies;
    
    /**
     * @return the binding
     */
    public Binding<?> getBinding() {
        return binding;
    }

    /**
     * @param binding the binding to set
     */
    public void setBinding(Binding<?> binding) {
        this.binding = binding;
    }

    /**
     * @return the policies
     */
    public Policy[] getPolicies() {
        return policies;
    }

    /**
     * @param policies the policies to set
     */
    public void setPolicies(Policy[] policies) {
        this.policies = policies;
    }

    /**
     * @return the useJbiWrapper
     */
    public boolean isUseJbiWrapper() {
        return useJbiWrapper;
    }

    /**
     * @param useJbiWrapper the useJbiWrapper to set
     */
    public void setUseJbiWrapper(boolean useJbiWrapper) {
        this.useJbiWrapper = useJbiWrapper;
    }

    public JmsContext createContext(Message message, ComponentContext context) throws Exception {
        return new Context(message, context);
    }

    public MessageExchange createExchange(JmsContext context) throws Exception {
        org.apache.servicemix.soap.api.Message msg = binding.createMessage();
        ((Context) context).msg = msg;
        msg.put(ComponentContext.class, ((Context) context).componentContext);
        msg.put(JbiConstants.USE_JBI_WRAPPER, useJbiWrapper);
        msg.setContent(InputStream.class, new ByteArrayInputStream(((TextMessage) context.getMessage()).getText().getBytes())); 
        InterceptorChain phase = getChain(Phase.ServerIn);
        phase.doIntercept(msg);
        return msg.getContent(MessageExchange.class);
    }

    public Message createOut(MessageExchange exchange, NormalizedMessage outMsg, Session session, JmsContext context) throws Exception {
        org.apache.servicemix.soap.api.Message in = ((Context) context).msg;
        org.apache.servicemix.soap.api.Message msg = binding.createMessage(in);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        msg.setContent(OutputStream.class, baos);
        msg.setContent(MessageExchange.class, exchange);
        msg.setContent(NormalizedMessage.class, outMsg);
        msg.put(SoapVersion.class, in.get(SoapVersion.class));
        msg.put(JbiConstants.USE_JBI_WRAPPER, useJbiWrapper);
        InterceptorChain phase = getChain(Phase.ServerOut);
        phase.doIntercept(msg);
        return session.createTextMessage(baos.toString());
    }
    
    public Message createFault(MessageExchange exchange, Fault fault, Session session, JmsContext context) throws Exception {
        org.apache.servicemix.soap.api.Message in = ((Context) context).msg;
        org.apache.servicemix.soap.api.Message msg = binding.createMessage(in);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        msg.setContent(OutputStream.class, baos);
        msg.setContent(MessageExchange.class, exchange);
        msg.setContent(NormalizedMessage.class, fault);
        msg.put(SoapVersion.class, in.get(SoapVersion.class));
        msg.put(JbiConstants.USE_JBI_WRAPPER, useJbiWrapper);
        InterceptorChain phase = getChain(Phase.ServerOutFault);
        QName code = (QName) fault.getProperty("org.apache.servicemix.soap.fault.code");
        String reason = (String) fault.getProperty("org.apache.servicemix.soap.fault.reason");
        SoapFault soapFault = new SoapFault(code, reason, null, null, fault.getContent());
        msg.setContent(Exception.class, soapFault);
        phase.doIntercept(msg);
        return session.createTextMessage(baos.toString());
    }

    public Message createError(MessageExchange exchange, Exception error, Session session, JmsContext context) throws Exception {
        org.apache.servicemix.soap.api.Message in = ((Context) context).msg;
        org.apache.servicemix.soap.api.Message msg = binding.createMessage(in);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        msg.setContent(OutputStream.class, baos);
        msg.setContent(MessageExchange.class, exchange);
        msg.put(SoapVersion.class, in.get(SoapVersion.class));
        msg.put(JbiConstants.USE_JBI_WRAPPER, useJbiWrapper);
        InterceptorChain phase = getChain(Phase.ServerOutFault);
        SoapFault soapFault;
        if (error instanceof SoapFault) {
            soapFault = (SoapFault) error;
        } else {
            soapFault = new SoapFault(error);
        }
        msg.setContent(Exception.class, soapFault);
        phase.doIntercept(msg);
        return session.createTextMessage(baos.toString());
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

    protected static class Context implements JmsContext {
        Message message;
        ComponentContext componentContext;
        org.apache.servicemix.soap.api.Message msg;
        Context(Message message, ComponentContext componentContext) {
            this.message = message;
            this.componentContext = componentContext;
        }
        public Message getMessage() {
            return this.message;
        }
    }

}
