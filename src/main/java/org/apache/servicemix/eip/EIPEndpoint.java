/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.eip;

import javax.jbi.component.ComponentContext;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.servicedesc.ServiceEndpoint;

import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.locks.LockManager;
import org.apache.servicemix.locks.impl.SimpleLockManager;
import org.apache.servicemix.store.Store;
import org.apache.servicemix.store.StoreFactory;
import org.apache.servicemix.store.memory.MemoryStoreFactory;
import org.apache.servicemix.timers.TimerManager;
import org.apache.servicemix.timers.impl.TimerManagerImpl;

/**
 * @author gnodet
 * @version $Revision: 376451 $
 */
public abstract class EIPEndpoint extends Endpoint implements ExchangeProcessor {

    private ServiceEndpoint activated;
    private DeliveryChannel channel;
    
    /**
     * The store to keep pending exchanges
     */
    protected Store store;
    /**
     * The store factory.
     */
    protected StoreFactory storeFactory;
    /**
     * The lock manager.
     */
    protected LockManager lockManager;
    /**
     * The timer manager.
     */
    protected TimerManager timerManager;
    /**
     * The exchange factory
     */
    protected MessageExchangeFactory exchangeFactory;
    
    /**
     * @return Returns the exchangeFactory.
     */
    public MessageExchangeFactory getExchangeFactory() {
        return exchangeFactory;
    }
    /**
     * @param exchangeFactory The exchangeFactory to set.
     */
    public void setExchangeFactory(MessageExchangeFactory exchangeFactory) {
        this.exchangeFactory = exchangeFactory;
    }
    /**
     * @return Returns the store.
     */
    public Store getStore() {
        return store;
    }
    /**
     * @param store The store to set.
     */
    public void setStore(Store store) {
        this.store = store;
    }
    /**
     * @return Returns the storeFactory.
     */
    public StoreFactory getStoreFactory() {
        return storeFactory;
    }
    /**
     * @param storeFactory The storeFactory to set.
     */
    public void setStoreFactory(StoreFactory storeFactory) {
        this.storeFactory = storeFactory;
    }
    /**
     * @return the lockManager
     */
    public LockManager getLockManager() {
        return lockManager;
    }
    /**
     * @param lockManager the lockManager to set
     */
    public void setLockManager(LockManager lockManager) {
        this.lockManager = lockManager;
    }
    /**
     * @return the timerManager
     */
    public TimerManager getTimerManager() {
        return timerManager;
    }
    /**
     * @param timerManager the timerManager to set
     */
    public void setTimerManager(TimerManager timerManager) {
        this.timerManager = timerManager;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.Endpoint#getRole()
     */
    public Role getRole() {
        return Role.PROVIDER;
    }

    public void activate() throws Exception {
        logger = this.serviceUnit.getComponent().getLogger();
        ComponentContext ctx = getContext();
        channel = ctx.getDeliveryChannel();
        exchangeFactory = channel.createExchangeFactory();
        activated = ctx.activateEndpoint(service, endpoint);
        if (store == null) {
            if (storeFactory == null) {
                storeFactory = new MemoryStoreFactory();
            }
            store = storeFactory.open(getService().toString() + getEndpoint());
        }
        if (lockManager == null) {
            lockManager = new SimpleLockManager();
        }
        if (timerManager == null) {
            timerManager = new TimerManagerImpl();
        }
        start();
    }

    public void deactivate() throws Exception {
        if (timerManager != null) {
            timerManager.stop();
        }
        stop();
        ServiceEndpoint ep = activated;
        activated = null;
        ComponentContext ctx = getServiceUnit().getComponent().getComponentContext();
        ctx.deactivateEndpoint(ep);
    }

    public ExchangeProcessor getProcessor() {
        return this;
    }
    
    public void validate() throws DeploymentException {
    }
    
    protected ComponentContext getContext() {
        return getServiceUnit().getComponent().getComponentContext();
    }
    
    protected void send(MessageExchange me) throws MessagingException {
        if (me.getRole() == MessageExchange.Role.CONSUMER &&
            me.getStatus() == ExchangeStatus.ACTIVE) {
            BaseLifeCycle lf = (BaseLifeCycle) getServiceUnit().getComponent().getLifeCycle();
            lf.sendConsumerExchange(me, (Endpoint) this);
        } else {
            channel.send(me);
        }
    }
    
    protected void done(MessageExchange me) throws MessagingException {
        me.setStatus(ExchangeStatus.DONE);
        send(me);
    }
    
    protected void fail(MessageExchange me, Exception error) throws MessagingException {
        me.setError(error);
        send(me);
    }
    
    public void start() throws Exception {
    }
    
    public void stop() {
    }

}
