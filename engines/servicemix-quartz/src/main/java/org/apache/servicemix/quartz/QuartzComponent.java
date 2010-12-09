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

import java.util.List;

import org.apache.servicemix.common.DefaultComponent;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;
import org.quartz.impl.StdSchedulerFactory;

/**
 * @org.apache.xbean.XBean element="component"
 */
public class QuartzComponent extends DefaultComponent {

    private QuartzEndpoint[] endpoints;
    private Scheduler scheduler;
    private SchedulerFactory factory;
    
    /**
     * @return the factory
     */
    public SchedulerFactory getFactory() {
        return factory;
    }

    /**
     * @param factory the factory to set
     */
    public void setFactory(SchedulerFactory factory) {
        this.factory = factory;
    }

    /**
     * @return the endpoints
     */
    public QuartzEndpoint[] getEndpoints() {
        return endpoints;
    }

    /**
     * @param endpoints the endpoints to set
     */
    public void setEndpoints(QuartzEndpoint[] endpoints) {
        this.endpoints = endpoints;
    }

    /**
     * @return the scheduler
     */
    public Scheduler getScheduler() {
        return scheduler;
    }

    /**
     * @param scheduler the scheduler to set
     */
    public void setScheduler(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.DefaultComponent#getConfiguredEndpoints()
     */
    @Override
    protected List getConfiguredEndpoints() {
        return asList(endpoints);
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.DefaultComponent#getEndpointClasses()
     */
    @Override
    protected Class[] getEndpointClasses() {
        return new Class[] {QuartzEndpoint.class };
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.common.DefaultComponent#doInit()
     */
    @Override
    public void doInit() throws Exception {
        if (scheduler == null) {
            if (factory == null) {
                factory = new StdSchedulerFactory();
            }
            scheduler = factory.getScheduler();
        }
        scheduler.getContext().setAllowsTransientData(true);
        scheduler.getContext().put(getComponentName(), this);
        super.doInit();
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.common.DefaultComponent#doStart()
     */
    @Override
    public void doStart() throws Exception {
        scheduler.start();
        super.doStart();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.DefaultComponent#doStopt()
     */
    @Override
    public void doStop() throws Exception {
        super.doStop();
        scheduler.standby();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.DefaultComponent#doShutDown()
     */
    @Override
    public void doShutDown() throws Exception {
        super.doShutDown();
        scheduler.shutdown();
        scheduler = null;
    }

}
