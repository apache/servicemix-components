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
package org.apache.servicemix.jms.endpoints;

import javax.jbi.management.DeploymentException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.transaction.TransactionManager;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.jms.JmsEndpointType;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.connection.JmsTransactionManager102;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer102;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.listener.SimpleMessageListenerContainer;
import org.springframework.jms.listener.SimpleMessageListenerContainer102;
import org.springframework.jms.listener.serversession.ServerSessionFactory;
import org.springframework.jms.listener.serversession.ServerSessionMessageListenerContainer;
import org.springframework.jms.listener.serversession.ServerSessionMessageListenerContainer102;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.jta.JtaTransactionManager;

/**
 * A Sping-based JMS consumer endpoint.
 *
 * @author gnodet
 * @org.apache.xbean.XBean element="consumer" 
 * @since 3.2
 */
public class JmsConsumerEndpoint extends AbstractConsumerEndpoint implements JmsEndpointType {

    public static final String LISTENER_TYPE_DEFAULT = "default";
    public static final String LISTENER_TYPE_SIMPLE = "simple";
    public static final String LISTENER_TYPE_SERVER = "server";
    
    public static final String TRANSACTED_NONE = "none";
    public static final String TRANSACTED_XA = "xa";
    public static final String TRANSACTED_JMS = "jms";
    
    // type of listener
    private String listenerType = LISTENER_TYPE_DEFAULT;
    private String transacted = TRANSACTED_NONE;
    
    // Standard jms properties
    private String clientId;
    private Destination destination;
    private String destinationName;
    private String durableSubscriptionName;
    private ExceptionListener exceptionListener;
    private String messageSelector;
    private int sessionAcknowledgeMode = Session.AUTO_ACKNOWLEDGE;
    private boolean subscriptionDurable;
    
    // simple and default listener properties
    private boolean pubSubNoLocal;
    private int concurrentConsumers = 1;
    
    // default listener properties
    private int idleTaskExecutionLimit = 1;
    private int maxConcurrentConsumers = 1;
    private int cacheLevel = DefaultMessageListenerContainer.CACHE_NONE;
    private long receiveTimeout = DefaultMessageListenerContainer.DEFAULT_RECEIVE_TIMEOUT;
    private long recoveryInterval = DefaultMessageListenerContainer.DEFAULT_RECOVERY_INTERVAL;

    // default and server listener properties
    private int maxMessagesPerTask = Integer.MIN_VALUE;
    
    // server listener properties
    private ServerSessionFactory serverSessionFactory;
    
    private AbstractMessageListenerContainer listenerContainer;
    

    public JmsConsumerEndpoint() {
        super();
    }

