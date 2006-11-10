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
package org.apache.servicemix.http.endpoints;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface HttpConsumerMarshaler {

    MessageExchange createExchange(HttpServletRequest request, 
                                   ComponentContext context) throws Exception;

    void sendOut(MessageExchange exchange, 
                 NormalizedMessage outMsg, 
                 HttpServletRequest request, 
                 HttpServletResponse response) throws Exception;

    void sendFault(MessageExchange exchange, 
                   Fault fault, 
                   HttpServletRequest request, 
                   HttpServletResponse response) throws Exception;

    void sendError(MessageExchange exchange, 
                   Exception error, 
                   HttpServletRequest request, 
                   HttpServletResponse response) throws Exception;

    void sendAccepted(MessageExchange exchange, 
                      HttpServletRequest request, 
                      HttpServletResponse response) throws Exception;

}
