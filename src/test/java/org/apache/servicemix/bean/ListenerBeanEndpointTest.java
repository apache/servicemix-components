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

import org.apache.servicemix.bean.beans.ListenerBean;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.resolver.URIResolver;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.w3c.dom.DocumentFragment;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.servicedesc.ServiceEndpoint;

public class ListenerBeanEndpointTest extends SpringTestSupport {

    public void testSendingToDynamicEndpointForExchangeProcessorBeanWithFooOperation() throws Exception {
        // now lets make a request on this endpoint
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);

        DocumentFragment epr = URIResolver.createWSAEPR("bean:listenerBean");
        ServiceEndpoint se = client.getContext().resolveEndpointReference(epr);
        assertNotNull("We should find a service endpoint!", se);

        InOnly exchange = client.createInOnlyExchange();
        exchange.setEndpoint(se);
        exchange.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        client.sendSync(exchange);

        assertExchangeWorked(exchange);

        ListenerBean bean = (ListenerBean) getBean("listenerBean");
        MessageExchange answer = bean.getLastExchange();

        log.info("Bean's process() method has been invoked: " + answer);

        assertNotNull("Bean's process() method should bave been invoked", answer);
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
        return new ClassPathXmlApplicationContext("spring-no-endpoints.xml");
    }

}
