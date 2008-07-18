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
package org.apache.servicemix.cxfse;

import java.util.List;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.servicemix.common.DefaultComponent;

/**
 * 
 * @author gnodet
 * @org.apache.xbean.XBean element="component"
 */
public class CxfSeComponent extends DefaultComponent {

    private static final String[] CXF_CONFIG = new String[] {
        "META-INF/cxf/cxf.xml",
        "META-INF/cxf/cxf-extension-soap.xml",
        "META-INF/cxf/transport/jbi/cxf-transport-jbi.xml",
        "META-INF/cxf/binding/jbi/cxf-binding-jbi.xml"
    };

    private CxfSeEndpoint[] endpoints;
    private Bus bus;
    
    public CxfSeComponent() {
        
    }
    
    /**
     * @return the endpoints
     */
    public CxfSeEndpoint[] getEndpoints() {
        return endpoints;
    }

    /**
     * @param endpoints the endpoints to set
     */
    public void setEndpoints(CxfSeEndpoint[] endpoints) {
        this.endpoints = endpoints;
    }

    @Override
    protected List getConfiguredEndpoints() {
        return asList(endpoints);
    }

    @Override
    protected Class[] getEndpointClasses() {
        return new Class[] {CxfSeEndpoint.class };
    }
    
    @Override
    protected void doInit() throws Exception {
        if (bus == null) {
            bus = new SpringBusFactory().createBus(CXF_CONFIG);
        }
        super.doInit();
    }
    
    public Bus getBus() {
        return bus;
    }

    public void setBus(Bus bus) {
        this.bus = bus;
    }
}
