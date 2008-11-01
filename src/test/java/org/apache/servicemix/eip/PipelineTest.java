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
package org.apache.servicemix.eip;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;
import javax.xml.namespace.QName;

import org.apache.servicemix.eip.patterns.Pipeline;
import org.apache.servicemix.tck.ReceiverComponent;

public class PipelineTest extends AbstractEIPTest {

    protected Pipeline pipeline;
    
    protected void setUp() throws Exception {
        super.setUp();

        pipeline = new Pipeline();
        pipeline.setTransformer(createServiceExchangeTarget(new QName("transformer")));
        pipeline.setTarget(createServiceExchangeTarget(new QName("target")));
        configurePattern(pipeline);
        activateComponent(pipeline, "pipeline");
    }
    
    public void testInOut() throws Exception {
        InOut me = client.createInOutExchange();
        populateAndSendExchange(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
    }
    
    public void testInOptionalOut() throws Exception {
        InOptionalOut me = client.createInOptionalOutExchange();
        populateAndSendExchange(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
    }
    
    public void testInOnly() throws Exception {
        pipeline.setCopyProperties(true);
        
        activateComponent(new ReturnOutComponent(), "transformer");
        ReceiverComponent target = activateReceiver("target");

        InOnly me = client.createInOnlyExchange();
        me.getInMessage().setProperty("prop", "value");
        populateAndSendExchange(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());

        target.getMessageList().assertMessagesReceived(1);
        assertEquals("value", ((NormalizedMessage) target.getMessageList().flushMessages().get(0)).getProperty("prop"));

        listener.assertExchangeCompleted();
        
        me = client.createInOnlyExchange();
        me.getInMessage().setProperty("prop", "value");
        populateAndSendExchange(me, false);
        
        me = (InOnly) client.receive();
        assertEquals(ExchangeStatus.DONE, me.getStatus());

        target.getMessageList().assertMessagesReceived(1);
        assertEquals("value", ((NormalizedMessage) target.getMessageList().flushMessages().get(0)).getProperty("prop"));
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOnlyWithTransformerFault() throws Exception {
        activateComponent(new ReturnFaultComponent(), "transformer");
        ReceiverComponent target = activateReceiver("target");

        InOnly me = client.createInOnlyExchange();
        populateAndSendExchange(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        target.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOnlyWithTransformerFaultSentToTarget() throws Exception {
        pipeline.setCopyProperties(true);
        pipeline.setSendFaultsToTarget(true);
        activateComponent(new ReturnFaultComponent(), "transformer");
        ReceiverComponent target = activateReceiver("target");

        InOnly me = client.createInOnlyExchange();
        me.getInMessage().setProperty("prop", "value");
        populateAndSendExchange(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        target.getMessageList().assertMessagesReceived(1);
        assertEquals("value", ((NormalizedMessage) target.getMessageList().flushMessages().get(0)).getProperty("prop"));
        
        listener.assertExchangeCompleted();
        
        me = client.createInOnlyExchange();
        me.getInMessage().setProperty("prop", "value");
        populateAndSendExchange(me, false);
        
        me = (InOnly) client.receive();
        assertEquals(ExchangeStatus.DONE, me.getStatus());

        target.getMessageList().assertMessagesReceived(1);
        assertEquals("value", ((NormalizedMessage) target.getMessageList().flushMessages().get(0)).getProperty("prop"));
    }
    
    public void testInOnlyWithTransformerError() throws Exception {
        activateComponent(new ReturnErrorComponent(), "transformer");
        ReceiverComponent target = activateReceiver("target");

        InOnly me = client.createInOnlyExchange();
        populateAndSendExchange(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        target.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOnlyWithTargetError() throws Exception {
        activateComponent(new ReturnOutComponent(), "transformer");
        activateComponent(new ReturnErrorComponent(), "target");

        InOnly me = client.createInOnlyExchange();
        populateAndSendExchange(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        listener.assertExchangeCompleted();
    }
    
    public void testRobustInOnly() throws Exception {
        pipeline.setCopyProperties(true);
        activateComponent(new ReturnOutComponent(), "transformer");
        ReceiverComponent target = activateReceiver("target");

        RobustInOnly me = client.createRobustInOnlyExchange();
        me.getInMessage().setProperty("prop", "value");
        populateAndSendExchange(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        target.getMessageList().assertMessagesReceived(1);
        assertEquals("value", ((NormalizedMessage) target.getMessageList().flushMessages().get(0)).getProperty("prop"));
        
        listener.assertExchangeCompleted();
        
        me = client.createRobustInOnlyExchange();
        me.getInMessage().setProperty("prop", "value");
        populateAndSendExchange(me, false);
        
        me = (RobustInOnly) client.receive();
        assertEquals(ExchangeStatus.DONE, me.getStatus());

        target.getMessageList().assertMessagesReceived(1);
        assertEquals("value", ((NormalizedMessage) target.getMessageList().flushMessages().get(0)).getProperty("prop"));

        listener.assertExchangeCompleted();
    }
    
    public void testRobustInOnlyWithTransformerFault() throws Exception {
        activateComponent(new ReturnFaultComponent(), "transformer");
        ReceiverComponent target = activateReceiver("target");

        RobustInOnly me = client.createRobustInOnlyExchange();
        populateAndSendExchange(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.done(me);
        
        target.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
        
        me = client.createRobustInOnlyExchange();
        me.getInMessage().setProperty("prop", "value");
        populateAndSendExchange(me, false);
        
        me = (RobustInOnly) client.receive();
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.done(me);

        target.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testRobustInOnlyWithTransformerFaultSentToTarget() throws Exception {
        pipeline.setCopyProperties(true);
        pipeline.setSendFaultsToTarget(true);
        activateComponent(new ReturnFaultComponent(), "transformer");
        ReceiverComponent target = activateReceiver("target");

        RobustInOnly me = client.createRobustInOnlyExchange();
        me.getInMessage().setProperty("prop", "value");
        populateAndSendExchange(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        target.getMessageList().assertMessagesReceived(1);
        assertEquals("value", ((NormalizedMessage) target.getMessageList().flushMessages().get(0)).getProperty("prop"));
        
        listener.assertExchangeCompleted();
        
        me = client.createRobustInOnlyExchange();
        me.getInMessage().setProperty("prop", "value");
        populateAndSendExchange(me, false);
        
        me = (RobustInOnly) client.receive();
        assertEquals(ExchangeStatus.DONE, me.getStatus());

        target.getMessageList().assertMessagesReceived(1);
        assertEquals("value", ((NormalizedMessage) target.getMessageList().flushMessages().get(0)).getProperty("prop"));

        listener.assertExchangeCompleted();
    }
    
    public void testRobustInOnlyWithTransformerFaultAndError() throws Exception {
        activateComponent(new ReturnFaultComponent(), "transformer");
        ReceiverComponent target = activateReceiver("target");

        RobustInOnly me = client.createRobustInOnlyExchange();
        populateAndSendExchange(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.fail(me, new Exception("I do not like faults"));
        
        target.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testRobustInOnlyWithTransformerError() throws Exception {
        activateComponent(new ReturnErrorComponent(), "transformer");
        ReceiverComponent target = activateReceiver("target");

        RobustInOnly me = client.createRobustInOnlyExchange();
        populateAndSendExchange(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        target.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testRobustInOnlyWithTargetFault() throws Exception {
        activateComponent(new ReturnOutComponent(), "transformer");
        activateComponent(new ReturnFaultComponent(), "target");

        RobustInOnly me = client.createRobustInOnlyExchange();
        populateAndSendExchange(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.done(me);
        
        listener.assertExchangeCompleted();
    }
    
    public void testRobustInOnlyWithTargetFaultAndError() throws Exception {
        activateComponent(new ReturnOutComponent(), "transformer");
        activateComponent(new ReturnFaultComponent(), "target");

        RobustInOnly me = client.createRobustInOnlyExchange();
        populateAndSendExchange(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.fail(me, new Exception("I do not like faults"));
        
        listener.assertExchangeCompleted();
    }

    public void testRobustInOnlyWithTargetError() throws Exception {
        activateComponent(new ReturnOutComponent(), "transformer");
        activateComponent(new ReturnErrorComponent(), "target");

        RobustInOnly me = client.createRobustInOnlyExchange();
        populateAndSendExchange(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        listener.assertExchangeCompleted();
    }

    private void populateAndSendExchange(MessageExchange me) throws Exception {
    	populateAndSendExchange(me, true);
    }
    
    private void populateAndSendExchange(MessageExchange me, boolean synchronous) throws Exception {
    	me.setService(new QName("pipeline"));
    	NormalizedMessage message = me.getMessage("in");
		message.setContent(createSource("<hello/>"));
		if (synchronous) 
			client.sendSync(me);
		else
			client.send(me);
    }
    
}
