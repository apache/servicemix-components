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

import javax.annotation.Resource;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.apache.servicemix.bean.support.BeanSupport;
import org.apache.servicemix.common.util.MessageUtil;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.listener.MessageExchangeListener;

/**
 * A set of tests for checking InOptionalOut exchange handling by a bean endpoint
 */
public class BeanEndpointInOptionalOutTest extends AbstractBeanComponentTest {
    
    private static final QName IN_OPTIONAL_OUT_PRODUCER = new QName("urn:test", "ioo-producer");
    private static final QName IN_OPTIONAL_OUT_CONSUMER = new QName("urn:test", "ioo-consumer");
    
    //we first have a set of tests that send an InOptionalOut exchange to the bean endpoint
    public void testInOptionalOutWithBeanType() throws Exception {
        BeanEndpoint endpoint = createBeanEndpoint(MyInOptionalOutBean.class, IN_OPTIONAL_OUT_PRODUCER);
        component.addEndpoint(endpoint);
        
        MessageExchange io = client.createInOptionalOutExchange();
        io.setService(IN_OPTIONAL_OUT_PRODUCER);
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.DONE, io.getStatus());
        assertBeanEndpointRequestsMapEmpty(endpoint);        
    }
    
    public void testInOptionalOutWithSimplePojo() throws Exception {
        MySimplePojo pojo = new MySimplePojo();
        BeanEndpoint endpoint = createBeanEndpoint(pojo, IN_OPTIONAL_OUT_PRODUCER);
        component.addEndpoint(endpoint);
        
        MessageExchange io = client.createInOptionalOutExchange();
        io.setService(IN_OPTIONAL_OUT_PRODUCER);
        io.getMessage("in").setContent(new StringSource("<request status='ok'/>"));
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.DONE, io.getStatus());
        assertBeanEndpointRequestsMapEmpty(endpoint);        
    }
    
    public void testInOptionalOutWithPojoHandlingMessageExchange() throws Exception {
        MyMessageExchangePojo pojo = new MyMessageExchangePojo();
        BeanEndpoint endpoint = createBeanEndpoint(pojo, IN_OPTIONAL_OUT_PRODUCER);
        component.addEndpoint(endpoint);
        
        MessageExchange io = client.createInOptionalOutExchange();
        io.setService(IN_OPTIONAL_OUT_PRODUCER);
        io.getMessage("in").setContent(new StringSource("<request status='ok'/>"));
        io.getMessage("in").setProperty("todo", "fault");
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.ACTIVE, io.getStatus());
        assertNotNull(io.getFault());
        client.done(io);
        assertBeanEndpointRequestsMapEmpty(endpoint);        
    }
    
    public void testInOptionalOutWithPojoHandlingMessageExchangeClientFault() throws Exception {
        MyMessageExchangePojo pojo = new MyMessageExchangePojo();
        BeanEndpoint endpoint = createBeanEndpoint(pojo, IN_OPTIONAL_OUT_PRODUCER);
        component.addEndpoint(endpoint);
        
        MessageExchange io = client.createInOptionalOutExchange();
        io.setService(IN_OPTIONAL_OUT_PRODUCER);
        io.getMessage("in").setContent(new StringSource("<request status='ok'/>"));
        io.getMessage("in").setProperty("todo", "reply");
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.ACTIVE, io.getStatus());
        io.setFault(io.createFault());
        client.send(io);
        assertBeanEndpointRequestsMapEmpty(endpoint);        
    }
    
    public void testInOptionalOutWithPojoHandlingMessageExchangeClientError() throws Exception {
        MyMessageExchangePojo pojo = new MyMessageExchangePojo();
        BeanEndpoint endpoint = createBeanEndpoint(pojo, IN_OPTIONAL_OUT_PRODUCER);
        component.addEndpoint(endpoint);
        
        MessageExchange io = client.createInOptionalOutExchange();
        io.setService(IN_OPTIONAL_OUT_PRODUCER);
        io.getMessage("in").setContent(new StringSource("<request status='ok'/>"));
        io.getMessage("in").setProperty("todo", "fault");
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.ACTIVE, io.getStatus());
        assertNotNull(io.getFault());
        client.fail(io, new RuntimeException("Abort! Abort!"));
        assertBeanEndpointRequestsMapEmpty(endpoint);        
    }
    
    public void testInOptionalOutReturnsOut() throws Exception {
        MyInOptionalOutBean bean = new MyInOptionalOutBean();
        bean.response = new StringSource("<goodbye/>");
        BeanEndpoint endpoint = createBeanEndpoint(bean, IN_OPTIONAL_OUT_PRODUCER);
        component.addEndpoint(endpoint);
        
        MessageExchange io = client.createInOptionalOutExchange();
        io.setService(IN_OPTIONAL_OUT_PRODUCER);
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.ACTIVE, io.getStatus());
        client.done(io);
        assertBeanEndpointRequestsMapEmpty(endpoint);        
    }

    public void testInOptionalOutReturnsFault() throws Exception {
        MyInOptionalOutBean bean = new MyInOptionalOutBean();
        bean.fault = new StringSource("<failed_at_provider/>");
        BeanEndpoint endpoint = createBeanEndpoint(bean, IN_OPTIONAL_OUT_PRODUCER);
        component.addEndpoint(endpoint);
        
        MessageExchange io = client.createInOptionalOutExchange();
        io.setService(IN_OPTIONAL_OUT_PRODUCER);
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.ACTIVE, io.getStatus());
        client.done(io);
        assertBeanEndpointRequestsMapEmpty(endpoint);        
    }

    public void testInOptionalOutClientFault() throws Exception {
        MyInOptionalOutBean bean = new MyInOptionalOutBean();
        bean.response = new StringSource("<goodbye/>");
        BeanEndpoint endpoint = createBeanEndpoint(bean, IN_OPTIONAL_OUT_PRODUCER);
        component.addEndpoint(endpoint);
        
        MessageExchange io = client.createInOptionalOutExchange();
        io.setService(IN_OPTIONAL_OUT_PRODUCER);
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.ACTIVE, io.getStatus());
        Fault fault = io.createFault();
        fault.setContent(new StringSource("<failed_at_consumer/>"));
        client.fail(io, fault);
        assertBeanEndpointRequestsMapEmpty(endpoint);        
    }


    // this is a set of tests where the bean endpoint also acts as consumer and sends InOptionalOut exchanges
    public void testInOptionalOutConsumerDone() throws Exception {
        BeanEndpoint provider = createBeanEndpoint(MyInOptionalOutBean.class, IN_OPTIONAL_OUT_PRODUCER);
        component.addEndpoint(provider);
        BeanEndpoint consumer = createConsumerEndpoint();
                
        MessageExchange io = client.createInOnlyExchange();
        io.setService(IN_OPTIONAL_OUT_CONSUMER);
        io.setOperation(new QName("send"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.DONE, io.getStatus());
        assertBeanEndpointRequestsMapEmpty(provider);        
        assertBeanEndpointRequestsMapEmpty(consumer);
    }
    
    public void testConsumerInOptionalOutProviderReturnsOut() throws Exception {
        MyInOptionalOutBean bean = new MyInOptionalOutBean();
        bean.response = new StringSource("<goodbye/>");
        BeanEndpoint provider = createBeanEndpoint(bean, IN_OPTIONAL_OUT_PRODUCER);
        component.addEndpoint(provider);
        BeanEndpoint consumer = createConsumerEndpoint();
                
        MessageExchange io = client.createInOnlyExchange();
        io.setService(IN_OPTIONAL_OUT_CONSUMER);
        io.setOperation(new QName("send"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.DONE, io.getStatus());
        assertBeanEndpointRequestsMapEmpty(provider);        
        assertBeanEndpointRequestsMapEmpty(consumer);
    }
    
    public void testConsumerInOptionalOutProviderReturnsFault() throws Exception {
        MyInOptionalOutBean bean = new MyInOptionalOutBean();
        bean.fault = new StringSource("<fault_at_provider/>");
        BeanEndpoint provider = createBeanEndpoint(bean, IN_OPTIONAL_OUT_PRODUCER);
        component.addEndpoint(provider);
        BeanEndpoint consumer = createConsumerEndpoint();
                
        MessageExchange io = client.createInOnlyExchange();
        io.setService(IN_OPTIONAL_OUT_CONSUMER);
        io.setOperation(new QName("send"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.DONE, io.getStatus());
        assertBeanEndpointRequestsMapEmpty(provider);        
        assertBeanEndpointRequestsMapEmpty(consumer);
    }
    
    public void testConsumerInOptionalOutConsumerReturnsFault() throws Exception {
        MyInOptionalOutBean bean = new MyInOptionalOutBean();
        bean.response = new StringSource("<goodbye/>");
        BeanEndpoint provider = createBeanEndpoint(bean, IN_OPTIONAL_OUT_PRODUCER);
        component.addEndpoint(provider);
        BeanEndpoint consumer = createConsumerEndpoint();
                
        MessageExchange io = client.createInOnlyExchange();
        io.setService(IN_OPTIONAL_OUT_CONSUMER);
        io.setOperation(new QName("sendAndFault"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.send(io);
        
        io = client.receive();
        assertEquals(ExchangeStatus.DONE, io.getStatus());
        assertBeanEndpointRequestsMapEmpty(provider);        
        assertBeanEndpointRequestsMapEmpty(consumer);
    }
    
    private BeanEndpoint createConsumerEndpoint() throws Exception {
        MyConsumerBean bean = new MyConsumerBean();
        bean.target = IN_OPTIONAL_OUT_PRODUCER;
        BeanEndpoint endpoint = new BeanEndpoint();
        endpoint.setBean(bean);
        endpoint.setService(IN_OPTIONAL_OUT_CONSUMER);
        endpoint.setEndpoint("endpoint");
        component.addEndpoint(endpoint);
        return endpoint;
    }
    
    private BeanEndpoint createBeanEndpoint(Object bean, QName service) {
        BeanEndpoint transformEndpoint = new BeanEndpoint();
        transformEndpoint.setBean(bean);
        transformEndpoint.setService(service);
        transformEndpoint.setEndpoint("endpoint");
        return transformEndpoint;
    }
      
    private BeanEndpoint createBeanEndpoint(Class<?> type, QName service) {
        BeanEndpoint endpoint = new BeanEndpoint();
        endpoint.setBeanType(type);
        endpoint.setService(service);
        endpoint.setEndpoint("endpoint");
        return endpoint;
    }
    
    public static final class MyInOptionalOutBean extends BeanSupport implements MessageExchangeListener {
        
        private Source fault;
        private Source response;
        
        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange instanceof InOptionalOut) {
                onInOptionalOut((InOptionalOut) exchange);
            } else {
                exchange.setError(new Exception("Only InOptionalOut supported here"));
            }
        }

        private void onInOptionalOut(InOptionalOut exchange) throws MessagingException {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                if (response != null) {
                    answer(exchange, response);
                    response = null;
                } else if (fault != null) {
                    fail(exchange, exchange.createFault());
                    fault = null;
                } else {
                    done(exchange);
                }
            }
        }
    }
    
    public static final class MySimplePojo {
        
        public void handle(@XPath(xpath = "/request/@status") String status) {
            if (status.equalsIgnoreCase("ok")) {
                // ok
            } else {
                // let's throw an Exception to see if we can blow things up
                throw new RuntimeException("Expecting OK, but was " + status);
            }
        }
    }
    
    public static final class MyMessageExchangePojo {
        
        @Resource
        private DeliveryChannel channel;
        
        public void handle(MessageExchange exchange, @Property(name = "todo") String todo) throws MessagingException {
            if ("fault".equals(todo)) {
                exchange.setFault(exchange.createFault());
                exchange.getFault().setContent(new StringSource("<fault>It went astray!</fault>"));
                channel.send(exchange);
            } else if ("reply".equals(todo)) {
                NormalizedMessage out = exchange.createMessage();
                exchange.setMessage(out, "out");
                MessageUtil.transfer(exchange.getMessage("in"), out);
                channel.send(exchange);
            } else {
                // everything is OK
            }
        }
    }
    
    public static final class MyConsumerBean extends BeanSupport implements MessageExchangeListener {
        
        @Resource
        private QName target;
        private MessageExchange original;
        private Source fault;
                
        public void send() throws MessagingException {
            InOptionalOut ioo = getExchangeFactory().createInOptionalOutExchange();
            ioo.setService(target);
            ioo.setInMessage(ioo.createMessage());
            ioo.getMessage("in").setContent(new StringSource("<hello/>"));
            send(ioo);
        }

        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange.getRole() == Role.PROVIDER) {
                original = exchange;
                if (exchange.getOperation().equals(new QName("sendAndFault"))) {
                    fault = new StringSource("<faulted_by_consumer/>");
                }
                send();
            } else {                
                if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                    if (fault != null) {
                        fail(exchange, exchange.createFault());
                        fault = null;
                    } else {
                        done(exchange);
                        done(original);
                    }
                } else {
                    done(original);
                }
            }
        }
    }
}
