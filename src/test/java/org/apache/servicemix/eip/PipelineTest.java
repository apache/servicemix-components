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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.RobustInOnly;
import javax.xml.namespace.QName;

import org.apache.activemq.util.IdGenerator;
import org.apache.servicemix.eip.patterns.Pipeline;
import org.apache.servicemix.store.memory.MemoryStore;
import org.apache.servicemix.tck.ReceiverComponent;

public class PipelineTest extends AbstractEIPTest {

    protected Pipeline pipeline;
    
    protected void setUp() throws Exception {
        super.setUp();

        pipeline = new Pipeline();
        pipeline.setTransformer(createServiceExchangeTarget(new QName("transformer")));
        pipeline.setTarget(createServiceExchangeTarget(new QName("target")));
        configurePipeline();
        activateComponent(pipeline, "pipeline");
    }
    
    protected void configurePipeline() throws Exception {
        pipeline.setStore(new MemoryStore(new IdGenerator()) {
            public void store(String id, Object exchange) throws IOException {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                new ObjectOutputStream(baos).writeObject(exchange);
                super.store(id, exchange);
            }
        });
    }

    public void testInOut() throws Exception {
        InOut me = client.createInOutExchange();
        me.setService(new QName("pipeline"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
    }
    
    public void testInOptionalOut() throws Exception {
        InOptionalOut me = client.createInOptionalOutExchange();
        me.setService(new QName("pipeline"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
    }
    
    public void testInOnly() throws Exception {
        activateComponent(new ReturnOutComponent(), "transformer");
        ReceiverComponent target = activateReceiver("target");

        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("pipeline"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        target.getMessageList().assertMessagesReceived(1);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOnlyWithTransformerFault() throws Exception {
        activateComponent(new ReturnFaultComponent(), "transformer");
        ReceiverComponent target = activateReceiver("target");

        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("pipeline"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        target.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOnlyWithTransformerError() throws Exception {
        activateComponent(new ReturnErrorComponent(), "transformer");
        ReceiverComponent target = activateReceiver("target");

        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("pipeline"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        target.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOnlyWithTargetError() throws Exception {
        activateComponent(new ReturnOutComponent(), "transformer");
        activateComponent(new ReturnErrorComponent(), "target");

        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("pipeline"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        listener.assertExchangeCompleted();
    }
    
    public void testRobustInOnly() throws Exception {
        activateComponent(new ReturnOutComponent(), "transformer");
        ReceiverComponent target = activateReceiver("target");

        RobustInOnly me = client.createRobustInOnlyExchange();
        me.setService(new QName("pipeline"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        target.getMessageList().assertMessagesReceived(1);
        
        listener.assertExchangeCompleted();
    }
    
    public void testRobustInOnlyWithTransformerFault() throws Exception {
        activateComponent(new ReturnFaultComponent(), "transformer");
        ReceiverComponent target = activateReceiver("target");

        RobustInOnly me = client.createRobustInOnlyExchange();
        me.setService(new QName("pipeline"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.done(me);
        
        target.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testRobustInOnlyWithTransformerFaultAndError() throws Exception {
        activateComponent(new ReturnFaultComponent(), "transformer");
        ReceiverComponent target = activateReceiver("target");

        RobustInOnly me = client.createRobustInOnlyExchange();
        me.setService(new QName("pipeline"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
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
        me.setService(new QName("pipeline"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        target.getMessageList().assertMessagesReceived(0);
        
        listener.assertExchangeCompleted();
    }
    
    public void testRobustInOnlyWithTargetFault() throws Exception {
        activateComponent(new ReturnOutComponent(), "transformer");
        activateComponent(new ReturnFaultComponent(), "target");

        RobustInOnly me = client.createRobustInOnlyExchange();
        me.setService(new QName("pipeline"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.done(me);
        
        listener.assertExchangeCompleted();
    }
    
    public void testRobustInOnlyWithTargetFaultAndError() throws Exception {
        activateComponent(new ReturnOutComponent(), "transformer");
        activateComponent(new ReturnFaultComponent(), "target");

        RobustInOnly me = client.createRobustInOnlyExchange();
        me.setService(new QName("pipeline"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.fail(me, new Exception("I do not like faults"));
        
        listener.assertExchangeCompleted();
    }
    
    public void testRobustInOnlyWithTargetError() throws Exception {
        activateComponent(new ReturnOutComponent(), "transformer");
        activateComponent(new ReturnErrorComponent(), "target");

        RobustInOnly me = client.createRobustInOnlyExchange();
        me.setService(new QName("pipeline"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        listener.assertExchangeCompleted();
    }
    
}
