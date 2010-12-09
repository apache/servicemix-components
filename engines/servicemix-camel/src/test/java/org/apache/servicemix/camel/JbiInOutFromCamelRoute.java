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

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.servicemix.jbi.container.ActivationSpec;

/**
 * Tests on sending JBI InOut exchanges from within a Camel route
 */
public class JbiInOutFromCamelRoute extends JbiTestSupport {

    private static final String MESSAGE = "<just><a>test</a></just>";
    private static final String REPLY = "<just><a>reply</a></just>";
    private static final String REPLY_HEADER = "replies?";

    public void testInOutEchoesReply() throws Exception {
        MockEndpoint inout = getMockEndpoint("mock:in-out");
        inout.expectedMessageCount(2);
        
        MockEndpoint done = getMockEndpoint("mock:done");
        done.expectedMessageCount(2);

        // when the route creates a reply, expect that as the out message
        doTestInOutReply("direct:in-out", true, REPLY);
        
        // otherwise, just expect it to echo back the original message
        doTestInOutReply("direct:in-out", false, MESSAGE);
        
        inout.assertIsSatisfied();
        done.assertIsSatisfied();
              
        // let's wait a moment to make sure that the last DONE MessageExchange is handled
        Thread.sleep(1000);
    }
    
    public void testInOutEchoesReplyAsync() throws Exception {
        MockEndpoint inout = getMockEndpoint("mock:in-out");
        inout.expectedMessageCount(2);
        
        MockEndpoint done = getMockEndpoint("mock:done");
        done.expectedMessageCount(2);

        // when the route creates a reply, expect that as the out message
        doTestInOutReply("direct:async-in-out", true, REPLY);
        
        // otherwise, just expect it to echo back the original message
        doTestInOutReply("direct:async-in-out", false, MESSAGE);
        
        inout.assertIsSatisfied();
        done.assertIsSatisfied();
        
        // let's wait a moment to make sure that the last DONE MessageExchange is handled
        Thread.sleep(1000);
    }


    private void doTestInOutReply(String uri, Boolean reply, Object result) {
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setHeader(REPLY_HEADER, reply);
        exchange.getIn().setBody(MESSAGE);
        client.send(uri, exchange);
        assertEquals(result, exchange.getOut().getBody(String.class));
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
                from("direct:in-out")
                    .choice()
                    .when(header(REPLY_HEADER).isEqualTo(Boolean.TRUE))
                        .to("jbi:service:urn:test:in-out-reply?mep=in-out")
                    .otherwise().to("jbi:service:urn:test:in-out-quiet?mep=in-out")
                    .end().to("mock:done");

                from("direct:async-in-out")
                    .threads(1)
                    .choice()
                    .when(header(REPLY_HEADER).isEqualTo(Boolean.TRUE))
                        .to("jbi:service:urn:test:in-out-reply?mep=in-out")
                    .otherwise().to("jbi:service:urn:test:in-out-quiet?mep=in-out")
                    .end().to("mock:done");

                from("jbi:service:urn:test:in-out-quiet").to("mock:in-out");
                from("jbi:service:urn:test:in-out-reply").setBody(constant(REPLY)).to("mock:in-out");
            }
        };
    }
}
