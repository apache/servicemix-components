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

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.component.Component;
import javax.xml.namespace.QName;

import junit.framework.TestCase;
import org.apache.servicemix.bean.support.TransformBeanSupport;
import org.apache.servicemix.bean.pojos.LoggingPojo;
import org.apache.servicemix.common.util.MessageUtil;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.listener.MessageExchangeListener;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.components.util.TransformComponentSupport;
import org.apache.servicemix.components.util.ComponentSupport;
import org.apache.servicemix.bean.support.ExchangeTarget;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.client.DefaultServiceMixClient;

public class TransformBeanSupportTest extends TestCase {

    protected JBIContainer container;
    protected BeanComponent component;

    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setEmbedded(true);
        container.init();

        component = new BeanComponent();
        container.activateComponent(component, "servicemix-bean");

        container.start();
    }

    protected void tearDown() throws Exception {
        container.shutDown();
    }

    public void testInOnlyWithError() throws Exception {
        MyTransformer transformer = new MyTransformer();
        ExchangeTarget target = new ExchangeTarget();
        target.setService(new QName("error"));
        transformer.setTarget(target);
        BeanEndpoint transformEndpoint = new BeanEndpoint();
        transformEndpoint.setBean(transformer);
        transformEndpoint.setService(new QName("transform"));
        transformEndpoint.setEndpoint("endpoint");
        component.addEndpoint(transformEndpoint);

        SendErrorComponent sendErrorComponent = new SendErrorComponent();
        container.activateComponent(sendErrorComponent, "error");

        ServiceMixClient client = new DefaultServiceMixClient(container);
        MessageExchange io = client.createInOnlyExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);
        io = client.receive();

        assertEquals(ExchangeStatus.ERROR, io.getStatus());
    }

    public static class MyTransformer extends TransformBeanSupport {
        protected boolean transform(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws Exception {
            MessageUtil.transfer(in, out);
            return true;
        }
    }

    public static class SendErrorComponent extends ComponentSupport implements MessageExchangeListener {
        public SendErrorComponent() {
            setService(new QName("error"));
        }

        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            exchange.setStatus(ExchangeStatus.ERROR);
            exchange.setError(new Exception());
            send(exchange);
        }
    }

}
