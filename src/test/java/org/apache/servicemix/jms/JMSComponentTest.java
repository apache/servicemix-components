/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.NormalizedMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import junit.framework.TestCase;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.jndi.ActiveMQInitialContextFactory;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.activemq.xbean.BrokerFactoryBean;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.components.jms.JmsReceiverComponent;
import org.apache.servicemix.components.jms.JmsServiceComponent;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jms.JmsComponent;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.ReceiverComponent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jms.core.JmsTemplate;

public class JMSComponentTest extends TestCase {

    protected JBIContainer container;
    protected BrokerService broker;
    protected ActiveMQConnectionFactory connectionFactory;
    
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
        container.init();
        
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
    
    public void testProviderInOnly() throws Exception {
        // JMS Component
        JmsComponent component = new JmsComponent();
        container.activateComponent(component, "JMSComponent");
        
        // Add a jms receiver
        JmsReceiverComponent jmsReceiver = new JmsReceiverComponent();
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setDefaultDestinationName("queue/A");
        jmsReceiver.setTemplate(template);
        jmsReceiver.afterPropertiesSet();
        ActivationSpec asJmsReceiver = new ActivationSpec("jmsReceiver", jmsReceiver);
        asJmsReceiver.setDestinationService(new QName("test", "receiver"));
        container.activateComponent(asJmsReceiver);
        
        // Add a trace component
        Receiver receiver = new ReceiverComponent();
        ActivationSpec asReceiver = new ActivationSpec("receiver", receiver);
        asReceiver.setService(new QName("test", "receiver"));
        container.activateComponent(asReceiver);
        
        // Start container
        container.start();
        
        // Deploy SU
        URL url = getClass().getClassLoader().getResource("provider/jms.wsdl");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("provider", path.getAbsolutePath());
        component.getServiceUnitManager().start("provider");
        
        // Call it
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
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
        JmsTemplate template = new JmsTemplate(new PooledConnectionFactory(connectionFactory));
        template.setDefaultDestinationName("queue/A");
        jmsReceiver.setTemplate(template);
        jmsReceiver.afterPropertiesSet();
        ActivationSpec asJmsReceiver = new ActivationSpec("jmsReceiver", jmsReceiver);
        asJmsReceiver.setDestinationService(new QName("test", "receiver"));
        container.activateComponent(asJmsReceiver);
        
        // Add an echo component
        EchoComponent echo = new EchoComponent();
        ActivationSpec asEcho = new ActivationSpec("receiver", echo);
        asEcho.setService(new QName("test", "receiver"));
        container.activateComponent(asEcho);
        
        // Start container
        container.start();
        
        // Deploy SU
        URL url = getClass().getClassLoader().getResource("provider/jms.wsdl");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("provider", path.getAbsolutePath());
        component.getServiceUnitManager().start("provider");
        
        // Call it
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOut inout = client.createInOutExchange();
        inout.setInterfaceName(new QName("http://jms.servicemix.org/Test", "ProviderInterface"));
        inout.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        boolean result = client.sendSync(inout);
        assertTrue(result);
        NormalizedMessage out = inout.getOutMessage();
        assertNotNull(out);
        Source src = out.getContent();
        assertNotNull(src);
        System.err.println(new SourceTransformer().toString(src));
    }
    
    /*
     * This test is not finished.
     * But the feature is actually in the testProviderConsumerInOut test
     * 
    public void testConsumerInOut() throws Exception {
        // JMS Component
        JmsComponent component = new JmsComponent();
        container.activateComponent(component, "JMSComponent");
        
        // Start container
        container.start();
        
        // Deploy SU
        URL url = getClass().getClassLoader().getResource("consumer/jms.wsdl");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("consumer", path.getAbsolutePath());
        component.getServiceUnitManager().start("consumer");
        
        // Call it
        JmsTemplate template = new JmsTemplate(new PooledConnectionFactory(connectionFactory));
        template.setDefaultDestinationName("queue/A");
        template.afterPropertiesSet();
        template.send(new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                return session.createTextMessage("<hello>world</hello>");
            }
        });

        System.err.println("Sent");
    }
    */
    
    public void testProviderConsumerInOut() throws Exception {
        // JMS Component
        JmsComponent component = new JmsComponent();
        container.activateComponent(component, "JMSComponent");
        
        // Add an echo component
        EchoComponent echo = new EchoComponent();
        ActivationSpec asEcho = new ActivationSpec("receiver", echo);
        asEcho.setService(new QName("http://jms.servicemix.org/Test", "Echo"));
        container.activateComponent(asEcho);
        
        // Start container
        container.start();
        
        // Deploy Provider SU
        {
            URL url = getClass().getClassLoader().getResource("provider/jms.wsdl");
            File path = new File(new URI(url.toString()));
            path = path.getParentFile();
            component.getServiceUnitManager().deploy("provider", path.getAbsolutePath());
            component.getServiceUnitManager().start("provider");
        }
        
        // Deploy Consumer SU
        {
            URL url = getClass().getClassLoader().getResource("consumer/jms.wsdl");
            File path = new File(new URI(url.toString()));
            path = path.getParentFile();
            component.getServiceUnitManager().deploy("consumer", path.getAbsolutePath());
            component.getServiceUnitManager().start("consumer");
        }
        
        // Call it
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOut inout = client.createInOutExchange();
        inout.setInterfaceName(new QName("http://jms.servicemix.org/Test", "ProviderInterface"));
        inout.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        boolean result = client.sendSync(inout);
        assertTrue(result);
        NormalizedMessage out = inout.getOutMessage();
        assertNotNull(out);
        Source src = out.getContent();
        assertNotNull(src);
        System.err.println(new SourceTransformer().toString(src));
        
    }
    
}
