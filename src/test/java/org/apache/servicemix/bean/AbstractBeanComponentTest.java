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
package org.apache.servicemix.bean;

import java.lang.reflect.Field;
import java.util.Map;

import junit.framework.TestCase;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.tck.ExchangeCompletedListener;

public abstract class AbstractBeanComponentTest extends TestCase {
    
    protected DefaultServiceMixClient client;
    protected JBIContainer container;
    protected ExchangeCompletedListener listener;
    protected BeanComponent component;

    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setEmbedded(true);
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        configureContainer();
        listener = new ExchangeCompletedListener();
        container.addListener(listener);
        
        container.init();
        container.start();

        component = new BeanComponent();
        container.activateComponent(component, "servicemix-bean");
        
        client = new DefaultServiceMixClient(container);
    }

    protected void tearDown() throws Exception {
        listener.assertExchangeCompleted();
        container.shutDown();
    }

    protected abstract void configureContainer();
    
    @SuppressWarnings("unchecked")
    protected void assertBeanEndpointRequestsMapEmpty(BeanEndpoint beanEndpoint) throws Exception {
        Field requestsMapField = BeanEndpoint.class.getDeclaredField("requests");
        requestsMapField.setAccessible(true);
        Map requestsMap = (Map) requestsMapField.get(beanEndpoint);
        if (requestsMap.size() > 0) {
            Thread.sleep(1000);
        }
        assertEquals("There should be no more pending requests on " + beanEndpoint, 0, requestsMap.size());
    }
}
