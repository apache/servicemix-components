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

import java.util.Map;

import javax.jbi.JBIException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.endpoints.ConsumerEndpoint;
import org.apache.servicemix.jms.endpoints.JmsConsumerMarshaler.JmsContext;
import org.apache.servicemix.store.Store;
import org.apache.servicemix.store.StoreFactory;
import org.apache.servicemix.store.memory.MemoryStoreFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.JmsTemplate102;
import org.springframework.jms.core.SessionCallback;
import org.springframework.jms.listener.adapter.ListenerExecutionFailedException;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;

public abstract class AbstractConsumerEndpoint extends ConsumerEndpoint {
    
    protected static final String PROP_JMS_CONTEXT = JmsContext.class.getName();

    private JmsConsumerMarshaler marshaler = new DefaultConsumerMarshaler();
    private boolean synchronous = true;
    private DestinationChooser destinationChooser;
    private DestinationResolver destinationResolver = new DynamicDestinationResolver();
    private boolean pubSubDomain;
    private ConnectionFactory connectionFactory;
    private JmsTemplate template;

    // Reply properties
    private Boolean useMessageIdInResponse;
    private Destination replyDestination;
    private String replyDestinationName;
    private boolean replyExplicitQosEnabled;
    private int replyDeliveryMode = Message.DEFAULT_DELIVERY_MODE;
    private int replyPriority = Message.DEFAULT_PRIORITY;
    private long replyTimeToLive = Message.DEFAULT_TIME_TO_LIVE;
    private Map<String, Object> replyProperties;

    private boolean stateless;
    private StoreFactory storeFactory;
    private Store store;
    private boolean jms102;

    public AbstractConsumerEndpoint() {
        super();
    }

