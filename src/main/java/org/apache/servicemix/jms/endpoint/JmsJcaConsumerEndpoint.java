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
package org.apache.servicemix.jms.endpoint;

import java.util.Timer;

import javax.jbi.management.DeploymentException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.resource.spi.ActivationSpec;
import javax.resource.spi.BootstrapContext;
import javax.resource.spi.ResourceAdapter;
import javax.resource.spi.UnavailableException;
import javax.resource.spi.XATerminator;
import javax.resource.spi.endpoint.MessageEndpointFactory;
import javax.resource.spi.work.WorkManager;
import javax.transaction.TransactionManager;

import org.apache.servicemix.executors.Executor;
import org.apache.servicemix.executors.WorkManagerWrapper;
import org.apache.servicemix.jms.JmsEndpointType;
import org.jencks.SingletonEndpointFactory;
import org.springframework.jms.listener.adapter.ListenerExecutionFailedException;

/**
 * 
 * @author gnodet
 * @org.apache.xbean.XBean element="jca-consumer"
 */
public class JmsJcaConsumerEndpoint extends AbstractConsumerEndpoint implements JmsEndpointType {

    private ResourceAdapter resourceAdapter;
    private ActivationSpec activationSpec;
    private BootstrapContext bootstrapContext;
    private MessageEndpointFactory endpointFactory;
    
    /**
     * @return the bootstrapContext
     */
    public BootstrapContext getBootstrapContext() {
        return bootstrapContext;
    }

    /**
     * @param bootstrapContext the bootstrapContext to set
     */
    public void setBootstrapContext(BootstrapContext bootstrapContext) {
        this.bootstrapContext = bootstrapContext;
    }

    /**
     * @return the activationSpec
     */
    public ActivationSpec getActivationSpec() {
        return activationSpec;
    }

    /**
     * @param activationSpec the activationSpec to set
     */
    public void setActivationSpec(ActivationSpec activationSpec) {
        this.activationSpec = activationSpec;
    }

    /**
     * @return the resourceAdapter
     */
    public ResourceAdapter getResourceAdapter() {
        return resourceAdapter;
    }

    /**
     * @param resourceAdapter the resourceAdapter to set
     */
    public void setResourceAdapter(ResourceAdapter resourceAdapter) {
        this.resourceAdapter = resourceAdapter;
    }

    public String getLocationURI() {
        // TODO: Need to return a real URI
        return getService() + "#" + getEndpoint();
    }

    public synchronized void start() throws Exception {
        if (bootstrapContext == null) {
            Executor executor = getServiceUnit().getComponent().getExecutor();
            WorkManager wm = new WorkManagerWrapper(executor); 
            bootstrapContext = new SimpleBootstrapContext(wm);
        }
        resourceAdapter.start(bootstrapContext);
        activationSpec.setResourceAdapter(resourceAdapter);
        if (endpointFactory == null) {
            TransactionManager tm = (TransactionManager) getContext().getTransactionManager();
            endpointFactory = new SingletonEndpointFactory(new MessageListener() {
                public void onMessage(Message message) {
                    try {
                        JmsJcaConsumerEndpoint.this.onMessage(message, null);
                    } catch (JMSException e) {
                        throw new ListenerExecutionFailedException("Unable to handle message", e);
                    }
                }
            }, tm);
        }
        resourceAdapter.endpointActivation(endpointFactory, activationSpec);
        super.start();
    }

    public synchronized void stop() throws Exception {
        resourceAdapter.endpointDeactivation(endpointFactory, activationSpec);
        resourceAdapter.stop();
        super.stop();
    }
    
    public void validate() throws DeploymentException {
        super.validate();
        if (resourceAdapter == null) {
            throw new DeploymentException("resourceAdapter must be set");
        }
        if (activationSpec == null) {
            throw new DeploymentException("activationSpec must be set");
        }
    }
    
    protected static class SimpleBootstrapContext implements BootstrapContext {
        private final WorkManager workManager;
        public SimpleBootstrapContext(WorkManager workManager) {
            this.workManager = workManager;
        }
        public Timer createTimer() throws UnavailableException {
            throw new UnsupportedOperationException();
        }
        public WorkManager getWorkManager() {
            return workManager;
        }
        public XATerminator getXATerminator() {
            throw new UnsupportedOperationException();
        }
    }

}
