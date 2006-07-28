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
package org.apache.servicemix.jms;

import java.util.List;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;

import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.xbean.BrokerFactoryBean;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.messaging.MessageExchangeSupport;
import org.apache.servicemix.jbi.resolver.URIResolver;
import org.apache.servicemix.tck.ReceiverComponent;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;

public class JmsURITest extends TestCase {

    protected JBIContainer container;
    protected BrokerService broker;
    protected ActiveMQConnectionFactory connectionFactory;

    protected void setUp() throws Exception {
        BrokerFactoryBean bfb = new BrokerFactoryBean(new ClassPathResource("org/apache/servicemix/jms/activemq.xml"));
        bfb.afterPropertiesSet();
        broker = bfb.getBroker();
        broker.start();

        container = new JBIContainer();
        container.setUseMBeanServer(true);
        container.setCreateMBeanServer(true);
        container.setMonitorInstallationDirectory(false);
        container.init();
        container.start();

        connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61216");
    }

    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
        if (broker != null) {
            broker.stop();
        }
    }

    public void testResolveEndpoint() throws Exception {
        JmsSpringComponent jms = new JmsSpringComponent();
        ((JmsLifeCycle) jms.getLifeCycle()).getConfiguration().setConnectionFactory(connectionFactory);
        JmsEndpoint ep = new JmsEndpoint();
        ep.setRole(MessageExchange.Role.CONSUMER);
        ep.setService(ReceiverComponent.SERVICE);
        ep.setEndpoint(ReceiverComponent.ENDPOINT);
        ep.setDefaultMep(MessageExchangeSupport.IN_ONLY);
        ep.setJmsProviderDestinationName("foo.bar.myqueue");
        ep.setDestinationStyle(AbstractJmsProcessor.STYLE_QUEUE);
        jms.setEndpoints(new JmsEndpoint[] { ep });
        container.activateComponent(jms, "servicemix-jms");

        ReceiverComponent receiver = new ReceiverComponent();
        container.activateComponent(receiver, "receiver");

        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        DocumentFragment epr = URIResolver.createWSAEPR("jms://queue/foo.bar.myqueue?jms.soap=true");
        ServiceEndpoint se = client.getContext().resolveEndpointReference(epr);
        assertNotNull(se);

        InOnly inonly = client.createInOnlyExchange();
        inonly.setEndpoint(se);
        inonly.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        client.sendSync(inonly);

        assertEquals(ExchangeStatus.DONE, inonly.getStatus());
        receiver.getMessageList().assertMessagesReceived(1);
        List msgs = receiver.getMessageList().flushMessages();
        NormalizedMessage msg = (NormalizedMessage) msgs.get(0);
        Document doc = (Document) new SourceTransformer().toDOMNode(msg);
        assertEquals("http://www.w3.org/2003/05/soap-envelope", doc.getDocumentElement().getNamespaceURI());
        assertEquals("env:Envelope", doc.getDocumentElement().getNodeName());
        System.out.println(new SourceTransformer().contentToString(msg));

        // Wait for DONE status
        Thread.sleep(50);
    }

}
