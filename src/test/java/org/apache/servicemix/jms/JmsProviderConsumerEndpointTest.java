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

import java.net.URI;
import java.net.URISyntaxException;

import javax.activation.DataHandler;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.ConnectionFactory;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.apache.activemq.pool.PooledConnectionFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jms.endpoints.DefaultConsumerMarshaler;
import org.apache.servicemix.jms.endpoints.DefaultProviderMarshaler;
import org.apache.servicemix.jms.endpoints.JmsConsumerEndpoint;
import org.apache.servicemix.jms.endpoints.JmsProviderEndpoint;

public class JmsProviderConsumerEndpointTest extends AbstractJmsTestSupport {

    private static Log logger = LogFactory.getLog(JmsProviderConsumerEndpointTest.class);

    public void testProviderConsumerInOut() throws Exception {
        ConnectionFactory connFactory = new PooledConnectionFactory(connectionFactory);
        JmsComponent jmsComponent = new JmsComponent();
        JmsConsumerEndpoint consumerEndpoint = createConsumerEndpoint(connFactory);
        JmsProviderEndpoint providerEndpoint = createProviderEndpoint(connFactory);
        jmsComponent.setEndpoints(new JmsEndpointType[] {consumerEndpoint, providerEndpoint});
        container.activateComponent(jmsComponent, "servicemix-jms");

        // Add an echo component
        EchoComponent echo = new EchoComponent();
        ActivationSpec asEcho = new ActivationSpec("receiver", echo);
        asEcho.setService(new QName("http://jms.servicemix.org/Test", "Echo"));
        container.activateComponent(asEcho);

        InOut inout = null;
        boolean result = false;
        DataHandler dh = null;
        
        // Test successful return
        inout = client.createInOutExchange();
        inout.setService(new QName("http://jms.servicemix.org/Test", "Provider"));
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
        inout.setService(new QName("http://jms.servicemix.org/Test", "Provider"));
        inout.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        result = client.sendSync(inout);
        assertTrue(result);
        assertNotNull(inout.getFault());
        assertTrue(new SourceTransformer().contentToString(inout.getFault()).indexOf("<fault/>") > 0);
        client.done(inout);

        // Test error return
        container.deactivateComponent("receiver");
        ReturnErrorComponent error = new ReturnErrorComponent(new IllegalArgumentException());
        ActivationSpec asError = new ActivationSpec("receiver", error);
        asError.setService(new QName("http://jms.servicemix.org/Test", "Echo"));
        container.activateComponent(asError);
        
        inout = client.createInOutExchange();
        inout.setService(new QName("http://jms.servicemix.org/Test", "Provider"));
        inout.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        client.sendSync(inout);
        assertEquals(ExchangeStatus.ERROR, inout.getStatus());
        assertTrue("An IllegalArgumentException was expected", inout.getError() instanceof IllegalArgumentException);

    }

    private JmsConsumerEndpoint createConsumerEndpoint(ConnectionFactory connFactory) throws URISyntaxException {
        JmsConsumerEndpoint endpoint = new JmsConsumerEndpoint();
        endpoint.setService(new QName("http://jms.servicemix.org/Test", "Consumer"));
        endpoint.setEndpoint("endpoint");
        DefaultConsumerMarshaler marshaler = new DefaultConsumerMarshaler();
        marshaler.setMep(new URI("http://www.w3.org/2004/08/wsdl/in-out"));
        marshaler.setRollbackOnError(false);
        endpoint.setMarshaler(marshaler);
        endpoint.setListenerType("simple");
        endpoint.setConnectionFactory(connFactory);
        endpoint.setDestinationName("destination");
        endpoint.setTargetService(new QName("http://jms.servicemix.org/Test", "Echo"));
        return endpoint;
    }
    
    private JmsProviderEndpoint createProviderEndpoint(ConnectionFactory connFactory) {
        JmsProviderEndpoint endpoint = new JmsProviderEndpoint();
        endpoint.setService(new QName("http://jms.servicemix.org/Test", "Provider"));
        endpoint.setEndpoint("endpoint");
        DefaultProviderMarshaler marshaler = new DefaultProviderMarshaler();
        endpoint.setMarshaler(marshaler);
        endpoint.setConnectionFactory(connFactory);
        endpoint.setDestinationName("destination");
        return endpoint;
    }
}
