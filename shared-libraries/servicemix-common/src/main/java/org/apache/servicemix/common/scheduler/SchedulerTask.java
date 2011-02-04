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
package org.apache.servicemix.common.scheduler;

import java.util.TimerTask;

/**
 * <p>
 * A task run by a {@link Scheduler}.
 * </p>
 * 
 * @author George Gastaldi (gastaldi)
 */
public abstract class SchedulerTask implements Runnable {

    static final int VIRGIN = 0;

    static final int SCHEDULED = 1;

    static final int CANCELLED = 2;

    final Object lock = new Object();

    int state = VIRGIN;

    TimerTask timerTask;

    protected SchedulerTask() {
    }

    public abstract void run();

    /**
     * <p>
     * Cancels task.
     * </p>
     * 
     * @return true if task already scheduled
     */
    public boolean cancel() {
        synchronized (lock) {
            if (timerTask != null) {
                timerTask.cancel();
            }
            boolean result = state == SCHEDULED;
            state = CANCELLED;
            return result;
        }
    }

    public long scheduledExecutionTime() {
        synchronized (lock) {
            return timerTask == null ? 0 : timerTask.scheduledExecutionTime();
        }
    }

}
