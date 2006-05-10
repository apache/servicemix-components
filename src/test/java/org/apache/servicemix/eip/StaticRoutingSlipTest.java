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
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.RobustInOnly;
import javax.xml.namespace.QName;

import org.apache.servicemix.eip.patterns.StaticRoutingSlip;
import org.apache.servicemix.eip.support.ExchangeTarget;


public class StaticRoutingSlipTest extends AbstractEIPTest {

    protected StaticRoutingSlip routingSlip;
    
    protected void setUp() throws Exception {
        super.setUp();

        routingSlip = new StaticRoutingSlip();
        routingSlip.setTargets(
                new ExchangeTarget[] {
                        createServiceExchangeTarget(new QName("target1")),
                        createServiceExchangeTarget(new QName("target2")),
                        createServiceExchangeTarget(new QName("target3"))
                });
        configurePattern(routingSlip);
        activateComponent(routingSlip, "routingSlip");
    }

    public void testInOnly() throws Exception {
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("routingSlip"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        assertNotNull(me.getError());
        assertEquals("Use an InOut MEP", me.getError().getMessage());
    }

    public void testRobustInOnly() throws Exception {
        RobustInOnly me = client.createRobustInOnlyExchange();
        me.setService(new QName("routingSlip"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        assertNotNull(me.getError());
        assertEquals("Use an InOut MEP", me.getError().getMessage());
    }

    public void testInOptionalOut() throws Exception {
        InOptionalOut me = client.createInOptionalOutExchange();
        me.setService(new QName("routingSlip"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        assertNotNull(me.getError());
        assertEquals("Use an InOut MEP", me.getError().getMessage());
    }

    public void testDone() throws Exception {
        activateComponent(new ReturnOutComponent(), "target1");
        activateComponent(new ReturnOutComponent(), "target2");
        activateComponent(new ReturnOutComponent(), "target3");

        InOut me = client.createInOutExchange();
        me.setService(new QName("routingSlip"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getOutMessage());
        client.done(me);
        
        listener.assertExchangeCompleted();
    }
    
    public void testFaultOnFirst() throws Exception {
        activateComponent(new ReturnFaultComponent(), "target1");
        activateComponent(new ReturnOutComponent(), "target2");
        activateComponent(new ReturnOutComponent(), "target3");

        InOut me = client.createInOutExchange();
        me.setService(new QName("routingSlip"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.done(me);
        
        listener.assertExchangeCompleted();
    }
    
    public void testErrorOnFirst() throws Exception {
        activateComponent(new ReturnErrorComponent(), "target1");
        activateComponent(new ReturnOutComponent(), "target2");
        activateComponent(new ReturnOutComponent(), "target3");

        InOut me = client.createInOutExchange();
        me.setService(new QName("routingSlip"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        listener.assertExchangeCompleted();
    }
    
    public void testFaultOnSecond() throws Exception {
        activateComponent(new ReturnOutComponent(), "target1");
        activateComponent(new ReturnFaultComponent(), "target2");
        activateComponent(new ReturnOutComponent(), "target3");

        InOut me = client.createInOutExchange();
        me.setService(new QName("routingSlip"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.done(me);
        
        listener.assertExchangeCompleted();
    }
    
    public void testErrorOnSecond() throws Exception {
        activateComponent(new ReturnOutComponent(), "target1");
        activateComponent(new ReturnErrorComponent(), "target2");
        activateComponent(new ReturnOutComponent(), "target3");

        InOut me = client.createInOutExchange();
        me.setService(new QName("routingSlip"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        listener.assertExchangeCompleted();
    }
    
    public void testFaultOnThird() throws Exception {
        activateComponent(new ReturnOutComponent(), "target1");
        activateComponent(new ReturnOutComponent(), "target2");
        activateComponent(new ReturnFaultComponent(), "target3");

        InOut me = client.createInOutExchange();
        me.setService(new QName("routingSlip"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        client.done(me);
        
        listener.assertExchangeCompleted();
    }
    
    public void testErrorOnThird() throws Exception {
        activateComponent(new ReturnOutComponent(), "target1");
        activateComponent(new ReturnOutComponent(), "target2");
        activateComponent(new ReturnErrorComponent(), "target3");

        InOut me = client.createInOutExchange();
        me.setService(new QName("routingSlip"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        
        listener.assertExchangeCompleted();
    }
    

}
