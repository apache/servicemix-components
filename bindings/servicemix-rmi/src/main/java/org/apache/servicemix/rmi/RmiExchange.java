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
package org.apache.servicemix.rmi;

import java.lang.reflect.Method;
import java.rmi.Remote;

/**
 * <p>
 * SMX representation of a RMI exchange.
 * </p>
 * 
 * @author jbonofre
 */
public class RmiExchange {
    
    private Object object;
    private Method method;
    private Object[] args;
    
    public Object getObject() {
        return object;
    }
    
    public void setObject(Object object) {
        this.object = object;
    }
    
    public Method getMethod() {
        return method;
    }
    
    public void setMethod(Method method) {
        this.method = method;
    }
    
    public Object[] getArgs() {
        return args;
    }
    
    public void setArgs(Object[] args) {
        this.args = args;
    }
    
    /**
     * <p>
     * Invoke the RMI exchange on to the give stub.
     * </p>
     * 
     * @param stub the Remote on which invoke the RMI exchange.
     * @return the response object.
     * @throws Exception in case of RMI invocation failure.
     */
    public Object invoke(Remote stub) throws Exception {
        return method.invoke(stub, args);
    }

}
