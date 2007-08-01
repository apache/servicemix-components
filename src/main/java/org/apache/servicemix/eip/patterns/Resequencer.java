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
package org.apache.servicemix.eip.patterns;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.eip.support.resequence.DefaultComparator;
import org.apache.servicemix.eip.support.resequence.ResequencerBase;
import org.apache.servicemix.eip.support.resequence.ResequencerEngine;
import org.apache.servicemix.eip.support.resequence.SequenceElementComparator;
import org.apache.servicemix.eip.support.resequence.SequenceReader;
import org.apache.servicemix.eip.support.resequence.SequenceSender;
import org.apache.servicemix.executors.Executor;

/**
 * @author Martin Krasser
 * 
 * @org.apache.xbean.XBean element="resequencer"
 */
public class Resequencer extends ResequencerBase implements SequenceSender {

    private ResequencerEngine<MessageExchange> reseq;
    
    private SequenceReader reader;

    private Executor executor;

    private int capacity;
    
    private long timeout;
    
    private SequenceElementComparator<MessageExchange> comparator;
    
    public Resequencer() {
        this.reader = new SequenceReader(this);
        this.comparator = new DefaultComparator();
    }
    
    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
    
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public void setComparator(SequenceElementComparator<MessageExchange> comparator) {
        this.comparator = comparator;
    }
    
    @Override
    public void start() throws Exception {
        super.start();
        if (executor == null) {
            executor = getServiceUnit().getComponent().getExecutor();
        }
        BlockingQueue<MessageExchange> queue = new LinkedBlockingQueue<MessageExchange>();
        reseq = new ResequencerEngine<MessageExchange>(comparator, capacity);
        reseq.setTimeout(timeout);
        reseq.setOutQueue(queue);
        reader.setQueue(queue);
        reader.start(executor);
    }

    @Override
    public void stop() throws Exception {
        reseq.stop();
        reader.stop();
        super.stop();
    }
    
    public void sendSync(MessageExchange exchange) throws MessagingException {
        super.sendSync(exchange);
    }
    
    public void sendSync(List<MessageExchange> exchanges) throws MessagingException {
        for (MessageExchange exchange : exchanges) {
            sendSync(exchange);
        }
    }

    @Override
    protected void processSync(MessageExchange exchange) throws Exception {
        fail(exchange, new UnsupportedOperationException("synchronous resequencing not supported"));
    }

    @Override
    protected void processAsync(MessageExchange exchange) throws Exception {
        validateMessageExchange(exchange);
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            return;
        } else if (exchange.getFault() != null) {
            done(exchange);
            return;
        }
        processMessage(exchange);
        done(exchange);
    }

    private void processMessage(MessageExchange sourceExchange) throws MessagingException, InterruptedException {
        NormalizedMessage source = sourceExchange.getMessage("in");
        NormalizedMessage copy = getMessageCopier().copy(source);
        MessageExchange targetExchange = createTargetExchange(copy, sourceExchange.getPattern());
        // add target exchange to resequencer (blocking if capacity is reached)
        reseq.put(targetExchange);
    }
    
}
