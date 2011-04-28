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
 * @org.apache.xbean.XBean element="component"  description="a component for hosting JAX-WS pojos. It is based on the CXF runtime."
 */
public class CxfSeComponent extends DefaultComponent {

	public static final String JBI_TRANSPORT_ID = "http://cxf.apache.org/transports/jbi";
	
    private static final String[] CXF_CONFIG = new String[] {
        "META-INF/cxf/cxf.xml",
        "META-INF/cxf/cxf-extension-soap.xml",
        "META-INF/cxf/transport/jbi/cxf-transport-jbi.xml",
        "META-INF/cxf/binding/jbi/cxf-binding-jbi.xml"
    };

    private CxfSeEndpoint[] endpoints;
    private Bus bus;
    private CxfSeConfiguration configuration = new CxfSeConfiguration();
    private static Object componentRegistry;
    
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
     * @org.apache.xbean.Property description="the endpoints hosted by a component"
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
        //Load configuration
        configuration.setRootDir(context.getWorkspaceRoot());
        configuration.setComponentName(context.getComponentName());
        configuration.load();
        if (configuration.getBusCfg() != null && configuration.getBusCfg().length() > 0) {
            CXF_CONFIG[0] = configuration.getBusCfg();
        } 
        if (bus == null) {
            bus = new SpringBusFactory().createBus();
            bus.getProperties().put(org.apache.cxf.interceptor.OneWayProcessorInterceptor.USE_ORIGINAL_THREAD, "true");
        }
        super.doInit();
    }
    
    @Override
    protected void doShutDown() throws Exception {
        // Bus should no longer be the thread default since the component's threads will end now
        if (bus != null) {
            BusFactory.setThreadDefaultBus(null);
        }
        super.doShutDown();
    }
    
    
    public Bus getBus() {
        return bus;
    }

     /**
     * @param bus the bus to set
     * @org.apache.xbean.Property description="the CXF bus"
     */
   public void setBus(Bus bus) {
        this.bus = bus;
    }

    public void setComponentRegistry(Object componentRegistry) {
        this.componentRegistry = componentRegistry;
    }

    public static Object getComponentRegistry() {
        return componentRegistry;
    }
}
