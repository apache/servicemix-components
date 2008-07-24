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

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.xml.namespace.QName;

import org.oasis_open.docs.wsn.b_2.CreatePullPoint;
import org.oasis_open.docs.wsn.b_2.CreatePullPointResponse;
import org.apache.servicemix.id.IdGenerator;
import org.apache.servicemix.wsn.client.AbstractWSAClient;

public class WSNPullPointEndpoint extends WSNDeployableEndpoint {

    private CreatePullPoint request;

    private CreatePullPointResponse response;

    public WSNPullPointEndpoint(CreatePullPoint request) throws DeploymentException {
        this(request, null, null);
    }

    public WSNPullPointEndpoint(CreatePullPoint request, QName service, String endpoint) {
        this.request = request;
        if (service != null) {
            this.service = service;
        } else {
            this.service = new QName("http://servicemix.org/wsnotification", "PullPoint");
        }
        if (endpoint != null) {
            this.endpoint = endpoint;
        } else {
            this.endpoint = new IdGenerator().generateSanitizedId();
        }
    }

    @Override
    public MessageExchange.Role getRole() {
        return MessageExchange.Role.PROVIDER;
    }

    @Override
    public void activate() throws Exception {
        response = getCreatePullPoint().createPullPoint(request);
    }

    @Override
    public void deactivate() throws Exception {
        getCreatePullPoint().destroyPullPoint(AbstractWSAClient.getWSAAddress(response.getPullPoint()));
    }

}
