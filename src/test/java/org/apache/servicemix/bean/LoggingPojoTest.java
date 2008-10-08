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

import javax.xml.namespace.QName;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;

import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.bean.pojos.LoggingPojo;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.client.ServiceMixClientFacade;
import junit.framework.TestCase;

public class LoggingPojoTest extends TestCase {

    protected JBIContainer container;
    protected BeanComponent component;

    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setEmbedded(true);
        container.init();

        component = new BeanComponent();
        BeanEndpoint loggingEndpoint = new BeanEndpoint();
        loggingEndpoint.setBeanClassName(LoggingPojo.class.getName());
        loggingEndpoint.setService(new QName("logging"));
        loggingEndpoint.setEndpoint("endpoint");
        component.addEndpoint(loggingEndpoint);
        container.activateComponent(component, "servicemix-bean");

        container.start();
    }

    protected void tearDown() throws Exception {
        container.shutDown();
    }

    public void testInOnly() throws Exception {
        ServiceMixClient client = new ServiceMixClientFacade(component.getComponentContext());
        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("logging"));
        exchange.getInMessage().setContent(new StringSource("<hello/>"));
        exchange.getInMessage().setProperty("key", "value");
        client.sendSync(exchange);
    }

    public void testInOut() throws Exception {
        ServiceMixClient client = new ServiceMixClientFacade(component.getComponentContext());
        InOut exchange = client.createInOutExchange();
        exchange.setService(new QName("logging"));
        exchange.getInMessage().setContent(new StringSource("<hello/>"));
        exchange.getInMessage().setProperty("key", "value");
        client.sendSync(exchange);
        client.done(exchange);
    }
}
