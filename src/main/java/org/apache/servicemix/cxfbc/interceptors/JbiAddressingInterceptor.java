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
package org.apache.servicemix.cxfbc.interceptors;


import javax.jbi.messaging.MessageExchange;
import javax.xml.namespace.QName;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.servicemix.cxfbc.CxfBcConsumer;
import org.apache.servicemix.jbi.resolver.URIResolver;


public class JbiAddressingInterceptor extends AbstractPhaseInterceptor<Message> {
    
    
    //private static final Logger LOG = Logger.getLogger(JbiAddressingInInterceptor.class.getName());
        
    public JbiAddressingInterceptor() {
        super(Phase.INVOKE);
        getBefore().add(CxfBcConsumer.JbiInvokerInterceptor.class.getName());
    }
       
    public void handleMessage(Message message) throws Fault {
        
        final AddressingPropertiesImpl maps = ContextUtils.retrieveMAPs(message, false, false);
        if (null == maps) {
            return;
        }
        AttributedURIType action = maps.getAction();
        if (action != null) {
            String value = action.getValue();
            
            if (value != null && value.endsWith(JbiConstants.JBI_SUFFIX)) {
                value = value.substring(0, value.indexOf(JbiConstants.JBI_SUFFIX) - 1);
                String[] parts = URIResolver.split3(value);
                MessageExchange exchange = message.getContent(MessageExchange.class);
                exchange.setOperation(new QName(parts[0], parts[2]));
                exchange.setInterfaceName(new QName(parts[0], parts[1]));
            }
        }
        
        AttributedURIType to = maps.getTo();
        if (to != null) {
            String toAddress = to.getValue();
            if (toAddress != null && toAddress.endsWith(JbiConstants.JBI_SUFFIX)) {
                toAddress = toAddress.substring(0, toAddress.indexOf(JbiConstants.JBI_SUFFIX) - 1);
                String[] parts = URIResolver.split3(toAddress);
                MessageExchange exchange = message.getContent(MessageExchange.class);
                exchange.setService(new QName(parts[0], parts[1]));
            }
        }
        
    }
    
    
}
