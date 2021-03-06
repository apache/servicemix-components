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

import org.apache.servicemix.client.ServiceMixClient;

/**
 * @version $Revision$
 */
public class JavaCamelRouteDslTest extends JbiInOutTest {
    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        suName = "su7";
    }
    
    @Override
    protected void configureExchange(ServiceMixClient client,
            MessageExchange exchange) {
        ServiceEndpoint endpoint = client.getContext().getEndpoint(
                CamelProviderEndpoint.SERVICE_NAME, "cheese");
        assertNotNull("Should have a Camel endpoint exposed in JBI!", endpoint);
        exchange.setEndpoint(endpoint);
    }

    @Override
    protected void checkResult(MessageExchange exchange) {
        assertNotNull(exchange.getMessage("out"));
        assertNotNull(exchange.getMessage("out").getProperty("operation"));
        assertEquals(exchange.getMessage("out").getProperty("operation").toString(), "{http://test}echo");
    }
}
