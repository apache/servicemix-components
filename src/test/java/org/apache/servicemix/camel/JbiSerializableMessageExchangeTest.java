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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.tck.ReceiverComponent;

/**
 * Tests on handling JBI InOnly exchanges by Camel 
 */
public class JbiSerializableMessageExchangeTest extends JbiTestSupport {
    
    private static final String MESSAGE = "<just><a>test</a></just>";

    public void testInOnlyExchangeConvertBody() throws Exception {
        MockEndpoint done = getMockEndpoint("mock:done");
        done.expectedBodiesReceived(MESSAGE);
        
        // make sure to add a non-serializable header to the exchange
        client.sendBodyAndHeader("seda:in-only", MESSAGE, "header", this);
        
        done.assertIsSatisfied();
    }

    private void assertSerializable(Object object) throws IOException {
        ObjectOutputStream stream = new ObjectOutputStream(new ByteArrayOutputStream());
        stream.writeObject(object);
    }

    protected void appendJbiActivationSpecs(List<ActivationSpec> activationSpecList) {
        // no additional activation specs required
        activationSpecList.add(createActivationSpec(new ReceiverComponent() {
            @Override
            public void onMessageExchange(MessageExchange exchange) throws MessagingException {
                try {
                    assertSerializable(exchange);
                    super.onMessageExchange(exchange);
                } catch (IOException e) {
                    fail(exchange, e);
                }
            }
        }, new QName("urn:test", "receiver")));
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                errorHandler(deadLetterChannel("log:error?showHeaders=true").maximumRedeliveries(0));
                from("seda:in-only").to("jbi:service:urn:test:receiver").to("mock:done");
            }
            
        };
    }
}
