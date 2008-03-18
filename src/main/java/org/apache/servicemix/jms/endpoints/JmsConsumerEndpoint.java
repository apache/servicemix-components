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
    private boolean jms102;
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
     * @param transacted the transacted to set
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
     * @param concurrentConsumers the concurrentConsumers to set
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
     * @param destination the destination to set
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
     * @param destinationName the destinationName to set
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
     * @param durableSubscriptionName the durableSubscriptionName to set
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
     * @param exceptionListener the exceptionListener to set
     * @see org.springframework.jms.listener.AbstractMessageListenerContainer#setExceptionListener(ExceptionListener)
     */
    public void setExceptionListener(ExceptionListener exceptionListener) {
        this.exceptionListener = exceptionListener;
    }

    /**
     * @return the jms102
     */
    public boolean isJms102() {
        return jms102;
    }

    /**
     * @param jms102 the jms102 to set
     */
    public void setJms102(boolean jms102) {
        this.jms102 = jms102;
    }

    /**
     * @return the listenerType
     */
    public String getListenerType() {
        return listenerType;
    }

    /**
     * @param listenerType the listenerType to set
     */
    public void setListenerType(String listenerType) {
        this.listenerType = listenerType;
    }

    /**
     * @return the maxMessagesPerTask
     */
    public int getMaxMessagesPerTask() {
        return maxMessagesPerTask;
    }

    /**
     * @param maxMessagesPerTask the maxMessagesPerTask to set
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
     * @param messageSelector the messageSelector to set
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
     * @param pubSubNoLocal the pubSubNoLocal to set
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
     * @param receiveTimeout the receiveTimeout to set
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
     * @param recoveryInterval the recoveryInterval to set
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
     * @param serverSessionFactory the serverSessionFactory to set
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
     * @param subscriptionDurable the subscriptionDurable to set
     * @see org.springframework.jms.listener.AbstractMessageListenerContainer#setSubscriptionDurable(boolean)
     */
    public void setSubscriptionDurable(boolean subscriptionDurable) {
        this.subscriptionDurable = subscriptionDurable;
    }

    public String getLocationURI() {
        // TODO: Need to return a real URI
        return getService() + "#" + getEndpoint();
    }

    public synchronized void start() throws Exception {
        listenerContainer = createListenerContainer();
        listenerContainer.setMessageListener(new SessionAwareMessageListener() {
            public void onMessage(Message message, Session session) throws JMSException {
                JmsConsumerEndpoint.this.onMessage(message, session);
            }
        });
        listenerContainer.setAutoStartup(true);
        listenerContainer.afterPropertiesSet();
        super.start();
    }
    
    public synchronized void stop() throws Exception {
        if (listenerContainer != null) {
            listenerContainer.stop();
            listenerContainer.shutdown();
            listenerContainer = null;
        }
        super.stop();
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
        if (jms102) {
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
        if (jms102) {
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
        if (jms102) {
            cont = new DefaultMessageListenerContainer102();
        } else {
            cont = new DefaultMessageListenerContainer();
        }
        cont.setCacheLevel(cacheLevel);
        cont.setConcurrentConsumers(concurrentConsumers);
        cont.setMaxMessagesPerTask(maxMessagesPerTask);
        cont.setPubSubNoLocal(pubSubNoLocal);
        cont.setReceiveTimeout(receiveTimeout);
        cont.setRecoveryInterval(recoveryInterval);
        if (TRANSACTED_XA.equals(transacted)) {
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
            if (jms102) {
                cont.setTransactionManager(new JmsTransactionManager102(getConnectionFactory(), isPubSubDomain()));
            } else {
                cont.setTransactionManager(new JmsTransactionManager(getConnectionFactory()));
            }
        }
        return cont;
    }

}
