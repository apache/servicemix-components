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

import java.io.ByteArrayOutputStream;

import javax.jms.Message;
import javax.jms.TextMessage;
import javax.xml.namespace.QName;


import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.components.util.MockServiceComponent;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.messaging.MessageExchangeSupport;
import org.apache.servicemix.jbi.util.FileUtil;
import org.apache.servicemix.jms.endpoints.DefaultConsumerMarshaler;
import org.apache.servicemix.jms.endpoints.JmsConsumerEndpoint;
import org.apache.servicemix.jms.endpoints.JmsSoapConsumerEndpoint;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.ReceiverComponent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.ClassPathResource;
import org.w3c.dom.Element;

public class JmsConsumerEndpointTest extends AbstractJmsTestSupport {

    private static Log logger =  LogFactory.getLog(JmsConsumerEndpointTest.class);

    protected Receiver receiver;
    protected SourceTransformer sourceTransformer = new SourceTransformer();

    protected void setUp() throws Exception {
        super.setUp();
        
        ReceiverComponent rec = new ReceiverComponent();
        rec.setService(new QName("receiver"));
        rec.setEndpoint("endpoint");
        container.activateComponent(rec, "receiver");
        receiver = rec;
        
        EchoComponent echo = new EchoComponent();
        echo.setService(new QName("echo"));
        echo.setEndpoint("endpoint");
        container.activateComponent(echo, "echo");
    }
    
    public void testConsumerSimple() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsConsumerEndpoint endpoint = new JmsConsumerEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setTargetService(new QName("receiver"));
        endpoint.setListenerType("simple");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        component.setEndpoints(new JmsConsumerEndpoint[] { endpoint });
        container.activateComponent(component, "servicemix-jms");

