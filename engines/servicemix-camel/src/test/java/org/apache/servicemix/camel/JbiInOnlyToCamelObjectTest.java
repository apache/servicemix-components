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

import javax.jbi.messaging.InOnly;
import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxp.StringSource;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.container.ActivationSpec;

/**
 * Tests on handling JBI InOnly exchanges by Camel
 * The tests here try to convert to a non-xml body 
 */
public class JbiInOnlyToCamelObjectTest extends JbiTestSupport {
    
    private static final String MESSAGE = "<just><a>test</a></just>";

    public void testInOnlyExchangeConvertBody() throws Exception {
        MockEndpoint done = getMockEndpoint("mock:done");
        done.expectedBodiesReceived(new MessageContainer(MESSAGE));
        
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "in-only"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.send(exchange);
        
        done.assertIsSatisfied();
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
                from("jbi:service:urn:test:in-only").process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        String message = exchange.getIn().getBody(String.class);
                        exchange.getOut().setBody(new MessageContainer(message));
                    }
                }).to("mock:done");
            }
            
        };
    }
        
    public static final class MessageContainer {
        
        private String message;

        private MessageContainer(String message) {
            this.message = message;
        }
        
        @Override
        public int hashCode() {
            return message.hashCode();
        }
        
        @Override
        public boolean equals(Object arg0) {
            if (arg0 instanceof MessageContainer) {
                return ((MessageContainer) arg0).message.equals(message);
            }
            return super.equals(arg0);
        }
        
    }
}
