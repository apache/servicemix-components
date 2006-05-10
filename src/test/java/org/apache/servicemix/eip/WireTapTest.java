/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.eip;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.RobustInOnly;
import javax.xml.namespace.QName;

import org.apache.servicemix.eip.patterns.WireTap;
import org.apache.servicemix.tck.ReceiverComponent;

public class WireTapTest extends AbstractEIPTest {

    protected ReceiverComponent inReceiver;
    protected ReceiverComponent outReceiver;
    protected ReceiverComponent faultReceiver;
    protected WireTap wireTap;
    
    protected void setUp() throws Exception {
        super.setUp();
        
        inReceiver = activateReceiver("in");
        outReceiver = activateReceiver("out");
        faultReceiver = activateReceiver("fault");
        wireTap = new WireTap();
        wireTap.setInListener(createServiceExchangeTarget(new QName("in")));
        wireTap.setOutListener(createServiceExchangeTarget(new QName("out")));
        wireTap.setFaultListener(createServiceExchangeTarget(new QName("fault")));
        wireTap.setTarget(createServiceExchangeTarget(new QName("target")));
        configurePattern(wireTap);
        activateComponent(wireTap, "wireTap");
    }
    
    public void testInOnly() throws Exception {
        ReceiverComponent target = activateReceiver("target");

        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        target.getMessageList().assertMessagesReceived(1);
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(0);
        faultReceiver.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOnlyWithError() throws Exception {
        activateComponent(new ReturnErrorComponent(), "target");

        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(0);
        faultReceiver.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testRobustInOnly() throws Exception {
        ReceiverComponent target = activateReceiver("target");

        RobustInOnly me = client.createRobustInOnlyExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        target.getMessageList().assertMessagesReceived(1);
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(0);
        faultReceiver.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testRobustInOnlyWithError() throws Exception {
        activateComponent(new ReturnErrorComponent(), "target");
        
        RobustInOnly me = client.createRobustInOnlyExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(0);
        faultReceiver.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testRobustInOnlyWithFault() throws Exception {
        activateComponent(new ReturnFaultComponent(), "target");
        
        RobustInOnly me = client.createRobustInOnlyExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.done(me);
        
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(0);
        faultReceiver.getMessageList().assertMessagesReceived(1);
        
        listener.assertExchangeCompleted();
    }
    
    public void testRobustInOnlyWithFaultAndError() throws Exception {
        activateComponent(new ReturnFaultComponent(), "target");
        
        RobustInOnly me = client.createRobustInOnlyExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.fail(me, new Exception("I do not like faults"));
        
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(0);
        faultReceiver.getMessageList().assertMessagesReceived(1);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOut() throws Exception {
        activateComponent(new ReturnOutComponent(), "target");

        InOut me = client.createInOutExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getOutMessage());
        client.done(me);
        
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(1);
        faultReceiver.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOutWithError() throws Exception {
        activateComponent(new ReturnErrorComponent(), "target");

        InOut me = client.createInOutExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(0);
        faultReceiver.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOutWithFault() throws Exception {
        activateComponent(new ReturnFaultComponent(), "target");

        InOut me = client.createInOutExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.done(me);
        
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(0);
        faultReceiver.getMessageList().assertMessagesReceived(1);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOutAnswerAndError() throws Exception {
        activateComponent(new ReturnOutComponent(), "target");

        InOut me = client.createInOutExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getOutMessage());
        client.fail(me, new Exception("I do not like your answer"));
        
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(1);
        faultReceiver.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOutFaultAndError() throws Exception {
        activateComponent(new ReturnFaultComponent(), "target");

        InOut me = client.createInOutExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.fail(me, new Exception("I do not like your fault"));
        
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(0);
        faultReceiver.getMessageList().assertMessagesReceived(1);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOptionalOutNoAnswer() throws Exception {
        activateComponent(new ReceiverComponent(), "target");

        InOptionalOut me = client.createInOptionalOutExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(0);
        faultReceiver.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOptionalOutWithAnswer() throws Exception {
        activateComponent(new ReturnOutComponent(), "target");

        InOptionalOut me = client.createInOptionalOutExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getOutMessage());
        client.done(me);
        
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(1);
        faultReceiver.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOptionalOutWithFault() throws Exception {
        activateComponent(new ReturnFaultComponent(), "target");

        InOptionalOut me = client.createInOptionalOutExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.done(me);
        
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(0);
        faultReceiver.getMessageList().assertMessagesReceived(1);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOptionalOutWithError() throws Exception {
        activateComponent(new ReturnErrorComponent(), "target");

        InOptionalOut me = client.createInOptionalOutExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(0);
        faultReceiver.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOptionalOutWithFaultAndError() throws Exception {
        activateComponent(new ReturnFaultComponent(), "target");

        InOptionalOut me = client.createInOptionalOutExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.fail(me, new Exception("I do not like faults"));
        
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(0);
        faultReceiver.getMessageList().assertMessagesReceived(1);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOptionalOutWithAnswerAndFault() throws Exception {
        activateComponent(new ReturnOutComponent(), "target");

        InOptionalOut me = client.createInOptionalOutExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getOutMessage());
        Fault fault = me.createFault();
        fault.setContent(createSource("<fault/>"));
        me.setFault(fault);
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(1);
        faultReceiver.getMessageList().assertMessagesReceived(1);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOptionalOutWithAnswerAndError() throws Exception {
        activateComponent(new ReturnOutComponent(), "target");

        InOptionalOut me = client.createInOptionalOutExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getOutMessage());
        client.fail(me, new Exception("Dummy error"));
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(1);
        faultReceiver.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOptionalOutWithAnswerAndFaultAndError() throws Exception {
        activateComponent(new ReturnOutAndErrorComponent(), "target");

        InOptionalOut me = client.createInOptionalOutExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getOutMessage());
        Fault fault = me.createFault();
        fault.setContent(createSource("<fault/>"));
        me.setFault(fault);
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(1);
        faultReceiver.getMessageList().assertMessagesReceived(1);
        
        listener.assertExchangeCompleted();
    }
    
}
