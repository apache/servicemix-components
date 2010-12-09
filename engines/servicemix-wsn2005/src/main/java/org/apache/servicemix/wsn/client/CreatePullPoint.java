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
package org.apache.servicemix.wsn.client;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.xml.namespace.QName;

import org.oasis_open.docs.wsn.b_2.CreatePullPointResponse;

public class CreatePullPoint extends AbstractWSAClient {

    public static final String WSN_URI = "http://servicemix.org/wsnotification";

    public static final String WSN_SERVICE = "CreatePullPoint";

    public static final QName NOTIFICATION_BROKER = new QName(WSN_URI, WSN_SERVICE);

    public CreatePullPoint(ComponentContext context) {
        this(context, "Broker");
    }

    public CreatePullPoint(ComponentContext context, String brokerName) {
        super(context, createWSA(WSN_URI + "/" + WSN_SERVICE + "/" + brokerName));
    }

    public PullPoint createPullPoint() throws JBIException {
        CreatePullPointResponse response = (CreatePullPointResponse) request(
                new org.oasis_open.docs.wsn.b_2.CreatePullPoint());
        return new PullPoint(getContext(), response.getPullPoint());
    }

}
