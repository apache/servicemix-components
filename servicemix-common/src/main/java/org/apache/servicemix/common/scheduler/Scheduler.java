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
 * Class to handle scheduling tasks.
 * <p>
 * This class is thread-safe
 * <p>
 * 
 * @author George Gastaldi (gastaldi)
 */
public class Scheduler {

    private Timer timer;

    /**
     * Creates a new Scheduler.
     */
    public Scheduler() {
        this.timer = new Timer();
    }

    /**
     * Creates a new Daemon Scheduler
     * 
     * @param daemon
     *            Thread must be executed as "daemon".
     */
    public Scheduler(boolean daemon) {
        this.timer = new Timer(daemon);
    }

    /**
     * Cancels the scheduler task
     */
    public void cancel() {
        timer.cancel();
    }

    /**
     * Schedules a task
     * 
     * @param task
     *            scheduled tasl
     * @param iterator
     *            iterator for schedulingque descreve o agendamento
     * @throws IllegalStateException
     *             if task scheduled or canceled
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
     * Internal TimerTask instance
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
