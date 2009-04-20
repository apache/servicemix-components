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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jbi.JBIException;
import javax.xml.namespace.QName;

import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.TestSupport;
import org.apache.camel.AsyncCallback;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.container.SpringJBIContainer;
import org.apache.servicemix.tck.ExchangeCompletedListener;

/**
 * @version $Revision: 563665 $
 */
public abstract class JbiTestSupport extends TestSupport {

    protected Exchange receivedExchange;

    protected CamelContext camelContext;

    protected SpringJBIContainer jbiContainer = new SpringJBIContainer();

    protected ExchangeCompletedListener exchangeCompletedListener;

    protected CountDownLatch latch = new CountDownLatch(1);

    protected Endpoint<Exchange> endpoint;

    protected String startEndpointUri = "jbi:endpoint:serviceNamespace:serviceA:endpointA";

    protected ProducerTemplate<Exchange> client;

    protected ServiceMixClient servicemixClient;

    /**
     * Sends an exchange to the endpoint
     */
    protected void sendExchange(final Object expectedBody) {
        client.send(endpoint, new Processor() {
            public void process(Exchange exchange) {
                Message in = exchange.getIn();
                in.setBody(expectedBody);
                in.setHeader("cheese", 123);
            }
        });
    }

    /**
     * Sends an exchange to the endpoint
     */
    protected AtomicBoolean sendExchangeAsync(final Object expectedBody) {
        final AtomicBoolean bool = new AtomicBoolean();
        client.send(endpoint, new Processor() {
            public void process(Exchange exchange) {
                Message in = exchange.getIn();
                in.setBody(expectedBody);
                in.setHeader("cheese", 123);
            }
        }, new AsyncCallback() {
            public void done(boolean b) {
                bool.set(true);
                bool.notify();
            }
        });
        return bool;
    }

    protected Object assertReceivedValidExchange(Class type) throws Exception {
        // lets wait on the message being received
        boolean received = latch.await(5, TimeUnit.SECONDS);
        assertTrue("Did not receive the message!", received);

        assertNotNull(receivedExchange);
        Message receivedMessage = receivedExchange.getIn();

        assertEquals("cheese header", 123, receivedMessage.getHeader("cheese"));
        Object body = receivedMessage.getBody();
        log.debug("Received body: " + body);
        return body;
    }

    @Override
    protected void setUp() throws Exception {
        if (camelContext == null) {
            camelContext = createCamelContext();
            client = camelContext.createProducerTemplate();
        }
        
        configureContainer(jbiContainer);
        List<ActivationSpec> activationSpecList = new ArrayList<ActivationSpec>();

        // lets add the Camel endpoint
        CamelJbiComponent component = new CamelJbiComponent();
        activationSpecList.add(createActivationSpec(component, new QName("camel", "camel"), "camelEndpoint"));

        // and provide a callback method for adding more services
        appendJbiActivationSpecs(activationSpecList);
        jbiContainer.setActivationSpecs(activationSpecList.toArray(new ActivationSpec[activationSpecList.size()]));

        jbiContainer.afterPropertiesSet();

        exchangeCompletedListener = new ExchangeCompletedListener();
        jbiContainer.addListener(exchangeCompletedListener);

        // allow for additional configuration of the compenent (e.g. deploying SU)
        configureComponent(component);

        // lets add some routes
        RouteBuilder builder = createRoutes();
        if (builder != null) {
            camelContext.addRoutes(builder);
        }
        endpoint = camelContext.getEndpoint(startEndpointUri);

        camelContext.start();
    }

    protected CamelContext createCamelContext() {
        return new DefaultCamelContext();
    }

    protected void configureComponent(CamelJbiComponent component) throws Exception {
        // add the ServiceMix Camel component to the CamelContext
        camelContext.addComponent("jbi", new JbiComponent(component));
    }

    protected void configureContainer(final JBIContainer container) throws Exception {
        container.setEmbedded(true);
    }

    public ServiceMixClient getServicemixClient() throws JBIException {
        if (servicemixClient == null) {
            servicemixClient = new DefaultServiceMixClient(jbiContainer);
        }
        return servicemixClient;
    }

    protected ActivationSpec createActivationSpec(Object component, QName service) {
        return createActivationSpec(component, service, "endpoint");
    }

    protected ActivationSpec createActivationSpec(Object component, QName service, String endpoint) {
        ActivationSpec spec = new ActivationSpec(component);
        spec.setService(service);
        spec.setEndpoint(endpoint);
        return spec;
    }

    @Override
    protected void tearDown() throws Exception {
        getServicemixClient().close();
        client.stop();
        camelContext.stop();
        super.tearDown();
    }

    protected MockEndpoint getMockEndpoint(String uri) {
        return (MockEndpoint)camelContext.getEndpoint(uri);
    }


    protected abstract void appendJbiActivationSpecs(
            List<ActivationSpec> activationSpecList);

    protected abstract RouteBuilder createRoutes();
}
