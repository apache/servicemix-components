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
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.w3c.dom.DocumentFragment;

import org.apache.servicemix.bean.beans.AnnotatedBean;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.resolver.URIResolver;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class AnnotatedBeanEndpointTest extends SpringTestSupport {

    public void testSendingToDynamicEndpointForAnnotatedBeanWithFooOperation() throws Exception {
        // now lets make a request on this endpoint
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);

        DocumentFragment epr = URIResolver.createWSAEPR("bean:annotatedBean");
        ServiceEndpoint se = client.getContext().resolveEndpointReference(epr);
        assertNotNull("We should find a service endpoint!", se);

        InOnly exchange = client.createInOnlyExchange();
        exchange.setEndpoint(se);
        exchange.setOperation(new QName("foo"));
        exchange.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        client.sendSync(exchange);

        assertExchangeWorked(exchange);

        AnnotatedBean bean = (AnnotatedBean) getBean("annotatedBean");
        MessageExchange answer = bean.getMyExchangeMethod();

        log.info("Bean's myExchangeMethod() method has been invoked: " + answer);

        assertNotNull("Bean's myExchangeMethod() method should bave been invoked", answer);
    }

    /*
    public void testSendingToDynamicEndpointForAnnotatedBeanWithBarOperation() throws Exception {
        // now lets make a request on this endpoint
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);

        DocumentFragment epr = URIResolver.createWSAEPR("bean:annotatedBean");
        ServiceEndpoint se = client.getContext().resolveEndpointReference(epr);
        assertNotNull("We should find a service endpoint!", se);

        InOnly exchange = client.createInOnlyExchange();
        exchange.setEndpoint(se);
        exchange.setOperation(new QName("bar"));
        exchange.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        client.sendSync(exchange);

        assertExchangeWorked(exchange);

        AnnotatedBean bean = (AnnotatedBean) getBean("annotatedBean");
        MessageExchange bar = bean.getBar();
        log.info("Bean's bar() method has been invoked: " + bar);

        assertNotNull("Bean's bar() method should bave been invoked", bar);
    }

    public void testSendingToDynamicEndpointForAnnotatedBeanWithPropertyExpressionParamameter() throws Exception {
        // now lets make a request on this endpoint
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);

        DocumentFragment epr = URIResolver.createWSAEPR("bean:annotatedBean");
        ServiceEndpoint se = client.getContext().resolveEndpointReference(epr);
        assertNotNull("We should find a service endpoint!", se);

        InOnly exchange = client.createInOnlyExchange();
        exchange.setEndpoint(se);
        exchange.setOperation(new QName("methodWithPropertyParameter"));
        NormalizedMessage inMessage = exchange.getInMessage();
        inMessage.setProperty("person", "James");
        inMessage.setContent(new StringSource("<hello>world</hello>"));
        client.sendSync(exchange);

        assertExchangeWorked(exchange);

        AnnotatedBean bean = (AnnotatedBean) getBean("annotatedBean");
        Object answer = bean.getPropertyParameter();
        log.info("Bean's methodWithPropertyParameter() method has been invoked: " + answer);

        assertEquals("Bean's methodWithPropertyParameter() method should bave been invoked", "James", answer);
    }

    public void testSendingToDynamicEndpointForAnnotatedBeanWithPropertyAndXPathExpressionParamameter()
            throws Exception {
        // now lets make a request on this endpoint
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);

        DocumentFragment epr = URIResolver.createWSAEPR("bean:annotatedBean");
        ServiceEndpoint se = client.getContext().resolveEndpointReference(epr);
        assertNotNull("We should find a service endpoint!", se);

        InOnly exchange = client.createInOnlyExchange();
        exchange.setEndpoint(se);
        exchange.setOperation(new QName("methodWithPropertyParameterAndXPath"));
        NormalizedMessage inMessage = exchange.getInMessage();
        inMessage.setProperty("person", "James");
        inMessage.setContent(new StringSource("<hello address='London'>world</hello>"));
        client.sendSync(exchange);

        assertExchangeWorked(exchange);

        AnnotatedBean bean = (AnnotatedBean) getBean("annotatedBean");
        Object property = bean.getPropertyParameter();
        Object xpath = bean.getXpathParameter();
        log.info("Bean's methodWithPropertyParameterAndXPath() method has been with property: "
                + property + " and xpath: " + xpath);

        assertEquals("property parameter", "James", property);
        assertEquals("xpath parameter", "London", xpath);
    }
    */

    protected void assertExchangeWorked(MessageExchange me) throws Exception {
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        }
    }

    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("spring-no-endpoints.xml");
    }

}
