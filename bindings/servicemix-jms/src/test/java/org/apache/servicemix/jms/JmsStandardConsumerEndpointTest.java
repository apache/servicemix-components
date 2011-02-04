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
import java.util.LinkedList;
import java.util.List;

import javax.jbi.messaging.NormalizedMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.namespace.QName;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;

import org.apache.servicemix.common.JbiConstants;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.components.util.MockServiceComponent;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.util.FileUtil;
import org.apache.servicemix.tck.MessageList;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.ReceiverComponent;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jms.core.MessageCreator;

public class JmsStandardConsumerEndpointTest extends AbstractJmsTestSupport {

    private final Logger logger =  LoggerFactory.getLogger(JmsStandardConsumerEndpointTest.class);

    /**
     * Test property name.
     */
    private static final String MSG_PROPERTY = "PropertyTest";
    private static final String MSG_PROPERTY_BLACKLISTED = "BadPropertyTest";

    protected Receiver receiver;
    protected SourceTransformer sourceTransformer = new SourceTransformer();
    protected List<String> blackList;

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

        // initialize the black list
        blackList = new LinkedList<String>();
        blackList.add(MSG_PROPERTY_BLACKLISTED);
    }

    public void testWithoutProperties() throws Exception {
        container.activateComponent(createEndpoint(), "servicemix-jms");
        jmsTemplate.send("destination", new InternalCreator());
        MessageList messageList = receiver.getMessageList();
        messageList.assertMessagesReceived(1);
        NormalizedMessage message = (NormalizedMessage) messageList.getMessages().get(0);
        assertNull("Not expected property found", message.getProperty(MSG_PROPERTY));
        assertNull("Not expected property found", message.getProperty(MSG_PROPERTY_BLACKLISTED));
    }

    public void testConsumerDefaultInOut() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsEndpoint endpoint = new JmsEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setTargetService(new QName("echo"));
        endpoint.setRoleAsString("consumer");
        endpoint.setProcessorName("standard");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setJmsProviderDestinationName("destination");
        endpoint.setJmsProviderReplyToName("replyDestination"); // Doesn't seem to work so have to use msg.replyto
        endpoint.setDestinationStyle("queue");
        endpoint.setDefaultMep(JbiConstants.IN_OUT);
        component.setEndpoints(new JmsEndpoint[] {endpoint});
        container.activateComponent(component, "servicemix-jms");

        jmsTemplate.send("destination", new InternalCreator());
        jmsTemplate.setReceiveTimeout(4000);

        TextMessage msg = (TextMessage) jmsTemplate.receive("replyDestination");
        Element e = sourceTransformer.toDOMElement(new StringSource(msg.getText()));

        assertEquals("hello", e.getTagName());
        assertEquals("world", e.getTextContent());
    }

    public void testSoapConsumerSimple() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsEndpoint endpoint = new JmsEndpoint();
        endpoint.setService(new QName("uri:HelloWorld", "HelloService"));
        endpoint.setEndpoint("HelloPort");
        endpoint.setTargetService(new QName("mock"));
        endpoint.setRoleAsString("consumer");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setJmsProviderDestinationName("destination");
        endpoint.setJmsProviderReplyToName("replyDestination");
        endpoint.setDestinationStyle("queue");
        endpoint.setSoap(true);
        endpoint.setWsdlResource(new ClassPathResource("org/apache/servicemix/jms/HelloWorld-RPC.wsdl"));
        component.setEndpoints(new JmsEndpoint[] {endpoint});
        container.activateComponent(component, "servicemix-jms");

        MockServiceComponent mock = new MockServiceComponent();
        mock.setService(new QName("mock"));
        mock.setEndpoint("endpoint");
        mock.setResponseXml(
                "<jbi:message xmlns:jbi=\"http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper\"><jbi:part>hello</jbi:part></jbi:message>");
        container.activateComponent(mock, "mock");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtil.copyInputStream(new ClassPathResource("org/apache/servicemix/jms/HelloWorld-RPC-Input.xml").getInputStream(), baos);
        jmsTemplate.send("destination", new InternalCreator(baos.toString()));

        jmsTemplate.setReceiveTimeout(4000);
        Message msg = jmsTemplate.receive("replyDestination");
        assertNotNull(msg);
        logger.info(((TextMessage) msg).getText());
        
    }

    private JmsComponent createEndpoint() {
        JmsComponent component = new JmsComponent();
        JmsEndpoint endpoint = new JmsEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setTargetService(new QName("receiver"));
        endpoint.setProcessorName("standard");
        endpoint.setRoleAsString("consumer");
        endpoint.setDefaultMep(JbiConstants.IN_ONLY);
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setJmsProviderDestinationName("destination");
        endpoint.setDestinationStyle("queue");

        component.setEndpoints(new JmsEndpoint[] {endpoint});
        return component;
    }

    /**
     * Simple interface implementation - sets message body and one property.
     */
    protected static class InternalCreator implements MessageCreator {
        private String message;

        public InternalCreator() {
        }

        public InternalCreator(String message) {
            this.message = message;
        }

        public Message createMessage(Session session) throws JMSException {
            TextMessage message = session.createTextMessage(null != this.message ? this.message : "<hello>world</hello>");
            message.setJMSReplyTo(session.createQueue("replyDestination"));
            message.setStringProperty(MSG_PROPERTY, "test");
            message.setObjectProperty(MSG_PROPERTY_BLACKLISTED, new String("unwanted property"));
            return message;
        }
    }
}