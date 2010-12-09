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
package org.apache.servicemix.wsn.component;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import javax.jbi.management.LifeCycleMBean;

import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.DefaultServiceUnit;

public class WSNServiceUnit extends DefaultServiceUnit {

    @Override
    public void init() throws Exception {
        if (this.status == LifeCycleMBean.SHUTDOWN) {
            List<Endpoint> activated = new ArrayList<Endpoint>();
            try {
                for (Iterator iter = getEndpoints().iterator(); iter.hasNext();) {
                    Endpoint endpoint = (Endpoint) iter.next();
                    if (endpoint instanceof WSNPullPointEndpoint) {
                        endpoint.activate();
                        activated.add(endpoint);
                    }
                }
                for (Iterator iter = getEndpoints().iterator(); iter.hasNext();) {
                    Endpoint endpoint = (Endpoint) iter.next();
                    if (endpoint instanceof WSNSubscriptionEndpoint) {
                        endpoint.activate();
                        activated.add(endpoint);
                    }
                }
                for (Iterator iter = getEndpoints().iterator(); iter.hasNext();) {
                    Endpoint endpoint = (Endpoint) iter.next();
                    if (endpoint instanceof WSNPublisherEndpoint) {
                        endpoint.activate();
                        activated.add(endpoint);
                    }
                }
                this.status = LifeCycleMBean.STOPPED;
            } catch (Exception e) {
                // Deactivate activated endpoints
                for (Endpoint endpoint : activated) {
                    try {
                        endpoint.deactivate();
                    } catch (Exception e2) {
                        // do nothing
                    }
                }
                throw e;
            }
        }
    }

    @Override
    public void start() throws Exception {
        if (this.status == LifeCycleMBean.STOPPED) {
            List<Endpoint> started = new ArrayList<Endpoint>();
            try {
                for (Iterator iter = getEndpoints().iterator(); iter.hasNext();) {
                    Endpoint endpoint = (Endpoint) iter.next();
                    if (endpoint instanceof WSNPullPointEndpoint) {
                        endpoint.start();
                        started.add(endpoint);
                    }
                }
                for (Iterator iter = getEndpoints().iterator(); iter.hasNext();) {
                    Endpoint endpoint = (Endpoint) iter.next();
                    if (endpoint instanceof WSNSubscriptionEndpoint) {
                        endpoint.start();
                        started.add(endpoint);
                    }
                }
                for (Iterator iter = getEndpoints().iterator(); iter.hasNext();) {
                    Endpoint endpoint = (Endpoint) iter.next();
                    if (endpoint instanceof WSNPublisherEndpoint) {
                        endpoint.start();
                        started.add(endpoint);
                    }
                }
                this.status = LifeCycleMBean.STARTED;
            } catch (Exception e) {
                // Deactivate activated endpoints
                for (Endpoint endpoint : started) {
                    try {
                        endpoint.stop();
                    } catch (Exception e2) {
                        // do nothing
                    }
                }
                throw e;
            }
        }
    }

}
