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

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.transaction.xa.XAException;
import javax.xml.namespace.QName;

import org.apache.geronimo.transaction.manager.GeronimoTransactionManager;
import org.apache.servicemix.bean.TransformBeanSupportTest.ReturnErrorComponent;
import org.apache.servicemix.bean.TransformBeanSupportTest.ReturnFaultComponent;
import org.apache.servicemix.bean.support.TransformBeanSupport;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.ReceiverComponent;
import org.w3c.dom.Element;


public class TransformBeanSupportSedaFlowTest extends TransformBeanSupportTest {

    private GeronimoTransactionManager txmanager;

    public void testInOutWithTx() throws Exception {
        TransformBeanSupport transformer = new MyTransformer();
        BeanEndpoint transformEndpoint = createBeanEndpoint(transformer);
        component.addEndpoint(transformEndpoint);

        txmanager.begin();
        MessageExchange io = client.createInOutExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.sendSync(io);
        
        assertEquals(ExchangeStatus.ACTIVE, io.getStatus());
        Element e = new SourceTransformer().toDOMElement(io.getMessage("out"));
        assertEquals("hello", e.getNodeName());
        
        client.done(io);
        assertEquals(ExchangeStatus.DONE, io.getStatus());
        txmanager.commit();
        assertBeanEndpointRequestsMapEmpty(transformEndpoint);
    }
    
    public void testInOnlyWithTx() throws Exception {
        TransformBeanSupport transformer = createTransformer("receiver");
        BeanEndpoint transformEndpoint = createBeanEndpoint(transformer);
        component.addEndpoint(transformEndpoint);

        ReceiverComponent receiver = new ReceiverComponent();
        activateComponent(receiver, "receiver");

        txmanager.begin();
        MessageExchange io = client.createInOnlyExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.sendSync(io);
        
        assertEquals(ExchangeStatus.DONE, io.getStatus());
        txmanager.commit();
        assertBeanEndpointRequestsMapEmpty(transformEndpoint);
        
        receiver.getMessageList().assertMessagesReceived(1);
    }
    
    public void testInOnlyWithErrorTx() throws Exception {
        TransformBeanSupport transformer = createTransformer("error");
        BeanEndpoint transformEndpoint = createBeanEndpoint(transformer);
        component.addEndpoint(transformEndpoint);

        activateComponent(new ReturnErrorComponent(), "error");

        txmanager.begin();
        MessageExchange io = client.createInOnlyExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.sendSync(io);
        
        assertEquals(ExchangeStatus.ERROR, io.getStatus());
        txmanager.commit();
        assertBeanEndpointRequestsMapEmpty(transformEndpoint);
    }
    
    public void testRobustInOnlyWithFaultTx() throws Exception {
        TransformBeanSupport transformer = createTransformer("fault");
        BeanEndpoint transformEndpoint = createBeanEndpoint(transformer);
        component.addEndpoint(transformEndpoint);

        activateComponent(new ReturnFaultComponent() {
            @Override
            public void onMessageExchange(MessageExchange exchange) throws MessagingException {
                if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                    Fault fault = exchange.createFault();
                    fault.setContent(new StringSource("<fault/>"));
                    exchange.setFault(fault);
                    sendSync(exchange);
                }
            }
        }, "fault");

        txmanager.begin();
        MessageExchange io = client.createRobustInOnlyExchange();
        io.setService(new QName("transform"));
        io.getMessage("in").setContent(new StringSource("<hello/>"));
        client.sendSync(io);

        assertEquals(ExchangeStatus.ACTIVE, io.getStatus());
        assertNotNull(io.getFault());
        client.done(io);
        txmanager.commit();
        
        assertBeanEndpointRequestsMapEmpty(transformEndpoint);
    }
    
    protected void configureContainer() {
        try {
            txmanager = new GeronimoTransactionManager();
            container.setTransactionManager(txmanager);
            container.setAutoEnlistInTransaction(true);
            container.setFlowName("seda");
        } catch (XAException e) {
            fail("Unable to create TransactionMaanger: " + e.getMessage());
        }
    }
    
}
