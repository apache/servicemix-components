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
package org.apache.servicemix.bean;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.annotation.Resource;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.servicemix.bean.support.ExchangeTarget;
import org.apache.servicemix.bean.support.TransformBeanSupport;
import org.apache.servicemix.common.util.MessageUtil;
import org.apache.servicemix.components.util.ComponentSupport;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.expression.JAXPXPathExpression;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.listener.MessageExchangeListener;
import org.apache.servicemix.jbi.transformer.CopyTransformer;
import org.apache.servicemix.tck.ReceiverComponent;

public class TransformBeanSupportTest extends AbstractBeanComponentTest {
    
    public void testInOut() throws Exception {
        TransformBeanSupport transformer = new MyTransformer();
        BeanEndpoint transformEndpoint = createBeanEndpoint(transformer);
        component.addEndpoint(transformEndpoint);

        MessageExchange io = client.createInOutExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.ACTIVE, io.getStatus());
        Element e = new SourceTransformer().toDOMElement(io.getMessage("out"));
        assertEquals("hello", e.getNodeName());
        
        client.done(io);
        assertEquals(ExchangeStatus.DONE, io.getStatus());
        assertBeanEndpointRequestsMapEmpty(transformEndpoint);
    }
    
    public void testInOutWithFault() throws Exception {
        TransformBeanSupport transformer = new MyTransformer();
        BeanEndpoint transformEndpoint = createBeanEndpoint(transformer);
        component.addEndpoint(transformEndpoint);

        MessageExchange io = client.createInOutExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.ACTIVE, io.getStatus());
        Element e = new SourceTransformer().toDOMElement(io.getMessage("out"));
        assertEquals("hello", e.getNodeName());
        
        client.fail(io, new Exception("We failed to handle the reponse"));
        assertEquals(ExchangeStatus.ERROR, io.getStatus());
        assertBeanEndpointRequestsMapEmpty(transformEndpoint);
    }
    
    public void testInOutWithBeanType() throws Exception {
        BeanEndpoint endpoint = createBeanEndpoint(AssertSameInstancePojo.class);
        component.addEndpoint(endpoint);
        
        MessageExchange io = client.createInOutExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.ACTIVE, io.getStatus());
        Element e = new SourceTransformer().toDOMElement(io.getMessage("out"));
        assertEquals("hello", e.getNodeName());
        
        client.done(io);
        assertEquals(ExchangeStatus.DONE, io.getStatus());
        assertBeanEndpointRequestsMapEmpty(endpoint);        
    }
    
    

    public void testInOnly() throws Exception {
        TransformBeanSupport transformer = createTransformer("receiver");
        BeanEndpoint transformEndpoint = createBeanEndpoint(transformer);
        component.addEndpoint(transformEndpoint);

        ReceiverComponent receiver = new ReceiverComponent();
        activateComponent(receiver, "receiver");
        
        MessageExchange io = client.createInOnlyExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.DONE, io.getStatus());
        assertBeanEndpointRequestsMapEmpty(transformEndpoint);
        
        receiver.getMessageList().assertMessagesReceived(1);
    }
    
    public void testInOnlyWithCorrelation() throws Exception {
        TransformBeanSupport transformer = createTransformer("receiver");
        BeanEndpoint transformEndpoint = createBeanEndpoint(transformer);
        transformEndpoint.setCorrelationExpression(new JAXPXPathExpression("/message/@id"));
        component.addEndpoint(transformEndpoint);

        ReceiverComponent receiver = new ReceiverComponent();
        activateComponent(receiver, "receiver");
        
        MessageExchange io = client.createInOnlyExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<message id='1'/>"));
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.DONE, io.getStatus());
        assertBeanEndpointRequestsMapEmpty(transformEndpoint);
        
        receiver.getMessageList().assertMessagesReceived(1);
    }

    public void testInOnlyWithError() throws Exception {
        TransformBeanSupport transformer = createTransformer("error");
        BeanEndpoint transformEndpoint = createBeanEndpoint(transformer);
        component.addEndpoint(transformEndpoint);

        activateComponent(new ReturnErrorComponent(), "error");
        
        MessageExchange io = client.createInOnlyExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.ERROR, io.getStatus());
        assertBeanEndpointRequestsMapEmpty(transformEndpoint);
    }
    
    public void testInOnlyWithDestination() throws Exception {
        BeanEndpoint endpoint = createBeanEndpoint(MyDestinationTransformer.class);
        component.addEndpoint(endpoint);

        ActivationSpec spec = new ActivationSpec(new EchoComponent());
        spec.setService(new QName("test", "receiver"));
        spec.setComponentName("receiver");
        container.activateComponent(spec);
        
        MessageExchange io = client.createInOnlyExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.DONE, io.getStatus());
        assertBeanEndpointRequestsMapEmpty(endpoint);
    }

    public void testRobustInOnly() throws Exception {
        TransformBeanSupport transformer = createTransformer("receiver");
        BeanEndpoint transformEndpoint = createBeanEndpoint(transformer);
        component.addEndpoint(transformEndpoint);

        ReceiverComponent receiver = new ReceiverComponent();
        activateComponent(receiver, "receiver");
        
        MessageExchange io = client.createRobustInOnlyExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.DONE, io.getStatus());
        assertBeanEndpointRequestsMapEmpty(transformEndpoint);
        
        receiver.getMessageList().assertMessagesReceived(1);
    }

    public void testRobustInOnlyWithFault() throws Exception {
        TransformBeanSupport transformer = createTransformer("fault");
        BeanEndpoint transformEndpoint = createBeanEndpoint(transformer);
        component.addEndpoint(transformEndpoint);

        activateComponent(new ReturnFaultComponent(), "fault");

        MessageExchange io = client.createRobustInOnlyExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);

        io = client.receive();
        assertEquals(ExchangeStatus.ACTIVE, io.getStatus());
        assertNotNull(io.getFault());
        client.done(io);
        assertBeanEndpointRequestsMapEmpty(transformEndpoint);
    }

    public void testRobustInOnlyWithFaultAndError() throws Exception {
        TransformBeanSupport transformer = createTransformer("fault");
        BeanEndpoint transformEndpoint = createBeanEndpoint(transformer);
        component.addEndpoint(transformEndpoint);

        activateComponent(new ReturnFaultComponent(), "fault");

        MessageExchange io = client.createRobustInOnlyExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);

        io = client.receive();
        assertEquals(ExchangeStatus.ACTIVE, io.getStatus());
        assertNotNull(io.getFault());
        client.fail(io, new Exception("I do not like faults"));
        assertBeanEndpointRequestsMapEmpty(transformEndpoint);
    }
    
    public void testSetCopyProperties() throws Exception {
        TransformBeanSupport transformer = createTransformer("fault");
        transformer.setCopyProperties(true);
        assertTrue(transformer.isCopyProperties());
        assertTrue(((CopyTransformer) transformer.getMessageTransformer()).isCopyProperties());
        transformer.setCopyProperties(false);
        assertFalse(transformer.isCopyProperties());
        assertFalse(((CopyTransformer) transformer.getMessageTransformer()).isCopyProperties());
    }
    
    public void testSetCopyAttachements() throws Exception {
        TransformBeanSupport transformer = createTransformer("fault");
        transformer.setCopyAttachments(true);
        assertTrue(transformer.isCopyAttachments());
        assertTrue(((CopyTransformer) transformer.getMessageTransformer()).isCopyAttachments());
        transformer.setCopyAttachments(false);
        assertFalse(transformer.isCopyAttachments());
        assertFalse(((CopyTransformer) transformer.getMessageTransformer()).isCopyAttachments());
    }

    protected MyTransformer createTransformer(String targetService) {
        MyTransformer transformer = new MyTransformer();
        ExchangeTarget target = new ExchangeTarget();
        target.setService(new QName(targetService));
        transformer.setTarget(target);
        return transformer;
    }

    protected BeanEndpoint createBeanEndpoint(TransformBeanSupport transformer) {
        BeanEndpoint transformEndpoint = new BeanEndpoint();
        transformEndpoint.setBean(transformer);
        transformEndpoint.setService(new QName("transform"));
        transformEndpoint.setEndpoint("endpoint");
        return transformEndpoint;
    }
    
    private BeanEndpoint createBeanEndpoint(Class<?> type) {
        BeanEndpoint endpoint = new BeanEndpoint();
        endpoint.setBeanType(type);
        endpoint.setService(new QName("transform"));
        endpoint.setEndpoint("endpoint");
        return endpoint;
    }
    
    protected void activateComponent(ComponentSupport comp, String name) throws Exception {
        comp.setService(new QName(name));
        comp.setEndpoint("endpoint");
        container.activateComponent(comp, name);
    }
    
    public static class MyTransformer extends TransformBeanSupport {
        protected boolean transform(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws Exception {
            MessageUtil.transfer(in, out);
            return true;
        }
    }
    
    public static class ReturnErrorComponent extends ComponentSupport implements org.apache.servicemix.MessageExchangeListener {

        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                fail(exchange, new Exception());
            }
        }
    }

    public static class ReturnFaultComponent extends ComponentSupport implements org.apache.servicemix.MessageExchangeListener {
        
        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                Fault fault = exchange.createFault();
                fault.setContent(new StringSource("<fault/>"));
                fail(exchange, fault);
            }
        }
    }
    
    public static class AssertSameInstancePojo implements MessageExchangeListener {
        
        @Resource 
        private DeliveryChannel channel;
        
        private String id;

        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            assertId(exchange);
            if (ExchangeStatus.ACTIVE.equals(exchange.getStatus())) {
                MessageUtil.enableContentRereadability(exchange.getMessage("in"));
                MessageUtil.transferInToOut(exchange, exchange);
                channel.send(exchange);
            }
        }

        private void assertId(MessageExchange exchange) {
            if (exchange.getStatus().equals(ExchangeStatus.ACTIVE)) {
                id = exchange.getExchangeId();
            } else {
                // make sure that the same object is being used to handle the Exchange with status DONE 
                assertEquals(id, exchange.getExchangeId());
            }
        }        
    }
    
    public static class MyDestinationTransformer implements MessageExchangeListener {
        
        @org.apache.servicemix.bean.ExchangeTarget(uri = "service:test:receiver")
        private Destination receiver;
        
        @Resource
        private DeliveryChannel channel;
        
        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE && exchange instanceof InOnly) {
                NormalizedMessage forward = receiver.createMessage();
                forward.setContent(exchange.getMessage("in").getContent());
                Future<NormalizedMessage> response = receiver.send(forward);
                //let's wait for the response to come back
                try {
                    response.get();
                    exchange.setStatus(ExchangeStatus.DONE);
                } catch (InterruptedException e) {
                    exchange.setError(e);
                } catch (ExecutionException e) {
                    exchange.setError(e);
                } finally {
                    channel.send(exchange);
                }
            }
        }
    }
}
