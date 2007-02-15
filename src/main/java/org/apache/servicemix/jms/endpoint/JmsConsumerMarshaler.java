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
package org.apache.servicemix.jms.endpoint;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.Message;
import javax.jms.Session;

public interface JmsConsumerMarshaler {

    public interface JmsContext {
        public Message getMessage();
    }
    
    JmsContext createContext(Message message, ComponentContext context) throws Exception;
    
    MessageExchange createExchange(JmsContext context) throws Exception;
    
    Message createOut(MessageExchange exchange, 
                      NormalizedMessage outMsg,
                      Session session, 
                      JmsContext context) throws Exception;
    
    Message createFault(MessageExchange exchange, 
                        Fault fault,
                        Session session, 
                        JmsContext context) throws Exception;
    
    Message createError(MessageExchange exchange,
                        Exception error,
                        Session session, 
                        JmsContext context) throws Exception;

}
