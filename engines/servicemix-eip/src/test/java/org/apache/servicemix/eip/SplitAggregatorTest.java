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

import java.util.concurrent.atomic.AtomicReference;

import javax.jbi.JBIException;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.ExchangeStatus;
import javax.xml.namespace.QName;

import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.eip.patterns.SplitAggregator;
import org.apache.servicemix.eip.support.AbstractSplitter;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.tck.ReceiverComponent;

public class SplitAggregatorTest extends AbstractEIPTest {

    private SplitAggregator aggregator;

    protected void setUp() throws Exception {
        super.setUp();

        aggregator = new SplitAggregator();
        aggregator.setTarget(createServiceExchangeTarget(new QName("target")));
        aggregator.setCopyProperties(true);
        configurePattern(aggregator);
        activateComponent(aggregator, "aggregator");
    }
    
    protected NormalizedMessage testRun(boolean[] msgs, boolean reportTimeoutAsErrors) throws Exception {
        ReceiverComponent rec = activateReceiver("target");
        
        int nbMessages = 3;
        int nbMessagesSent = 0;
        String corrId = Long.toString(System.currentTimeMillis());
        for (int i = 0; i < 3; i++) {
            if (msgs == null || msgs[i]) {
                InOnly me = client.createInOnlyExchange();
                me.setService(new QName("aggregator"));
                me.getInMessage().setContent(createSource("<hello id='" + i + "' />"));
                me.getInMessage().setProperty(AbstractSplitter.SPLITTER_COUNT, new Integer(nbMessages));
                me.getInMessage().setProperty(AbstractSplitter.SPLITTER_INDEX, new Integer(i));
                me.getInMessage().setProperty(AbstractSplitter.SPLITTER_CORRID, corrId);
                me.getInMessage().setProperty("prop", "value");
                client.send(me);
                nbMessagesSent++;
            }
        }        
        
        NormalizedMessage msg = null;
        if (reportTimeoutAsErrors && (nbMessages != nbMessagesSent)) {
            for (int i = 0; i < nbMessagesSent; i++) {
                MessageExchange me = (InOnly)client.receive();
                assertEquals(ExchangeStatus.ERROR, me.getStatus());
            }
        } else {
            rec.getMessageList().assertMessagesReceived(1);
            msg = (NormalizedMessage) rec.getMessageList().flushMessages().get(0);
            int nbElements = new SourceTransformer().toDOMElement(msg).getChildNodes().getLength();
            assertEquals(nbMessagesSent, nbElements);
            assertEquals("value", msg.getProperty("prop"));
        }
        return msg;
    }

    public void testWithoutReportErrorsAndWithReportTimeoutAsErrors() {
        SplitAggregator aggr = new SplitAggregator();
        aggr.setTarget(createServiceExchangeTarget(new QName("target")));
        aggr.setReportErrors(false);
        aggr.setReportTimeoutAsErrors(true);
        configurePattern(aggr);
        try {
        	activateComponent(aggr, "aggr");
			fail("An IllegalArgumentException should have been thrown!");
		} catch (Exception e) {
			assertTrue(e instanceof JBIException);
			assertTrue(e.getCause() instanceof IllegalArgumentException);
		}
    }
    
    public void testSimple() throws Exception {
    	aggregator.setTimeout(5000);
        testRun(null, false);
    }
    
    public void testSimpleWithQNames() throws Exception {
        aggregator.setAggregateElementName(new QName("uri:test", "agg", "sm"));
        aggregator.setMessageElementName(new QName("uri:test", "msg", "sm"));
        testRun(null, false);
    }
    
    public void testWithTimeout() throws Exception {
        aggregator.setTimeout(500);
        testRun(new boolean[] {true, false, true }, false);
    }
    
    public void testWithTimeoutReportedAsErrors() throws Exception {
        aggregator.setTimeout(500);
        aggregator.setReportErrors(true);
        aggregator.setReportTimeoutAsErrors(true);
        testRun(new boolean[] {true, false, true }, true);
    }
    
