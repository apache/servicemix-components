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
package org.apache.servicemix.eip.support.resequence;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.BlockingQueue;

import javax.jbi.messaging.MessageExchange;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.executors.Executor;

/**
 * @author Martin Krasser
 */
public class SequenceReader implements Runnable {

    private static final Log LOG = LogFactory.getLog(SequenceReader.class);
    
    private static final MessageExchange STOP = createStopSignal();
    
    private BlockingQueue<MessageExchange> queue;
    
    private SequenceSender sender;
    
    public SequenceReader(SequenceSender sender) {
        this.sender = sender;
    }
    
    public void setQueue(BlockingQueue<MessageExchange> queue) {
        this.queue = queue;
    }

    public void run() {
        while (true) {
            try {
                // block until message exchange is available
                MessageExchange me = queue.take();
                if (me == STOP) {
                    LOG.info("exit processing loop after cancellation");
                    return;
                }
                // send sync to preserve message order
                sender.sendSync(me);
            } catch (InterruptedException e) {
                LOG.info("exit processing loop after interrupt");
                return;
            } catch (Exception e) {
                // TODO: handle sendSync errors and faults
                LOG.error("caught and ignored exception", e);
            }
        }
    }
    
    public void start(Executor executor) {
        executor.execute(this);
    }
    
    public void stop() throws InterruptedException {
        queue.put(STOP);
    }
    
    private static MessageExchange createStopSignal() {
        return (MessageExchange)Proxy.newProxyInstance(SequenceReader.class.getClassLoader(), 
                new Class[] {MessageExchange.class}, createStopHandler());
    }
    
    private static InvocationHandler createStopHandler() {
        return new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                throw new IllegalStateException("illegal method invocation on stop signal");
            }
        };
    }
    
}
