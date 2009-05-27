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
import javax.jbi.messaging.InOptionalOut;
import javax.xml.namespace.QName;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxp.StringSource;
import org.apache.servicemix.jbi.container.ActivationSpec;

/**
 * Tests on handling JBI InOnly exchanges by Camel
 */
public class JbiInOptionalOutCamelTest extends JbiTestSupport {
    
    public void testInOptionalOutFromCamelEndpoint() throws Exception {
        MockEndpoint inonly = getMockEndpoint("mock:in-optional-out");
        inonly.expectedMessageCount(1);
        
        client.sendBody("direct:in-optional-out", new StringSource("<request>Does this MEP confuse you?</request>"));
        
        inonly.assertIsSatisfied();
        // let's wait for a moment to ensure that all pending Exchanges are handled
        Thread.sleep(500);
    }
    
    public void testInOptionalOutFromJbiWithDone() throws Exception {
        MockEndpoint inonly = getMockEndpoint("mock:in-optional-out");
        inonly.expectedMessageCount(1);
        
        InOptionalOut exchange = getServicemixClient().createInOptionalOutExchange();
        exchange.setService(new QName("urn:test", "in-optional-out"));
        exchange.getInMessage().setContent(new StringSource("<request>Does this MEP confuse you?</request>"));
        getServicemixClient().sendSync(exchange);
        assertEquals(ExchangeStatus.ACTIVE, exchange.getStatus());
        inonly.assertIsSatisfied();
        
        // let's send the done and wait for a moment until it's handled
        getServicemixClient().done(exchange);
        Thread.sleep(500);
    }
    
    public void testInOptionalOutFromJbiWithFault() throws Exception {
        MockEndpoint inonly = getMockEndpoint("mock:in-optional-out");
        inonly.expectedMessageCount(1);
        
        InOptionalOut exchange = getServicemixClient().createInOptionalOutExchange();
        exchange.setService(new QName("urn:test", "in-optional-out"));
        exchange.getInMessage().setContent(new StringSource("<request>Does this MEP confuse you?</request>"));
        getServicemixClient().sendSync(exchange);
        assertEquals(ExchangeStatus.ACTIVE, exchange.getStatus());
        inonly.assertIsSatisfied();
        
        // let's send back a fault to the Camel provider endpoint
        exchange.setFault(exchange.createFault());
        exchange.getFault().setContent(new StringSource("<response>Oh no, things are going astray!</response>"));
        getServicemixClient().sendSync(exchange);
        assertEquals(ExchangeStatus.DONE, exchange.getStatus());
    }
    
    public void testInOptionalOutFromJbiWithError() throws Exception {
        MockEndpoint inonly = getMockEndpoint("mock:in-optional-out");
        inonly.expectedMessageCount(1);
        
        InOptionalOut exchange = getServicemixClient().createInOptionalOutExchange();
        exchange.setService(new QName("urn:test", "in-optional-out"));
        exchange.getInMessage().setContent(new StringSource("<request>Does this MEP confuse you?</request>"));
        getServicemixClient().sendSync(exchange);
        assertEquals(ExchangeStatus.ACTIVE, exchange.getStatus());
        inonly.assertIsSatisfied();
        
        // let's back an error to the Camel provider endpoint and wait for a moment until it's handled
        exchange.setError(new JbiException("Oh no, things are going astray!"));
        getServicemixClient().send(exchange);
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
                from("direct:in-optional-out").to("jbi:service:urn:test:in-optional-out?mep=in-optional-out");
                from("jbi:service:urn:test:in-optional-out").to("mock:in-optional-out");
            }
        };
    }
}
