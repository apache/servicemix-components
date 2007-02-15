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

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jms.Message;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.jndi.ActiveMQInitialContextFactory;
import org.apache.activemq.xbean.BrokerFactoryBean;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jms.endpoint.JmsProviderEndpoint;
import org.jencks.GeronimoPlatformTransactionManager;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jms.core.JmsTemplate;

import junit.framework.TestCase;

public class JmsProviderEndpointTest extends TestCase {

    protected JBIContainer container;
    protected BrokerService broker;
    protected ActiveMQConnectionFactory connectionFactory;
    private JmsTemplate jmsTemplate;
    private ServiceMixClient client;

    protected void setUp() throws Exception {
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, ActiveMQInitialContextFactory.class.getName());
        System.setProperty(Context.PROVIDER_URL, "tcp://localhost:61216");

        BrokerFactoryBean bfb = new BrokerFactoryBean(new ClassPathResource("org/apache/servicemix/jms/activemq.xml"));
        bfb.afterPropertiesSet();
        broker = bfb.getBroker();
        broker.start();

        container = new JBIContainer();
        container.setUseMBeanServer(true);
        container.setCreateMBeanServer(true);
        container.setMonitorInstallationDirectory(false);
        container.setNamingContext(new InitialContext());
        container.setTransactionManager(new GeronimoPlatformTransactionManager());
        container.init();
        
        client = new DefaultServiceMixClient(container);
        connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61216");
        jmsTemplate = new JmsTemplate(connectionFactory);
    }

    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
        if (broker != null) {
            broker.stop();
        }
    }
    
    public void testSendSimple() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsProviderEndpoint endpoint = new JmsProviderEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        component.setEndpoints(new JmsProviderEndpoint[] { endpoint });
        container.activateComponent(component, "servicemix-jms");
        
        container.start();
        
        InOnly me = client.createInOnlyExchange();
        me.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        me.setService(new QName("jms"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        Message msg = jmsTemplate.receive("destination");
        assertNotNull(msg);
    }
    
}
