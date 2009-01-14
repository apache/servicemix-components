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
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.namespace.QName;

import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.servicemix.common.JbiConstants;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.util.FileUtil;
import org.apache.servicemix.jms.endpoints.DefaultProviderMarshaler;
import org.apache.servicemix.jms.endpoints.JmsProviderEndpoint;
import org.apache.servicemix.jms.endpoints.JmsSoapProviderEndpoint;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jms.core.MessageCreator;

public class JmsProviderEndpointTest extends AbstractJmsTestSupport {

    /**
     * Test property name.
     */
    private static final String MSG_PROPERTY = "PropertyTest";
    private static final String MSG_PROPERTY_BLACKLISTED = "BadPropertyTest";

    protected List<String> blackList;
    
    public void testSendWithoutProperties() throws Exception {
        container.activateComponent(createEndpoint(false), "servicemix-jms");
        
        InOnly me = client.createInOnlyExchange();
        NormalizedMessage inMessage = me.getInMessage();
        inMessage.setProperty(MSG_PROPERTY, "Test-Value");
        inMessage.setProperty(MSG_PROPERTY_BLACKLISTED, "Unwanted value");
        inMessage.setContent(new StringSource("<hello>world</hello>"));
        me.setService(new QName("jms"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        Message msg = jmsTemplate.receive("destination");
        assertNull("Found not expected property", msg
            .getStringProperty(MSG_PROPERTY));
        assertNull("Found blacklisted property", msg
                   .getStringProperty(MSG_PROPERTY_BLACKLISTED));
        assertNotNull(msg);
    }

    public void testSendSimple() throws Exception {
        container.activateComponent(createEndpoint(), "servicemix-jms");
        
        InOnly me = client.createInOnlyExchange();
        NormalizedMessage inMessage = me.getInMessage();
        inMessage.setProperty(MSG_PROPERTY, "Test-Value");
        inMessage.setProperty(MSG_PROPERTY_BLACKLISTED, "Unwanted value");
        inMessage.setProperty(JbiConstants.DATESTAMP_PROPERTY_NAME, Calendar.getInstance().getTime());
        inMessage.setContent(new StringSource("<hello>world</hello>"));
        me.setService(new QName("jms"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        Message msg = jmsTemplate.receive("destination");
        assertNotNull("Expected property not found", msg
            .getStringProperty(MSG_PROPERTY));
        assertNull("Found blacklisted property", msg
            .getStringProperty(MSG_PROPERTY_BLACKLISTED));
        assertNull("Found " + JbiConstants.DATESTAMP_PROPERTY_NAME + " property", msg
            .getObjectProperty(JbiConstants.DATESTAMP_PROPERTY_NAME));
        assertNotNull(msg);
    }
    
    public void testProviderInOnlyWithoutReplyDest() throws Exception {
        JmsComponent component = new JmsComponent();

        JmsProviderEndpoint endpoint = new JmsProviderEndpoint();
        endpoint.setService(new QName("uri:HelloWorld", "HelloService"));
        endpoint.setEndpoint("HelloPort");
        endpoint.setDestinationName("destination");
        endpoint.setConnectionFactory(connectionFactory);
        component.setEndpoints(new JmsProviderEndpoint[] {endpoint});
        container.activateComponent(component, "servicemix-jms");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtil.copyInputStream(new ClassPathResource("org/apache/servicemix/jms/HelloWorld-RPC-Input-OneWay.xml").getInputStream(), baos);
        InOnly me = client.createInOnlyExchange();
        me.getInMessage().setContent(new StringSource(baos.toString()));

        me.setOperation(new QName("uri:HelloWorld", "OneWay"));
        me.setService(new QName("uri:HelloWorld", "HelloService"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());

        Message msg = jmsTemplate.receive("destination");
        assertNotNull(msg);
        System.err.println(((TextMessage) msg).getText());
    }

    public void testProviderInOutWithoutReplyDest() throws Exception {
        JmsComponent component = new JmsComponent();

        JmsProviderEndpoint endpoint = new JmsProviderEndpoint();
        endpoint.setService(new QName("uri:HelloWorld", "HelloService"));
        endpoint.setEndpoint("HelloPort");
        endpoint.setDestinationName("destination");
        endpoint.setConnectionFactory(new PooledConnectionFactory(connectionFactory));
        component.setEndpoints(new JmsProviderEndpoint[] {endpoint});
        container.activateComponent(component, "servicemix-jms");

        Thread th = new Thread() {
            public void run() {
                try {
                    final Message msg = jmsTemplate.receive("destination");
                    assertNotNull(msg);
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    FileUtil.copyInputStream(new ClassPathResource("org/apache/servicemix/jms/HelloWorld-RPC-Output.xml")
                                .getInputStream(), baos);
                    jmsTemplate.send(msg.getJMSReplyTo(), new MessageCreator() {
                        public Message createMessage(Session session) throws JMSException {
                            TextMessage rep = session.createTextMessage(baos.toString());
                            rep.setJMSCorrelationID(msg.getJMSCorrelationID() != null ? msg.getJMSCorrelationID() : msg.getJMSMessageID());
                            return rep;
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtil.copyInputStream(new ClassPathResource("org/apache/servicemix/jms/HelloWorld-RPC-Input-OneWay.xml").getInputStream(), baos);
        InOut me = client.createInOutExchange();
        me.getInMessage().setContent(new StringSource(baos.toString()));
        me.setOperation(new QName("uri:HelloWorld", "OneWay"));
        me.setService(new QName("uri:HelloWorld", "HelloService"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getOutMessage());
        assertNotNull(me.getOutMessage().getContent());
        System.err.println(new SourceTransformer().contentToString(me.getOutMessage()));
        client.done(me);
    }

    public void testSoapProviderInOnly() throws Exception {
        JmsComponent component = new JmsComponent();
        
        JmsSoapProviderEndpoint endpoint = new JmsSoapProviderEndpoint();
        endpoint.setService(new QName("uri:HelloWorld", "HelloService"));
        endpoint.setEndpoint("HelloPort");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        endpoint.setWsdl(new ClassPathResource("org/apache/servicemix/jms/HelloWorld-RPC.wsdl"));
        component.setEndpoints(new JmsProviderEndpoint[] {endpoint});
        container.activateComponent(component, "servicemix-jms");
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtil.copyInputStream(new ClassPathResource("org/apache/servicemix/jms/HelloWorld-RPC-Input-OneWay.xml").getInputStream(), baos);
        InOnly me = client.createInOnlyExchange();
        me.getInMessage().setContent(new StringSource(baos.toString()));

        me.setOperation(new QName("uri:HelloWorld", "OneWay"));
        me.setService(new QName("uri:HelloWorld", "HelloService"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
    }
    
    public void testSoapProviderInOut() throws Exception {
        JmsComponent component = new JmsComponent();
        
        JmsSoapProviderEndpoint endpoint = new JmsSoapProviderEndpoint();
        endpoint.setService(new QName("uri:HelloWorld", "HelloService"));
        endpoint.setEndpoint("HelloPort");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        endpoint.setReplyDestinationName("reply");
        endpoint.setWsdl(new ClassPathResource("org/apache/servicemix/jms/HelloWorld-RPC.wsdl"));
        component.setEndpoints(new JmsProviderEndpoint[] {endpoint});
        container.activateComponent(component, "servicemix-jms");
        
        Thread th = new Thread() {
            public void run() {
                try {
                    final Message msg = jmsTemplate.receive("destination");
                    assertNotNull(msg);
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    FileUtil.copyInputStream(new ClassPathResource("org/apache/servicemix/jms/HelloWorld-RPC-Output.xml")
                                .getInputStream(), baos);
                    jmsTemplate.send("reply", new MessageCreator() {
                        public Message createMessage(Session session) throws JMSException {
                            TextMessage rep = session.createTextMessage(baos.toString());
                            rep.setJMSCorrelationID(msg.getJMSCorrelationID() != null ? msg.getJMSCorrelationID() : msg.getJMSMessageID());
                            return rep;
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtil.copyInputStream(new ClassPathResource("org/apache/servicemix/jms/HelloWorld-RPC-Input-Hello.xml").getInputStream(), baos);
        InOut me = client.createInOutExchange();
        me.getInMessage().setContent(new StringSource(baos.toString()));
        me.setOperation(new QName("uri:HelloWorld", "Hello"));
        me.setService(new QName("uri:HelloWorld", "HelloService"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getOutMessage());
        assertNotNull(me.getOutMessage().getContent());
        System.err.println(new SourceTransformer().contentToString(me.getOutMessage()));
        client.done(me);
    }

    public void testSoapProviderInOutWithoutReplyDest() throws Exception {
        JmsComponent component = new JmsComponent();
        
        JmsSoapProviderEndpoint endpoint = new JmsSoapProviderEndpoint();
        endpoint.setService(new QName("uri:HelloWorld", "HelloService"));
        endpoint.setEndpoint("HelloPort");
        endpoint.setConnectionFactory(new PooledConnectionFactory(connectionFactory));
        endpoint.setDestinationName("destination");
        endpoint.setWsdl(new ClassPathResource("org/apache/servicemix/jms/HelloWorld-RPC.wsdl"));
        component.setEndpoints(new JmsProviderEndpoint[] {endpoint});
        container.activateComponent(component, "servicemix-jms");
        
        Thread th = new Thread() {
            public void run() {
                try {
                    final Message msg = jmsTemplate.receive("destination");
                    assertNotNull(msg);
                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    FileUtil.copyInputStream(new ClassPathResource("org/apache/servicemix/jms/HelloWorld-RPC-Output.xml")
                                .getInputStream(), baos);
                    jmsTemplate.send(msg.getJMSReplyTo(), new MessageCreator() {
                        public Message createMessage(Session session) throws JMSException {
                            TextMessage rep = session.createTextMessage(baos.toString());
                            rep.setJMSCorrelationID(msg.getJMSCorrelationID() != null ? msg.getJMSCorrelationID() : msg.getJMSMessageID());
                            return rep;
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtil.copyInputStream(new ClassPathResource("org/apache/servicemix/jms/HelloWorld-RPC-Input-Hello.xml").getInputStream(), baos);
        InOut me = client.createInOutExchange();
        me.getInMessage().setContent(new StringSource(baos.toString()));
        me.setOperation(new QName("uri:HelloWorld", "Hello"));
        me.setService(new QName("uri:HelloWorld", "HelloService"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getOutMessage());
        assertNotNull(me.getOutMessage().getContent());
        System.err.println(new SourceTransformer().contentToString(me.getOutMessage()));
        client.done(me);
        
    }

    // Helper methods
    private JmsComponent createEndpoint() {
        return createEndpoint(true);
    }

    private JmsComponent createEndpoint(boolean copyProperties) {
        // initialize the black list
        blackList = new LinkedList<String>();
        blackList.add(MSG_PROPERTY_BLACKLISTED);
        
        JmsComponent component = new JmsComponent();
        JmsProviderEndpoint endpoint = new JmsProviderEndpoint();
        endpoint.setService(new QName("jms"));
        DefaultProviderMarshaler marshaler = new DefaultProviderMarshaler();
        marshaler.setCopyProperties(copyProperties);
        marshaler.setPropertyBlackList(blackList);
        endpoint.setMarshaler(marshaler);
        endpoint.setEndpoint("endpoint");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        component.setEndpoints(new JmsProviderEndpoint[] {endpoint});
        return component;
    }
}
