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
import javax.transaction.Status;
import javax.xml.namespace.QName;

import org.apache.servicemix.eip.patterns.StaticRoutingSlip;
import org.apache.servicemix.eip.support.ExchangeTarget;


public class StaticRoutingSlipTxTest extends AbstractEIPTransactionalTest {

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

    public void testSync() throws Exception {
        activateComponent(new ReturnOutComponent(), "target1");
        activateComponent(new ReturnOutComponent(), "target2");
        activateComponent(new ReturnOutComponent(), "target3");

        tm.begin();
        
        InOut me = client.createInOutExchange();
        me.setService(new QName("routingSlip"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getOutMessage());
        client.done(me);
        
        tm.commit();
        
        listener.assertExchangeCompleted();
    }
    
    public void testAsync() throws Exception {
        activateComponent(new ReturnOutComponent(), "target1");
        activateComponent(new ReturnOutComponent(), "target2");
        activateComponent(new ReturnOutComponent(), "target3");

        tm.begin();
        
        InOut me = client.createInOutExchange();
        me.setService(new QName("routingSlip"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.send(me);
        
        tm.commit();

        me = (InOut) client.receive();
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertEquals(Status.STATUS_ACTIVE, tm.getStatus());
        assertNotNull(me.getOutMessage());
        client.done(me);
        
        listener.assertExchangeCompleted();
    }
    
}
