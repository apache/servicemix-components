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

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.management.LifeCycleMBean;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.SystemException;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.servicemix.executors.Executor;
import org.apache.servicemix.executors.ExecutorFactory;
import org.apache.servicemix.executors.impl.ExecutorFactoryImpl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Base class for life cycle management of components. This class may be used as
 * is.
 * 
 * @author Guillaume Nodet
 * @version $Revision: 399873 $
 * @since 3.0
 */
public class AsyncBaseLifeCycle implements ComponentLifeCycle {

    public static final String INITIALIZED = "Initialized";

    protected transient Log logger;

    protected ServiceMixComponent component;

    protected ComponentContext context;

    protected ObjectName mbeanName;

    protected ExecutorFactory executorFactory;
    
    protected Executor executor;

    protected AtomicBoolean running;

    protected DeliveryChannel channel;

    protected Thread poller;

    protected AtomicBoolean polling;

    protected TransactionManager transactionManager;

    protected boolean workManagerCreated;

    protected ThreadLocal<String> correlationId;
    
    protected String currentState = LifeCycleMBean.UNKNOWN;

    protected Container container;

    protected Map<String, Set<String>> knownExchanges;

    public AsyncBaseLifeCycle() {
        this.running = new AtomicBoolean(false);
        this.polling = new AtomicBoolean(false);
        this.correlationId = new ThreadLocal<String>();
        this.knownExchanges = new ConcurrentHashMap<String, Set<String>>();
    }

    public AsyncBaseLifeCycle(ServiceMixComponent component) {
        this();
        setComponent(component);
    }

    public Container getContainer() {
        return container;
    }

