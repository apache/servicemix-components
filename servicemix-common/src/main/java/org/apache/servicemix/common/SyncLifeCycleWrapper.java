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
package org.apache.servicemix.common;

import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.component.ComponentContext;
import javax.jbi.JBIException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.management.ObjectName;

import org.apache.servicemix.MessageExchangeListener;

/**
 * Wrap an AsyncBaseLifeCycle into a lifecycle implementing MessageExchangeListener
 */
public class SyncLifeCycleWrapper implements ComponentLifeCycle, MessageExchangeListener {

    private AsyncBaseLifeCycle lifeCycle;

    public SyncLifeCycleWrapper(AsyncBaseLifeCycle lifeCycle) {
        this.lifeCycle = lifeCycle;
    }

    public ObjectName getExtensionMBeanName() {
        return lifeCycle.getExtensionMBeanName();
    }

    public void init(ComponentContext componentContext) throws JBIException {
        lifeCycle.init(componentContext);
    }

    public void shutDown() throws JBIException {
        lifeCycle.shutDown();
    }

    public void start() throws JBIException {
        lifeCycle.start();
    }

    public void stop() throws JBIException {
        lifeCycle.stop();
    }

    public void onMessageExchange(MessageExchange messageExchange) throws MessagingException {
        lifeCycle.onMessageExchange(messageExchange);
    }
}
