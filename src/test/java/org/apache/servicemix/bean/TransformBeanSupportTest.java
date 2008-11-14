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
package org.apache.servicemix.bean;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.servicemix.MessageExchangeListener;
import org.apache.servicemix.bean.support.ExchangeTarget;
import org.apache.servicemix.bean.support.TransformBeanSupport;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.common.util.MessageUtil;
import org.apache.servicemix.components.util.ComponentSupport;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.ExchangeCompletedListener;
import org.apache.servicemix.tck.ReceiverComponent;

public class TransformBeanSupportTest extends TestCase {

    protected DefaultServiceMixClient client;
    protected JBIContainer container;
    protected ExchangeCompletedListener listener;
    protected BeanComponent component;

    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setEmbedded(true);
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        configureContainer();
        listener = new ExchangeCompletedListener();
        container.addListener(listener);

        container.init();
        container.start();

        component = new BeanComponent();
        container.activateComponent(component, "servicemix-bean");

        client = new DefaultServiceMixClient(container);
    }

    protected void tearDown() throws Exception {
        container.shutDown();
        listener.assertExchangeCompleted();
    }

    protected void configureContainer() throws Exception {
        container.setFlowName("st");
    }

    public void testInOnly() throws Exception {
        TransformBeanSupport transformer = createTransformer("receiver");
        BeanEndpoint transformEndpoint = createBeanEndpoint(transformer);
        component.addEndpoint(transformEndpoint);

        ReceiverComponent receiver = new ReceiverComponent();
        activateComponent(receiver, "receiver");

        MessageExchange io = client.createInOnlyExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);

        io = client.receive();
        assertEquals(ExchangeStatus.DONE, io.getStatus());

        receiver.getMessageList().assertMessagesReceived(1);
    }


    public void testInOnlyWithError() throws Exception {
        TransformBeanSupport transformer = createTransformer("error");
        BeanEndpoint transformEndpoint = createBeanEndpoint(transformer);
        component.addEndpoint(transformEndpoint);

        activateComponent(new ReturnErrorComponent(), "error");

        MessageExchange io = client.createInOnlyExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);

        io = client.receive();
        assertEquals(ExchangeStatus.ERROR, io.getStatus());
    }

    public void testRobustInOnly() throws Exception {
        TransformBeanSupport transformer = createTransformer("receiver");
        BeanEndpoint transformEndpoint = createBeanEndpoint(transformer);
        component.addEndpoint(transformEndpoint);

        ReceiverComponent receiver = new ReceiverComponent();
        activateComponent(receiver, "receiver");

        MessageExchange io = client.createRobustInOnlyExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);

        io = client.receive();
        assertEquals(ExchangeStatus.DONE, io.getStatus());

        receiver.getMessageList().assertMessagesReceived(1);
    }

    public void testRobustInOnlyWithFaultAndError() throws Exception {
        TransformBeanSupport transformer = createTransformer("fault");
        BeanEndpoint transformEndpoint = createBeanEndpoint(transformer);
        component.addEndpoint(transformEndpoint);

        activateComponent(new ReturnFaultComponent(), "fault");

        MessageExchange io = client.createRobustInOnlyExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);

        io = client.receive();
        assertEquals(ExchangeStatus.ACTIVE, io.getStatus());
        assertNotNull(io.getFault());
        client.fail(io, new Exception("I do not like faults"));
    }

    private MyTransformer createTransformer(String targetService) {
        MyTransformer transformer = new MyTransformer();
        ExchangeTarget target = new ExchangeTarget();
        target.setService(new QName(targetService));
        transformer.setTarget(target);
        return transformer;
    }

    private BeanEndpoint createBeanEndpoint(TransformBeanSupport transformer) {
        BeanEndpoint transformEndpoint = new BeanEndpoint();
        transformEndpoint.setBean(transformer);
        transformEndpoint.setService(new QName("transform"));
        transformEndpoint.setEndpoint("endpoint");
        return transformEndpoint;
    }

    protected void activateComponent(ComponentSupport cmp, String name) throws Exception {
        cmp.setService(new QName(name));
        cmp.setEndpoint("endpoint");
        container.activateComponent(cmp, name);
    }

    public static class MyTransformer extends TransformBeanSupport {
        protected boolean transform(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws Exception {
            MessageUtil.transfer(in, out);
            return true;
        }
    }

    public static class ReturnErrorComponent extends ComponentSupport implements MessageExchangeListener {

        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                fail(exchange, new Exception());
            }
        }
    }

    public static class ReturnFaultComponent extends ComponentSupport implements MessageExchangeListener {

        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                Fault fault = exchange.createFault();
                fault.setContent(new StringSource("<fault/>"));
                fail(exchange, fault);
            }
        }
    }

}
