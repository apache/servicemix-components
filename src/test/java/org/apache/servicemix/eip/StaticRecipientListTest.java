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

import org.apache.servicemix.eip.patterns.StaticRecipientList;
import org.apache.servicemix.eip.support.ExchangeTarget;
import org.apache.servicemix.tck.ReceiverComponent;

public class StaticRecipientListTest extends AbstractEIPTest {

    protected StaticRecipientList recipientList;
    
    protected void setUp() throws Exception {
        super.setUp();

        recipientList = new StaticRecipientList();
        recipientList.setRecipients(
                new ExchangeTarget[] {
                        createServiceExchangeTarget(new QName("recipient1")),
                        createServiceExchangeTarget(new QName("recipient2")),
                        createServiceExchangeTarget(new QName("recipient3"))
                });
        configurePattern(recipientList);
        activateComponent(recipientList, "recipientList");
    }

    public void testInOut() throws Exception {
        InOut me = client.createInOutExchange();
        me.setService(new QName("recipientList"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
    }
    
    public void testInOptionalOut() throws Exception {
        InOptionalOut me = client.createInOptionalOutExchange();
        me.setService(new QName("recipientList"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
    }
    
    public void testInOnly() throws Exception {
        ReceiverComponent r1 = activateReceiver("recipient1");
        ReceiverComponent r2 = activateReceiver("recipient2");
        ReceiverComponent r3 = activateReceiver("recipient3");
        
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("recipientList"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        r1.getMessageList().assertMessagesReceived(1);
        r2.getMessageList().assertMessagesReceived(1);
        r3.getMessageList().assertMessagesReceived(1);
    }
    
    public void testRobustInOnly() throws Exception {
        ReceiverComponent r1 = activateReceiver("recipient1");
        ReceiverComponent r2 = activateReceiver("recipient2");
        ReceiverComponent r3 = activateReceiver("recipient3");
        
        RobustInOnly me = client.createRobustInOnlyExchange();
        me.setService(new QName("recipientList"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        r1.getMessageList().assertMessagesReceived(1);
        r2.getMessageList().assertMessagesReceived(1);
        r3.getMessageList().assertMessagesReceived(1);
    }
    
}
