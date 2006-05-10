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

import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.apache.activemq.util.IdGenerator;
import org.apache.servicemix.eip.patterns.SplitAggregator;
import org.apache.servicemix.eip.support.AbstractSplitter;
import org.apache.servicemix.store.memory.MemoryStore;
import org.apache.servicemix.tck.ReceiverComponent;

public class SplitAggregatorTest extends AbstractEIPTest {

    private SplitAggregator aggregator;

    protected void setUp() throws Exception {
        super.setUp();

        aggregator = new SplitAggregator();
        aggregator.setTarget(createServiceExchangeTarget(new QName("target")));
        configureAggregator();
        activateComponent(aggregator, "aggregator");
    }
    
    protected void configureAggregator() throws Exception {
        aggregator.setStore(new MemoryStore(new IdGenerator()) {
            public void store(String id, Object exchange) throws IOException {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                new ObjectOutputStream(baos).writeObject(exchange);
                super.store(id, exchange);
            }
        });
    }
    
    protected NormalizedMessage testRun(boolean[] msgs) throws Exception {
        ReceiverComponent rec = activateReceiver("target");
        
        int nbMessages = 3;
        for (int i = 0; i < 3; i++) {
            if (msgs == null || msgs[i]) {
                InOnly me = client.createInOnlyExchange();
                me.setService(new QName("aggregator"));
                me.getInMessage().setContent(createSource("<hello id='" + i + "' />"));
                me.getInMessage().setProperty(AbstractSplitter.SPLITTER_COUNT, new Integer(nbMessages));
                me.getInMessage().setProperty(AbstractSplitter.SPLITTER_INDEX, new Integer(i));
                me.getInMessage().setProperty(AbstractSplitter.SPLITTER_CORRID, "corrId");
                client.send(me);
            }
        }        
        
        rec.getMessageList().assertMessagesReceived(1);
        return (NormalizedMessage) rec.getMessageList().flushMessages().get(0);
    }
    
    public void testSimple() throws Exception {
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
