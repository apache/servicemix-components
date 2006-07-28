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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.MessageExchangeListener;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.components.util.ComponentSupport;
import org.apache.servicemix.eip.support.ExchangeTarget;
import org.apache.servicemix.id.IdGenerator;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.store.memory.MemoryStore;
import org.apache.servicemix.tck.ExchangeCompletedListener;
import org.apache.servicemix.tck.ReceiverComponent;

public abstract class AbstractEIPTest extends TestCase {

    protected JBIContainer jbi;
    protected DefaultServiceMixClient client;
    protected ExchangeCompletedListener listener;

    protected void setUp() throws Exception {
        jbi = new JBIContainer();
        jbi.setEmbedded(true);
        jbi.setUseMBeanServer(false);
        jbi.setCreateMBeanServer(false);
        configureContainer();
        listener = new ExchangeCompletedListener();
        jbi.addListener(listener);
     
        jbi.init();
        jbi.start();

        client = new DefaultServiceMixClient(jbi);

        //LogManager.getLogger(DeliveryChannel.class).setLevel(Level.OFF);
    }
    
    protected void tearDown() throws Exception {
        listener.assertExchangeCompleted();
        jbi.shutDown();
    }
    
    protected void configureContainer() throws Exception {
        jbi.setFlowName("st");
    }
    
    protected void configurePattern(EIPEndpoint endpoint) {
        endpoint.setStore(new MemoryStore(new IdGenerator()) {
            public void store(String id, Object exchange) throws IOException {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                new ObjectOutputStream(baos).writeObject(exchange);
                super.store(id, exchange);
            }
        });
    }
    
    protected ExchangeTarget createServiceExchangeTarget(QName name) {
        ExchangeTarget target = new ExchangeTarget();
        target.setService(name);
        return target;
    }
    
    protected ReceiverComponent activateReceiver(String name) throws Exception {
        ReceiverComponent receiver = new ReceiverComponent();
        activateComponent(receiver, name);
        return receiver;
    }
    
    protected void activateComponent(EIPEndpoint endpoint, String name) throws Exception {
        EIPSpringComponent eip = new EIPSpringComponent();
        endpoint.setService(new QName(name));
        endpoint.setEndpoint("ep");
        eip.setEndpoints(new EIPEndpoint[] { endpoint });
        jbi.activateComponent(eip, name);
    }
    
    protected void activateComponent(ComponentSupport component, String name) throws Exception {
        component.setService(new QName(name));
        component.setEndpoint("ep");
        jbi.activateComponent(component, name);
    }
    
    protected static Source createSource(String msg) {
        return new StreamSource(new ByteArrayInputStream(msg.getBytes()));
    }
    
    protected static class ReturnOutComponent extends ComponentSupport implements MessageExchangeListener {
        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                boolean txSync = exchange.isTransacted() && Boolean.TRUE.equals(exchange.getProperty(JbiConstants.SEND_SYNC));
                if (exchange.getMessage("out") == null) {
                    NormalizedMessage out = exchange.createMessage();
                    out.setContent(createSource("<outMsg/>"));
                    exchange.setMessage(out, "out");
                    if (txSync) {
                        sendSync(exchange);
                    } else {
                        send(exchange);
                    }
                } else if (exchange.getFault() == null) {
                    Fault fault = exchange.createFault();
                    fault.setContent(createSource("<fault/>"));
                    exchange.setMessage(fault, "fault");
                    if (txSync) {
                        sendSync(exchange);
                    } else {
                        send(exchange);
                    }
                } else {
                    done(exchange);
                }
            }
        }
    }
    
    protected static class ReturnMockComponent extends ComponentSupport implements MessageExchangeListener {
        private String response;
        public ReturnMockComponent(String response) {
            this.response = response;
        }
        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                boolean txSync = exchange.isTransacted() && Boolean.TRUE.equals(exchange.getProperty(JbiConstants.SEND_SYNC));
                NormalizedMessage out = exchange.createMessage();
                out.setContent(createSource(response));
                exchange.setMessage(out, "out");
                if (txSync) {
                    sendSync(exchange);
                } else {
                    send(exchange);
                }
            }
        }
    }
    
    protected static class ReturnOutAndErrorComponent extends ComponentSupport implements MessageExchangeListener {
        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                if (exchange.getMessage("out") == null) {
                    boolean txSync = exchange.isTransacted() && Boolean.TRUE.equals(exchange.getProperty(JbiConstants.SEND_SYNC));
                    NormalizedMessage out = exchange.createMessage();
                    out.setContent(createSource("<outMsg/>"));
                    exchange.setMessage(out, "out");
                    if (txSync) {
                        sendSync(exchange);
                    } else {
                        send(exchange);
                    }
                } else {
                    fail(exchange, new Exception("Dummy error"));
                }
            }
        }
    }
    
    protected static class ReturnErrorComponent extends ComponentSupport implements MessageExchangeListener {
        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                fail(exchange, new Exception("Dummy error"));
            }
        }
    }
    
    protected static class ReturnFaultComponent extends ComponentSupport implements MessageExchangeListener {
        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                Fault fault = exchange.createFault();
                fault.setContent(createSource("<fault/>"));
                fail(exchange, fault);
            }
        }
    }
    
}
