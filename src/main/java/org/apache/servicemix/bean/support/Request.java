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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jbi.messaging.MessageExchange;

public class Request {
    private Object bean;
    private MessageExchange exchange;
    private Set<String> sentExchanges = new HashSet<String>();
    // Keep track of callbacks already called, so that the same callback
    // can not be called twice
    private Map<Method, Boolean> callbacks = new HashMap<Method, Boolean>();
    
    public Request() {
    }
    
    public Request(Object bean, MessageExchange exchange) {
        this.bean = bean;
        this.exchange = exchange;
    }
    
    /**
     * @return the bean
     */
    public Object getBean() {
        return bean;
    }
    /**
     * @param bean the bean to set
     */
    public void setBean(Object bean) {
        this.bean = bean;
    }
    /**
     * @return the exchange
     */
    public MessageExchange getExchange() {
        return exchange;
    }
    /**
     * @param exchange the exchange to set
     */
    public void setExchange(MessageExchange exchange) {
        this.exchange = exchange;
    }
    /**
     * @param id the id of the exchange sent 
     */
    public void addSentExchange(String id) {
        sentExchanges.add(id);
    }

    /**
     * @return the callbacks
     */
    public Map<Method, Boolean> getCallbacks() {
        return callbacks;
    }

}