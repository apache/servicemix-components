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
import javax.xml.namespace.QName;

import org.apache.servicemix.eip.patterns.Pipeline;
import org.apache.servicemix.tck.ReceiverComponent;

public class PipelineTxTest extends AbstractEIPTransactionalTest {

    protected Pipeline pipeline;
    
    protected void setUp() throws Exception {
        super.setUp();

        pipeline = new Pipeline();
        pipeline.setTransformer(createServiceExchangeTarget(new QName("transformer")));
        pipeline.setTarget(createServiceExchangeTarget(new QName("target")));
        configurePattern(pipeline);
        activateComponent(pipeline, "pipeline");
    }
    
    public void testInOnlySync() throws Exception {
        activateComponent(new ReturnOutComponent(), "transformer");
        ReceiverComponent target = activateReceiver("target");

        tm.begin();
        
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("pipeline"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        tm.commit();
        
        target.getMessageList().assertMessagesReceived(1);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOnlyAsync() throws Exception {
        activateComponent(new ReturnOutComponent(), "transformer");
        ReceiverComponent target = activateReceiver("target");

        tm.begin();
        
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("pipeline"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.send(me);
        
        tm.commit();
        
        me = (InOnly) client.receive();
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        target.getMessageList().assertMessagesReceived(1);
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOnlySyncWithError() throws Exception {
        activateComponent(new ReturnErrorComponent(), "transformer");
        activateReceiver("target");

        tm.begin();
        
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("pipeline"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        tm.commit();
        
        listener.assertExchangeCompleted();
    }
    
    public void testInOnlyAsyncWithError() throws Exception {
        activateComponent(new ReturnErrorComponent(), "transformer");
        activateReceiver("target");

        tm.begin();
        
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("pipeline"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.send(me);
        
        tm.commit();
        
        me = (InOnly) client.receive();
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        listener.assertExchangeCompleted();
    }
    
}
