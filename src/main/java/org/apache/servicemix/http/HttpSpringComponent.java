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
package org.apache.servicemix.http;

import javax.jbi.servicedesc.ServiceEndpoint;

import org.apache.servicemix.common.BaseComponent;
import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.http.jetty.JCLLogger;
import org.w3c.dom.DocumentFragment;

/**
 * 
 * @author gnodet
 * @version $Revision$
 * @org.apache.xbean.XBean element="component"
 *                  description="An http component"
 */
public class HttpSpringComponent extends BaseComponent {

    private HttpEndpoint[] endpoints;
    
    static {
        JCLLogger.init();
    }
    
    /* (non-Javadoc)
     * @see org.servicemix.common.BaseComponent#createLifeCycle()
     */
    protected BaseLifeCycle createLifeCycle() {
        return new LifeCycle();
    }

    /* (non-Javadoc)
     * @see javax.jbi.component.Component#resolveEndpointReference(org.w3c.dom.DocumentFragment)
     */
    public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
        return HttpResolvedEndpoint.resolveEndpoint(epr);
    }
    
    public HttpEndpoint[] getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(HttpEndpoint[] endpoints) {
        this.endpoints = endpoints;
    }
    
    public class LifeCycle extends HttpLifeCycle {

        protected ServiceUnit su;
        
        public LifeCycle() {
            super(HttpSpringComponent.this);
        }
        
        /* (non-Javadoc)
         * @see org.servicemix.common.BaseLifeCycle#doInit()
         */
        protected void doInit() throws Exception {
            super.doInit();
            su = new ServiceUnit();
            su.setComponent(HttpSpringComponent.this);
            if (endpoints != null) {
	            for (int i = 0; i < endpoints.length; i++) {
	                endpoints[i].setServiceUnit(su);
	                su.addEndpoint(endpoints[i]);
	            }
            }
            getRegistry().registerServiceUnit(su);
        }

        /* (non-Javadoc)
         * @see org.servicemix.common.BaseLifeCycle#doStart()
         */
        protected void doStart() throws Exception {
            super.doStart();
            su.start();
        }
        
        /* (non-Javadoc)
         * @see org.servicemix.common.BaseLifeCycle#doStop()
         */
        protected void doStop() throws Exception {
            su.stop();
            super.doStop();
        }
        
        /* (non-Javadoc)
         * @see org.servicemix.common.BaseLifeCycle#doShutDown()
         */
        protected void doShutDown() throws Exception {
            su.shutDown();
            super.doShutDown();
        }
    }

    /**
     * @return the configuration
     * @org.apache.xbean.Flat
     */
    public HttpConfiguration getConfiguration() {
        return ((HttpLifeCycle) getLifeCycle()).getConfiguration();
    }

    /**
     * @param configuration the configuration to set
     */
    public void setConfiguration(HttpConfiguration configuration) {
        ((HttpLifeCycle) getLifeCycle()).setConfiguration(configuration);
    }

    public HttpProcessor getMainProcessor() {
        return ((HttpLifeCycle) getLifeCycle()).getMainProcessor();
    }

}
