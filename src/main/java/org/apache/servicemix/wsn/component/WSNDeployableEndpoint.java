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

import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.wsn.EndpointManager;
import org.apache.servicemix.wsn.EndpointRegistrationException;
import org.apache.servicemix.wsn.jms.JmsCreatePullPoint;
import org.apache.servicemix.wsn.jbi.JbiNotificationBroker;

public abstract class WSNDeployableEndpoint extends Endpoint implements EndpointManager {

    public JbiNotificationBroker getNotificationBroker() {
        return ((WSNComponent) serviceUnit.getComponent()).getNotificationBroker();
    }

    public JmsCreatePullPoint getCreatePullPoint() {
        return ((WSNComponent) serviceUnit.getComponent()).getCreatePullPoint();
    }

    @Override
    public ExchangeProcessor getProcessor() {
        return null;
    }

    public Object register(String address, Object service) throws EndpointRegistrationException {
        return null;
    }

    public void unregister(Object endpoint) throws EndpointRegistrationException {
    }

}
