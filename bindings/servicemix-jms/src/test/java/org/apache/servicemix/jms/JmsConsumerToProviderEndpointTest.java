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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.namespace.QName;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.servicemix.jms.endpoints.JmsConsumerEndpoint;
import org.apache.servicemix.jms.endpoints.JmsProviderEndpoint;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

public class JmsConsumerToProviderEndpointTest extends AbstractJmsTestSupport {

    private static final String MESSAGE = "<hello>world</hello>";

    protected void setUp() throws Exception {
        super.setUp();
        
        JmsComponent component = new JmsComponent();
        JmsConsumerEndpoint from = new JmsConsumerEndpoint();
        from.setConnectionFactory(connectionFactory);
        from.setDestinationName("from");
        from.setListenerType("simple");
        from.setService(new QName("jms"));
        from.setEndpoint("from");
        from.setTargetService(new QName("jms"));
        from.setTargetEndpoint("to");
        
        JmsProviderEndpoint to = new JmsProviderEndpoint();
        to.setConnectionFactory(connectionFactory);
        to.setDestinationName("to");
        to.setService(new QName("jms"));
        to.setEndpoint("to");
        component.setEndpoints(new JmsEndpointType[] {from, to});

        container.activateComponent(component, "servicemix-jms");
    }

    public void testStaxSourceHandling() throws Exception {
        //switch to info log level to ensure use of StaxSource
        Level old = switchLogLevel(Level.INFO);
        
        //send a mesage to the 'from' queue
        jmsTemplate.send("from", new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                return session.createTextMessage(MESSAGE);
            }           
        });

        //assert that the message has been forwarded to the 'to' queue
        jmsTemplate.setReceiveTimeout(1000);
        Message result = jmsTemplate.receive("to");
        assertNotNull(result);
        assertTrue(result instanceof TextMessage);
        assertEquals(MESSAGE, ((TextMessage) result).getText());
        
        //and switch back to whatever log leve we started with
        switchLogLevel(old);
    }
    
    @Override
    protected void createJmsTemplate() throws Exception {
        jmsTemplate = new JmsTemplate(connectionFactory);
    }
    
    private Level switchLogLevel(Level level) {
        Logger logger = Logger.getLogger("org.apache.servicemix");
        Level old = logger.getLevel();
        logger.setLevel(level);
        return old;
    }
}
