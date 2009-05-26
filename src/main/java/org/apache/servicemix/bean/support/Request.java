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
package org.apache.servicemix.bean.support;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;

import org.apache.servicemix.bean.BeanEndpoint;

public class Request {
    private final Object bean;
    // Keep track of callbacks already called, so that the same callback
    // can not be called twice
    private Map<Method, Boolean> callbacks;
    private final Object correlationId;
    private final Set<MessageExchange> exchanges = new HashSet<MessageExchange>();
    
    public Request(Object correlationId, Object bean, MessageExchange exchange) {
        this.correlationId = correlationId;
        this.bean = bean;
        exchanges.add(exchange);
    }
    
    /**
     * @return the bean
     */
    public Object getBean() {
        return bean;
    }
    
    public Object getCorrelationId() {
        return correlationId;
    }

    /**
     * @return the callbacks
     */
    public Map<Method, Boolean> getCallbacks() {
        if (callbacks == null) {
            callbacks = new HashMap<Method, Boolean>();
        }
        return callbacks;
    }

    /**
     * Check if this request is completely finished.  
     *  
     * @return <code>true</code> if both the Exchange is DONE and there are no more outstanding sent exchanges
     */
    public boolean isFinished() {
        for (MessageExchange exchange : exchanges) {
            if (ExchangeStatus.ACTIVE.equals(exchange.getStatus())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Add an exchange to this request.  All exchanges that are added to the request have to be finished 
     * @param exchange
     */
    public void addExchange(MessageExchange exchange) {
        exchanges.add(exchange);
        exchange.setProperty(BeanEndpoint.CORRELATION_ID, correlationId);
    }
    
    /**
     * Get all the MessageExchanges that are involved in this request
     * 
     * @return an unmodifiable list of {@link MessageExchange}s
     */
    public Set<MessageExchange> getExchanges() {
        return Collections.unmodifiableSet(exchanges);
    }
}
