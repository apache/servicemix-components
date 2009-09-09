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

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.muc.MultiUserChat;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

/**
 * Represents a group chat endpoint
 *
 * @version $Revision: $
 * @org.apache.xbean.XBean element="groupChatEndpoint"
 */
public class GroupChatEndpoint extends XMPPEndpoint implements XMPPEndpointType {

    private MultiUserChat chat;
    private String room;

    public void start() throws Exception {
        super.start();
        if (this.chat == null) {
            if (this.room == null) {
                throw new IllegalArgumentException("No room property specified");
            }
            this.chat = new MultiUserChat(getConnection(), this.room);
        }
    }

    public void stop() throws Exception {
        if (chat != null) {
            chat.leave();
            chat = null;
        }
        super.stop();
    }

    // Properties
    //-------------------------------------------------------------------------
    public MultiUserChat getChat() {
        return this.chat;
    }

    public void setChat(MultiUserChat chat) {
        this.chat = chat;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }


    // Implementation methods
    //-------------------------------------------------------------------------
    protected void processInOnly(MessageExchange exchange, NormalizedMessage normalizedMessage) throws Exception {
        Message message = chat.createMessage();
        message.setTo(room);
        message.setFrom(getUser());
        getMarshaler().fromJBI(message, exchange, normalizedMessage);
        chat.sendMessage(message);
        done(exchange);
    }
}
