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

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.cxf.databinding.DataBinding;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.rmi.RmiExchange;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Default RMI marshaler.
 * </p>
 * 
 * @author jbonofre
 */
public class DefaultRmiMarshaler implements RmiMarshalerSupport {
    
    private final Logger logger = LoggerFactory.getLogger(DefaultRmiMarshaler.class);
    
    private DataBinding dataBinding;
    
    public void setDataBinding(DataBinding dataBinding) {
        this.dataBinding = dataBinding;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.rmi.marshaler.RmiMarshalerSupport#rmiExchangeFromNmr(javax.jbi.messaging.MessageExchange)
     */
    public RmiExchange rmiExchangeFromNmr(MessageExchange exchange) throws MessagingException {
        if (exchange instanceof InOut) {
            // it's an InOut exchange
            // create the out message
            NormalizedMessage out = exchange.createMessage();
            // marshal the object using the data binding
            //DataWriter dataWriter = dataBinding.createWriter(objectClass);
            // put the marshaled response into the out message
            //out.setContent(dataWriter.write(arg0, arg1)));
        } else if (exchange instanceof InOptionalOut) {
            // it's an InOptionalOut, send the response only if the object is not null
            //if (object != null) {
                
            //} else {
                exchange.setStatus(ExchangeStatus.DONE);
            //}
        } else {
            // it's an InOnly exchange
            exchange.setStatus(ExchangeStatus.DONE);
        }
        return null;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.rmi.marshaler.RmiMarshalerSupport#rmiCallFromNMR(javax.jbi.messaging.NormalizedMessage)
     */
    public void rmiExchangeToNmr(NormalizedMessage in, RmiExchange rmiExchange) throws MessagingException {
        // marshal the RMI exchange into the "in" normalized message using JAXB
        logger.debug("Marshal a RMI exchange into the in normalized message using JAXB.");
        
        // TODO only for testing purpose
        in.setContent(new StringSource("<TEST></TEST>")); 
    }

}
