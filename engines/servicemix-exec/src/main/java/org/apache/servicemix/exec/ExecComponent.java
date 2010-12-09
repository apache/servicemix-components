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
package org.apache.servicemix.exec;

import java.util.List;

import org.apache.servicemix.common.DefaultComponent;

/**
 * A JBI component for executing system command using incoming message from the bus,
 * get the system execution output and send back into the bus.
 * 
 * @author jbonofre
 * @org.apache.xbean.XBean element="component" description="Exec Component"
 */
public class ExecComponent extends DefaultComponent {
    
    private ExecEndpoint[] endpoints;
    
    /**
     * Get the component endpoints
     * @return the component endpoints
     */
    public ExecEndpoint[] getEndpoints() {
        return endpoints;
    }
    
    /**
     * Set the component endpoints
     * @param endpoints the component endpoints
     */
    public void setEndpoints(ExecEndpoint[] endpoints) {
        this.endpoints = endpoints;
    }
    
    /* 
     * (non-Javadoc)
     * @see org.apache.servicemix.common.DefaultComponent#getConfiguredEndpoints()
     */
    @Override
    protected List getConfiguredEndpoints() {
        return asList(endpoints);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.DefaultComponent#getEndpointClasses()
     */
    @Override
    protected Class[] getEndpointClasses() {
        return new Class[] { ExecEndpoint.class };
    }

}
