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
package org.apache.servicemix.cxfbc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.jbi.messaging.MessageExchange;

import org.apache.cxf.interceptor.Interceptor;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;

/**
 * 
 * @author gnodet
 * @org.apache.xbean.XBean element="provider"
 */
public class CxfBcProvider extends ProviderEndpoint implements
        CxfBcEndpointWithInterceptor {

    List<Interceptor> in = new CopyOnWriteArrayList<Interceptor>();

    List<Interceptor> out = new CopyOnWriteArrayList<Interceptor>();

    List<Interceptor> outFault = new CopyOnWriteArrayList<Interceptor>();

    List<Interceptor> inFault = new CopyOnWriteArrayList<Interceptor>();

    public void processExchange(MessageExchange exchange) {

    }

    public List<Interceptor> getOutFaultInterceptors() {
        return outFault;
    }

    public List<Interceptor> getInFaultInterceptors() {
        return inFault;
    }

    public List<Interceptor> getInInterceptors() {
        return in;
    }

    public List<Interceptor> getOutInterceptors() {
        return out;
    }

    public void setInInterceptors(List<Interceptor> interceptors) {
        in = interceptors;
    }

    public void setInFaultInterceptors(List<Interceptor> interceptors) {
        inFault = interceptors;
    }

    public void setOutInterceptors(List<Interceptor> interceptors) {
        out = interceptors;
    }

    public void setOutFaultInterceptors(List<Interceptor> interceptors) {
        outFault = interceptors;
    }

}