    public AbstractConsumerEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component, endpoint);
    }

    public AbstractConsumerEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    /**
     * @return the destinationChooser
     */
    public DestinationChooser getDestinationChooser() {
        return destinationChooser;
    }

    /**
     * @param destinationChooser the destinationChooser to set
     */
    public void setDestinationChooser(DestinationChooser destinationChooser) {
        this.destinationChooser = destinationChooser;
    }

    /**
     * @return the replyDeliveryMode
     */
    public int getReplyDeliveryMode() {
        return replyDeliveryMode;
    }

    /**
     * @param replyDeliveryMode the replyDeliveryMode to set
     */
    public void setReplyDeliveryMode(int replyDeliveryMode) {
        this.replyDeliveryMode = replyDeliveryMode;
    }

    /**
     * @return the replyDestination
     */
    public Destination getReplyDestination() {
        return replyDestination;
    }

    /**
     * @param replyDestination the replyDestination to set
     */
    public void setReplyDestination(Destination replyDestination) {
        this.replyDestination = replyDestination;
    }

    /**
     * @return the replyDestinationName
     */
    public String getReplyDestinationName() {
        return replyDestinationName;
    }

    /**
     * @param replyDestinationName the replyDestinationName to set
     */
    public void setReplyDestinationName(String replyDestinationName) {
        this.replyDestinationName = replyDestinationName;
    }

    /**
     * @return the replyExplicitQosEnabled
     */
    public boolean isReplyExplicitQosEnabled() {
        return replyExplicitQosEnabled;
    }

    /**
     * @param replyExplicitQosEnabled the replyExplicitQosEnabled to set
     */
    public void setReplyExplicitQosEnabled(boolean replyExplicitQosEnabled) {
        this.replyExplicitQosEnabled = replyExplicitQosEnabled;
    }

    /**
     * @return the replyPriority
     */
    public int getReplyPriority() {
        return replyPriority;
    }

    /**
     * @param replyPriority the replyPriority to set
     */
    public void setReplyPriority(int replyPriority) {
        this.replyPriority = replyPriority;
    }

    /**
     * @return the replyProperties
     */
    public Map<String, Object> getReplyProperties() {
        return replyProperties;
    }

    /**
     * @param replyProperties the replyProperties to set
     */
    public void setReplyProperties(Map<String, Object> replyProperties) {
        this.replyProperties = replyProperties;
    }

    /**
     * @return the replyTimeToLive
     */
    public long getReplyTimeToLive() {
        return replyTimeToLive;
    }

    /**
     * @param replyTimeToLive the replyTimeToLive to set
     */
    public void setReplyTimeToLive(long replyTimeToLive) {
        this.replyTimeToLive = replyTimeToLive;
    }

    /**
     * @return the useMessageIdInResponse
     */
    public Boolean getUseMessageIdInResponse() {
        return useMessageIdInResponse;
    }

    /**
     * @param useMessageIdInResponse the useMessageIdInResponse to set
     */
    public void setUseMessageIdInResponse(Boolean useMessageIdInResponse) {
        this.useMessageIdInResponse = useMessageIdInResponse;
    }

    /**
     * @return the connectionFactory
     */
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * @param connectionFactory the connectionFactory to set
     */
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * @return the pubSubDomain
     */
    public boolean isPubSubDomain() {
        return pubSubDomain;
    }

    /**
     * @param pubSubDomain the pubSubDomain to set
     */
    public void setPubSubDomain(boolean pubSubDomain) {
        this.pubSubDomain = pubSubDomain;
    }

    /**
     * @return the destinationResolver
     */
    public DestinationResolver getDestinationResolver() {
        return destinationResolver;
    }

    /**
     * @param destinationResolver the destinationResolver to set
     */
    public void setDestinationResolver(DestinationResolver destinationResolver) {
        this.destinationResolver = destinationResolver;
    }

    /**
     * @return the marshaler
     */
    public JmsConsumerMarshaler getMarshaler() {
        return marshaler;
    }

    /**
     * @param marshaler the marshaler to set
     */
    public void setMarshaler(JmsConsumerMarshaler marshaler) {
        this.marshaler = marshaler;
    }

    /**
     * @return the synchronous
     */
    public boolean isSynchronous() {
        return synchronous;
    }

    /**
     * @param synchronous the synchronous to set
     */
    public void setSynchronous(boolean synchronous) {
        this.synchronous = synchronous;
    }

    public boolean isStateless() {
        return stateless;
    }

    public void setStateless(boolean stateless) {
        this.stateless = stateless;
    }

    public Store getStore() {
        return store;
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public StoreFactory getStoreFactory() {
        return storeFactory;
    }

    public void setStoreFactory(StoreFactory storeFactory) {
        this.storeFactory = storeFactory;
    }

    public String getLocationURI() {
        // TODO: Need to return a real URI
        return getService() + "#" + getEndpoint();
    }
    
    public synchronized void start() throws Exception {
        super.start();
        if (template == null) {
            if (isJms102()) {
                template = new JmsTemplate102(getConnectionFactory(), isPubSubDomain());
            } else {
                template = new JmsTemplate(getConnectionFactory());
            }
        }
        if (store == null && !stateless) {
            if (storeFactory == null) {
                storeFactory = new MemoryStoreFactory();
            }
            store = storeFactory.open(getService().toString() + getEndpoint());
        }
    }

    public synchronized void stop() throws Exception {
        if (store != null) {
            if (storeFactory != null) {
                storeFactory.close(store);
            }
            store = null;
        }
        super.stop();
    }

    public void process(MessageExchange exchange) throws Exception {
        JmsContext context;
        if (stateless) {
            context = (JmsContext) exchange.getProperty(PROP_JMS_CONTEXT);
        } else {
            context = (JmsContext) store.load(exchange.getExchangeId());
        }
        processExchange(exchange, null, context);
    }

    protected void processExchange(final MessageExchange exchange, final Session session, final JmsContext context) throws Exception {
        // Ignore DONE exchanges
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            return;
        }
        // Create session if needed
        if (session == null) {
            template.execute(new SessionCallback() {
                public Object doInJms(Session session) throws JMSException {
                    try {
                        processExchange(exchange, session, context);
                    } catch (Exception e) {
                        throw new ListenerExecutionFailedException("Exchange processing failed", e);
                    }
                    return null;
                }
            });
            return;
        }
        // Handle exchanges
        Message msg = null;
        Destination dest = null;
        if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            if (exchange.getFault() != null) {
                msg = marshaler.createFault(exchange, exchange.getFault(), session, context);
                dest = getReplyDestination(exchange, exchange.getFault(), session, context);
            } else if (exchange.getMessage("out") != null) {
                msg = marshaler.createOut(exchange, exchange.getMessage("out"), session, context);
                dest = getReplyDestination(exchange, exchange.getMessage("out"), session, context);
            }
            if (msg == null) {
                throw new IllegalStateException("Unable to send back answer or fault");
            }
            setCorrelationId(context.getMessage(), msg);
            try {
                send(msg, session, dest);
                done(exchange);
            } catch (Exception e) {
                fail(exchange, e);
                throw e;
            }
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            Exception error = exchange.getError();
            if (error == null) {
                error = new JBIException("Exchange in ERROR state, but no exception provided");
            }
            msg = marshaler.createError(exchange, error, session, context);
            dest = getReplyDestination(exchange, error, session, context);
            setCorrelationId(context.getMessage(), msg);
            send(msg, session, dest);
        } else {
            throw new IllegalStateException("Unrecognized exchange status");
        }
    }

    protected void send(Message msg, Session session, Destination dest) throws JMSException {
        MessageProducer producer;
        if (isJms102()) {
            if (isPubSubDomain()) {
                producer = ((TopicSession) session).createPublisher((Topic) dest);
            } else {
                producer = ((QueueSession) session).createSender((Queue) dest);
            }
        } else {
            producer = session.createProducer(dest);
        }
        try {
            if (replyProperties != null) {
                for (Map.Entry<String, Object> e : replyProperties.entrySet()) {
                    msg.setObjectProperty(e.getKey(), e.getValue());
                }
            }
            if (isJms102()) {
                if (isPubSubDomain()) {
                    if (replyExplicitQosEnabled) {
                        ((TopicPublisher) producer).publish(msg, replyDeliveryMode, replyPriority, replyTimeToLive);
                    } else {
                        ((TopicPublisher) producer).publish(msg);
                    }
                } else {
                    if (replyExplicitQosEnabled) {
                        ((QueueSender) producer).send(msg, replyDeliveryMode, replyPriority, replyTimeToLive);
                    } else {
                        ((QueueSender) producer).send(msg);
                    }
                }
            } else {
                if (replyExplicitQosEnabled) {
                    producer.send(msg, replyDeliveryMode, replyPriority, replyTimeToLive);
                } else {
                    producer.send(msg);
                }
            }
        } finally {
            JmsUtils.closeMessageProducer(producer);
        }
    }

    protected void onMessage(Message jmsMessage, Session session) throws JMSException {
        if (logger.isTraceEnabled()) {
            logger.trace("Received: " + jmsMessage);
        }
        try {
            JmsContext context = marshaler.createContext(jmsMessage);
            MessageExchange exchange = marshaler.createExchange(context, getContext());
            configureExchangeTarget(exchange);
            if (synchronous) {
                try {
                    sendSync(exchange);
                } catch (Exception e) {
                    handleException(exchange, e, session, context);
                }
                if (exchange.getStatus() != ExchangeStatus.DONE) {
                    processExchange(exchange, session, context);
                }
            } else {
                if (stateless) {
                    exchange.setProperty(PROP_JMS_CONTEXT, context);
                } else {
                    store.store(exchange.getExchangeId(), context);
                }
                boolean success = false;
                try {
                    send(exchange);
                    success = true;
                } catch (Exception e) {
                    handleException(exchange, e, session, context);
                } finally {
                    if (!success && !stateless) {
                        store.load(exchange.getExchangeId());
                    }
                }
            }
        } catch (JMSException e) {
            throw e;
        } catch (Exception e) {
            throw (JMSException) new JMSException("Error sending JBI exchange").initCause(e);
        }
    }

    protected Destination getReplyDestination(
            MessageExchange exchange, Object message, Session session, JmsContext context) throws JMSException {
        // If a JMS ReplyTo property is set, use it
        if (context.getMessage().getJMSReplyTo() != null) {
            return context.getMessage().getJMSReplyTo();
        }
        Object dest = null;
        // Let the destinationChooser a chance to choose the destination 
        if (destinationChooser != null) {
            dest = destinationChooser.chooseDestination(exchange, message);
        }
        // Default to replyDestination / replyDestinationName properties
        if (dest == null) {
            dest = replyDestination;
        }
        if (dest == null) {
            dest = replyDestinationName;
        }
        // Resolve destination if needed
        if (dest instanceof Destination) {
            return (Destination) dest;
        } else if (dest instanceof String) {
            return destinationResolver.resolveDestinationName(session, 
                                                              (String) dest, 
                                                              isPubSubDomain());
        }
        throw new IllegalStateException("Unable to choose destination for exchange " + exchange);
    }

    protected void setCorrelationId(Message query, Message reply) throws Exception {
        if (useMessageIdInResponse == null) {
            if (query.getJMSCorrelationID() != null) {
                reply.setJMSCorrelationID(query.getJMSCorrelationID());
            } else if (query.getJMSMessageID() != null) {
                reply.setJMSCorrelationID(query.getJMSMessageID());
            } else {
                throw new IllegalStateException("No JMSCorrelationID or JMSMessageID set on query message");
            }
        } else if (useMessageIdInResponse.booleanValue()) {
            if (query.getJMSMessageID() != null) {
                reply.setJMSCorrelationID(query.getJMSMessageID());
            } else {
                throw new IllegalStateException("No JMSMessageID set on query message");
            }
        } else {
            if (query.getJMSCorrelationID() != null) {
                reply.setJMSCorrelationID(query.getJMSCorrelationID());
            } else {
                throw new IllegalStateException("No JMSCorrelationID set on query message");
            }
        }
    }
    
    protected void handleException(MessageExchange exchange, 
                                 Exception error, 
                                 Session session, 
                                 JmsContext context) throws Exception {
        // For InOnly, the consumer does not expect any response back, so
        // just rethrow it and let the fault behavior
        if (exchange instanceof InOnly) {
            throw error;
        }
        // Check if the exception should lead to an error back
        if (treatExceptionAsFault(error)) {
            sendError(exchange, error, session, context);
        } else {
            throw error;
        }
    }
    
    protected boolean treatExceptionAsFault(Exception error) {
        return error instanceof SecurityException;
    }

    protected void sendError(final MessageExchange exchange, 
                             final Exception error, 
                             Session session, 
                             final JmsContext context) throws Exception {
        // Create session if needed
        if (session == null) {
            template.execute(new SessionCallback() {
                public Object doInJms(Session session) throws JMSException {
                    try {
                        sendError(exchange, error, session, context);
                    } catch (Exception e) {
                        throw new ListenerExecutionFailedException("Exchange processing failed", e);
                    }
                    return null;
                }
            });
            return;
        }
        Message msg = marshaler.createError(exchange, error, session, context);
        Destination dest = getReplyDestination(exchange, error, session, context);
        setCorrelationId(context.getMessage(), msg);
        send(msg, session, dest);
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

}
