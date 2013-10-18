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

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.StringSource;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.junit.Test;

/**
 * Tests on conveying a simple exception thrown by a bean in the camel route
 */
public class JbiCamelExceptionsTest extends JbiCamelErrorHandlingTestSupport {

    @Test
    public void testInOnlyConveysException() throws Exception {
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "service"));
        exchange.getInMessage().setContent(new StringSource("<request>I would like a NPE, please!</request>"));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ERROR, exchange.getStatus());

        assertTrue("A NullPointerException was expected", exchange.getError() instanceof NullPointerException);
    }

    @Test
    public void testInOutHandlingBusinessException() throws Exception {
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOut exchange = client.createInOutExchange();
        exchange.setService(new QName("urn:test", "failsafe"));
        exchange.getInMessage().setContent(new StringSource("<request>I would like to do business, please!</request>"));
        client.sendSync(exchange);
        assertEquals(ExchangeStatus.ACTIVE, exchange.getStatus());

        client.done(exchange);

        // let's wait a moment to make sure that the last DONE MessageExchange is handled
        Thread.sleep(500);
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                errorHandler(noErrorHandler());

                from("jbi:endpoint:urn:test:failsafe:endpoint")
                    .doTry()
                        .to("jbi:endpoint:urn:test:service:endpoint")
                    .doCatch(BusinessException.class)
                        .setBody(constant("<response>We handled that pretty well, didn't we?</response>"));

                from("jbi:endpoint:urn:test:service:endpoint").bean(new MyPojo());
            }
        };
    }

    public class MyPojo {

        public void handle(String message) {
            if (message.contains("NPE")) {
                throw new NullPointerException("You asked for a NPE, you got it!");
            } else {
                throw new BusinessException("Something went wrong, but we can still recover, can't we?");
            }
        }

    }

    private class BusinessException extends RuntimeException {
        public BusinessException(String message) {
            super(message);
        }
    }
}