        jmsTemplate.convertAndSend("destination", "<hello>world</hello>");
        receiver.getMessageList().assertMessagesReceived(1);
    }

    public void testConsumerSimpleJmsTx() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsConsumerEndpoint endpoint = new JmsConsumerEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setTargetService(new QName("receiver"));
        endpoint.setListenerType("simple");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        endpoint.setTransacted("jms");
        component.setEndpoints(new JmsConsumerEndpoint[] { endpoint });
        container.activateComponent(component, "servicemix-jms");
        
        container.start();
        
        jmsTemplate.convertAndSend("destination", "<hello>world</hello>");
        receiver.getMessageList().assertMessagesReceived(1);
    }

    public void testConsumerDefault() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsConsumerEndpoint endpoint = new JmsConsumerEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setTargetService(new QName("receiver"));
        endpoint.setListenerType("default");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        component.setEndpoints(new JmsConsumerEndpoint[] { endpoint });
        container.activateComponent(component, "servicemix-jms");
        
        container.start();
        
        jmsTemplate.convertAndSend("destination", "<hello>world</hello>");
        receiver.getMessageList().assertMessagesReceived(1);
    }

    public void testConsumerDefaultInOut() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsConsumerEndpoint endpoint = new JmsConsumerEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setTargetService(new QName("echo"));
        endpoint.setListenerType("default");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        endpoint.setReplyDestinationName("replyDestination");
        endpoint.setMarshaler(new DefaultConsumerMarshaler(MessageExchangeSupport.IN_OUT));
        component.setEndpoints(new JmsConsumerEndpoint[] { endpoint });
        container.activateComponent(component, "servicemix-jms");

        jmsTemplate.convertAndSend("destination", "<hello>world</hello>");
        TextMessage msg = (TextMessage) jmsTemplate.receive("replyDestination");
        Element e = sourceTransformer.toDOMElement(new StringSource(msg.getText()));
        assertEquals("hello", e.getTagName());
        assertEquals("world", e.getTextContent());
    }

    public void testConsumerDefaultJmsTx() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsConsumerEndpoint endpoint = new JmsConsumerEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setTargetService(new QName("receiver"));
        endpoint.setListenerType("default");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        endpoint.setTransacted("jms");
        component.setEndpoints(new JmsConsumerEndpoint[] { endpoint });
        container.activateComponent(component, "servicemix-jms");

        jmsTemplate.convertAndSend("destination", "<hello>world</hello>");
        receiver.getMessageList().assertMessagesReceived(1);
    }

    public void testConsumerDefaultInOutJmsTx() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsConsumerEndpoint endpoint = new JmsConsumerEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setTargetService(new QName("echo"));
        endpoint.setListenerType("default");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        endpoint.setReplyDestinationName("replyDestination");
        endpoint.setTransacted("jms");
        endpoint.setMarshaler(new DefaultConsumerMarshaler(MessageExchangeSupport.IN_OUT));
        component.setEndpoints(new JmsConsumerEndpoint[] { endpoint });
        container.activateComponent(component, "servicemix-jms");

        jmsTemplate.convertAndSend("destination", "<hello>world</hello>");
        TextMessage msg = (TextMessage) jmsTemplate.receive("replyDestination");
        Element e = sourceTransformer.toDOMElement(new StringSource(msg.getText()));
        assertEquals("hello", e.getTagName());
        assertEquals("world", e.getTextContent());
    }

    public void testConsumerDefaultXaTx() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsConsumerEndpoint endpoint = new JmsConsumerEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setTargetService(new QName("receiver"));
        endpoint.setListenerType("default");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        endpoint.setTransacted("xa");
        component.setEndpoints(new JmsConsumerEndpoint[] { endpoint });
        container.activateComponent(component, "servicemix-jms");

        jmsTemplate.convertAndSend("destination", "<hello>world</hello>");
        receiver.getMessageList().assertMessagesReceived(1);
    }

    public void testConsumerServer() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsConsumerEndpoint endpoint = new JmsConsumerEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setTargetService(new QName("receiver"));
        endpoint.setListenerType("server");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        component.setEndpoints(new JmsConsumerEndpoint[] { endpoint });
        container.activateComponent(component, "servicemix-jms");
        
        jmsTemplate.convertAndSend("destination", "<hello>world</hello>");
        receiver.getMessageList().assertMessagesReceived(1);
    }

    public void testConsumerServerJmsTx() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsConsumerEndpoint endpoint = new JmsConsumerEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setTargetService(new QName("receiver"));
        endpoint.setListenerType("server");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        endpoint.setTransacted("jms");
        component.setEndpoints(new JmsConsumerEndpoint[] { endpoint });
        container.activateComponent(component, "servicemix-jms");
        
        jmsTemplate.convertAndSend("destination", "<hello>world</hello>");
        receiver.getMessageList().assertMessagesReceived(1);
    }

    public void testSoapConsumerSimple() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsSoapConsumerEndpoint endpoint = new JmsSoapConsumerEndpoint();
        endpoint.setService(new QName("uri:HelloWorld", "HelloService"));
        endpoint.setEndpoint("HelloPort");
        endpoint.setTargetService(new QName("mock"));
        endpoint.setListenerType("simple");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        endpoint.setReplyDestinationName("reply");
        endpoint.setWsdl(new ClassPathResource("org/apache/servicemix/jms/HelloWorld-RPC.wsdl"));
        component.setEndpoints(new JmsConsumerEndpoint[] { endpoint });
        container.activateComponent(component, "servicemix-jms");
        
        MockServiceComponent mock = new MockServiceComponent();
        mock.setService(new QName("mock"));
        mock.setEndpoint("endpoint");
        mock.setResponseXml("<jbi:message xmlns:jbi=\"http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper\"><jbi:part>hello</jbi:part></jbi:message>");
        container.activateComponent(mock, "mock");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtil.copyInputStream(new ClassPathResource("org/apache/servicemix/jms/HelloWorld-RPC-Input.xml").getInputStream(), baos);
        jmsTemplate.convertAndSend("destination", baos.toString());
        
        Message msg = jmsTemplate.receive("reply");
        assertNotNull(msg);
        logger.info(((TextMessage) msg).getText());
    }

}
