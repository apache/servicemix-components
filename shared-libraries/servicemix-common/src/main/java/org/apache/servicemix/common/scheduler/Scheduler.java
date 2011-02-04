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

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * <p>
 * Class to handle scheduling tasks.
 * This class is thread-safe.
 * </p>
 * 
 * @author George Gastaldi (gastaldi)
 */
public class Scheduler {

    private Timer timer;

    /**
     * <p>
     * Creates a new Scheduler.
     * </p>
     */
    public Scheduler() {
        this.timer = new Timer();
    }

    /**
     * <p>
     * Creates a new Daemon Scheduler
     * </p>
     * 
     * @param daemon Thread must be executed as "daemon".
     */
    public Scheduler(boolean daemon) {
        this.timer = new Timer(daemon);
    }

    /**
     * <p>
     * Cancels the scheduler task.
     * </p>
     */
    public void cancel() {
        timer.cancel();
    }

    /**
     * <p>
     * Schedules a task.
     * </p>
     * 
     * @param task scheduled task
     * @param iterator
     * @throws IllegalStateException if task scheduled or canceled.
     */
    public void schedule(SchedulerTask task, ScheduleIterator iterator) {
        Date time = iterator.nextExecution();
        if (time == null) {
            task.cancel();
        } else {
            synchronized (task.lock) {
                if (task.state != SchedulerTask.VIRGIN) {
                    throw new IllegalStateException("Task already scheduled or cancelled");
                }
                task.state = SchedulerTask.SCHEDULED;
                task.timerTask = new SchedulerTimerTask(task, iterator);
                timer.schedule(task.timerTask, time);
            }
        }
    }

    private void reschedule(SchedulerTask task, ScheduleIterator iterator) {
        Date time = iterator.nextExecution();
        if (time == null) {
            task.cancel();
        } else {
            synchronized (task.lock) {
                if (task.state != SchedulerTask.CANCELLED) {
                    task.timerTask = new SchedulerTimerTask(task, iterator);
                    timer.schedule(task.timerTask, time);
                }
            }
        }
    }

    /**
     * <p>
     * Internal TimerTask instance.
     * </p>
     */
    class SchedulerTimerTask extends TimerTask {
        private SchedulerTask task;

        private ScheduleIterator iterator;

        public SchedulerTimerTask(SchedulerTask task, ScheduleIterator iterator) {
            this.task = task;
            this.iterator = iterator;
        }

        public void run() {
            task.run();
            reschedule(task, iterator);
        }
    }

}
