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

import java.io.File;
import java.net.URI;
import java.net.URL;

import javax.activation.DataHandler;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.components.jms.JmsReceiverComponent;
import org.apache.servicemix.components.jms.JmsServiceComponent;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.ReceiverComponent;
import org.springframework.jms.core.MessageCreator;

public class JMSComponentTest extends AbstractJmsTestSupport {

    private static Log logger = LogFactory.getLog(JMSComponentTest.class);

    public void testProviderInOnly() throws Exception {
        // JMS Component
        JmsComponent component = new JmsComponent();
        container.activateComponent(component, "JMSComponent");

        // Add a jms receiver
        JmsReceiverComponent jmsReceiver = new JmsReceiverComponent();
        jmsTemplate.setDefaultDestinationName("queue/A");
        jmsReceiver.setTemplate(jmsTemplate);
        jmsReceiver.afterPropertiesSet();
        ActivationSpec asJmsReceiver = new ActivationSpec("jmsReceiver", jmsReceiver);
        asJmsReceiver.setDestinationService(new QName("test", "receiver"));
        container.activateComponent(asJmsReceiver);

        // Add a receiver component
        Receiver receiver = new ReceiverComponent();
        ActivationSpec asReceiver = new ActivationSpec("receiver", receiver);
        asReceiver.setService(new QName("test", "receiver"));
        container.activateComponent(asReceiver);

        // Deploy SU
        URL url = getClass().getClassLoader().getResource("provider/jms.wsdl");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("provider", path.getAbsolutePath());
        component.getServiceUnitManager().init("provider", path.getAbsolutePath());
        component.getServiceUnitManager().start("provider");

        // Call it
        InOnly in = client.createInOnlyExchange();
        in.setInterfaceName(new QName("http://jms.servicemix.org/Test", "ProviderInterface"));
        in.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        client.send(in);

        // Check we received the message
        receiver.getMessageList().assertMessagesReceived(1);
    }

    public void testProviderInOut() throws Exception {
        // JMS Component
        JmsComponent component = new JmsComponent();
        container.activateComponent(component, "JMSComponent");

        // Add a jms receiver
        JmsServiceComponent jmsReceiver = new JmsServiceComponent();
        jmsTemplate.setDefaultDestinationName("queue/A");
        jmsReceiver.setTemplate(jmsTemplate);
        jmsReceiver.afterPropertiesSet();
        ActivationSpec asJmsReceiver = new ActivationSpec("jmsReceiver", jmsReceiver);
        asJmsReceiver.setDestinationService(new QName("test", "receiver"));
        container.activateComponent(asJmsReceiver);

        // Add an echo component
        EchoComponent echo = new EchoComponent();
        ActivationSpec asEcho = new ActivationSpec("receiver", echo);
        asEcho.setService(new QName("test", "receiver"));
        container.activateComponent(asEcho);

        // Deploy SU
        URL url = getClass().getClassLoader().getResource("provider/jms.wsdl");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("provider", path.getAbsolutePath());
        component.getServiceUnitManager().init("provider", path.getAbsolutePath());
        component.getServiceUnitManager().start("provider");

        // Call it
        InOut inout = client.createInOutExchange();
        inout.setInterfaceName(new QName("http://jms.servicemix.org/Test", "ProviderInterface"));
        inout.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        boolean result = client.sendSync(inout);
        assertTrue(result);
        NormalizedMessage out = inout.getOutMessage();
        assertNotNull(out);
        Source src = out.getContent();
        assertNotNull(src);
        logger.info(new SourceTransformer().toString(src));
    }

