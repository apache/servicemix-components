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
package org.apache.servicemix.http.endpoints;

import junit.framework.TestCase;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.http.HttpComponent;
import org.apache.servicemix.http.HttpEndpointType;
import org.apache.servicemix.http.LateResponseStrategy;
import org.apache.servicemix.http.PortFinder;
import org.apache.servicemix.http.exception.LateResponseException;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.helper.MessageUtil;

import javax.jbi.JBIException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test cases for the {@link HttpConsumerEndpoint#lateResponseStrategy} behaviour
 */
public class HttpConsumerLateResponseHandlingTest extends TestCase {

    private static final long TIMEOUT = 500;

    protected JBIContainer container;

    private static final int port1 = PortFinder.find("http.port1");

    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        container.setEmbedded(true);
        container.init();
    }

    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
    }

    public void testInOutWithStrategyError() throws Exception {
        MessageExchange exchange = doTestInOutWithStrategy(LateResponseStrategy.error);
        assertEquals("Exchange should have ended in ERROR", ExchangeStatus.ERROR, exchange.getStatus());
        assertTrue("Expecting a LateResponseException, but was " + exchange.getError().getClass().getName(),
                   exchange.getError() instanceof LateResponseException);
    }

    public void testInOutWithStrategyWarning() throws Exception {
        MessageExchange exchange = doTestInOutWithStrategy(LateResponseStrategy.warning);
        assertEquals("Exchange should have ended normally", ExchangeStatus.DONE, exchange.getStatus());
    }

    /*
     * Perform test for strategy and return MessageExchange object being sent/received
     */
    private MessageExchange doTestInOutWithStrategy(LateResponseStrategy strategy) throws JBIException, IOException, InterruptedException {
        HttpComponent http = new HttpComponent();
        HttpConsumerEndpoint ep = new HttpConsumerEndpoint();
        ep.setService(new QName("urn:test", "svc"));
        ep.setEndpoint("ep");
        ep.setLateResponseStrategy(strategy.name());
        ep.setTimeout(TIMEOUT);
        ep.setTargetService(new QName("urn:test", "echo"));
        ep.setLocationURI("http://localhost:" + port1 + "/ep1/");
        http.setEndpoints(new HttpEndpointType[]{ep});
        container.activateComponent(http, "http");

        final CountDownLatch latch = new CountDownLatch(2);
        final AtomicReference<MessageExchange> reference = new AtomicReference<MessageExchange>();

        EchoComponent echo = new EchoComponent() {
            @Override
            public void onMessageExchange(MessageExchange exchange) throws MessagingException {
                // enable content re-readability now before HTTP request has timed out
                MessageUtil.enableContentRereadability(exchange.getMessage("in"));

                reference.set(exchange);

                if (ExchangeStatus.ACTIVE.equals(exchange.getStatus())) {
                    try {
                        Thread.sleep(2 * TIMEOUT);
                    } catch (InterruptedException e) {
                        // graciously ignore this, the unit test itself will fail because it never hits the timemout
                    }
                }

                super.onMessageExchange(exchange);
                latch.countDown();
            }
        };
        echo.setService(new QName("urn:test", "echo"));
        echo.setEndpoint("endpoint");
        container.activateComponent(echo, "echo");

        container.start();

        PostMethod post = new PostMethod("http://localhost:" + port1 + "/ep1/");
        post.setRequestEntity(new StringRequestEntity("<hello>world</hello>"));
        new HttpClient().executeMethod(post);

        assertEquals("HTTP request should have timed out", 500, post.getStatusCode());

        post.releaseConnection();

        // let's wait for the MEP to be completely handled
        latch.await(2 * TIMEOUT, TimeUnit.MILLISECONDS);

        container.deactivateComponent("echo");
        container.deactivateComponent("http");

        return reference.get();
    }
}
