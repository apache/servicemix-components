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

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.transform.Source;
import javax.xml.stream.XMLStreamReader;
import javax.activation.DataHandler;

import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.soap.core.PhaseInterceptorChain;
import org.apache.servicemix.soap.core.MessageImpl;
import org.apache.servicemix.soap.interceptors.mime.AttachmentsInInterceptor;
import org.apache.servicemix.soap.interceptors.mime.AttachmentsOutInterceptor;
import org.apache.servicemix.soap.interceptors.xml.StaxInInterceptor;
import org.apache.servicemix.soap.interceptors.xml.StaxOutInterceptor;
import org.apache.servicemix.soap.interceptors.xml.BodyOutInterceptor;
import org.apache.servicemix.soap.util.stax.StaxSource;

public class DefaultProviderMarshaler extends AbstractJmsMarshaler implements
    JmsProviderMarshaler {

    private Map<String, Object> jmsProperties;
    private SourceTransformer transformer = new SourceTransformer();

    /**
     * @return the jmsProperties
     */
    public Map<String, Object> getJmsProperties() {
        return jmsProperties;
    }

    /**
     * @param jmsProperties the jmsProperties to set
     */
    public void setJmsProperties(Map<String, Object> jmsProperties) {
        this.jmsProperties = jmsProperties;
    }

    public Message createMessage(MessageExchange exchange, NormalizedMessage in, Session session) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PhaseInterceptorChain chain = new PhaseInterceptorChain();
        chain.add(new AttachmentsOutInterceptor());
        chain.add(new StaxOutInterceptor());
        chain.add(new BodyOutInterceptor());
        org.apache.servicemix.soap.api.Message msg = new MessageImpl();
        msg.setContent(Source.class, in.getContent());
        msg.setContent(OutputStream.class, baos);
        for (String attId : (Set<String>) in.getAttachmentNames()) {
            msg. getAttachments().put(attId, in.getAttachment(attId));
        }
        chain.doIntercept(msg);
        TextMessage text = session.createTextMessage(baos.toString());
        text.setStringProperty(org.apache.servicemix.soap.api.Message.CONTENT_TYPE,
                               (String) msg.get(org.apache.servicemix.soap.api.Message.CONTENT_TYPE));
        if (jmsProperties != null) {
            for (Map.Entry<String, Object> e : jmsProperties.entrySet()) {
                text.setObjectProperty(e.getKey(), e.getValue());
            }
        }

        if (isCopyProperties()) {
            copyPropertiesFromNM(in, text);
        }

        return text;
    }

    public void populateMessage(Message message, MessageExchange exchange, NormalizedMessage normalizedMessage) throws Exception {
        if (message instanceof TextMessage) {
            PhaseInterceptorChain chain = new PhaseInterceptorChain();
            chain.add(new AttachmentsInInterceptor());
            chain.add(new StaxInInterceptor());
            org.apache.servicemix.soap.api.Message msg = new MessageImpl();
            msg.setContent(InputStream.class, new ByteArrayInputStream(((TextMessage) message).getText().getBytes()));
            String contentType = message.getStringProperty(org.apache.servicemix.soap.api.Message.CONTENT_TYPE);
            if (contentType != null) {
                msg.put(org.apache.servicemix.soap.api.Message.CONTENT_TYPE, contentType);
            }
            chain.doIntercept(msg);
            XMLStreamReader xmlReader = msg.getContent(XMLStreamReader.class);
            normalizedMessage.setContent(new StaxSource(xmlReader));
            for (Map.Entry<String, DataHandler> attachment : msg.getAttachments().entrySet()) {
                normalizedMessage.addAttachment(attachment.getKey(), attachment.getValue());
            }
            if (isCopyProperties()) {
                copyPropertiesFromJMS(message, normalizedMessage);
            }
        } else {
            throw new UnsupportedOperationException("JMS message is not a TextMessage");
        }
    }

}
