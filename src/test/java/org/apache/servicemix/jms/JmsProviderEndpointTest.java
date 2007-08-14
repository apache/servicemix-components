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

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.namespace.QName;

import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.util.FileUtil;
import org.apache.servicemix.jms.endpoints.JmsProviderEndpoint;
import org.apache.servicemix.jms.endpoints.JmsSoapProviderEndpoint;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jms.core.MessageCreator;

public class JmsProviderEndpointTest extends AbstractJmsTestSupport {

    public void testSendSimple() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsProviderEndpoint endpoint = new JmsProviderEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        component.setEndpoints(new JmsProviderEndpoint[] {endpoint});
        container.activateComponent(component, "servicemix-jms");
        
        InOnly me = client.createInOnlyExchange();
        me.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        me.setService(new QName("jms"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        Message msg = jmsTemplate.receive("destination");
        assertNotNull(msg);
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
        
        Message msg = jmsTemplate.receive("destination");
        assertNotNull(msg);
        System.err.println(((TextMessage) msg).getText());
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
                            rep.setJMSCorrelationID(msg.getJMSMessageID());
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
    
}
