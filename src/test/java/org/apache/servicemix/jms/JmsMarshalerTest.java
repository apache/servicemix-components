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

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.QueueConnection;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import junit.framework.TestCase;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.xbean.BrokerFactoryBean;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.messaging.MessageExchangeSupport;
import org.apache.servicemix.tck.ReceiverComponent;
import org.springframework.core.io.ClassPathResource;

public class JmsMarshalerTest extends TestCase {
    
    protected JBIContainer container;
    protected BrokerService broker;
    protected ActiveMQConnectionFactory connectionFactory;
    protected ActiveMQQueue queue;

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

        connectionFactory = new ActiveMQConnectionFactory("vm://localhost");
        queue = new ActiveMQQueue("foo.queue");
    }

    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
        if (broker != null) {
            broker.stop();
        }
    }

    public void testMarshalTextMessage() throws Exception {
        JmsComponent jms = new JmsComponent();
        jms.getConfiguration().setConnectionFactory(connectionFactory);
        JmsEndpoint ep = new JmsEndpoint();
        ep.setService(ReceiverComponent.SERVICE);
        ep.setEndpoint("jms");
        ep.setTargetService(ReceiverComponent.SERVICE);
        ep.setTargetEndpoint(ReceiverComponent.ENDPOINT);
        ep.setRole(MessageExchange.Role.CONSUMER);
        ep.setDestinationStyle(AbstractJmsProcessor.STYLE_QUEUE);
        ep.setDestination(queue);
        ep.setDefaultMep(MessageExchangeSupport.IN_ONLY);
        ep.setMarshaler(new DefaultJmsMarshaler(ep));
        jms.setEndpoints(new JmsEndpoint[] {ep });
        container.activateComponent(jms, "servicemix-jms");

        ReceiverComponent receiver = new ReceiverComponent();
        container.activateComponent(receiver, "receiver");

        QueueConnection qConn = connectionFactory.createQueueConnection();
        QueueSession qSess = qConn.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        QueueSender qSender = qSess.createSender(queue);
        TextMessage message = qSess.createTextMessage("<?xml version=\"1.0\" encoding=\"UTF-8\"?><hello>world</hello>");
        qSender.send(message);

        receiver.getMessageList().assertMessagesReceived(1);
        List msgs = receiver.getMessageList().flushMessages();
        NormalizedMessage msg = (NormalizedMessage) msgs.get(0);
        assertEquals("Messages match", message.getText(), new SourceTransformer().contentToString(msg));

        // Wait for DONE status
        Thread.sleep(50);
    }
    

}