    public void testProcessCorrelationIdPropagationWithTimeout() throws Exception {
        aggregator.setTimeout(500);
        aggregator.setReportTimeoutAsErrors(false);

        final AtomicReference<String> receivedCorrId = new AtomicReference<String>();

        final String processCorrId = Long.toString(System.currentTimeMillis());
        ReceiverComponent rec = new ReceiverComponent() {
        	@Override
            public void onMessageExchange(MessageExchange exchange) throws MessagingException {
                String corrId = (String) exchange.getProperty(JbiConstants.CORRELATION_ID);
                receivedCorrId.set(corrId);
                super.onMessageExchange(exchange);
            }
		};
        activateComponent(rec, "target");

        String corrId = Long.toString(System.currentTimeMillis());
        InOnly me = client.createInOnlyExchange();
        me.setProperty(JbiConstants.CORRELATION_ID, processCorrId);
        me.setService(new QName("aggregator"));
        me.getInMessage().setContent(createSource("<hello id='" + 0 + "' />"));
        me.getInMessage().setProperty(AbstractSplitter.SPLITTER_COUNT, new Integer(2));
        me.getInMessage().setProperty(AbstractSplitter.SPLITTER_INDEX, new Integer(0));
        me.getInMessage().setProperty(AbstractSplitter.SPLITTER_CORRID, corrId);
        client.send(me);

        rec.getMessageList().waitForMessagesToArrive(1);
        rec.getMessageList().flushMessages();
        assertEquals(processCorrId, receivedCorrId.get());

        me = client.createInOnlyExchange();
        me.setProperty(JbiConstants.CORRELATION_ID, processCorrId);
        me.setService(new QName("aggregator"));
        me.getInMessage().setContent(createSource("<hello id='" + 0 + "' />"));
        me.getInMessage().setProperty(AbstractSplitter.SPLITTER_COUNT, new Integer(2));
        me.getInMessage().setProperty(AbstractSplitter.SPLITTER_INDEX, new Integer(1));
        me.getInMessage().setProperty(AbstractSplitter.SPLITTER_CORRID, corrId);
        client.sendSync(me);

        assertEquals(ExchangeStatus.DONE, me.getStatus());

        Thread.sleep(500);
        rec.getMessageList().assertMessagesReceived(0);
    }
    
    // Test aggregator when InOut MEP is used.  An exception should be thrown.
    public void testInvalidInOutMep() throws Exception {
    	
        int nbMessages = 3;
        int nbMessagesSent = 0;
        String corrId = Long.toString(System.currentTimeMillis());
        try {
            for (int i = 0; i < 3; i++) {
                InOut me = client.createInOutExchange();
                me.setService(new QName("aggregator"));
                me.getInMessage().setContent(createSource("<hello id='" + i + "' />"));
                me.getInMessage().setProperty(AbstractSplitter.SPLITTER_COUNT, new Integer(nbMessages));
                me.getInMessage().setProperty(AbstractSplitter.SPLITTER_INDEX, new Integer(i));
                me.getInMessage().setProperty(AbstractSplitter.SPLITTER_CORRID, corrId);
                me.getInMessage().setProperty("prop", "value");
                client.send(me);
                nbMessagesSent++;
            }
        } catch (UnsupportedOperationException uoe) {
        	// test succeeds
        }
    }
    
    // Test aggregator when no correlation id is set on the message.
    public void testNullCorrelationId() throws Exception {
    	int nbMessages = 3;
        int nbMessagesSent = 0;
        try {
            for (int i = 0; i < 3; i++) {
                InOut me = client.createInOutExchange();
                me.setService(new QName("aggregator"));
                me.getInMessage().setContent(createSource("<hello id='" + i + "' />"));
                me.getInMessage().setProperty(AbstractSplitter.SPLITTER_COUNT, new Integer(nbMessages));
                me.getInMessage().setProperty(AbstractSplitter.SPLITTER_INDEX, new Integer(i));
                me.getInMessage().setProperty("prop", "value");
                client.send(me);
                nbMessagesSent++;
            }
        } catch (IllegalArgumentException iae) {
        	// test succeeds
        }
    }
}
