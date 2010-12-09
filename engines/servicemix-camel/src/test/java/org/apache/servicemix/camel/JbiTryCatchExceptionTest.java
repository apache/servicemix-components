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

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.servicemix.jbi.container.ActivationSpec;

import java.util.List;

/**
 * Tests on handling Camel exception with the doTry()...doCatch() DSL
 */
public class JbiTryCatchExceptionTest extends JbiTestSupport {
    
    private static final String MESSAGE = "<just><a>test</a></just>";

    public void testInOnlyExchangeConvertBody() throws Exception {
        MockEndpoint caught = getMockEndpoint("mock:caught");
        caught.expectedMessageCount(1);

        client.sendBody("direct:test", MESSAGE);

        caught.assertIsSatisfied();
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
                errorHandler(deadLetterChannel("mock:errors").maximumRedeliveries(0).handled(false));

                // throwing an exception from within the Camel route
                from("jbi:endpoint:urn:test:throwsException")
                    .errorHandler(noErrorHandler())
                    .to("mock:thrown")
                    .throwException(new CustomBusinessException());

                // Camel route that handles this exception with doTry()...doCatch()...
                from("direct:test")
                    .doTry()
                        .to("jbi:endpoint:urn:test:throwsException?mep=in-out")
                    .doCatch(CustomBusinessException.class)
                        .to("mock:caught")
                    .end();                
            }
        };
    }

    /*
     * Exception class to test with
     */
    private static final class CustomBusinessException extends Exception {

    }
}
