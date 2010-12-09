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
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.MessageList;
import org.apache.servicemix.tck.ReceiverComponent;

public class ResequencerTxTest extends AbstractEIPTransactionalTest {

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
        resequencer.setTimeout(1500L);
        configurePattern(resequencer);
        activateComponent(resequencer, RESEQUENCER_NAME);
    }

    public void testAsyncTx() throws Exception {
        int numMessages = 5;
        ReceiverComponent receiver = activateReceiver(TARGET_NAME);
        tm.begin();
        client.send(createTestMessageExchange(4));
        client.send(createTestMessageExchange(1));
        client.send(createTestMessageExchange(3));
        client.send(createTestMessageExchange(5));
        client.send(createTestMessageExchange(2));
        tm.commit();
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
        me.getInMessage().setContent(new StringSource("<number>" + num + "</number>"));
        return me;
    }
    
    private static void assertSequenceProperties(NormalizedMessage m, long num) throws Exception {
        Long l = (Long)m.getProperty(SEQNUM_KEY);
        if (l == null) {
            // get sequence number from message content
            long n = getNumber(new SourceTransformer().toString(m.getContent()));
            assertEquals("wrong sequence number", num, n);
            // TODO: investigate JDK 1.6.0_01 issues here
            // When using JDK 1.6.0_01 then messages transported to receiver
            // conponent don't have any properties set. This is only the case
            // if this test is running with all other EIP tests. When running
            // alone, messages contain properties as expected. When using  
            // JDK 1.5.0_12 messages always contain properties as expected.
        } else {
            // get sequence number from message properties
            assertEquals("wrong sequence number", num, l.longValue());
        }
    }
    
    private static long getNumber(String content) {
        int idx1 = content.indexOf("<number>") + 8;
        int idx2 = content.indexOf("</number>");
        return Long.parseLong(content.substring(idx1, idx2));
    }

}