    public JmsConsumerEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component, endpoint);
    }

    public JmsConsumerEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    
    /**
     * @return the transacted
     */
    public String getTransacted() {
        return transacted;
    }

    /**
    * Specifies the type of transaction used to wrap the message exchanges. 
    * Valid values are <code>none</code>, <code>xa</code>, and <code>jms</code>.
    *
     * @param transacted the type of transaction wrapper to use
     */
    public void setTransacted(String transacted) {
        this.transacted = transacted;
    }

    /**
     * @return the cacheLevel
     */
    public int getCacheLevel() {
        return cacheLevel;
    }

    /**
    * Specifies the level of caching allowed by the listener. Valid values are 
    * 0 through 3. The values map to the following:
    * <ul>
    * <li>0 - <code>CACHE_NONE</code></li>
    * <li>1 - <code>CACHE_CONNECTION</code></li>
    * <li>2 - <code>CACHE_SESSION</code></li>
    * <li>3 - <code>CACHE_CONSUMER</code></li>
    * </ul>
    * The default is <code>CACHE_NONE</code>.<br/>
    * This property only effects consumers whose <code>listenerType</code> 
    * property is set to <code>default</code>.
    *
     * @param cacheLevel the cacheLevel to set
     * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setCacheLevel(int)
     */
    public void setCacheLevel(int cacheLevel) {
        this.cacheLevel = cacheLevel;
    }

    /**
     * @return the clientId
     */
    public String getClientId() {
        return clientId;
    }

    /**
    * Specifies the JMS client id for a shared <code>Connection</code> created and used by 
    * this listener.
    * 
     * @param clientId the clientId to set
     * @see org.springframework.jms.listener.AbstractMessageListenerContainer#setClientId(String)
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * @return the concurrentConsumers
     */
    public int getConcurrentConsumers() {
        return concurrentConsumers;
    }

    /**
    * Specifies the number of concurrent consumers created by the listener.
    * This property is only used for consumers whose <code>listenerType</code> 
    * property is set to either <code>simple</code> or <code>default</code>.
    * 
     * @param concurrentConsumers the number of concurrent consumers to create
     * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setConcurrentConsumers(int)
     * @see org.springframework.jms.listener.SimpleMessageListenerContainer#setConcurrentConsumers(int)
     */
    public void setConcurrentConsumers(int concurrentConsumers) {
        this.concurrentConsumers = concurrentConsumers;
    }

    /**
     * @return the destination
     */
    public Destination getDestination() {
        return destination;
    }

    /**
    * Specifies the JMS <code>Destination</code> used to receive messages.
    *
     * @param destination the JMS destination
     * @see org.springframework.jms.listener.AbstractMessageListenerContainer#setDestination(Destination)
     */
    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    /**
     * @return the destinationName
     */
    public String getDestinationName() {
        return destinationName;
    }

    /**
    * Specifies a string identifying the JMS destination used to recieve 
     * messages. The destination is resolved using the 
     * <code>DesitinationResolver</code>.
     *
     * @param destinationName the destination name
     * @see org.springframework.jms.listener.AbstractMessageListenerContainer#setDestinationName(String)
     */
    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    /**
     * @return the durableSubscriptionName
     */
    public String getDurableSubscriptionName() {
        return durableSubscriptionName;
    }

    /**
    * Specifies the name used to register the durable subscription.
    *
     * @param durableSubscriptionName the registration name
     * @see org.springframework.jms.listener.AbstractMessageListenerContainer#setDurableSubscriptionName(String)
     */
    public void setDurableSubscriptionName(String durableSubscriptionName) {
        this.durableSubscriptionName = durableSubscriptionName;
    }

    /**
     * @return the exceptionListener
     */
    public ExceptionListener getExceptionListener() {
        return exceptionListener;
    }

    /**
    * Specifies an <code>ExceptionListener</code> to notify in case of a 
    * <code>JMSException</code> is thrown by the registered message listener or 
    * the invocation infrastructure.
    *
     * @param exceptionListener the exception listener
     * @see org.springframework.jms.listener.AbstractMessageListenerContainer#setExceptionListener(ExceptionListener)
     */
    public void setExceptionListener(ExceptionListener exceptionListener) {
        this.exceptionListener = exceptionListener;
    }

    /**
     * @return the idleTaskExecutionLimit
     */
    public int getIdleTaskExecutionLimit() {
        return idleTaskExecutionLimit;
    }
     
    /**
     * Specifies the limit for idle executions of a receive task, not having received any message within its execution. 
     * If this limit is reached, the task will shut down and leave receiving to other executing tasks 
     * (in case of dynamic scheduling; see the "maxConcurrentConsumers" setting).
     * Within each task execution, a number of message reception attempts (according to the "maxMessagesPerTask" setting) 
     * will each wait for an incoming message (according to the "receiveTimeout" setting). 
     * If all of those receive attempts in a given task return without a message, 
     * the task is considered idle with respect to received messages. 
     * Such a task may still be rescheduled; however, once it reached the specified "idleTaskExecutionLimit", 
     * it will shut down (in case of dynamic scaling).
     * Raise this limit if you encounter too frequent scaling up and down. 
     * With this limit being higher, an idle consumer will be kept around longer, 
     * avoiding the restart of a consumer once a new load of messages comes in. 
     * Alternatively, specify a higher "maxMessagePerTask" and/or "receiveTimeout" value, 
     * which will also lead to idle consumers being kept around for a longer time 
     * (while also increasing the average execution time of each scheduled task). 
     * 
     * This property is only used for consumers whose <code>listenerType</code> 
     * property is set to <code>default</code>.
     * 
     * @param idleTaskExecutionLimit the number of concurrent consumers to create
     * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setIdleTaskExecutionLimit(int)
     */
    public void setIdleTaskExecutionLimit(int idleTaskExecutionLimit) {
        this.idleTaskExecutionLimit = idleTaskExecutionLimit;
    }

    /**
     * @return the listenerType
     */
    public String getListenerType() {
        return listenerType;
    }

    /**
    * Specifies the type of Spring JMS message listener to use. Valid values 
    * are: <code>default</code>, <code>simple</code>, and <code>server</code>.
    *
     * @param listenerType the listener type
     */
    public void setListenerType(String listenerType) {
        this.listenerType = listenerType;
    }

    /**
     * @return the maxConcurrentConsumers
     */
    public int getMaxConcurrentConsumers() {
        return maxConcurrentConsumers;
    }

    /**
     * Specifies the maximum number of concurrent consumers created by the listener.
     * If this setting is higher than "concurrentConsumers", the listener container 
     * will dynamically schedule new consumers at runtime, provided that enough incoming 
     * messages are encountered. Once the load goes down again, the number of consumers 
     * will be reduced to the standard level ("concurrentConsumers") again.
     * This property is only used for consumers whose <code>listenerType</code> 
     * property is set to <code>default</code>.
     * 
     * @param maxConcurrentConsumers the maximum number of concurrent consumers to create
     * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setMaxConcurrentConsumers(int)
     */
    public void setMaxConcurrentConsumers(int maxConcurrentConsumers) {
        this.maxConcurrentConsumers = maxConcurrentConsumers;
    }

    /**
     * @return the maxMessagesPerTask
     */
    public int getMaxMessagesPerTask() {
        return maxMessagesPerTask;
    }

    /**
    * Specifies the number of attempts to receive messages per task. The 
    * default is -1 which specifies an unlimited number of attempts.<br/>
    * This property only effects consumers whose <code>listenerType</code> 
    * property is set to either <code>default</code> or <code>simple</code>.
    * 
     * @param maxMessagesPerTask the number of attempts to make
     * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setMaxMessagesPerTask(int)
     * @see org.springframework.jms.listener.serversession.ServerSessionMessageListenerContainer#setMaxMessagesPerTask(int)
     */
    public void setMaxMessagesPerTask(int maxMessagesPerTask) {
        this.maxMessagesPerTask = maxMessagesPerTask;
    }

    /**
     * @return the messageSelector
     */
    public String getMessageSelector() {
        return messageSelector;
    }

    /**
    * Specifies the message selector string to use. The message selector string 
    * should conform to the descrition in the JMS spec.
    *
     * @param messageSelector the message selector string
     * @see org.springframework.jms.listener.AbstractMessageListenerContainer#setMessageSelector(String)
     */
    public void setMessageSelector(String messageSelector) {
        this.messageSelector = messageSelector;
    }

    /**
     * @return the pubSubNoLocal
     */
    public boolean isPubSubNoLocal() {
        return pubSubNoLocal;
    }

    /**
    * Specifies if messages published by the listener's <code>Connection</code> 
    * are suppressed. The default is <code>false</code>.<br/>
    * This property only effects consumers whose <code>listenerType</code> 
    * property is set to either <code>default</code> or <code>simple</code>.
    *
     * @param pubSubNoLocal messages are surpressed?
     * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setPubSubNoLocal(boolean)
     * @see org.springframework.jms.listener.SimpleMessageListenerContainer#setPubSubNoLocal(boolean)
     */
    public void setPubSubNoLocal(boolean pubSubNoLocal) {
        this.pubSubNoLocal = pubSubNoLocal;
    }

    /**
     * @return the receiveTimeout
     */
    public long getReceiveTimeout() {
        return receiveTimeout;
    }

    /**
    * Specifies the timeout for receiving a message in milliseconds. Defaults 
    * to 1000.<br/>
    * This property only effects consumers whose <code>listenerType</code> 
    * property is set to <code>default</code>.
    *
     * @param receiveTimeout the number of milliseconds before timing out
     * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setReceiveTimeout(long)
     */
    public void setReceiveTimeout(long receiveTimeout) {
        this.receiveTimeout = receiveTimeout;
    }

    /**
     * @return the recoveryInterval
     */
    public long getRecoveryInterval() {
        return recoveryInterval;
    }

    /**
    *Specifies the interval, in milliseconds, between attempts to recover after 
    * a failed listener set-up. Defaults to 5000.<br/>
    * This property only effects consumers whose <code>listenerType</code> 
    * property is set to <code>default</code>.
    *
     * @param recoveryInterval the number of milliseconds to wait
     * @see org.springframework.jms.listener.DefaultMessageListenerContainer#setRecoveryInterval(long)
     */
    public void setRecoveryInterval(long recoveryInterval) {
        this.recoveryInterval = recoveryInterval;
    }

    /**
     * @return the serverSessionFactory
     */
    public ServerSessionFactory getServerSessionFactory() {
        return serverSessionFactory;
    }

    /**
    * Specifies the <code>ServerSessionFactory</code> to use. The default is 
    * <code>SimpleServerSessionFactory</code>.<br/>
    * This property only effects consumers whose <code>listenerType</code> 
    * property is set to <code>server</code>.
    *
     * @param serverSessionFactory an implementation of the <code>ServerSessionFactory</code> interface
     * @see ServerSessionMessageListenerContainer#setServerSessionFactory(ServerSessionFactory)
     */
    public void setServerSessionFactory(ServerSessionFactory serverSessionFactory) {
        this.serverSessionFactory = serverSessionFactory;
    }

    /**
     * @return the sessionAcknowledgeMode
     */
    public int getSessionAcknowledgeMode() {
        return sessionAcknowledgeMode;
    }

    /**
    * Specifies the acknowledgment mode that is used when creating a 
    * <code>Session</code> to send a message. Deafults to 
    * <code>Session.AUTO_ACKNOWLEDGE</code>.
    *
     * @param sessionAcknowledgeMode the sessionAcknowledgeMode to set
     * @see org.springframework.jms.support.JmsAccessor#setSessionAcknowledgeMode(int)
     */
    public void setSessionAcknowledgeMode(int sessionAcknowledgeMode) {
        this.sessionAcknowledgeMode = sessionAcknowledgeMode;
    }

    /**
     * @return the subscriptionDurable
     */
    public boolean isSubscriptionDurable() {
        return subscriptionDurable;
    }

    /**
    * Specifies if the listener uses a durable subscription to listen for 
    * messages. Defaults to <code>false</code>.
    *
     * @param subscriptionDurable the listener uses a durable subscription?
     * @see org.springframework.jms.listener.AbstractMessageListenerContainer#setSubscriptionDurable(boolean)
     */
    public void setSubscriptionDurable(boolean subscriptionDurable) {
        this.subscriptionDurable = subscriptionDurable;
    }

    public String getLocationURI() {
        // TODO: Need to return a real URI
        return getService() + "#" + getEndpoint();
    }

    public synchronized void activate() throws Exception {
        super.activate();
        listenerContainer = createListenerContainer();
        listenerContainer.setMessageListener(new SessionAwareMessageListener() {
            public void onMessage(Message message, Session session) throws JMSException {
                JmsConsumerEndpoint.this.onMessage(message, session);
            }
        });
        listenerContainer.setAutoStartup(false);
        listenerContainer.afterPropertiesSet();
    }
    
    public synchronized void start() throws Exception {
        listenerContainer.start();
    }

    public synchronized void stop() throws Exception {
        listenerContainer.stop();
    }

    public synchronized void deactivate() throws Exception {
        if (listenerContainer != null) {
            listenerContainer.stop();
            listenerContainer.shutdown();
            listenerContainer = null;
        }
        super.deactivate();
    }
    
    public void validate() throws DeploymentException {
        // TODO: check service, endpoint
        super.validate();
        if (getConnectionFactory() == null) {
            throw new DeploymentException("connectionFactory is required");
        }
        if (destination == null && destinationName == null) {
            throw new DeploymentException("destination or destinationName is required");
        }
        if (!LISTENER_TYPE_DEFAULT.equals(listenerType)
            && !LISTENER_TYPE_SIMPLE.equals(listenerType)
            && !LISTENER_TYPE_SERVER.equals(listenerType)) {
            throw new DeploymentException("listenerType must be default, simple or server");
        }
        if (TRANSACTED_XA.equals(transacted)
            && !LISTENER_TYPE_DEFAULT.equals(listenerType)) {
            throw new DeploymentException("XA transactions are only supported on default listener");
        }
        if (!TRANSACTED_NONE.equals(transacted)
            && !TRANSACTED_JMS.equals(transacted)
            && !TRANSACTED_XA.equals(transacted)) {
            throw new DeploymentException("transacted must be none, jms or xa");
        }

        // Provide some intelligent defaults for rollback policy
        if (TRANSACTED_XA.equals(transacted) || TRANSACTED_JMS.equals(transacted)) {
            JmsConsumerMarshaler marshaler = getMarshaler();
            if (marshaler instanceof DefaultConsumerMarshaler) {
                ((DefaultConsumerMarshaler)marshaler).setRollbackOnError(true);
            }
        }
    }
    
    protected AbstractMessageListenerContainer createListenerContainer() {
        final AbstractMessageListenerContainer container;
        if (LISTENER_TYPE_DEFAULT.equals(listenerType)) {
            container = createDefaultMessageListenerContainer();
        } else if (LISTENER_TYPE_SIMPLE.equals(listenerType)) {
            container = createSimpleMessageListenerContainer();
        } else if (LISTENER_TYPE_SERVER.equals(listenerType)) {
            container = createServerSessionMessageListenerContainer();
        } else {
            throw new IllegalStateException();
        }
        container.setAutoStartup(false);
        container.setClientId(clientId);
        container.setConnectionFactory(getConnectionFactory());
        if (destination != null) {
            container.setDestination(destination);
        } else if (destinationName != null) {
            container.setDestinationName(destinationName);
        }
        if (getDestinationResolver() != null) {
            container.setDestinationResolver(getDestinationResolver());
        }
        if (subscriptionDurable) {
            if (durableSubscriptionName == null) {
                // Use unique name generated from this endpoint
                durableSubscriptionName = getService() + "#" + getEndpoint();
            }
            container.setDurableSubscriptionName(durableSubscriptionName);
        }
        container.setExceptionListener(exceptionListener);
        container.setMessageSelector(messageSelector);
        container.setPubSubDomain(isPubSubDomain());
        container.setSessionAcknowledgeMode(sessionAcknowledgeMode);
        container.setSubscriptionDurable(subscriptionDurable);
        return container;
    }

    private AbstractMessageListenerContainer createServerSessionMessageListenerContainer() {
        final ServerSessionMessageListenerContainer cont;
        if (isJms102()) {
            cont = new ServerSessionMessageListenerContainer102();
        } else {
            cont = new ServerSessionMessageListenerContainer();
        }
        cont.setMaxMessagesPerTask(maxMessagesPerTask > 0 ? maxMessagesPerTask : 1);
        cont.setServerSessionFactory(serverSessionFactory);
        if (TRANSACTED_JMS.equals(transacted)) {
            cont.setSessionTransacted(true);
        }
        return cont;
    }

    private AbstractMessageListenerContainer createSimpleMessageListenerContainer() {
        final SimpleMessageListenerContainer cont;
        if (isJms102()) {
            cont = new SimpleMessageListenerContainer102();
        } else {
            cont = new SimpleMessageListenerContainer();
        }
        cont.setConcurrentConsumers(concurrentConsumers);
        cont.setPubSubNoLocal(pubSubNoLocal);
        cont.setTaskExecutor(null); // TODO: value ?
        if (TRANSACTED_JMS.equals(transacted)) {
            cont.setSessionTransacted(true);
        }
        return cont;
    }

    private AbstractMessageListenerContainer createDefaultMessageListenerContainer() {
        final DefaultMessageListenerContainer cont;
        if (isJms102()) {
            cont = new DefaultMessageListenerContainer102();
        } else {
            cont = new DefaultMessageListenerContainer();
        }
        cont.setCacheLevel(cacheLevel);
        cont.setConcurrentConsumers(concurrentConsumers);
        cont.setIdleTaskExecutionLimit(idleTaskExecutionLimit);
        cont.setMaxConcurrentConsumers(maxConcurrentConsumers);
        cont.setMaxMessagesPerTask(maxMessagesPerTask);
        cont.setPubSubNoLocal(pubSubNoLocal);
        cont.setReceiveTimeout(receiveTimeout);
        cont.setRecoveryInterval(recoveryInterval);
        if (TRANSACTED_XA.equals(transacted)) {
            cont.setSessionTransacted(true);
            TransactionManager tm = (TransactionManager) getContext().getTransactionManager();
            if (tm == null) {
                throw new IllegalStateException("No TransactionManager available");
            } else if (tm instanceof PlatformTransactionManager) {
                cont.setTransactionManager((PlatformTransactionManager) tm);
            } else {
                cont.setTransactionManager(new JtaTransactionManager(tm));
            }
        } else if (TRANSACTED_JMS.equals(transacted)) {
            cont.setSessionTransacted(true);
            if (isJms102()) {
                cont.setTransactionManager(new JmsTransactionManager102(getConnectionFactory(), isPubSubDomain()));
            } else {
                cont.setTransactionManager(new JmsTransactionManager(getConnectionFactory()));
            }
        }
        return cont;
    }

}
