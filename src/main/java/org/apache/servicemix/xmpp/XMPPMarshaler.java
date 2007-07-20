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
package org.apache.servicemix.xmpp;

import java.util.Date;
import java.util.Iterator;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.apache.servicemix.jbi.jaxp.SourceMarshaler;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

/**
 * Marshals Jabber messages into and out of NMS messages
 *
 * @version $Revision: 429277 $
 */
public class XMPPMarshaler {
    private SourceMarshaler sourceMarshaler;
    private String messageBodyOpenTag = "<message>";
    private String messageBodyCloseTag = "</message>";

    public XMPPMarshaler() {
        this(new SourceMarshaler());
    }

    public XMPPMarshaler(SourceMarshaler sourceMarshaler) {
        this.sourceMarshaler = sourceMarshaler;
    }

    /**
     * Marshals the Jabber message into an NMS message
     *
     * @throws javax.jbi.messaging.MessagingException
     *
     */
    public void toNMS(NormalizedMessage normalizedMessage, Packet packet) throws MessagingException {
        addNmsProperties(normalizedMessage, packet);
        if (packet instanceof Message) {
            Message message = (Message) packet;
            String text = message.getBody();
            if (text != null) {
                Source source = sourceMarshaler.asSource(messageBodyOpenTag + text + messageBodyCloseTag);
                normalizedMessage.setContent(source);
            }
        }

        // lets add the packet to the NMS
        normalizedMessage.setProperty("org.apache.servicemix.xmpp.packet", packet);
    }

    /**
     * Marshals from the Jabber message to the normalized message
     *
     * @param message
     * @param exchange
     * @param normalizedMessage @throws javax.xml.transform.TransformerException
     */
    public void fromNMS(Message message, MessageExchange exchange, NormalizedMessage normalizedMessage) throws TransformerException {
        // lets create a text message
        String xml = messageAsString(normalizedMessage);
        message.setBody(xml);
        addJabberProperties(message, exchange, normalizedMessage);
    }

    // Properties
    //-------------------------------------------------------------------------

    /**
     * @return the sourceMarshaler
     */
    public SourceMarshaler getSourceMarshaler() {
        return sourceMarshaler;
    }

    /**
     * @param sourceMarshaler the sourceMarshaler to set
     */
    public void setSourceMarshaler(SourceMarshaler sourceMarshaler) {
        this.sourceMarshaler = sourceMarshaler;
    }

    public String getMessageBodyOpenTag() {
        return messageBodyOpenTag;
    }

    /**
     * Sets the XML open tag used to wrap inbound Jabber text messages
     */
    public void setMessageBodyOpenTag(String messageBodyOpenTag) {
        this.messageBodyOpenTag = messageBodyOpenTag;
    }

    public String getMessageBodyCloseTag() {
        return messageBodyCloseTag;
    }

    /**
     * Sets the XML close tag used to wrap inbound Jabber text messages
     */
    public void setMessageBodyCloseTag(String messageBodyCloseTag) {
        this.messageBodyCloseTag = messageBodyCloseTag;
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
    protected void addJabberProperties(Message message, MessageExchange exchange, NormalizedMessage normalizedMessage) {
        for (Iterator iter = normalizedMessage.getPropertyNames().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            Object value = normalizedMessage.getProperty(name);
            if (shouldIncludeHeader(normalizedMessage, name, value)) {
                message.setProperty(name, value);
            }
        }
        message.setProperty("exchangeId", exchange.getExchangeId());
        setProperty(message, "interface", exchange.getInterfaceName());
        setProperty(message, "operation", exchange.getOperation());
        setProperty(message, "service", exchange.getService());
        //message.setProperty("pattern", exchange.getPattern());
        //message.setProperty("role", exchange.getRole());
        ServiceEndpoint endpoint = exchange.getEndpoint();
        if (endpoint != null) {
            message.setProperty("endpointName", endpoint.getEndpointName());
        }
    }

    protected void setProperty(Message message, String name, QName qName) {
        if (qName != null) {
            message.setProperty(name, qName.toString());
        }
    }

    protected void addNmsProperties(NormalizedMessage normalizedMessage, Packet message) {
        Iterator iter = message.getPropertyNames();
        while (iter.hasNext()) {
            String name = (String) iter.next();
            Object value = message.getProperty(name);
            normalizedMessage.setProperty(name, value);
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
