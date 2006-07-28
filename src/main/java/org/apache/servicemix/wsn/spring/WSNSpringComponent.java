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
package org.apache.servicemix.wsn.spring;

import javax.jms.ConnectionFactory;

import org.apache.servicemix.common.BaseComponent;
import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.wsn.component.WSNDeployer;
import org.apache.servicemix.wsn.component.WSNLifeCycle;
import org.springframework.core.io.Resource;

/**
 * 
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="component"
 *                  description="An WS-Notification component"
 */
public class WSNSpringComponent extends BaseComponent {

    private Resource[] resources;
    
    private Object[] requests; 
    
    /**
     * @return Returns the endpoints.
     */
    public Resource[] getResources() {
        return resources;
    }

    /**
     * @param endpoints The endpoints to set.
     */
    public void setResources(Resource[] endpoints) {
        this.resources = endpoints;
    }
    
    /**
     * @return Returns the requests.
     */
    public Object[] getRequests() {
        return requests;
    }

    /**
     * @param requests The requests to set.
     */
    public void setRequests(Object[] requests) {
        this.requests = requests;
    }

    /**
     * @return Returns the connectionFactory.
     */
    public ConnectionFactory getConnectionFactory() {
        return ((WSNLifeCycle) lifeCycle).getConnectionFactory();
    }

    /**
     * @param connectionFactory The connectionFactory to set.
     */
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        ((WSNLifeCycle) lifeCycle).setConnectionFactory(connectionFactory);
    }
    
    /* (non-Javadoc)
     * @see org.servicemix.common.BaseComponent#createLifeCycle()
     */
    protected BaseLifeCycle createLifeCycle() {
        return new LifeCycle();
    }

    public class LifeCycle extends WSNLifeCycle {

        protected ServiceUnit su;
        
        public LifeCycle() {
            super(WSNSpringComponent.this);
        }
        
        /* (non-Javadoc)
         * @see org.servicemix.common.BaseLifeCycle#doInit()
         */
        protected void doInit() throws Exception {
            super.doInit();
            su = new ServiceUnit();
            su.setComponent(WSNSpringComponent.this);
            WSNDeployer deployer = new WSNDeployer(WSNSpringComponent.this);
            if (resources != null) {
                for (int i = 0; i < resources.length; i++) {
                    Endpoint ep = deployer.createEndpoint(resources[i].getURL());
                    ep.setServiceUnit(su);
                    su.addEndpoint(ep);
                }
            }
            if (requests != null) {
                for (int i = 0; i < requests.length; i++) {
                    Endpoint ep = deployer.createEndpoint(requests[i]);
                    ep.setServiceUnit(su);
                    su.addEndpoint(ep);
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

}
