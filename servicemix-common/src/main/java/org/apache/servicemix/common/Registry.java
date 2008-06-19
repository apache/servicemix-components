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
package org.apache.servicemix.common;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Registry {

    protected ServiceMixComponent component;
    protected Map<String, Endpoint> endpoints;
    protected Map<String, ServiceUnit> serviceUnits;
    
    public Registry(ServiceMixComponent component) {
        this.component = component;
        this.endpoints = new HashMap<String, Endpoint>();
        this.serviceUnits = new HashMap<String, ServiceUnit>();
    }

    public Endpoint getEndpoint(String key) {
        return this.endpoints.get(key);
    }
    
    public boolean isRegistered(ServiceUnit su) {
        return isServiceUnitRegistered(su.getName());
    }
    
    public boolean isServiceUnitRegistered(String name) {
        return this.serviceUnits.containsKey(name);
    }
    
    public ServiceUnit getServiceUnit(String name) {
        return this.serviceUnits.get(name);
    }
    
    public void registerEndpoint(Endpoint ep) {
        String key = EndpointSupport.getKey(ep);
        if (this.endpoints.put(key, ep) != null) {
            throw new IllegalStateException("An endpoint is already registered for key: " + key);
        }
    }
    
    public void unregisterEndpoint(Endpoint ep) {
        this.endpoints.remove(EndpointSupport.getKey(ep));
    }
    
    public void registerServiceUnit(ServiceUnit su) {
        this.serviceUnits.put(su.getName(), su);
        Collection endpoints = (Collection) su.getEndpoints();
        for (Iterator iter = endpoints.iterator(); iter.hasNext();) {
            Endpoint ep = (Endpoint) iter.next();
            registerEndpoint(ep);
        }
    }
    
    public void unregisterServiceUnit(ServiceUnit su) {
        this.serviceUnits.remove(su.getName());
        Collection endpoints = (Collection) su.getEndpoints();
        for (Iterator iter = endpoints.iterator(); iter.hasNext();) {
            Endpoint ep = (Endpoint) iter.next();
            unregisterEndpoint(ep);
        }
    }
    
}
