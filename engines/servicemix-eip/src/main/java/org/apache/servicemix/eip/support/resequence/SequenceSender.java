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
package org.apache.servicemix.eip.support.resequence;

import java.util.List;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;

/**
 * A sender for synchronously sending message exchanges.
 * 
 * @author Martin Krasser
 */
public interface SequenceSender {

    /**
     * Synchronously sends a single message exchange.
     * 
     * @param exchange a message exchange.
     * @throws MessagingException if a system-level error occurs.
     */
    void sendSync(MessageExchange exchange) throws MessagingException;
    
    /**
     * Synchronously sends a list of message exchanges in the order given by the
     * argument list.
     * 
     * @param exchanges a list of message exchanges.
     * @throws MessagingException if a system-level error occurs.
     */
    void sendSync(List<MessageExchange> exchanges) throws MessagingException;
    
}
