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
package org.apache.servicemix.http.packaging;

import java.util.ArrayList;
import java.util.List;

import javax.jbi.messaging.MessageExchange;

import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.packaging.Consumes;
import org.apache.servicemix.common.xbean.AbstractXBeanServiceUnitAnalyzer;
import org.apache.servicemix.http.HttpEndpoint;

/**
 * Implementation of the ServiceUnitAnalyzer can be used in tooling to provide a
 * way to parse the artifact for the service unit and provide a list of consumes
 * and provides
 *
 * @author Philip Dodds
 * @see ServiceUnitAnalyzer, {@link ServiceUnitAnalyzer}
 * @since 3.0
 */
public class HttpServiceUnitAnalyzer extends AbstractXBeanServiceUnitAnalyzer {

    protected List getConsumes(Endpoint endpoint) {
        List consumesList = new ArrayList();
        Consumes consumes;
        if (endpoint.getRole().equals(MessageExchange.Role.CONSUMER)) {
            consumes = new Consumes();
            HttpEndpoint httpEndpoint = (HttpEndpoint) endpoint;
            consumes.setEndpointName(httpEndpoint.getTargetEndpoint());
            consumes.setInterfaceName(httpEndpoint.getTargetInterfaceName());
            consumes.setServiceName(httpEndpoint.getTargetService());
            if (consumes.isValid()) {
                consumesList.add(consumes);
            } else {
                consumes = new Consumes();
                consumes.setEndpointName(endpoint.getEndpoint());
                consumes.setInterfaceName(endpoint.getInterfaceName());
                consumes.setServiceName(endpoint.getService());
                consumesList.add(consumes);
            }
        }

        return consumesList;
    }

    protected String getXBeanFile() {
        return "xbean.xml";
    }

    protected boolean isValidEndpoint(Object bean) {
        return bean instanceof HttpEndpoint;
    }

}
