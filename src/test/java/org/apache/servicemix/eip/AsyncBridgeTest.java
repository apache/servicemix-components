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

import java.util.concurrent.TimeoutException;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import org.apache.servicemix.components.util.TraceComponent;
import org.apache.servicemix.eip.patterns.AsyncBridge;
import org.apache.servicemix.eip.patterns.WireTap;

public class AsyncBridgeTest extends AbstractEIPTest {

    protected AsyncBridge asyncBridge;
    
    protected void setUp() throws Exception {
        super.setUp();

        asyncBridge = new AsyncBridge();
        asyncBridge.setTarget(createServiceExchangeTarget(new QName("target")));
        asyncBridge.setTimeout(2000);
        configurePattern(asyncBridge);
        activateComponent(asyncBridge, "asyncBridge");
    }
    
    protected void configureContainer() throws Exception {
    }
    
    public void testInOut() throws Exception {
        WireTap wireTap = new WireTap();
        wireTap.setCopyProperties(true);
        wireTap.setTarget(createServiceExchangeTarget(new QName("asyncBridge")));
        activateComponent(wireTap, "target");
        
        InOut me = client.createInOutExchange();
        me.setService(new QName("asyncBridge"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getOutMessage());
        client.done(me);
        
        Thread.sleep(100);
    }
    
    public void testInOutWithTimeOut() throws Exception {
        activateComponent(new TraceComponent(), "target");
        
        InOut me = client.createInOutExchange();
        me.setService(new QName("asyncBridge"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        assertTrue(me.getError() instanceof TimeoutException);
        
        Thread.sleep(100);
    }
    
    
}
