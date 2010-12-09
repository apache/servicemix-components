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
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import org.apache.servicemix.eip.patterns.WireTap;
import org.apache.servicemix.tck.ReceiverComponent;

public class WireTapTxTest extends AbstractEIPTransactionalTest {

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
    
    public void testInOnlySync() throws Exception {
        ReceiverComponent target = activateReceiver("target");

        tm.begin();
        
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        tm.commit();
        
        target.getMessageList().assertMessagesReceived(1);
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(0);
        faultReceiver.getMessageList().assertMessagesReceived(0);
    }
    
    public void testInOnlyAsync() throws Exception {
        ReceiverComponent target = activateReceiver("target");

        tm.begin();
        
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.send(me);
        
        tm.commit();
        
        me = (InOnly) client.receive();
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        target.getMessageList().assertMessagesReceived(1);
        inReceiver.getMessageList().assertMessagesReceived(1);
        outReceiver.getMessageList().assertMessagesReceived(0);
        faultReceiver.getMessageList().assertMessagesReceived(0);
    }
    
    public void testInOutSync() throws Exception {
        activateComponent(new ReturnOutComponent(), "target");

        tm.begin();
        
        InOut me = client.createInOutExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        client.done(me);
        
        tm.commit();
    }
    
    public void testInOutAsync() throws Exception {
        activateComponent(new ReturnOutComponent(), "target");

        tm.begin();
        
        InOut me = client.createInOutExchange();
        me.setService(new QName("wireTap"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.send(me);
        
        tm.commit();
        
        me = (InOut) client.receive();
        client.done(me);
    }
    
}
