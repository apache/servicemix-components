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

import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.apache.servicemix.eip.patterns.SplitAggregator;
import org.apache.servicemix.eip.support.AbstractSplitter;
import org.apache.servicemix.tck.ReceiverComponent;

public class SplitAggregatorTest extends AbstractEIPTest {

    private SplitAggregator aggregator;

    protected void setUp() throws Exception {
        super.setUp();

        aggregator = new SplitAggregator();
        aggregator.setTarget(createServiceExchangeTarget(new QName("target")));
        configurePattern(aggregator);
        activateComponent(aggregator, "aggregator");
    }
    
    protected NormalizedMessage testRun(boolean[] msgs) throws Exception {
        ReceiverComponent rec = activateReceiver("target");
        
        int nbMessages = 3;
        String corrId = Long.toString(System.currentTimeMillis());
        for (int i = 0; i < 3; i++) {
            if (msgs == null || msgs[i]) {
                InOnly me = client.createInOnlyExchange();
                me.setService(new QName("aggregator"));
                me.getInMessage().setContent(createSource("<hello id='" + i + "' />"));
                me.getInMessage().setProperty(AbstractSplitter.SPLITTER_COUNT, new Integer(nbMessages));
                me.getInMessage().setProperty(AbstractSplitter.SPLITTER_INDEX, new Integer(i));
                me.getInMessage().setProperty(AbstractSplitter.SPLITTER_CORRID, corrId);
                client.send(me);
            }
        }        
        
        rec.getMessageList().assertMessagesReceived(1);
        return (NormalizedMessage) rec.getMessageList().flushMessages().get(0);
    }
    
    public void testSimple() throws Exception {
        aggregator.setTimeout(500);
        testRun(null);
    }
    
    public void testSimpleWithQNames() throws Exception {
        aggregator.setAggregateElementName(new QName("uri:test", "agg", "sm"));
        aggregator.setMessageElementName(new QName("uri:test", "msg", "sm"));
        testRun(null);
    }
    
    public void testWithTimeout() throws Exception {
        aggregator.setTimeout(500);
        testRun(new boolean[] { true, false, true });
    }
}
