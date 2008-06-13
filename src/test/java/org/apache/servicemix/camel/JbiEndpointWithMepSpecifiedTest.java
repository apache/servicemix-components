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
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;

import org.apache.camel.ExchangePattern;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultExchange;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.tck.ReceiverComponent;

/**
 * Tests to check correct handling of the ?mep=xxx setting on a Camel JBI endpoint
 */
public class JbiEndpointWithMepSpecifiedTest extends JbiTestSupport {
    
    private MyReceiverComponent component;
    
    @Override
    protected void setUp() throws Exception {
        component = new MyReceiverComponent();
        super.setUp();
    }
    
    public void testCamelInOutSendJbiInOnly() throws Exception {
        client.send("direct:a", new DefaultExchange(camelContext) {
            
            @Override
            public ExchangePattern getPattern() {
                //let's explicitly send an in-out Exchange
                return ExchangePattern.InOut;
            }
            
        });
        assertEquals(1, component.count);
    }

    @Override
    protected void appendJbiActivationSpecs(List<ActivationSpec> activationSpecList) {
        ActivationSpec spec = new ActivationSpec();
        spec.setComponent(component);
        spec.setComponentName("receiver");
        spec.setService(new QName("urn:test", "service"));
        activationSpecList.add(spec);
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                from("direct:a").to("jbi:service:urn:test:service?mep=in-only");
            }
        };
    }
    
    private class MyReceiverComponent extends ReceiverComponent {
        
        private int count;
        
        @Override
        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange instanceof InOnly) {
                count++;
                done(exchange);
            } else {
                fail(exchange, new Exception("Unexpected MEP: " + exchange.getPattern()));
            }            
        }
    }
}
