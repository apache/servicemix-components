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
package org.apache.servicemix.xmpp.marshaler.impl;

import org.apache.servicemix.jbi.jaxp.SourceMarshaler;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.xmpp.marshaler.XMPPMarshalerSupport;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import java.util.Date;

/**
 * Marshals Jabber messages into and out of NMS messages
 *
 * @version $Revision: 429277 $
 */
public class DefaultXMPPMarshaler implements XMPPMarshalerSupport
{
    private SourceMarshaler sourceMarshaler;
    private static SourceTransformer st = new SourceTransformer();
    private String messageBodyTag = "message";
    private String messageBodyOpenTag = "<" + messageBodyTag + ">";
    private String messageBodyCloseTag = "</" + messageBodyTag + ">";

    public DefaultXMPPMarshaler() {
        this(new SourceMarshaler());
    }

    public DefaultXMPPMarshaler(SourceMarshaler sourceMarshaler) {
        this.sourceMarshaler = sourceMarshaler;
    }

    /**
     * converts the normalized message into a XMPP message
     *
     * @param message           the XMPP message to fill
     * @param exchange          the exchange to use as source
     * @param normalizedMessage the normalized message to use as source
     * @throws javax.xml.transform.TransformerException
     *          on conversion errors
     */
    public void fromJBI(Message message, MessageExchange exchange, NormalizedMessage normalizedMessage) throws TransformerException {
        // lets create a text message
        try {
            Document doc = st.toDOMDocument(normalizedMessage);
            NodeList ls = doc.getElementsByTagName(this.getMessageBodyTag());
            if (ls != null && ls.getLength()>0) {
                String text = ls.item(0).getTextContent();
                message.setBody(text);
            } else {
                throw new RuntimeException("Missing node for tag " + getMessageBodyTag());
            }
        } catch (Exception e) {
            String xml = messageAsString(normalizedMessage);
            message.setBody(xml);
        }

        addJabberProperties(message, exchange, normalizedMessage);
    }

    /**
     * converts the xmpp message to a normalized message
     *
     * @param normalizedMessage the normalized message to fill
     * @param packet            the xmpp packet to use
     * @throws javax.jbi.messaging.MessagingException
     *          on conversion errors
     */
    public void toJBI(NormalizedMessage normalizedMessage, Packet packet) throws MessagingException {
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

    public String getMessageBodyTag() {
        return messageBodyTag;
    }

    /**
     * Sets the XML open tag used to wrap inbound Jabber text messages
     *
     * @param messageBodyTag    the tag to use
     */
    public void setMessageBodyTag(String messageBodyTag) {
        this.messageBodyTag = messageBodyTag.replace("<", "").replace(">", "");
        this.messageBodyOpenTag = "<" + this.messageBodyTag + ">";
        this.messageBodyCloseTag = "</" + this.messageBodyTag + ">";
    }

    // Implementation methods
    //-------------------------------------------------------------------------

    /**
     * Converts the inbound message to a String that can be sent
     *
     * @param normalizedMessage the normalized message to transform to string
     * @return  the string content of the normalized message
     * @throws javax.xml.transform.TransformerException on conversion errors
     */
    protected String messageAsString(NormalizedMessage normalizedMessage) throws TransformerException {
        return sourceMarshaler.asString(normalizedMessage.getContent());
    }

    /**
     * Appends properties on the NMS to the JMS Message
     *
     * @param message               the xmpp message
     * @param exchange              the message exchange
     * @param normalizedMessage     the normalized message
     */
    protected void addJabberProperties(Message message, MessageExchange exchange, NormalizedMessage normalizedMessage) {
        for (Object o : normalizedMessage.getPropertyNames()) {
            String name = (String) o;
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
        for (String name : message.getPropertyNames()) {
            Object value = message.getProperty(name);
            normalizedMessage.setProperty(name, value);
        }
    }

    /**
     * Decides whether or not the given header should be included in the JMS message.
     * By default this includes all suitable typed values
     *
     * @param normalizedMessage the normalized message
     * @param name              the header name
     * @param value             the header value
     * @return  true if it should be included
     */
    protected boolean shouldIncludeHeader(NormalizedMessage normalizedMessage, String name, Object value) {
        return value instanceof String || value instanceof Number || value instanceof Date;
    }
}
