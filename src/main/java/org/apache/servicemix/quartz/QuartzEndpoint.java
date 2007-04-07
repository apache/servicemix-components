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
package org.apache.servicemix.quartz;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.common.EndpointSupport;
import org.apache.servicemix.common.endpoints.ConsumerEndpoint;
import org.apache.servicemix.quartz.support.DefaultQuartzMarshaler;
import org.apache.servicemix.quartz.support.JobDetailBean;
import org.apache.servicemix.quartz.support.QuartzMarshaler;
import org.apache.servicemix.quartz.support.ServiceMixJob;
import org.quartz.Calendar;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.ObjectAlreadyExistsException;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.springframework.scheduling.quartz.JobDetailAwareTrigger;

/**
 * @org.apache.xbean.XBean element="endpoint"
 */
public class QuartzEndpoint extends ConsumerEndpoint {

    private Trigger trigger;
    private List<Trigger> triggers;
    private Map<String, Calendar> calendars;
    private JobDetail jobDetail;
    private QuartzMarshaler marshaler = new DefaultQuartzMarshaler();
    
    /**
     * @return the triggers
     */
    public List<Trigger> getTriggers() {
        return triggers;
    }

    /**
     * @param triggers the triggers to set
     */
    public void setTriggers(List<Trigger> triggers) {
        this.triggers = triggers;
    }

    /**
     * @return the trigger
     */
    public Trigger getTrigger() {
        return trigger;
    }

    /**
     * @param trigger the trigger to set
     */
    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    /**
     * @return the calendar
     */
    public Map<String, Calendar> getCalendars() {
        return calendars;
    }

    /**
     * @param calendar the calendar to set
     */
    public void setCalendars(Map<String, Calendar> calendars) {
        this.calendars = calendars;
    }

    /**
     * @return the job
     */
    public JobDetail getJobDetail() {
        return jobDetail;
    }

    /**
     * @param job the job to set
     */
    public void setJobDetail(JobDetail job) {
        this.jobDetail = job;
    }

    public QuartzMarshaler getMarshaler() {
        return marshaler;
    }

    public void setMarshaler(QuartzMarshaler marshaler) {
        this.marshaler = marshaler;
    }
    
    @Override
    public String getLocationURI() {
        return null;
    }

    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            throw new IllegalStateException("Unexpected ACTIVE exchange: " + exchange);
        }
    }

    public void onJobExecute(JobExecutionContext context) throws JobExecutionException {
        if (logger.isDebugEnabled()) {
            logger.debug("Firing Quartz Job with context: " + context);
        }
        try {
            InOnly exchange = getExchangeFactory().createInOnlyExchange();
            NormalizedMessage message = exchange.createMessage();
            getMarshaler().populateNormalizedMessage(message, context);
            exchange.setInMessage(message);
            configureExchangeTarget(exchange);
            send(exchange);
        } catch (MessagingException e) {
            throw new JobExecutionException(e);
        }
    }
    
    public void validate() throws DeploymentException {
        super.validate();
        if (trigger instanceof JobDetailAwareTrigger) {
            if (jobDetail != null) {
                throw new DeploymentException("jobDetail can not be set on endpoint and trigger at the same time");
            }
            jobDetail = ((JobDetailAwareTrigger) trigger).getJobDetail();
        }
        if (jobDetail == null) {
            JobDetailBean j = new JobDetailBean();
            j.setName(EndpointSupport.getKey(this));
            jobDetail = j;
        }
        if (triggers == null) {
            triggers = new ArrayList<Trigger>();
        }
        if (trigger != null && triggers != null && triggers.size() > 0) {
            throw new DeploymentException("trigger and triggers can not be set at the same time");
        }
        if (trigger != null) {
            triggers.add(trigger);
        }
        if (calendars == null) {
            calendars = new HashMap<String, Calendar>();
        }
        for (Trigger t : triggers) {
            if (t.getCalendarName() != null && calendars.get(t.getCalendarName()) == null) {
                throw new DeploymentException("Trigger references an unknown calendar " + t.getCalendarName());
            }
            t.setJobName(jobDetail.getName());
            t.setJobGroup(jobDetail.getGroup());
        }
    }
    
    public void start() throws Exception {
        QuartzComponent component = (QuartzComponent) getServiceUnit().getComponent(); 
        Scheduler scheduler = component.getScheduler();
        jobDetail.getJobDataMap().put(ServiceMixJob.COMPONENT_NAME, component.getComponentName());
        jobDetail.getJobDataMap().put(ServiceMixJob.ENDPOINT_NAME, EndpointSupport.getKey(this));
        for (Map.Entry<String, Calendar> e : getCalendars().entrySet()) {
            scheduler.addCalendar(e.getKey(), e.getValue(), true, true);
        }
        scheduler.addJob(getJobDetail(), true);
        for (Trigger trg : getTriggers()) {
            boolean triggerExists = scheduler.getTrigger(trg.getName(), trg.getGroup()) != null;
            if (!triggerExists) {
                try {
                    scheduler.scheduleJob(trg);
                } catch (ObjectAlreadyExistsException ex) {
                    scheduler.rescheduleJob(trg.getName(), trg.getGroup(), trg);
                }
            } else {
                scheduler.rescheduleJob(trg.getName(), trg.getGroup(), trg);
            }
        }
        super.start();
    }
    
    public void stop() throws Exception {
        super.stop();
        Scheduler scheduler = ((QuartzComponent) getServiceUnit().getComponent()).getScheduler();
        for (Trigger trg : getTriggers()) {
            scheduler.unscheduleJob(trg.getName(), trg.getGroup());
        }
        scheduler.deleteJob(getJobDetail().getName(), getJobDetail().getGroup());
        for (Map.Entry<String, Calendar> e : getCalendars().entrySet()) {
            scheduler.deleteCalendar(e.getKey());
        }
    }

}
