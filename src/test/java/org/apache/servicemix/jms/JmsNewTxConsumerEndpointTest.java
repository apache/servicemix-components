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

import javax.xml.namespace.QName;

import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jms.endpoints.JmsConsumerEndpoint;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.ReceiverComponent;

public class JmsNewTxConsumerEndpointTest extends AbstractJmsTestSupport {

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

    protected void configureJbiContainer() throws Exception {
        super.configureJbiContainer();
        container.setUseNewTransactionModel(true);
    }

    public void testConsumerDefaultXaTxAsync() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsConsumerEndpoint endpoint = new JmsConsumerEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setTargetService(new QName("receiver"));
        endpoint.setListenerType("default");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        endpoint.setTransacted("xa");
        endpoint.setSynchronous(false);
        component.setEndpoints(new JmsConsumerEndpoint[] {endpoint});
        container.activateComponent(component, "servicemix-jms");

        jmsTemplate.convertAndSend("destination", "<hello>world</hello>");
        receiver.getMessageList().assertMessagesReceived(1);
    }

    public void testConsumerDefaultXaTxSync() throws Exception {
        JmsComponent component = new JmsComponent();
        JmsConsumerEndpoint endpoint = new JmsConsumerEndpoint();
        endpoint.setService(new QName("jms"));
        endpoint.setEndpoint("endpoint");
        endpoint.setTargetService(new QName("receiver"));
        endpoint.setListenerType("default");
        endpoint.setConnectionFactory(connectionFactory);
        endpoint.setDestinationName("destination");
        endpoint.setTransacted("xa");
        endpoint.setSynchronous(true);
        component.setEndpoints(new JmsConsumerEndpoint[] {endpoint});
        container.activateComponent(component, "servicemix-jms");

        jmsTemplate.convertAndSend("destination", "<hello>world</hello>");
        receiver.getMessageList().assertMessagesReceived(1);
    }

}
