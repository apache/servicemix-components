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
package org.apache.servicemix.common.endpoints;

import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.jbi.JBIException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.scheduler.ScheduleIterator;
import org.apache.servicemix.common.scheduler.Scheduler;
import org.apache.servicemix.common.scheduler.SchedulerTask;
import org.apache.servicemix.executors.Executor;

/**
 * An implementation inheritance class for an endpoint which polls some resource
 * at periodic intervals to decide if there is an event to process.
 *
 * @version $Revision: 464478 $
 */
public abstract class PollingEndpoint extends ConsumerEndpoint {
    
    private Executor executor;
    private Scheduler scheduler;
    private Date firstTime;
    private long period = 5000;
    private long delay;
    private SchedulerTask schedulerTask;
    private ScheduleIterator scheduleIterator;
    private boolean started;
    private boolean scheduleExecutedFlag;
    private boolean concurrentPolling;
    private AtomicBoolean pollActive = new AtomicBoolean(false);
    
    public PollingEndpoint() {
    }

    public PollingEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    public PollingEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component.getServiceUnit(), endpoint.getServiceName(), endpoint.getEndpointName());
    }

    /**
     * Polls the underlying resource to see if some event is required
     *
     * @throws JBIException
     */
    public abstract void poll() throws Exception;


    // Properties
    // -------------------------------------------------------------------------
    public Executor getExecutor() {
        return executor;
    }


    /**
     * Sets the amount of time in milliseconds that the endpoint should wait before making the first poll.
     *
     * @param        delay   a long specifying the number of milliseconds to wait
     */
    public void setDelay(long delay) {
        this.delay = delay;
    }

    public long getDelay() {
        return delay;
    }


    /**
     * Sets the date on which the first poll will be executed. If a delay is 
     * also set using <code>setDelay</code>, the delay interval will be added 
     * after the date specified.
     *
     * @param        firstTime       a <code>Date</code> specifying when to make the 
     *                               first polling attempt
     */
    public void setFirstTime(Date firstTime) {
        this.firstTime = firstTime;
    }

    public Date getFirstTime() {
        return firstTime;
    }

    /**
     * Sets whether more than one poll can be active at a time (true means yes). Default value is <code>false</code>.
     * 
     * @param concurrentPolling The concurrentPolling to set.
     */
    public void setConcurrentPolling(boolean concurrentPolling) {
        this.concurrentPolling = concurrentPolling;
    }
   
    public boolean isConcurrentPolling() {
        return this.concurrentPolling;
    }

    
    /**
     * Sets the number of milliseconds between polling attempts.
     *
     * @param        period  a long specifying the gap between polling attempts
     */
    public void setPeriod(long period) {
        this.period = period;
    }

    public long getPeriod() {
        return period;
    }


    public Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * Set a custom Scheduler implementation if you need more fine-grained control over the polling schedule.
     *
     * @param scheduler
     */
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public synchronized void start() throws Exception {
        if (!started) {
            started = true;

            if (scheduler == null) {
                scheduler = new Scheduler(true);
            }
            if (scheduleIterator == null) {
                scheduleIterator = new PollingEndpoint.PollScheduleIterator();
            }

            if (executor == null) {
                executor = getServiceUnit().getComponent().getExecutor(MessageExchange.Role.CONSUMER);
            }
            if (schedulerTask != null) {
                schedulerTask.cancel();
            }
            schedulerTask = new PollingEndpoint.PollSchedulerTask();
            this.scheduler.schedule(schedulerTask, scheduleIterator);
        }
        super.start();
    }

    public synchronized void stop() throws Exception {
        if (schedulerTask != null) {
            schedulerTask.cancel();
            schedulerTask = null;
        }
        scheduleExecutedFlag = false;
        started = false;
        scheduler.cancel();
        scheduler = null;
        scheduleIterator = null;
        executor = null;
        super.stop();
    }

    // Implementation methods
    // -------------------------------------------------------------------------

    private class PollSchedulerTask extends SchedulerTask {
        public void run() {
            try {
                if (!isConcurrentPolling() && pollActive.get()) {
                    // do not disturb the active poll cycle
                    return;
                }
                    
                // lets run the work inside the JCA worker pools to ensure
                // the threads are setup correctly when we actually do stuff
                getExecutor().execute(new Runnable() {
                    public void run() {
                        try {
                            // set busy marker 
                            pollActive.set(true);

                            // call poll implementation
                            poll();
                            
                            // release busy marker
                            pollActive.set(false);
                        }
                        catch (Exception e) {
                            handlePollException(e);
                            pollActive.set(false);
                        }
                    }
                });
            }
            catch (Throwable e) {
                logger.error("Failed to schedule work: " + e, e);
                pollActive.set(false);
            }
        }
    }

    protected void handlePollException(Exception e) {
        logger.error("Caught exception while polling: " + e, e);
    }


    private class PollScheduleIterator implements ScheduleIterator {
        public Date nextExecution() {
            long nextTime = System.currentTimeMillis();
            if (scheduleExecutedFlag) {
                nextTime += period;
            }
            else {
                if (firstTime != null) {
                    nextTime = firstTime.getTime();
                }
                nextTime += delay;
                scheduleExecutedFlag = true;
            }
            return (started) ? new Date(nextTime) : null;
        }
    }
}
