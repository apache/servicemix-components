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
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxp.StringSource;
import org.apache.servicemix.camel.test.InvalidSerializableObject;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.exception.FaultException;

/**
 * Test the convertExceptions=true flag (converts all exceptions to JBI FaultExceptions)
 */
public class JbiConvertExceptionsTest extends JbiCamelErrorHandlingTestSupport {

    private static final String EXCEPTION = "This has completely gone wrong at runtime!";

    public void testInOnlyConvertExceptions() throws Exception {
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOut exchange = client.createInOutExchange();
        exchange.setService(new QName("urn:test", "exceptions"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());

        assertTrue("Exception should have been converted into a JBI FaultException",
                   exchange.getError() instanceof FaultException);
        assertTrue("Original message should have been preserved",
                   exchange.getError().getMessage().contains(EXCEPTION));
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                errorHandler(noErrorHandler());
                
                from("jbi:endpoint:urn:test:exceptions:endpoint?convertExceptions=true")
                  .process(new Processor() {
                      public void process(Exchange exchange) throws Exception {
                          throw new RuntimeException(EXCEPTION);
                      }
                  });
            }
        };
    }
}
