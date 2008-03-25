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
package org.apache.servicemix.jsr181;

import java.util.List;

import javax.jbi.component.ComponentContext;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.jsr181.xfire.JbiTransport;
import org.codehaus.xfire.DefaultXFire;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.transport.Transport;

/**
 * @org.apache.xbean.XBean element="component"
 *                  description="A jsr181 component"
 * @author gnodet
 *
 */
public class Jsr181Component extends DefaultComponent {

    private Jsr181Endpoint[] endpoints;
    private XFire xfire;
    
    public Jsr181Component() {
    }
    
    public Jsr181Endpoint[] getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Jsr181Endpoint[] endpoints) {
        this.endpoints = endpoints;
    }
    
    public List getConfiguredEndpoints() {
        return asList(endpoints);
    }
    
    protected Class[] getEndpointClasses() {
        return new Class[] {Jsr181Endpoint.class };
    }
    
    /**
     * @return Returns the xfire.
     */
    public XFire getXFire() {
        return xfire;
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.BaseLifeCycle#doInit()
     */
    protected void doInit() throws Exception {
        xfire = createXFire(this.context);
        super.doInit();
    }
    
    public static XFire createXFire(ComponentContext context) {
        XFire xfire = new DefaultXFire();
        Object[] transports = xfire.getTransportManager().getTransports().toArray();
        for (int i = 0; i < transports.length; i++) {
            xfire.getTransportManager().unregister((Transport) transports[i]);
        }
        xfire.getTransportManager().register(new JbiTransport(context));
        return xfire;
    }

}
