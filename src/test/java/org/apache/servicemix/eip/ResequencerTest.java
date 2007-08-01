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
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.apache.servicemix.eip.patterns.Resequencer;
import org.apache.servicemix.eip.support.resequence.DefaultComparator;
import org.apache.servicemix.tck.MessageList;
import org.apache.servicemix.tck.ReceiverComponent;

public class ResequencerTest extends AbstractEIPTest {

    private static final String RESEQUENCER_NAME = "resequencer";
    private static final String TARGET_NAME = "target";
    private static final String SEQNUM_KEY = "seqnum";
    
    private Resequencer resequencer;
    
    public void setUp() throws Exception {
        super.setUp();
        DefaultComparator comparator = new DefaultComparator();
        comparator.setSequenceNumberAsString(false);
        comparator.setSequenceNumberKey(SEQNUM_KEY);
        resequencer = new Resequencer();
        resequencer.setTarget(createServiceExchangeTarget(new QName(TARGET_NAME)));
        resequencer.setComparator(comparator);
        resequencer.setCapacity(100);
        resequencer.setTimeout(500L);
        configurePattern(resequencer);
        activateComponent(resequencer, RESEQUENCER_NAME);
    }

    @SuppressWarnings("unchecked")
    public void testAsync() throws Exception {
        int numMessages = 5;
        ReceiverComponent receiver = activateReceiver(TARGET_NAME);
        client.send(createTestMessageExchange(4));
        client.send(createTestMessageExchange(1));
        client.send(createTestMessageExchange(3));
        client.send(createTestMessageExchange(5));
        client.send(createTestMessageExchange(2));
        MessageList ml = receiver.getMessageList();
        ml.waitForMessagesToArrive(numMessages);
        assertEquals("wrong number of messages", numMessages, ml.getMessageCount());
        for (int i = 0; i < numMessages; i++) {
            assertSequenceProperties((NormalizedMessage)ml.getMessages().get(i), i + 1);
        }
        for (int i = 0; i < numMessages; i++) {
            MessageExchange me = (InOnly)client.receive();
            assertEquals(ExchangeStatus.DONE, me.getStatus());
        }
    }
    
    private MessageExchange createTestMessageExchange(long num) throws Exception {
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName(RESEQUENCER_NAME));
        me.getInMessage().setProperty(SEQNUM_KEY, new Long(num));
        return me;
    }
    
    private static void assertSequenceProperties(NormalizedMessage m, long num) {
        Long l = (Long)m.getProperty(SEQNUM_KEY);
        assertEquals("wrong sequence number", num, l.longValue());
    }
    
}
