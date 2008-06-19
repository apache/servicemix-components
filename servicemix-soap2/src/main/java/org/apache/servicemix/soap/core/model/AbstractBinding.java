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
package org.apache.servicemix.soap.core.model;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.servicemix.soap.api.InterceptorChain;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.api.model.Binding;
import org.apache.servicemix.soap.api.model.Operation;
import org.apache.servicemix.soap.core.AbstractInterceptorProvider;
import org.apache.servicemix.soap.core.MessageImpl;
import org.apache.servicemix.soap.core.PhaseInterceptorChain;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class AbstractBinding<T extends Operation> extends AbstractInterceptorProvider 
                             implements Binding<T> {

    private Map<QName, T> operations;
    private String location;
    
    public AbstractBinding() {
        operations = new HashMap<QName, T>();
    }
    
    public Message createMessage() {
        Message in = new MessageImpl();
        in.put(Binding.class, this);
        return in;
    }
    
    public Message createMessage(Message request) {
        Message out = new MessageImpl();
        out.put(Binding.class, this);
        out.put(Operation.class, request.get(Operation.class));
        return out;
    }
    
    public InterceptorChain getInterceptorChain(Phase phase) {
        InterceptorChain chain = new PhaseInterceptorChain();
        chain.add(getInterceptors(phase));
        return chain;
    }
    
    public T getOperation(QName name) {
        return operations.get(name);
    }

    public Collection<T> getOperations() {
        return operations.values();
    }
    
    public void addOperation(T operation) {
        operations.put(operation.getName(), operation);
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

}
