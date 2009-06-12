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
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

import org.apache.servicemix.common.JbiConstants;
import org.apache.servicemix.soap.core.MessageImpl;
import org.apache.servicemix.soap.core.PhaseInterceptorChain;
import org.apache.servicemix.soap.interceptors.mime.AttachmentsInInterceptor;
import org.apache.servicemix.soap.interceptors.mime.AttachmentsOutInterceptor;
import org.apache.servicemix.soap.interceptors.xml.BodyOutInterceptor;
import org.apache.servicemix.soap.interceptors.xml.StaxInInterceptor;
import org.apache.servicemix.soap.interceptors.xml.StaxOutInterceptor;
import org.apache.servicemix.soap.util.stax.StaxSource;

public class DefaultConsumerMarshaler extends AbstractJmsMarshaler implements JmsConsumerMarshaler {
    
    private URI mep;
    private boolean rollbackOnError;
    private boolean rollbackOnErrorDefault;
    private boolean rollbackConfigured;

    public DefaultConsumerMarshaler() {
        this.mep = JbiConstants.IN_ONLY;
    }
    
    public DefaultConsumerMarshaler(URI mep) {
        this.mep = mep;
    }

    /**
     * @return the mep
     */
    public URI getMep() {
        return mep;
    }

    /**
     * @param mep the mep to set
     */
    public void setMep(URI mep) {
        this.mep = mep;
    }

    public boolean isRollbackOnError() {
        return rollbackConfigured ? rollbackOnError : rollbackOnErrorDefault;
    }

    /**
     * @param rollbackOnError if exchange in errors should cause a rollback on the JMS side
     */
    public void setRollbackOnError(boolean rollbackOnError) {
        rollbackConfigured = true;
        this.rollbackOnError = rollbackOnError;
    }

    /**
     * This is called to set intelligent defaults if no
     * explicit rollbackOnError configuration is set.
     * If setRollbackOnError is explicitly set, it
     * will be used.
     *
     * @param rollbackDefault default rollbackOnError setting
     */
    public void setRollbackOnErrorDefault(boolean rollbackDefault) {
        this.rollbackOnErrorDefault = rollbackDefault;
    }


    public JmsContext createContext(Message message) throws Exception {
        return new Context(message);
    }

    public MessageExchange createExchange(JmsContext jmsContext, ComponentContext jbiContext) throws Exception {
        Context ctx = (Context) jmsContext;
        MessageExchange exchange = jbiContext.getDeliveryChannel().createExchangeFactory().createExchange(mep);
        NormalizedMessage inMessage = exchange.createMessage();
        populateMessage(ctx.message, inMessage);
        if (isCopyProperties()) {
            copyPropertiesFromJMS(ctx.message, inMessage);
        }
        exchange.setMessage(inMessage, "in");
        return exchange;
    }

    public Message createOut(MessageExchange exchange, NormalizedMessage outMsg, Session session, JmsContext context) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PhaseInterceptorChain chain = new PhaseInterceptorChain();
        chain.add(new AttachmentsOutInterceptor());
        chain.add(new StaxOutInterceptor());
        chain.add(new BodyOutInterceptor());
        org.apache.servicemix.soap.api.Message msg = new MessageImpl();
        msg.setContent(Source.class, outMsg.getContent());
        msg.setContent(OutputStream.class, baos);
        for (String attId : (Set<String>) outMsg.getAttachmentNames()) {
            msg. getAttachments().put(attId, outMsg.getAttachment(attId));
        }
        chain.doIntercept(msg);
        TextMessage text = session.createTextMessage(baos.toString());
        if (msg.get(org.apache.servicemix.soap.api.Message.CONTENT_TYPE) != null) {
            text.setStringProperty(CONTENT_TYPE_PROPERTY,
                                   (String) msg.get(org.apache.servicemix.soap.api.Message.CONTENT_TYPE));
        }
        if (isCopyProperties()) {
            copyPropertiesFromNM(outMsg, text);
        }
        return text;
    }

    public Message createFault(MessageExchange exchange, Fault fault, Session session, JmsContext context) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PhaseInterceptorChain chain = new PhaseInterceptorChain();
        chain.add(new AttachmentsOutInterceptor());
        chain.add(new StaxOutInterceptor());
        chain.add(new BodyOutInterceptor());
        org.apache.servicemix.soap.api.Message msg = new MessageImpl();
        msg.setContent(Source.class, fault.getContent());
        msg.setContent(OutputStream.class, baos);
        for (String attId : (Set<String>) fault.getAttachmentNames()) {
            msg. getAttachments().put(attId, fault.getAttachment(attId));
        }
        chain.doIntercept(msg);
        TextMessage text = session.createTextMessage(baos.toString());
        if (msg.get(org.apache.servicemix.soap.api.Message.CONTENT_TYPE) != null) {
            text.setStringProperty(CONTENT_TYPE_PROPERTY,
                                   (String) msg.get(org.apache.servicemix.soap.api.Message.CONTENT_TYPE));
        }
        text.setBooleanProperty(FAULT_JMS_PROPERTY, true);
        if (isCopyProperties()) {
            copyPropertiesFromNM(fault, text);
        }
        return text;
    }

    public Message createError(MessageExchange exchange, Exception error, Session session, JmsContext context) throws Exception {
        if (rollbackOnError) {
            throw error;
        } else {
            ObjectMessage message = session.createObjectMessage(error);
            message.setBooleanProperty(ERROR_JMS_PROPERTY, true);
            return message;
        }
    }

    protected void populateMessage(Message message, NormalizedMessage normalizedMessage) throws Exception {
        if (message instanceof TextMessage) {
            PhaseInterceptorChain chain = new PhaseInterceptorChain();
            chain.add(new AttachmentsInInterceptor());
            chain.add(new StaxInInterceptor());
            org.apache.servicemix.soap.api.Message msg = new MessageImpl();
            msg.setContent(InputStream.class, new ByteArrayInputStream(((TextMessage) message).getText().getBytes()));
            if (message.propertyExists(CONTENT_TYPE_PROPERTY)) {
                msg.put(org.apache.servicemix.soap.api.Message.CONTENT_TYPE, message.getStringProperty(CONTENT_TYPE_PROPERTY));
            }
            chain.doIntercept(msg);
            XMLStreamReader xmlReader = msg.getContent(XMLStreamReader.class);
            normalizedMessage.setContent(new StaxSource(xmlReader));
            for (Map.Entry<String, DataHandler> attachment : msg.getAttachments().entrySet()) {
                normalizedMessage.addAttachment(attachment.getKey(), attachment.getValue());
            }
        } else {
            throw new UnsupportedOperationException("JMS message is not a TextMessage");
        }
    }

    protected static class Context implements JmsContext, Serializable {
        Message message;
        Context(Message message) {
            this.message = message;
        }
        public Message getMessage() {
            return this.message;
        }
        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeObject(message);
        }
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            message = (Message) in.readObject();
        }
    }

}
