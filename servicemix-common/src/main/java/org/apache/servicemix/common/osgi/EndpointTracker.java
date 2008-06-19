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
package org.apache.servicemix.common.osgi;

import java.util.Map;

import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.DefaultComponent;

public class EndpointTracker {

    protected DefaultComponent component;

    public DefaultComponent getComponent() {
        return component;
    }

    public void setComponent(DefaultComponent component) {
        this.component = component;
    }

    public void register(EndpointWrapper wrapper, Map properties) throws Exception {
        System.out.println("Endpoint registered with properties: " + properties);
        Endpoint endpoint = wrapper.getEndpoint();
        if (component.isKnownEndpoint(endpoint)) {
	        System.out.println("Endpoint recognized");
            component.addEndpoint(endpoint);
        }
    }

    public void unregister(EndpointWrapper wrapper, Map properties) throws Exception {
        System.out.println("Endpoint unregistered with properties: " + properties);
        Endpoint endpoint = wrapper.getEndpoint();
        if (component.isKnownEndpoint(endpoint)) {
	        System.out.println("Endpoint recognized");
            component.removeEndpoint(endpoint);
        }
    }

}
