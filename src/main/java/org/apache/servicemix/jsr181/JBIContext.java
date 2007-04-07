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
package org.apache.servicemix.jsr181;

import javax.jbi.messaging.MessageExchange;

/**
 * A static method which provides access to some JBI informations
 * 
 * @author gnodet
 */
public final class JBIContext {

    private static ThreadLocal<MessageExchange> exchanges = new ThreadLocal<MessageExchange>();
    
    private JBIContext() {
    }

    /**
     * Retrieve the MessageExchange currently being handled
     * 
     * @return the current MessageExchange or null
     */
    public static MessageExchange getMessageExchange() {
        return exchanges.get();
    }
    
    protected static void setMessageExchange(MessageExchange exchange) {
        exchanges.set(exchange);
    }
    
}
