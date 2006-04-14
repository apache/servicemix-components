/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

import org.apache.servicemix.common.BaseComponent;
import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.xbean.XBeanServiceUnit;

/**
 * 
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="component"
 *                  description="An EIP component"
 */
public class EIPSpringComponent extends BaseComponent {

    private EIPEndpoint[] endpoints;

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
     * @see org.servicemix.common.BaseComponent#createLifeCycle()
     */
    protected BaseLifeCycle createLifeCycle() {
        return new LifeCycle();
    }

    /**
     * @author gnodet
     */
    public class LifeCycle extends EIPLifeCycle {

        protected ServiceUnit su;
        
        public LifeCycle() {
            super(EIPSpringComponent.this);
        }
        
        /* (non-Javadoc)
         * @see org.servicemix.common.BaseLifeCycle#doInit()
         */
        protected void doInit() throws Exception {
            super.doInit();
            su = new XBeanServiceUnit();
            su.setComponent(EIPSpringComponent.this);
            for (int i = 0; i < endpoints.length; i++) {
                endpoints[i].setServiceUnit(su);
                endpoints[i].validate();
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