    /**
     * @org.apache.xbean.Property hidden="true"
     */
    protected void setComponent(ServiceMixComponent component) {
        this.component = component;
        this.logger = component.getLogger();
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jbi.component.ComponentLifeCycle#getExtensionMBeanName()
     */
    public ObjectName getExtensionMBeanName() {
        return mbeanName;
    }

    protected Object getExtensionMBean() throws Exception {
        return null;
    }

    protected ObjectName createExtensionMBeanName() throws Exception {
        return this.context.getMBeanNames().createCustomComponentMBeanName("Configuration");
    }

    public QName getEPRServiceName() {
        return null;
    }

    public String getCurrentState() {
        return currentState;
    }

    /**
     * @org.apache.xbean.Property hidden="true"
     */
    protected void setCurrentState(String currentState) {
        this.currentState = currentState;
    }
    
    public boolean isStarted(){
        return currentState != null && currentState.equals(LifeCycleMBean.STARTED);
    }
    
    /**
    * @return true if the object is stopped
    */
   public boolean isStopped(){
       return currentState != null && currentState.equals(LifeCycleMBean.STOPPED);
   }
   
   /**
    * @return true if the object is shutDown
    */
   public boolean isShutDown(){
       return currentState != null && currentState.equals(LifeCycleMBean.SHUTDOWN);
   }
   
   /**
    * @return true if the object is shutDown
    */
   public boolean isInitialized(){
       return currentState != null && currentState.equals(INITIALIZED);
   }
   
   /**
    * @return true if the object is shutDown
    */
   public boolean isUnknown(){
       return currentState == null || currentState.equals(LifeCycleMBean.UNKNOWN);
   }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jbi.component.ComponentLifeCycle#init(javax.jbi.component.ComponentContext)
     */
    public void init(ComponentContext context) throws JBIException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Initializing component");
            }
            Thread.currentThread().setContextClassLoader(component.getClass().getClassLoader());
            this.context = context;
            this.channel = context.getDeliveryChannel();
            try {
                this.transactionManager = (TransactionManager) context.getTransactionManager();
            } catch (Throwable e) {
                // Ignore, this is just a safeguard against non compliant
                // JBI implementation which throws an exception instead of
                // return null
            }
            container = Container.detect(context);
            doInit();
            setCurrentState(INITIALIZED);
            if (logger.isDebugEnabled()) {
                logger.debug("Component initialized");
            }
        } catch (JBIException e) {
            throw e;
        } catch (Exception e) {
            throw new JBIException("Error calling init", e);
        }
    }

    protected void doInit() throws Exception {
        // Register extension mbean
        Object mbean = getExtensionMBean();
        if (mbean != null) {
            MBeanServer server = this.context.getMBeanServer();
            if (server == null) {
                // TODO: log a warning ?
                // throw new JBIException("null mBeanServer");
            } else {
                this.mbeanName = createExtensionMBeanName();
                if (server.isRegistered(this.mbeanName)) {
                    server.unregisterMBean(this.mbeanName);
                }
                server.registerMBean(mbean, this.mbeanName);
            }
        }
        // Obtain or create the work manager
        // When using the WorkManager from ServiceMix,
        // some class loader problems can appear when
        // trying to uninstall the components.
        // Some threads owned by the work manager have a
        // security context referencing the component class loader
        // so that every loaded classes are locked
        // this.workManager = findWorkManager();
        if (this.executorFactory == null) {
            this.executorFactory = findExecutorFactory();
        }
        if (this.executorFactory == null) {
            this.executorFactory = createExecutorFactory();
        }
        this.executor = this.executorFactory.createExecutor("component." + getContext().getComponentName());
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jbi.component.ComponentLifeCycle#shutDown()
     */
    public void shutDown() throws JBIException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Shutting down component");
            }
            Thread.currentThread().setContextClassLoader(component.getClass().getClassLoader());
            doShutDown();
            setCurrentState(LifeCycleMBean.SHUTDOWN);
            this.context = null;
            if (logger.isDebugEnabled()) {
                logger.debug("Component shut down");
            }
        } catch (JBIException e) {
            throw e;
        } catch (Exception e) {
            throw new JBIException("Error calling shutdown", e);
        }
    }

    protected void doShutDown() throws Exception {
        // Unregister mbean
        if (this.mbeanName != null) {
            MBeanServer server = this.context.getMBeanServer();
            if (server == null) {
                throw new JBIException("null mBeanServer");
            }
            if (server.isRegistered(this.mbeanName)) {
                server.unregisterMBean(this.mbeanName);
            }
        }
        // Destroy excutor
        executor.shutdown();
        executor = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jbi.component.ComponentLifeCycle#start()
     */
    public void start() throws JBIException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Starting component");
            }
            Thread.currentThread().setContextClassLoader(component.getClass().getClassLoader());
            if (this.running.compareAndSet(false, true)) {
                doStart();
                setCurrentState(LifeCycleMBean.STARTED);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Component started");
            }
        } catch (JBIException e) {
            throw e;
        } catch (Exception e) {
            throw new JBIException("Error calling start", e);
        }
    }

    protected void doStart() throws Exception {
        if (container.getType() != Container.Type.ServiceMix3) {
            synchronized (this.polling) {
                executor.execute(new Runnable() {
                    public void run() {
                        poller = Thread.currentThread();
                        pollDeliveryChannel();
                    }
                });
                polling.wait();
            }
        }
    }

    protected void pollDeliveryChannel() {
        synchronized (polling) {
            polling.set(true);
            polling.notify();
        }
        while (running.get()) {
            try {
                final MessageExchange exchange = channel.accept(1000L);
                if (exchange != null) {
                    final Transaction tx = (Transaction) exchange
                                    .getProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME);
                    if (tx != null && container.handleTransactions()) {
                        if (transactionManager == null) {
                            throw new IllegalStateException(
                                            "Exchange is enlisted in a transaction, but no transaction manager is available");
                        }
                        transactionManager.suspend();
                    }
                    executor.execute(new Runnable() {
                        public void run() {
                            processExchangeInTx(exchange, tx);
                        }
                    });
                }
            } catch (Throwable t) {
                if (running.get() == false) {
                    // Should have been interrupted, discard the throwable
                    if (logger.isDebugEnabled()) {
                        logger.debug("Polling thread will stop");
                    }
                } else {
                    logger.error("Error polling delivery channel", t);
                }
            }
        }
        synchronized (polling) {
            polling.set(false);
            polling.notify();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.jbi.component.ComponentLifeCycle#stop()
     */
    public void stop() throws JBIException {
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Stopping component");
            }
            Thread.currentThread().setContextClassLoader(component.getClass().getClassLoader());
            if (this.running.compareAndSet(true, false)) {
                doStop();
                setCurrentState(LifeCycleMBean.STOPPED);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Component stopped");
            }
        } catch (JBIException e) {
            throw e;
        } catch (Exception e) {
            throw new JBIException("Error calling stop", e);
        }
    }

    protected void doStop() throws Exception {
        // Interrupt the polling thread and await termination
        try {
            synchronized (polling) {
                if (polling.get()) {
                    poller.interrupt();
                    polling.wait();
                }
            }
        } finally {
            poller = null;
        }
    }

    /**
     * @return Returns the context.
     */
    public ComponentContext getContext() {
        return context;
    }

    public Executor getExecutor() {
        return executor;
    }

    /**
     * The executor to use for various tasks that need to be performed by the component.
     * If none is provided, one will be created from the executorFactory.
     *
     * @param executor
     * @see #setExecutorFactory(ExecutorFactory)
     */
    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public ExecutorFactory getExecutorFactory() {
        return executorFactory;
    }

    /**
     * The executor factory to use to create the executor.
     * If none is provided, one will be retrieved from the JBI container when the component
     * is deployed into ServiceMix 3.x, or a default implementation will be used.
     *
     * @param executorFactory
     * @see #setExecutor(Executor)
     */
    public void setExecutorFactory(ExecutorFactory executorFactory) {
        this.executorFactory = executorFactory;
    }

    protected ExecutorFactory createExecutorFactory() {
        // Create a very simple one
        return new ExecutorFactoryImpl();
    }

    public Object getSmx3Container() {
        if (container instanceof Container.Smx3Container) {
            return ((Container.Smx3Container) container).getSmx3Container();
        }
        return null;
    }

    protected ExecutorFactory findExecutorFactory() {
        // If inside ServiceMix, retrieve its executor factory
        try {
            Object container = getSmx3Container();
            if (container != null) {
                Method getWorkManagerMth = container.getClass().getMethod("getExecutorFactory", new Class[0]);
                return (ExecutorFactory) getWorkManagerMth.invoke(container, new Object[0]);
            }
        } catch (Throwable t) {
            // Ignore
        }
        // TODO: should look in jndi for an existing ExecutorFactory
        return null;
    }

    protected void processExchangeInTx(MessageExchange exchange, Transaction tx) {
        try {
            if (tx != null) {
                transactionManager.resume(tx);
            }
            processExchange(exchange);
        } catch (Throwable t) {
            logger.error("Error processing exchange " + exchange, t);
            try {
                // If we are transacted, check if this exception should
                // rollback the transaction
                if (transactionManager != null && transactionManager.getStatus() == Status.STATUS_ACTIVE) {
                    if (exceptionShouldRollbackTx(t)) {
                        transactionManager.setRollbackOnly();
                    }
                    if (!container.handleTransactions()) {
                        transactionManager.suspend();
                    }
                }
                exchange.setError(t instanceof Exception ? (Exception) t : new Exception(t));
                channel.send(exchange);
            } catch (Exception inner) {
                logger.error("Error setting exchange status to ERROR", inner);
            }
        } finally {
            try {
                // Check transaction status
                if (tx != null) {
                    int status = transactionManager.getStatus();
                    // We use pull delivery, so the transaction should already
                    // have been transfered to another thread because the
                    // component
                    // must have answered.
                    if (status != Status.STATUS_NO_TRANSACTION) {
                        logger.error("Transaction is still active after exchange processing. Trying to rollback transaction.");
                        try {
                            transactionManager.rollback();
                        } catch (Throwable t) {
                            logger.error("Error trying to rollback transaction.", t);
                        }
                    }
                }
            } catch (Throwable t) {
                logger.error("Error checking transaction status.", t);
            }
        }
    }

    protected boolean exceptionShouldRollbackTx(Throwable t) {
        return false;
    }

    public void onMessageExchange(MessageExchange exchange) {
        if (!container.handleTransactions()) {
            final Transaction tx = (Transaction) exchange.getProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME);
            processExchangeInTx(exchange, tx);
            return;
        }
        try {
            processExchange(exchange);
        } catch (Exception e) {
            logger.error("Error processing exchange " + exchange, e);
            try {
                // If we are transacted and this is a runtime exception
                // try to mark transaction as rollback
                if (transactionManager != null &&
                    transactionManager.getStatus() == Status.STATUS_ACTIVE &&
                    exceptionShouldRollbackTx(e)) {
                    transactionManager.setRollbackOnly();
                    if (!container.handleTransactions()) {
                        transactionManager.suspend();
                    }
                }
                exchange.setError(e);
                channel.send(exchange);
            } catch (Exception inner) {
                logger.error("Error setting exchange status to ERROR", inner);
            }
        }
    }

    protected void processExchange(MessageExchange exchange) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Received exchange: status: " + exchange.getStatus() + ", role: "
                            + (exchange.getRole() == Role.CONSUMER ? "consumer" : "provider"));
        }
        if (exchange.getRole() == Role.PROVIDER) {
            boolean dynamic = false;
            ServiceEndpoint endpoint = exchange.getEndpoint();
            String key = EndpointSupport.getKey(exchange.getEndpoint());
            Endpoint ep = this.component.getRegistry().getEndpoint(key);
            if (ep == null) {
                if (endpoint.getServiceName().equals(getEPRServiceName())) {
                    ep = getResolvedEPR(exchange.getEndpoint());
                    ep.activate();
                    ep.start();
                    dynamic = true;
                }
                if (ep == null) {
                    throw new IllegalStateException("Endpoint not found: " + key);
                }
            }
            try {
                doProcess(ep, exchange);
            } finally {
                // If the endpoint is dynamic, deactivate it
                if (dynamic) {
                    ep.stop();
                    ep.deactivate();
                }
            }
        } else {
            Endpoint ep = null;
            if (exchange.getProperty(JbiConstants.SENDER_ENDPOINT) != null) {
                String key = exchange.getProperty(JbiConstants.SENDER_ENDPOINT).toString();
                ep = this.component.getRegistry().getEndpoint(key);
            }
            if (ep == null) {
                throw new IllegalStateException("Endpoint not found for: " + exchange.getExchangeId());
            }
            doProcess(ep, exchange);
        }

    }

    /**
     * Thin wrapper around the call to the processor to ensure that the Endpoints
     * classloader is used where available
     * 
     */
    private void doProcess(Endpoint ep, MessageExchange exchange) throws Exception {
        ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
        boolean processed = false;
        try {
            ClassLoader cl = (ep != null) ? ep.getServiceUnit().getConfigurationClassLoader() : null;
            if (cl != null) {
                Thread.currentThread().setContextClassLoader(cl);
            }
            // Read the correlation id from the exchange and set it in the correlation id property
            String correlationID = (String)exchange.getProperty(JbiConstants.CORRELATION_ID);
            if (correlationID != null) {
                // Set the id in threadlocal variable
                correlationId.set(correlationID);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Retrieved correlation id: " + correlationID);
            }
            EndpointDeliveryChannel.setEndpoint(ep);
            handleExchange(ep, exchange, exchange.getStatus() == ExchangeStatus.ACTIVE);
            ep.process(exchange);
            processed = true;
        } finally {
            if (!processed) {
                handleExchange(ep, exchange, false);
            }
            EndpointDeliveryChannel.setEndpoint(null);
            Thread.currentThread().setContextClassLoader(oldCl);
            // Clean the threadlocal variable
            correlationId.set(null);
        }
    }

    public void prepareExchange(MessageExchange exchange, Endpoint endpoint) throws MessagingException {
        if (exchange.getRole() == Role.CONSUMER) {
            // Check if a correlation id is already set on the exchange, otherwise create it
            String correlationIDValue = (String) exchange.getProperty(JbiConstants.CORRELATION_ID);
            if (correlationIDValue == null) {
                // Retrieve correlation id from thread local variable, if exist
                correlationIDValue = correlationId.get();
                if (correlationIDValue == null) {
                    // Set a correlation id property that have to be propagated in all components
                    // to trace the process instance
                    correlationIDValue = exchange.getExchangeId();
                    exchange.setProperty(JbiConstants.CORRELATION_ID, exchange.getExchangeId());
                    if (logger.isDebugEnabled()) {
                        logger.debug("Created correlation id: " + correlationIDValue);
                    }
                } else {
                    // Use correlation id retrieved from previous message exchange
                    exchange.setProperty(JbiConstants.CORRELATION_ID, correlationIDValue);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Correlation id retrieved from ThreadLocal: " + correlationIDValue);
                    }
                }
            }
            // Set the sender endpoint property
            exchange.setProperty(JbiConstants.SENDER_ENDPOINT, endpoint.getKey());
        }
        // Handle transaction
        if (!container.handleTransactions()) {
            try {
                if ((exchange.getRole() == Role.CONSUMER && exchange.getStatus() == ExchangeStatus.ACTIVE) || exchange.getRole() == Role.PROVIDER) {
                    if (transactionManager != null) {
                        exchange.setProperty(MessageExchange.JTA_TRANSACTION_PROPERTY_NAME, transactionManager.suspend());
                    }
                }
            } catch (SystemException e) {
                throw new MessagingException("Error handling transaction", e);
            }
        }
    }

    public void prepareShutdown(Endpoint endpoint) throws InterruptedException {
        Set<String> exchanges = getKnownExchanges(endpoint);
        synchronized (exchanges) {
            if (!exchanges.isEmpty()) {
                exchanges.wait();
            }
        }
    }

    protected Set<String> getKnownExchanges(Endpoint endpoint) {
        Set<String> exchanges = knownExchanges.get(endpoint.getKey());
        if (exchanges == null) {
            synchronized (knownExchanges) {
                exchanges = knownExchanges.get(endpoint.getKey());
                if (exchanges == null) {
                    exchanges = new HashSet<String>();
                    knownExchanges.put(endpoint.getKey(), exchanges);
                }
            }
        }
        return exchanges;
    }

    public void handleExchange(Endpoint endpoint, MessageExchange exchange, boolean add) {
        Set<String> exchanges = getKnownExchanges(endpoint);
        synchronized (exchanges) {
            if (add) {
                exchanges.add(exchange.getExchangeId());
            } else {
                exchanges.remove(exchange.getExchangeId());
            }
            exchanges.notifyAll();
        }
    }

   /**
     * Handle an exchange sent to an EPR resolved by this component
     * 
     * @param ep the service endpoint
     * @return an endpoint to use for handling the exchange
     * @throws Exception
     */
    protected Endpoint getResolvedEPR(ServiceEndpoint ep) throws Exception {
        throw new UnsupportedOperationException("Component does not handle EPR exchanges");
    }

}
