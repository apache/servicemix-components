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

import javax.xml.namespace.QName;
import javax.jbi.messaging.MessageExchange;

import org.oasis_open.docs.wsn.br_2.RegisterPublisher;
import org.oasis_open.docs.wsn.br_2.RegisterPublisherResponse;
import org.apache.servicemix.id.IdGenerator;
import org.apache.servicemix.wsn.client.AbstractWSAClient;

public class WSNPublisherEndpoint extends WSNDeployableEndpoint {

    private RegisterPublisher request;

    private RegisterPublisherResponse response;

    public WSNPublisherEndpoint(RegisterPublisher request) {
        this(request, null, null);
    }

    public WSNPublisherEndpoint(RegisterPublisher request, QName service, String endpoint) {
        this.request = request;
        if (service != null) {
            this.service = service;
        } else {
            this.service = new QName("http://servicemix.org/wsnotification", "Publisher");
        }
        if (endpoint != null) {
            this.endpoint = endpoint;
        } else {
            this.endpoint = new IdGenerator().generateSanitizedId();
        }
    }

    @Override
    public MessageExchange.Role getRole() {
        return MessageExchange.Role.CONSUMER;
    }

    @Override
    public void activate() throws Exception {
        response = getNotificationBroker().handleRegisterPublisher(request, this);
    }

    @Override
    public void deactivate() throws Exception {
        getNotificationBroker().unsubscribe(AbstractWSAClient.getWSAAddress(response.getPublisherRegistrationReference()));
    }

}
