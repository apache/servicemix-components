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

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.servicemix.components.util.TransformComponentSupport;
import org.apache.servicemix.jbi.api.ServiceMixClient;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.junit.Test;

/**
 * Tests for handling StreamSource sent by JBI to Camel
 */
public class JbiToCamelStreamSourceTest extends JbiTestSupport {
    
    private static final QName ECHO_COMPONENT = new QName("urn:test", "echo");
    private static final String MESSAGE = "<just><a>test message</a></just>";
    private static final Level LOG_LEVEL = Logger.getLogger("org.apache.servicemix").getEffectiveLevel();
    
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // change the log level to avoid the conversion to DOMSource 
        Logger.getLogger("org.apache.servicemix").setLevel(Level.ERROR);
    }
    
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        
        // restore the original log level
        Logger.getLogger("org.apache.servicemix").setLevel(LOG_LEVEL);
    }

    @Test
    public void testSendingStreamSource() throws Exception {
        MockEndpoint result = getMockEndpoint("mock:result");
        result.expectedBodiesReceived(MESSAGE);
        
        ServiceMixClient smx = getServicemixClient();
        InOnly exchange = smx.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "stream"));
        exchange.getInMessage().setContent(new StreamSource(new ByteArrayInputStream(MESSAGE.getBytes())));
        smx.send(exchange);
        
        exchange = (InOnly) servicemixClient.receive();
        assertEquals(ExchangeStatus.DONE, exchange.getStatus());
        result.assertIsSatisfied();
    }

    @Override
    protected void appendJbiActivationSpecs(List<ActivationSpec> activationSpecList) {
        activationSpecList.add(createActivationSpec(new TransformComponentSupport() {

            @Override
            protected boolean transform(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws Exception {
                out.setContent(new StreamSource(new ByteArrayInputStream(MESSAGE.getBytes())));
                return true;
            }
            
        }, ECHO_COMPONENT));
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("jbi:service:urn:test:stream")
                    .to("jbi:service:urn:test:echo?mep=in-out")
                    .convertBodyTo(String.class).to("mock:result");
            }            
        };
    }       
}
