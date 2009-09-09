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

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.packet.Message;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import java.net.URI;

/**
 * Represents an endpoint for chatting to a single individual
 *
 * @version $Revision: $
 * @org.apache.xbean.XBean element="privateChatEndpoint"
 */
public class PrivateChatEndpoint extends XMPPEndpoint implements XMPPEndpointType, MessageListener
{
    private Chat chat;
    private String participant;

    public void start() throws Exception {
        super.start();

        if (this.chat == null) {
            if (getParticipant() == null) {
                throw new IllegalArgumentException("No participant property specified");
            }
            if (getConnection().isConnected()) {
                logger.debug("Creating chat with " + getParticipant());
                this.chat = getConnection().getChatManager().createChat(getParticipant(), this);
            }
        }
    }

    public void stop() throws Exception {
        chat.removeMessageListener(this);
        chat = null;

        super.stop();
    }

    public void processMessage(Chat chat, Message message)
        {
        logger.debug("Received message from " + getParticipant() + ":\n" + message.toXML());
        }

    // Properties
    //-------------------------------------------------------------------------
    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public String getParticipant() {
        return participant;
    }

    public void setParticipant(String participant) {
        this.participant = participant;
    }


    public void setUri(URI uri) {
        super.setUri(uri);
        String path = uri.getPath();
        if (path != null) {
            // lets strip the leading slash to make an XMPP resource
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            setParticipant(path);
        }
    }


    // Implementation methods
    //-------------------------------------------------------------------------
    protected void processInOnly(MessageExchange exchange, NormalizedMessage normalizedMessage) throws Exception {
        Message message = new Message();
        getMarshaler().fromJBI(message, exchange, normalizedMessage);
        message.setTo(getParticipant());
        message.setFrom(getUser());
        message.setThread(exchange.getExchangeId());
        message.setType(Message.Type.normal);
        this.chat.sendMessage(message);
    }
}
