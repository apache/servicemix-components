/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.bean;

import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.resolver.URIResolver;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.bean.beans.PlainBean;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.w3c.dom.DocumentFragment;
import org.springframework.context.support.AbstractXmlApplicationContext;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.ExchangeStatus;
import javax.xml.namespace.QName;

public class AutoDeployedEndpointTest extends SpringTestSupport {

    public void testSendingToDynamicEndpointForPlainBeanWithFooOperation() throws Exception {
        // now lets make a request on this endpoint
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);

        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "service"));
        exchange.setOperation(new QName("foo"));
        exchange.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        client.sendSync(exchange);

        assertExchangeWorked(exchange);

        /*
        PlainBean bean = (PlainBean) getBean("plainBean");
        MessageExchange answer = bean.getFoo();

        log.info("Bean's foo() method has been invoked: " + answer);

        assertNotNull("Bean's foo() method should bave been invoked", answer);
        */
    }

    public void testSendingToDynamicEndpointForPlainBeanWithBarOperation() throws Exception {
        // now lets make a request on this endpoint
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);

        InOnly exchange = client.createInOnlyExchange();
        exchange.setService(new QName("urn:test", "service"));
        exchange.setOperation(new QName("bar"));
        exchange.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        client.sendSync(exchange);

        assertExchangeWorked(exchange);

        /*
        PlainBean bean = (PlainBean) getBean("plainBean");
        MessageExchange bar = bean.getBar();
        log.info("Bean's bar() method has been invoked: " + bar);

        assertNotNull("Bean's bar() method should bave been invoked", bar);
        */
    }

    protected void assertExchangeWorked(MessageExchange me) throws Exception {
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getError() != null) {
                throw me.getError();
            }
            else {
                fail("Received ERROR status");
            }
        }
        else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        }
    }

    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("spring-no-endpoints-or-beans.xml");
    }

}
