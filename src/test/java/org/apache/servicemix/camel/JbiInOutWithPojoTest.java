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

import java.util.List;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import org.apache.camel.builder.RouteBuilder;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.jaxp.StringSource;

/**
 * Tests on handling JBI InOut exchanges by Camel when they return a non-Source payload 
 */
public class JbiInOutWithPojoTest extends JbiTestSupport {
    
    private static final String MESSAGE = "<just><a>test</a></just>";
    
    public void testInOutWithCamelReturningPojoPayload() throws Exception {
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOut exchange = client.createInOutExchange();
        exchange.setService(new QName("urn:test", "pojo-payload"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals("Should not fail because of the non-xml camel out payload",
                     ExchangeStatus.ACTIVE, exchange.getStatus());
        client.done(exchange);
                
        // let's wait a moment to make sure that the last DONE MessageExchange is handled
        Thread.sleep(500);
    }


    @Override
    protected void appendJbiActivationSpecs(List<ActivationSpec> activationSpecList) {
        // no additional activation specs required
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                // let's not retry things too often as this will only slow down the unit tests
                errorHandler(deadLetterChannel().maximumRedeliveries(0));
                
                from("jbi:service:urn:test:pojo-payload")
                    .setBody().constant(new Object());
            }
        };
    }
}
