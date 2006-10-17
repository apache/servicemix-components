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
package org.apache.servicemix.bean.beans;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.bean.Callback;
import org.apache.servicemix.bean.Destination;
import org.apache.servicemix.bean.ExchangeTarget;
import org.apache.servicemix.bean.Operation;
import org.apache.servicemix.jbi.util.MessageUtil;

public class ConsumerBean {

    private Future<NormalizedMessage> request1;
    private Future<NormalizedMessage> request2;

    @Resource
    private ComponentContext context;
    
    @Resource
    private DeliveryChannel channel;
    
    @ExchangeTarget(uri="service:urn:service1")
    private Destination service1;

    @ExchangeTarget(uri="service:urn:service2")
    private Destination service2;  
    
    /**
     * @return the request1
     */
    public Future<NormalizedMessage> getRequest1() {
        return request1;
    }

    /**
     * @return the request2
     */
    public Future<NormalizedMessage> getRequest2() {
        return request2;
    }

    @PostConstruct
    public void init() {
        if (service1 == null || service2 == null || context == null || channel == null) {
            throw new IllegalStateException("Bean not initialized");
        }
    }
    
    @PreDestroy
    public void destroy() {
    }
    
    @Operation
    public void receive(NormalizedMessage message) throws Exception {
        request1 = service1.send(MessageUtil.copy(message));
        request2 = service2.send(MessageUtil.copy(message));
    }
    
    @Callback(condition="this.request1.done && this.request2.done")
    public NormalizedMessage answer() throws InterruptedException, ExecutionException {
        NormalizedMessage answer1 = request1.get();
        NormalizedMessage answer2 = request2.get();
        return null;
    }
    
}
