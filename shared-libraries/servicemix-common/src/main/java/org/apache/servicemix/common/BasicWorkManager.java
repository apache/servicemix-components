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
package org.apache.servicemix.common;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.resource.spi.work.ExecutionContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkListener;
import javax.resource.spi.work.WorkManager;

/**
 * A simple WorkManager implementation on top of java.util.concurrent thread pool.
 * 
 * @deprecated Components should use the executor on the ServiceMixComponent
 *      for thread pools
 */
public class BasicWorkManager implements WorkManager {

    private final ExecutorService executor;
    
    public BasicWorkManager() {
        executor = Executors.newCachedThreadPool();
    }
    
    public void shutDown() {
        executor.shutdown();
    }
    
    /* (non-Javadoc)
     * @see javax.resource.spi.work.WorkManager#doWork(javax.resource.spi.work.Work)
     */
    public void doWork(Work work) throws WorkException {
        work.run();
    }

    /* (non-Javadoc)
     * @see javax.resource.spi.work.WorkManager#doWork(javax.resource.spi.work.Work, long, javax.resource.spi.work.ExecutionContext, javax.resource.spi.work.WorkListener)
     */
    public void doWork(
            Work work,
            long startTimeout,
            ExecutionContext execContext,
            WorkListener workListener)
            throws WorkException {
        throw new RuntimeException("Not implemented");
    }

    /* (non-Javadoc)
     * @see javax.resource.spi.work.WorkManager#startWork(javax.resource.spi.work.Work)
     */
    public long startWork(final Work work) throws WorkException {
        final CountDownLatch latch = new CountDownLatch(1);
        executor.execute(new Work() {
            public void release() {
                work.release();
            }
            public void run() {
                latch.countDown();
                work.run();
            }
        });
        long t = System.currentTimeMillis();
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new WorkException("Interrupted", e);
        }
        return System.currentTimeMillis() - t;
    }

    /* (non-Javadoc)
     * @see javax.resource.spi.work.WorkManager#startWork(javax.resource.spi.work.Work, long, javax.resource.spi.work.ExecutionContext, javax.resource.spi.work.WorkListener)
     */
    public long startWork(
            Work work,
            long startTimeout,
            ExecutionContext execContext,
            WorkListener workListener)
            throws WorkException {
        throw new RuntimeException("Not implemented");
    }

    /* (non-Javadoc)
     * @see javax.resource.spi.work.WorkManager#scheduleWork(javax.resource.spi.work.Work)
     */
    public void scheduleWork(Work work) throws WorkException {
        executor.execute(work);
    }

    /* (non-Javadoc)
     * @see javax.resource.spi.work.WorkManager#scheduleWork(javax.resource.spi.work.Work, long, javax.resource.spi.work.ExecutionContext, javax.resource.spi.work.WorkListener)
     */
    public void scheduleWork(
            Work work,
            long startTimeout,
            ExecutionContext execContext,
            WorkListener workListener)
            throws WorkException {
        throw new RuntimeException("Not implemented");
    }

}
