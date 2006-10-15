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
package org.apache.servicemix.eip;

import java.util.List;

import org.apache.servicemix.common.DefaultComponent;

/**
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="component"
 *                  description="An EIP component"
 */
public class EIPComponent extends DefaultComponent {

    private EIPEndpoint[] endpoints;
    private EIPConfiguration configuration;

    public EIPComponent() {
        configuration = new EIPConfiguration();
    }
    
    /**
     * @return the configuration
     */
    protected EIPConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * @param configuration the configuration to set
     */
    protected void setConfiguration(EIPConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * @return Returns the endpoints.
     */
    public EIPEndpoint[] getEndpoints() {
        return endpoints;
    }

    /**
     * @param endpoints The endpoints to set.
     */
    public void setEndpoints(EIPEndpoint[] endpoints) {
        this.endpoints = endpoints;
    }
    
    /* (non-Javadoc)
     * @see org.servicemix.common.BaseComponentLifeCycle#getExtensionMBean()
     */
    protected Object getExtensionMBean() throws Exception {
        return configuration;
    }

    protected void doInit() throws Exception {
        super.doInit();
        configuration.setRootDir(context.getWorkspaceRoot());
        configuration.load();
    }

    protected List getConfiguredEndpoints() {
        return asList(endpoints);
    }

    protected Class[] getEndpointClasses() {
        return new Class[] { EIPEndpoint.class };
    }

}
