/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.common.osgi;

import org.apache.servicemix.executors.impl.ExecutorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * Represents the configuration for the components that can be created from
 * information in the OSGi ConfigAdmin.
 * </p>
 */
public class Configuration {

    private final Logger logger = LoggerFactory.getLogger(Configuration.class);
    private static final Configuration INSTANCE = new Configuration();

    private final ExecutorConfig executorConfig = new ExecutorConfig();

    /*
     * Hiding the constructor to ensure singleton semantics
     */
    private Configuration() {
        super();
        logger.debug("Configuring JBI components using OSGi ConfigAdmin");
    }

    /**
     * Get the singleton configuration object instance
     */
    public static final Configuration getInstance() {
        return INSTANCE;
    }

    /**
     * Set the core thread pool size
     * @see org.apache.servicemix.executors.impl.ExecutorConfig#setCorePoolSize(int)
     */
    public void setCorePoolSize(int size) {
        logger.debug("Setting core thread pool size: " + size);
        executorConfig.setCorePoolSize(size);
    }

    /**
     * Set the maximum thread pool size
     * @see org.apache.servicemix.executors.impl.ExecutorConfig#setMaximumPoolSize(int)
     */
    public void setMaximumPoolSize(int size) {
        logger.debug("Setting maximum thread pool size: " + size);
        executorConfig.setMaximumPoolSize(size);
    }

    /**
     * Set the executor queue size
     * @see org.apache.servicemix.executors.impl.ExecutorConfig#setQueueSize(int)
     */
    public void setQueueSize(int size) {
        logger.debug("Setting executor queue size: " + size);
        executorConfig.setQueueSize(size);        
    }

    /**
     * Allow the core threads to time out
     * @see org.apache.servicemix.executors.impl.ExecutorConfig#setAllowCoreThreadsTimeout(boolean)  
     */
    public void setAllowCoreThreadTimeout(boolean timeout) {
        logger.debug("Setting core thread timeout allow: " + timeout);
        executorConfig.setAllowCoreThreadsTimeout(timeout);
    }

    /**
     * Set the keep alive time on the executor
     * @see org.apache.servicemix.executors.impl.ExecutorConfig#setKeepAliveTime(long) 
     */
    public void setKeepAliveTime(int time) {
        logger.debug("Setting thread keep-alive time: " + time);
        executorConfig.setKeepAliveTime(time);
    }

    /**
     * Get the executor configuration object
     */
    public ExecutorConfig getExecutorConfig() {
        return executorConfig;
    }
}
