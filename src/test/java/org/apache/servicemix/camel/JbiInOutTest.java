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
package org.apache.servicemix.camel;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.servicemix.client.ServiceMixClient;

/**
 * @version $Revision: 1.1 $
 */
public class JbiInOutTest extends
        NonJbiCamelEndpointsIntegrationTest {
    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        suName = "su5";
        super.setUp();

        MyEchoComponent echo = new MyEchoComponent();
        echo.setService(new QName("namespace", "echo"));
        echo.setEndpoint("endpoint");
        container.activateComponent(echo, "echo");
    }

    @Override
    protected void configureExchange(ServiceMixClient client,
            MessageExchange exchange) {
        ServiceEndpoint endpoint = client.getContext().getEndpoint(
                CamelJbiEndpoint.SERVICE_NAME, "cheese");
        assertNotNull("Should have a Camel endpoint exposed in JBI!", endpoint);
        exchange.setEndpoint(endpoint);
    }

    @Override
    protected void checkResult(MessageExchange exchange) {
        assertNotNull(exchange.getMessage("out"));
        assertNotNull(exchange.getMessage("out").getContent());
    }

}
