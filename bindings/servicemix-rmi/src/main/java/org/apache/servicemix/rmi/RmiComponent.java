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

import java.util.List;

import org.apache.servicemix.common.DefaultComponent;

/**
 * Binding component to work directly with RMI.
 * 
 * @author jbonofre
 * @org.apache.xbean.XBean element="component" description="RMI Component" RMI component
 *      allowing to use Java Remote Method Invocation (RMI). It provides a marshaler/unmarshaler
 *      to transfort RMI calls into a NMR format. It provide RMI proxy feature by delegating 
 *      method execution to a POJO.
 */
public class RmiComponent extends DefaultComponent {

    private RmiEndpointType[] endpoints; // list of RMI endpoints
    
    /**
     * <p>
     * Getter on the component endpoints.
     * </p>
     * 
     * @return the RMI endpoints list.
     */
    public RmiEndpointType[] getEndpoints() {
        return this.endpoints;
    }
    
    /**
     * <p>
     * Setter on the component endpoints.
     * </p>
     * 
     * @param endpoints the RMI endpoints list.
     */
    public void setEndpoints(RmiEndpointType[] endpoints) {
        this.endpoints = endpoints;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.DefaultComponent#getConfiguredEndpoints()
     */
    protected List getConfiguredEndpoints() {
        return asList(this.getEndpoints());
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.DefaultComponent#getEndpointClasses()
     */
    protected Class[] getEndpointClasses() {
        return new Class[] { RmiConsumerEndpoint.class, RmiProviderEndpoint.class };
    }
    
}
