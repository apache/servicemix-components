/** 
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.servicemix.http;

import org.servicemix.common.BaseComponent;
import org.servicemix.common.BaseLifeCycle;
import org.servicemix.common.ServiceUnit;

/**
 * 
 * @author gnodet
 * @version $Revision$
 * @org.xbean.XBean element="component"
 *                  description="An http component"
 */
public class HttpSpringComponent extends BaseComponent {

    private HttpEndpoint[] endpoints;
    
    /* (non-Javadoc)
     * @see org.servicemix.common.BaseComponent#createLifeCycle()
     */
    protected BaseLifeCycle createLifeCycle() {
        return new LifeCycle();
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
            for (int i = 0; i < endpoints.length; i++) {
                endpoints[i].setServiceUnit(su);
                su.addEndpoint(endpoints[i]);
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

}
