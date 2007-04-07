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
package org.apache.servicemix.quartz.support;

import org.apache.servicemix.quartz.QuartzComponent;
import org.apache.servicemix.quartz.QuartzEndpoint;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;

/**
 * A Quartz Job which dispatches a message to a component.
 *
 * @version $Revision$
 */
public class ServiceMixJob implements Job {
    
    public static final String COMPONENT_NAME = "org.apache.servicemix.quartz.ComponentName";
    public static final String ENDPOINT_NAME = "org.apache.servicemix.quartz.EndpointName";
    
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            String componentName = (String) context.getJobDetail().getJobDataMap().get(COMPONENT_NAME);
            if (componentName == null) {
                throw new JobExecutionException("No property '" + COMPONENT_NAME + "' defined. Bad job data map");
            }
            QuartzComponent component = (QuartzComponent) context.getScheduler().getContext().get(componentName);
            if (component == null) {
                throw new JobExecutionException("No quartz JBI component available for key: " + componentName + "."
                        + " Bad job data map");
            }
            String endpointName = (String) context.getJobDetail().getJobDataMap().get(ENDPOINT_NAME);
            if (endpointName == null) {
                throw new JobExecutionException("No property '" + ENDPOINT_NAME + "' defined. Bad job data map");
            }
            QuartzEndpoint endpoint = (QuartzEndpoint) component.getRegistry().getEndpoint(endpointName);
            if (endpoint == null) {
                throw new JobExecutionException("No quartz JBI endpoint available for key: " + endpointName + "."
                        + " Bad job data map");
            }
            endpoint.onJobExecute(context);
        } catch (SchedulerException e) {
            throw new JobExecutionException(e);
        }
    }
    
}
