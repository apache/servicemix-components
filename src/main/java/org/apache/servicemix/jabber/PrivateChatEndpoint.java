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
package org.apache.servicemix.jabber;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.packet.Message;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;

/**
 * Represents an endpoint for chatting to a single individual
 *
 * @version $Revision: $
 * @org.apache.xbean.XBean element="privateChatEndpoint"
 */
public class PrivateChatEndpoint extends JabberEndpoint {

    private Chat chat;
    private String participant;

    public PrivateChatEndpoint() {
    }

    public PrivateChatEndpoint(JabberComponent component, ServiceEndpoint serviceEndpoint) {
        super(component, serviceEndpoint);
    }

    public PrivateChatEndpoint(JabberComponent component, ServiceEndpoint serviceEndpoint, String participant) {
        super(component, serviceEndpoint);
        this.participant = participant;
    }

    public void start() throws Exception {
        super.start();
        if (chat == null) {
            if (participant == null) {
                throw new IllegalArgumentException("No participant property specified");
            }
            chat = getConnection().createChat(participant);
        }
    }

    public void stop() throws Exception {
        chat = null;
        super.stop();
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

    // Implementation methods
    //-------------------------------------------------------------------------
    protected void processInOnly(MessageExchange exchange, NormalizedMessage normalizedMessage) throws Exception {
        Message message = chat.createMessage();
        getMarshaler().fromNMS(message, normalizedMessage);
        chat.sendMessage(message);
        done(exchange);
    }
}
