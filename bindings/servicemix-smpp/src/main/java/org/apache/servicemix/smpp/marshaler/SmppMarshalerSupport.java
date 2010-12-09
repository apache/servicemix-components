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
package org.apache.servicemix.smpp.marshaler;

import org.jsmpp.bean.MessageRequest;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.TransformerException;

/**
 * this is the common marshaler interface for all SMPP marshalers
 *
 * @author jbonofre
 * @author lhein
 */
public interface SmppMarshalerSupport {
    /**
     * Converts a normalized message from the NMR into SMPP message
     *
     * @param exchange the <code>MessageExchange</code>
     * @param message  the <code>NormalizedMessage</code>
     * @return the <code>MessageRequest</code> SMS content wrapper
     */
    MessageRequest fromNMS(MessageExchange exchange, NormalizedMessage message) throws TransformerException;

    /**
     * Converts the received SMPP message into a normalized message
     *
     * @param message the <code>NormalizedMessage</code> to send on the NMR
     * @param mr      the <code>MessageRequest</code> SMS content wrapper
     */
    void toNMS(NormalizedMessage message, MessageRequest mr) throws MessagingException;
}
