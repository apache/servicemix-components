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
package org.apache.servicemix.xmpp.marshaler;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.TransformerException;

/**
 * @author lhein
 */
public interface XMPPMarshalerSupport
{
    /**
     * converts the xmpp message to a normalized message
     *
     * @param normalizedMessage     the normalized message to fill
     * @param packet                the xmpp packet to use
     * @throws MessagingException   on conversion errors
     */
    void toJBI(NormalizedMessage normalizedMessage, Packet packet) throws MessagingException;

    /**
     * converts the normalized message into a XMPP message
     *
     * @param message               the XMPP message to fill
     * @param exchange              the exchange to use as source
     * @param normalizedMessage     the normalized message to use as source
     * @throws TransformerException on conversion errors
     */
    void fromJBI(Message message, MessageExchange exchange, NormalizedMessage normalizedMessage) throws TransformerException;
}
