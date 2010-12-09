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
package org.apache.servicemix.rmi.marshaler;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.cxf.databinding.DataBinding;
import org.apache.servicemix.rmi.RmiExchange;

/**
 * <p>
 * Interface describing the behaviors of a RMI marshaler.
 * </p>
 * 
 * @author jbonofre
 */
public interface RmiMarshalerSupport {
    
    /**
     * <p>
     * Define the CXF data binding to use.
     * </p>
     * 
     * @param dataBinding
     */
    public void setDataBinding(DataBinding dataBinding);
    
    /**
     * <p>
     * Unmarshal a RMI call contained into a <code>NormalizedMessage</code>.
     * </p>
     * 
     * @param in the in normalized message.
     * @param rmiCall the RMI call to populate.
     * @throws MessagingException in case of unmarshaling failure.
     */
    public void rmiExchangeToNmr(NormalizedMessage in, RmiExchange rmiExchange) throws MessagingException;
    
    /**
     * <p>
     * Marshal an object and push into the <code>NormalizedMessage</code> content.
     * </p>
     *  
     * @param exchange the message exchange.
     * @return the RMI Exchange.
     * @throws MessagingException in case of marshaling failure.
     */
    public RmiExchange rmiExchangeFromNmr(MessageExchange exchange) throws MessagingException;

}