    public void testConsumerInOut() throws Exception {
        // JMS Component
        JmsComponent component = new JmsComponent();
        container.activateComponent(component, "JMSComponent");

        // Add an echo component
        EchoComponent echo = new EchoComponent();
        ActivationSpec asEcho = new ActivationSpec("receiver", echo);
        asEcho.setService(new QName("http://jms.servicemix.org/Test", "Echo"));
        container.activateComponent(asEcho);

        // Deploy Consumer SU
        URL url = getClass().getClassLoader().getResource("consumer/jms.wsdl");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("consumer", path.getAbsolutePath());
        component.getServiceUnitManager().init("consumer", path.getAbsolutePath());
        component.getServiceUnitManager().start("consumer");

        // Send test message
        jmsTemplate.setDefaultDestinationName("queue/A");
        jmsTemplate.afterPropertiesSet();
        jmsTemplate.send(new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                Message m = session.createTextMessage("<hello>world</hello>");
                m.setJMSReplyTo(session.createQueue("queue/B"));
                return m;
            }
        });

        // Receive echo message
        TextMessage reply = (TextMessage) jmsTemplate.receive("queue/B");
        assertNotNull(reply);
        logger.info(reply.getText());
    }

    public void testProviderConsumerInOut() throws Exception {
        // JMS Component
        JmsComponent component = new JmsComponent();
        container.activateComponent(component, "JMSComponent");

        // Add an echo component
        EchoComponent echo = new EchoComponent();
        ActivationSpec asEcho = new ActivationSpec("receiver", echo);
        asEcho.setService(new QName("http://jms.servicemix.org/Test", "Echo"));
        container.activateComponent(asEcho);

        // Deploy Provider SU

        URL url = getClass().getClassLoader().getResource("provider/jms.wsdl");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("provider", path.getAbsolutePath());
        component.getServiceUnitManager().init("provider", path.getAbsolutePath());
        component.getServiceUnitManager().start("provider");

        // Deploy Consumer SU

        url = getClass().getClassLoader().getResource("consumer/jms.wsdl");
        path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("consumer", path.getAbsolutePath());
        component.getServiceUnitManager().init("consumer", path.getAbsolutePath());
        component.getServiceUnitManager().start("consumer");

        InOut inout = null;
        boolean result = false;
        DataHandler dh = null;
        
        // Test successful return
        inout = client.createInOutExchange();
        inout.setInterfaceName(new QName("http://jms.servicemix.org/Test", "ProviderInterface"));
        inout.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        dh = new DataHandler(new ByteArrayDataSource("myImage", "application/octet-stream"));
        inout.getInMessage().addAttachment("myImage", dh);
        result = client.sendSync(inout);
        assertTrue(result);
        NormalizedMessage out = inout.getOutMessage();
        assertNotNull(out);
        Source src = out.getContent();
        assertNotNull(src);
        dh = out.getAttachment("myImage");
        assertNotNull(dh);
        
        logger.info(new SourceTransformer().toString(src));

        // Test fault return 
        container.deactivateComponent("receiver");
        ReturnFaultComponent fault = new ReturnFaultComponent();
        ActivationSpec asFault = new ActivationSpec("receiver", fault);
        asFault.setService(new QName("http://jms.servicemix.org/Test", "Echo"));
        container.activateComponent(asFault);

        inout = client.createInOutExchange();
        inout.setInterfaceName(new QName("http://jms.servicemix.org/Test", "ProviderInterface"));
        inout.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        result = client.sendSync(inout);
        assertTrue(result);
        Fault inoutFault = inout.getFault();
        assertNotNull(inoutFault);
        assertTrue(new SourceTransformer().contentToString(inoutFault).indexOf("<fault/>") > 0);
        client.done(inout);

        // Test error return
        container.deactivateComponent("receiver");
        ReturnErrorComponent error = new ReturnErrorComponent(new IllegalArgumentException());
        ActivationSpec asError = new ActivationSpec("receiver", error);
        asError.setService(new QName("http://jms.servicemix.org/Test", "Echo"));
        container.activateComponent(asError);

        inout = client.createInOutExchange();
        inout.setInterfaceName(new QName("http://jms.servicemix.org/Test", "ProviderInterface"));
        inout.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        client.sendSync(inout);
        assertEquals(ExchangeStatus.ERROR, inout.getStatus());
        assertTrue("An IllegalArgumentException was expected", inout.getError() instanceof IllegalArgumentException);

    }

}
