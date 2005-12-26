/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.jms;

import org.apache.servicemix.jbi.jaxp.SourceMarshaler;

import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;

/**
 * Marshalls JMS messages into and out of NMS messages
 *
 * @version $Revision$
 */
public class JmsMarshaler {
    private SourceMarshaler sourceMarshaler;

    public JmsMarshaler() {
        this(new SourceMarshaler());
    }

    public JmsMarshaler(SourceMarshaler sourceMarshaler) {
        this.sourceMarshaler = sourceMarshaler;
    }

    /**
     * Marshalls the JMS message into an NMS message
     *
     * @throws MessagingException
     */
    public void toNMS(NormalizedMessage normalizedMessage, Message message) throws JMSException, MessagingException {
        addNmsProperties(normalizedMessage, message);
        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            Source source = sourceMarshaler.asSource(textMessage.getText());
            normalizedMessage.setContent(source);
        }

        // lets add the message to the NMS
        normalizedMessage.setProperty("org.apache.servicemix.jms.message", message);
    }

    public Message createMessage(NormalizedMessage normalizedMessage, Session session) throws JMSException, TransformerException {
        // lets create a text message
        String xml = messageAsString(normalizedMessage);
        TextMessage message = session.createTextMessage(xml);
        addJmsProperties(message, normalizedMessage);
        return message;
    }

    // Properties
    //-------------------------------------------------------------------------
    public SourceMarshaler getSourceMarshaller() {
        return sourceMarshaler;
    }

    public void setSourceMarshaller(SourceMarshaler sourceMarshaler) {
        this.sourceMarshaler = sourceMarshaler;
    }

    // Implementation methods
    //-------------------------------------------------------------------------

    /**
     * Converts the inbound message to a String that can be sent
     */
    protected String messageAsString(NormalizedMessage normalizedMessage) throws TransformerException {
        return sourceMarshaler.asString(normalizedMessage.getContent());
    }

    /**
     * Appends properties on the NMS to the JMS Message
     */
    protected void addJmsProperties(Message message, NormalizedMessage normalizedMessage) throws JMSException {
        for (Iterator iter = normalizedMessage.getPropertyNames().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            Object value = normalizedMessage.getProperty(name);
            if (shouldIncludeHeader(normalizedMessage, name, value)) {
                message.setObjectProperty(name, value);
            }
        }
    }

    protected void addNmsProperties(NormalizedMessage message, Message jmsMessage) throws JMSException {
        Enumeration enumeration = jmsMessage.getPropertyNames();
        while (enumeration.hasMoreElements()) {
            String name = (String) enumeration.nextElement();
            Object value = jmsMessage.getObjectProperty(name);
            message.setProperty(name, value);
        }
    }

    /**
     * Decides whether or not the given header should be included in the JMS message.
     * By default this includes all suitable typed values
     */
    protected boolean shouldIncludeHeader(NormalizedMessage normalizedMessage, String name, Object value) {
        return value instanceof String || value instanceof Number || value instanceof Date;
    }
}
