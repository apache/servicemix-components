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

import java.net.URI;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.servicemix.jbi.FaultException;
import org.apache.servicemix.jbi.messaging.MessageExchangeSupport;

public class DefaultHttpConsumerMarshaler implements HttpConsumerMarshaler {
    
    private URI defaultMep = MessageExchangeSupport.IN_OUT;

    public URI getDefaultMep() {
        return defaultMep;
    }

    public void setDefaultMep(URI defaultMep) {
        this.defaultMep = defaultMep;
    }

    public MessageExchange createExchange(HttpServletRequest request, ComponentContext context) throws Exception {
        MessageExchange me;
        me = context.getDeliveryChannel().createExchangeFactory().createExchange(getDefaultMep());
        NormalizedMessage in = me.createMessage();
        me.setMessage(in, "in");
        return me;
    }

    public void sendOut(MessageExchange exchange, NormalizedMessage outMsg, HttpServletRequest request, HttpServletResponse response) throws Exception {
        // TODO Auto-generated method stub
        
    }
    
    public void sendFault(MessageExchange exchange, Fault fault, HttpServletRequest request, HttpServletResponse response) throws Exception {
        throw new FaultException("Fault occured", exchange, fault);
    }

    public void sendError(MessageExchange exchange, Exception error, HttpServletRequest request, HttpServletResponse response) throws Exception {
        throw error;
    }
    
    public void sendAccepted(MessageExchange exchange, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
    }

}
