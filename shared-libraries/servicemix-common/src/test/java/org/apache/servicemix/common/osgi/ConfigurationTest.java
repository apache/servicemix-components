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

import junit.framework.TestCase;
import org.apache.servicemix.executors.impl.ExecutorConfig;

/**
 * Test cases for {@link org.apache.servicemix.common.osgi.Configuration}
 */
public class ConfigurationTest extends TestCase {

    public void testSingleton() {
        assertSame("Same instance should be returned",
                   Configuration.getInstance(), Configuration.getInstance());
    }

    public void testThreadPoolConfiguration() {
        Configuration config = Configuration.getInstance();
        config.setCorePoolSize(10);
        config.setMaximumPoolSize(50);
        config.setQueueSize(100);
        config.setAllowCoreThreadTimeout(false);
        config.setKeepAliveTime(30000);

        ExecutorConfig executorConfig = config.getExecutorConfig();
        assertNotNull(executorConfig);
        assertEquals(new Integer(10), executorConfig.getCorePoolSize());
        assertEquals(new Integer(50), executorConfig.getMaximumPoolSize());
        assertEquals(new Integer(100), executorConfig.getQueueSize());
        assertFalse(executorConfig.isAllowCoreThreadsTimeout());
        assertEquals(new Long(30000), executorConfig.getKeepAliveTime());
    }

}
