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
package org.apache.servicemix.eip;

import java.util.concurrent.TimeoutException;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.apache.servicemix.MessageExchangeListener;
import org.apache.servicemix.components.util.ComponentSupport;
import org.apache.servicemix.eip.patterns.AsyncBridge;
import org.apache.servicemix.jbi.util.MessageUtil;

public class AsyncBridgeTest extends AbstractEIPTest {

    protected AsyncBridge asyncBridge;

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void configureContainer() throws Exception {
        // Use seda flow, so don't call base class
    }
    
    public void testInOut() throws Exception {
        createAsyncbridge(false);

        activateComponent(new TestComponent(false, false, true, false), "target");

        InOut me = client.createInOutExchange();
        me.setService(new QName("asyncBridge"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getOutMessage());
        client.done(me);
    }

    public void testInOutWithError() throws Exception {
        createAsyncbridge(false);

        activateComponent(new TestComponent(false, true, false, false), "target");

        InOut me = client.createInOutExchange();
        me.setService(new QName("asyncBridge"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
    }

    public void testInOutWithTimeOut() throws Exception {
        createAsyncbridge(false);

        activateComponent(new TestComponent(false, false, false, false), "target");

        InOut me = client.createInOutExchange();
        me.setService(new QName("asyncBridge"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        assertTrue(me.getError() instanceof TimeoutException);
    }

    public void testInOutWithDoneThenForward() throws Exception {
        createAsyncbridge(false);

        activateComponent(new TestComponent(false, false, false, true), "target");

        InOut me = client.createInOutExchange();
        me.setService(new QName("asyncBridge"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getOutMessage());
        client.done(me);
    }

    public void testInOutWithRobustInOnly() throws Exception {
        createAsyncbridge(true);

        activateComponent(new TestComponent(false, false, true, false), "target");

        InOut me = client.createInOutExchange();
        me.setService(new QName("asyncBridge"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getOutMessage());
        client.done(me);
    }

    public void testInOutWithRobustInOnlyAndFault() throws Exception {
        createAsyncbridge(true);

        activateComponent(new TestComponent(true, false, false, false), "target");

        InOut me = client.createInOutExchange();
        me.setService(new QName("asyncBridge"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.done(me);
    }

    public void testInOutWithRobustInOnlyAndError() throws Exception {
        createAsyncbridge(true);

        activateComponent(new TestComponent(false, true, false, false), "target");

        InOut me = client.createInOutExchange();
        me.setService(new QName("asyncBridge"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
    }

    public void testInOutWithRobustInOnlyAndTimeout() throws Exception {
        createAsyncbridge(true);

        activateComponent(new TestComponent(false, false, false, false), "target");

        InOut me = client.createInOutExchange();
        me.setService(new QName("asyncBridge"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        assertTrue(me.getError() instanceof TimeoutException);
    }

    public void testInOutWithRobustInOnlyAndDoneThenForward() throws Exception {
        createAsyncbridge(true);

        activateComponent(new TestComponent(false, false, false, true), "target");

        InOut me = client.createInOutExchange();
        me.setService(new QName("asyncBridge"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getOutMessage());
        client.done(me);
    }

    private void createAsyncbridge(boolean robustInOnly) throws Exception {
        asyncBridge = new AsyncBridge();
        asyncBridge.setTarget(createServiceExchangeTarget(new QName("target")));
        asyncBridge.setTimeout(1000);
        asyncBridge.setUseRobustInOnly(robustInOnly);
        configurePattern(asyncBridge);
        activateComponent(asyncBridge, "asyncBridge");
    }

    private class TestComponent extends ComponentSupport implements MessageExchangeListener {
        private boolean sendFault;
        private boolean sendError;
        private boolean forward;
        private boolean doneThenForward;

        public TestComponent(boolean sendFault, boolean sendError, boolean forward, boolean doneThenForward) {
            this.sendFault = sendFault;
            this.sendError = sendError;
            this.forward = forward;
            this.doneThenForward = doneThenForward;
        }

        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                if (sendFault) {
                    Fault f = exchange.createFault();
                    f.setContent(createSource("<fault/>"));
                    fail(exchange, f);
                } else if (sendError) {
                    fail(exchange, new Exception());
                } else if (forward) {
                    MessageExchange e = createRobustInOnlyExchange(exchange);
                    MessageUtil.transferInToIn(exchange, e);
                    e.setService(new QName("asyncBridge"));
                    sendSync(e);
                    done(exchange);
                } else if (doneThenForward) {
                    NormalizedMessage in = MessageUtil.copyIn(exchange);
                    done(exchange);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        throw new MessagingException(e);
                    }
                    MessageExchange e = createRobustInOnlyExchange(exchange);
                    MessageUtil.transferToIn(in, e);
                    e.setService(new QName("asyncBridge"));
                    sendSync(e);
                } else {
                    done(exchange);
                }
            }
        }
    }

}
