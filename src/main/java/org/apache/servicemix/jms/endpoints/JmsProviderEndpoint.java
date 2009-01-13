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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.Session;

import org.apache.servicemix.common.JbiConstants;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.jms.JmsEndpointType;
import org.apache.servicemix.store.Store;
import org.apache.servicemix.store.StoreFactory;
import org.apache.servicemix.store.memory.MemoryStoreFactory;
import org.springframework.jms.JmsException;
import org.springframework.jms.UncategorizedJmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.JmsTemplate102;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.SessionCallback;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer102;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;

/**
 * A Spring-based JMS provider endpoint
 *
 * @author gnodet
 * @org.apache.xbean.XBean element="provider"
 * @since 3.2
 */
public class JmsProviderEndpoint extends ProviderEndpoint implements JmsEndpointType {

    private static final String MSG_SELECTOR_START = "JMSCorrelationID='";    
    private static final String MSG_SELECTOR_END = "'";

    private JmsProviderMarshaler marshaler = new DefaultProviderMarshaler();
    private DestinationChooser destinationChooser = new SimpleDestinationChooser();
    private DestinationChooser replyDestinationChooser = new SimpleDestinationChooser();
    private JmsTemplate template;

    private boolean jms102;
    private ConnectionFactory connectionFactory;
    private boolean pubSubDomain;
    private DestinationResolver destinationResolver = new DynamicDestinationResolver();
    private Destination destination;
    private String destinationName;
    private boolean messageIdEnabled = true;
    private boolean messageTimestampEnabled = true;
    private boolean pubSubNoLocal;
    private long receiveTimeout = JmsTemplate.RECEIVE_TIMEOUT_INDEFINITE_WAIT;
    private boolean explicitQosEnabled;
    private int deliveryMode = Message.DEFAULT_DELIVERY_MODE;
    private int priority = Message.DEFAULT_PRIORITY;
    private long timeToLive = Message.DEFAULT_TIME_TO_LIVE;

    private Destination replyDestination;
    private String replyDestinationName;

    private StoreFactory storeFactory;
    private Store store;

    private AbstractMessageListenerContainer listenerContainer;

    /**
     * @return the destination
     */
    public Destination getDestination() {
        return destination;
    }

    /**
    * Specifies the JMS <code>Destination</code> used to send messages.
    *
     * @param destination the destination
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
    * Specifies a string identifying the JMS destination used to send 
     * messages. The destination is resolved using the 
     * <code>DesitinationResolver</code>.
     *
     * @param destinationName the destination name
     */
    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    /**
    * Determines if the provider used JMS 1.0.2 compliant APIs.
    *
    * @return <code>true</code> if the provider is JMS 1.0.2 compliant
    */
    public boolean isJms102() {
        return jms102;
    }

    /**
    * Specifies if the provider uses JMS 1.0.2 compliant APIs. Defaults to 
    * <code>false</code>.
    * 
     * @param jms102 provider is JMS 1.0.2 compliant?
     */
    public void setJms102(boolean jms102) {
        this.jms102 = jms102;
    }

    /**
     * @return the connectionFactory
     */
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
    * Specifies the <code>ConnectionFactory</code> used by the endpoint.
    *
     * @param connectionFactory the connectionFactory to set
     */
    public void setConnectionFactory(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    /**
     * @return the deliveryMode
     */
    public int getDeliveryMode() {
        return deliveryMode;
    }

    /**
    * Specifies the JMS delivery mode used for the reply. Defaults to 
    * (2)(<code>PERSISTENT</code>).
    *
     * @param deliveryMode the JMS delivery mode
     */
    public void setDeliveryMode(int deliveryMode) {
        this.deliveryMode = deliveryMode;
    }

    /**
     * @return the destinationChooser
     */
    public DestinationChooser getDestinationChooser() {
        return destinationChooser;
    }

    /**
    * Specifies a class implementing logic for choosing the destination used 
    * to send messages.
    *
     * @param destinationChooser the destination chooser for sending messages
     */
    public void setDestinationChooser(DestinationChooser destinationChooser) {
        if (destinationChooser == null) {
            throw new NullPointerException("destinationChooser is null");
        }
        this.destinationChooser = destinationChooser;
    }

    /**
     * @return the destination chooser for the reply destination
     */
    public DestinationChooser getReplyDestinationChooser() {
        return replyDestinationChooser;
    }

    /**
    * Specifies a class implementing logic for choosing the destination used 
    * to recieve replies.
    *
     * @param replyDestinationChooser the destination chooser used for the reply destination
     */
    public void setReplyDestinationChooser(DestinationChooser replyDestinationChooser) {
        this.replyDestinationChooser = replyDestinationChooser;
    }
    /**
     * @return the destinationResolver
     */
    public DestinationResolver getDestinationResolver() {
        return destinationResolver;
    }

    /**
    * Specifies the class implementing logic for converting strings into 
    * destinations. The default is <code>DynamicDestinationResolver</code>.
    *
     * @param destinationResolver the destination resolver implementation
     */
    public void setDestinationResolver(DestinationResolver destinationResolver) {
        this.destinationResolver = destinationResolver;
    }

    /**
     * @return the explicitQosEnabled
     */
    public boolean isExplicitQosEnabled() {
        return explicitQosEnabled;
    }

    /**
    * Specifies if the QoS values specified for the endpoint are explicitly 
    * used when a messages is sent. The default is <code>false</code>.
    *
     * @param explicitQosEnabled should the QoS values be sent?
     */
    public void setExplicitQosEnabled(boolean explicitQosEnabled) {
        this.explicitQosEnabled = explicitQosEnabled;
    }

    /**
     * @return the marshaler
     */
    public JmsProviderMarshaler getMarshaler() {
        return marshaler;
    }

    /**
    * Specifies the class implementing the message marshaler. The message 
    * marshaller is responsible for marshalling and unmarshalling JMS messages. 
    * The default is <code>DefaultProviderMarshaler</code>.
    *
     * @param marshaler the marshaler implementation
     */
    public void setMarshaler(JmsProviderMarshaler marshaler) {
        if (marshaler == null) {
            throw new NullPointerException("marshaler is null");
        }
        this.marshaler = marshaler;
    }

    /**
     * @return the messageIdEnabled
     */
    public boolean isMessageIdEnabled() {
        return messageIdEnabled;
    }

    /**
    * Specifies if your endpoint requires JMS message IDs. Setting the 
    * <code>messageIdEnabled</code> property to <code>false</code> causes the 
    * endpoint to call its message producer's 
    * <code>setDisableMessageID() </code> with a value of <code>true</code>. 
    * The JMS broker is then given a hint that it does not need to generate 
    * message IDs or add them to the messages from the endpoint. The JMS 
    * broker can choose to accept the hint or ignore it.
    * 
     * @param messageIdEnabled the endpoint requires message IDs?
     */
    public void setMessageIdEnabled(boolean messageIdEnabled) {
        this.messageIdEnabled = messageIdEnabled;
    }

    /**
     * @return the messageTimestampEnabled
     */
    public boolean isMessageTimestampEnabled() {
        return messageTimestampEnabled;
    }

    /**
    * Specifies if your endpoints requires time stamps on its messages. 
    * Setting the <code>messageTimeStampEnabled</code> property to 
    * <code>false</code> causes the endpoint to call its message producer's 
    * <code>setDisableMessageTimestamp() </code> method with a value of 
    * <code>true</code>. The JMS broker is then given a hint that it does not 
    * need to generate message IDs or add them to the messages from the 
    * endpoint. The JMS broker can choose to accept the hint or ignore it.
    * 
     * @param messageTimestampEnabled the endpoint requires time stamps?
     */
    public void setMessageTimestampEnabled(boolean messageTimestampEnabled) {
        this.messageTimestampEnabled = messageTimestampEnabled;
    }

    /**
     * @return the priority
     */
    public int getPriority() {
        return priority;
    }

    /**
    * Specifies the priority assigned to the JMS messages. Defaults to 4.
    *
     * @param priority the message priority
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * @return the pubSubDomain
     */
    public boolean isPubSubDomain() {
        return pubSubDomain;
    }

    /**
    * Specifies if the destination is a topic. <code>true</code> means the 
    * destination is a topic. <code>false</code> means the destination is a 
    * queue.
    *
     * @param pubSubDomain the destination is a topic?
     */
    public void setPubSubDomain(boolean pubSubDomain) {
        this.pubSubDomain = pubSubDomain;
    }

    /**
     * @return the pubSubNoLocal
     */
    public boolean isPubSubNoLocal() {
        return pubSubNoLocal;
    }

    /**
    * Specifies if messages published by the listener's <code>Connection</code> 
    * are suppressed. The default is <code>false</code>.
    *
     * @param pubSubNoLocal messages are surpressed?
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
    * Specifies the timeout for receiving a message in milliseconds.
    *
     * @param receiveTimeout milliseconds to wait
     */
    public void setReceiveTimeout(long receiveTimeout) {
        this.receiveTimeout = receiveTimeout;
    }

    /**
     * @return the timeToLive
     */
    public long getTimeToLive() {
        return timeToLive;
    }

    /**
    * Specifies the number of milliseconds a message is valid.
    *
     * @param timeToLive number of milliseonds a message lives
     */
    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    public StoreFactory getStoreFactory() {
        return storeFactory;
    }

    /**
     * Sets the store factory used to create the store.
     * If none is set, a {@link MemoryStoreFactory} will be created and used instead.
     *
     * @param storeFactory the factory
     */
    public void setStoreFactory(StoreFactory storeFactory) {
        this.storeFactory = storeFactory;
    }

    public Store getStore() {
        return store;
    }

    /**
     * Sets the store used to store JBI exchanges that are waiting for a response
     * JMS message.  The store will be automatically created if not set.
     *
     * @param store the store
     */
    public void setStore(Store store) {
        this.store = store;
    }

    public Destination getReplyDestination() {
        return replyDestination;
    }

    /**
     * Sets the reply destination.
     * This JMS destination will be used as the default destination for the response
     * messages when using an InOut JBI exchange.  It will be used if the
     * <code>replyDestinationChooser</code> does not return any value.
     *
     * @param replyDestination
     */
    public void setReplyDestination(Destination replyDestination) {
        this.replyDestination = replyDestination;
    }

    public String getReplyDestinationName() {
        return replyDestinationName;
    }

    /**
     * Sets the name of the reply destination.
     * This property will be used to create the <code>replyDestination</code>
     * using the <code>destinationResolver</code> when the endpoint starts if
     * the <code>replyDestination</code> has not been set.
     *
     * @param replyDestinationName
     */
    public void setReplyDestinationName(String replyDestinationName) {
        this.replyDestinationName = replyDestinationName;
    }

    /**
     * Process the incoming JBI exchange
     * @param exchange
     * @throws Exception
     */
    public void process(MessageExchange exchange) throws Exception {
        // The component acts as a provider, this means that another component has requested our service
        // As this exchange is active, this is either an in or a fault (out are sent by this component)
        if (exchange.getRole() == MessageExchange.Role.PROVIDER) {
            // Exchange is finished
            if (exchange.getStatus() == ExchangeStatus.DONE) {
                return;
            // Exchange has been aborted with an exception
            } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
                return;
            // Exchange is active
            } else {
                // Fault message
                if (exchange.getFault() != null) {
                    done(exchange);
                // In message
                } else {
                    NormalizedMessage in = exchange.getMessage("in");
                    if (in != null) {
                        if (exchange instanceof InOnly) {
                            processInOnly(exchange, in);
                            done(exchange);
                        } else {
                            processInOut(exchange, in);
                        }
                    // This is not compliant with the default MEPs
                    } else {
                        throw new IllegalStateException("Provider exchange is ACTIVE, but no in or fault is provided");
                    }
                }
            }
        // Unsupported role: this should never happen has we never create exchanges
        } else {
            throw new IllegalStateException("Unsupported role: " + exchange.getRole());
        }
    }

    /**
     * Process an InOnly or RobustInOnly exchange.
     * This method uses the JMS template to create a session and call the
     * {@link #processInOnlyInSession(javax.jbi.messaging.MessageExchange, javax.jbi.messaging.NormalizedMessage, javax.jms.Session)}
     * method.
     *
     * @param exchange
     * @param in
     * @throws Exception
     */
    protected void processInOnly(final MessageExchange exchange,
                                 final NormalizedMessage in) throws Exception {
        SessionCallback callback = new SessionCallback() {
            public Object doInJms(Session session) throws JMSException {
                try {
                    processInOnlyInSession(exchange, in, session);
                    return null;
                } catch (JMSException e) {
                    throw e;
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new UncategorizedJmsException(e);
                }
            }
        };
        template.execute(callback, true);
    }

    /**
     * Process an InOnly or RobustInOnly exchange inside a JMS session.
     * This method delegates the JMS message creation to the marshaler and uses
     * the JMS template to send it.
     *
     * @param exchange
     * @param in
     * @param session
     * @throws Exception
     */
    protected void processInOnlyInSession(final MessageExchange exchange,
                                          final NormalizedMessage in,
                                          final Session session) throws Exception {
        // Create destination
        final Destination dest = getDestination(exchange, in, session);
        // Create message and send it
        final Message message = marshaler.createMessage(exchange, in, session);
        template.send(dest, new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                return message;
            }
        });
    }

    /**
     * Process an InOut or InOptionalOut exchange.
     * This method uses the JMS template to create a session and call the
     * {@link #processInOutInSession(javax.jbi.messaging.MessageExchange, javax.jbi.messaging.NormalizedMessage, javax.jms.Session)}
     * method.
     *
     * @param exchange
     * @param in
     * @throws Exception
     */
    protected void processInOut(final MessageExchange exchange,
                                final NormalizedMessage in) throws Exception {
        SessionCallback callback = new SessionCallback() {
            public Object doInJms(Session session) throws JMSException {
                try {
                    processInOutInSession(exchange, in, session);
                    return null;
                } catch (JMSException e) {
                    throw e;
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new UncategorizedJmsException(e);
                }
            }
        };
        template.execute(callback, true);
    }
    
    /**
     * Process an InOnly or RobustInOnly exchange inside a JMS session.
     * This method delegates the JMS message creation to the marshaler and uses
     * the JMS template to send it.  If the JMS destination that was used to send
     * the message is not the default one, it synchronously wait for the message
     * to come back using a JMS selector.  Else, it just returns and the response
     * message will come back from the listener container.
     *
     * @param exchange
     * @param in
     * @param session
     * @throws Exception
     */
    protected void processInOutInSession(final MessageExchange exchange,
                                         final NormalizedMessage in,
                                         final Session session) throws Exception {
        // Create destinations
        final Destination dest = getDestination(exchange, in, session);
        final Destination replyDest = getReplyDestination(exchange, in, session);
        // Create message and send it
        final Message sendJmsMsg = marshaler.createMessage(exchange, in, session);
        sendJmsMsg.setJMSReplyTo(replyDest);
        // handle correlation ID
        String correlationId = sendJmsMsg.getJMSMessageID() != null ? sendJmsMsg.getJMSMessageID() : exchange.getExchangeId(); 
        sendJmsMsg.setJMSCorrelationID(correlationId);

        boolean asynchronous = replyDest.equals(replyDestination);

        if (asynchronous) {
            createAndStartListener();
            store.store(correlationId, exchange);
        }

        try {
            send(session, dest, sendJmsMsg);
        } catch (Exception e) {
            if (asynchronous) {
                store.load(exchange.getExchangeId());
            }
            throw e;
        }

        if (!asynchronous) {
            // Create selector
            String selector = MSG_SELECTOR_START + sendJmsMsg.getJMSCorrelationID() + MSG_SELECTOR_END;
            // Receiving JMS Message, Creating and Returning NormalizedMessage out
            Message receiveJmsMsg = receiveSelected(session, replyDest, selector);
            if (receiveJmsMsg == null) {
                throw new IllegalStateException("Unable to receive response");
            }
            if (receiveJmsMsg.getBooleanProperty(AbstractJmsMarshaler.DONE_JMS_PROPERTY)) {
                exchange.setStatus(ExchangeStatus.DONE);
            } else if (receiveJmsMsg.getBooleanProperty(AbstractJmsMarshaler.ERROR_JMS_PROPERTY)) {
                Exception e = (Exception) ((ObjectMessage) receiveJmsMsg).getObject();
                exchange.setError(e);
                exchange.setStatus(ExchangeStatus.ERROR);
            } else if (receiveJmsMsg.getBooleanProperty(AbstractJmsMarshaler.FAULT_JMS_PROPERTY)) {
                Fault fault = exchange.getFault();
                if (fault == null) {
                    fault = exchange.createFault();
                    exchange.setFault(fault);
                }
                marshaler.populateMessage(receiveJmsMsg, exchange, fault);
            } else {
                NormalizedMessage out = exchange.getMessage("out");
                if (out == null) {
                    out = exchange.createMessage();
                    exchange.setMessage(out, "out");
                }
                marshaler.populateMessage(receiveJmsMsg, exchange, out);
            }
            boolean txSync = exchange.getStatus() == ExchangeStatus.ACTIVE
                                && exchange.isTransacted()
                                && Boolean.TRUE.equals(exchange.getProperty(JbiConstants.SEND_SYNC));
            if (txSync) {
                sendSync(exchange);
            } else {
                send(exchange);
            }
        }
    }

    private void send(final Session session, final Destination dest, final Message message) throws JmsException {
        // Do not call directly the template to avoid the cost of creating a new connection / session
//        template.send(dest, new MessageCreator() {
//            public Message createMessage(Session session) throws JMSException {
//                return message;
//            }
//        });
        try {
            Method method = JmsTemplate.class.getDeclaredMethod("doSend", Session.class, Destination.class, MessageCreator.class);
            method.setAccessible(true);
            method.invoke(template, session, dest, new MessageCreator() {
                public Message createMessage(Session session) throws JMSException {
                    return message;
                }
            });
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    private Message receiveSelected(final Session session, 
                                    final Destination dest, 
                                    final String messageSelector) throws JMSException {
        // Do not call directly the template to avoid the cost of creating a new connection / session
//        return template.doReceive(session, dest, messageSelector);
        try {
            Method method = JmsTemplate.class.getDeclaredMethod("doReceive", Session.class, Destination.class, String.class);
            method.setAccessible(true);
            return (Message) method.invoke(template, session, dest, messageSelector);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Process a JMS response message.
     * This method delegates to the marshaler for the JBI out message creation
     * and sends it in to the NMR.
     * 
     * @param message
     */
    protected void onMessage(Message message) {
        MessageExchange exchange = null;
        try {
            exchange = (InOut) store.load(message.getJMSCorrelationID());
            if (exchange == null) {
                throw new IllegalStateException("Could not find exchange " + message.getJMSCorrelationID());
            }
        } catch (Exception e) {
            logger.error("Unable to load exchange related to incoming JMS message " + message, e);
        }
        try {
            if (message.getBooleanProperty(AbstractJmsMarshaler.DONE_JMS_PROPERTY)) {
                exchange.setStatus(ExchangeStatus.DONE);
            } else if (message.getBooleanProperty(AbstractJmsMarshaler.ERROR_JMS_PROPERTY)) {
                Exception e = (Exception) ((ObjectMessage) message).getObject();
                exchange.setError(e);
                exchange.setStatus(ExchangeStatus.ERROR);
            } else if (message.getBooleanProperty(AbstractJmsMarshaler.FAULT_JMS_PROPERTY)) {
                Fault fault = exchange.getFault();
                if (fault == null) {
                    fault = exchange.createFault();
                    exchange.setFault(fault);
                }
                marshaler.populateMessage(message, exchange, fault);
            } else {
                NormalizedMessage out = exchange.getMessage("out");
                if (out == null) {
                    out = exchange.createMessage();
                    exchange.setMessage(out, "out");
                }
                marshaler.populateMessage(message, exchange, out);
            }
        } catch (Exception e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Error while populating JBI exchange " + exchange, e);
            }
            exchange.setError(e);
        }
        try {
            boolean txSync = exchange.getStatus() == ExchangeStatus.ACTIVE
                                && exchange.isTransacted()
                                && Boolean.TRUE.equals(exchange.getProperty(JbiConstants.SEND_SYNC));
            if (txSync) {
                sendSync(exchange);
            } else {
                send(exchange);
            }
        } catch (Exception e) {
            logger.error("Unable to send JBI exchange " + exchange, e);
        }
    }

    /**
     * Retrieve the destination where the JMS message should be sent to.
     *
     * @param exchange
     * @param message
     * @param session
     * @return
     * @throws JMSException
     */
    protected Destination getDestination(MessageExchange exchange, Object message, Session session) throws JMSException {
        Destination dest = chooseDestination(exchange, message, session, destinationChooser, 
                                             destination != null ? destination : destinationName);
        if (dest == null) {
            throw new IllegalStateException("Unable to choose a destination for exchange " + exchange);
        }
        return dest;
    }

    /**
     * Choose the JMS destination for the reply message.
     * If no default destination is specified or can be extracted from the JBI exchange,
     * a temporary destination will be created.
     *
     * @param exchange
     * @param message
     * @param session
     * @return
     * @throws JMSException
     */
    protected Destination getReplyDestination(MessageExchange exchange, Object message, Session session) throws JMSException {
        Destination dest = chooseDestination(exchange, message, session, replyDestinationChooser, 
                                             replyDestination != null ? replyDestination : replyDestinationName);
        if (dest == null) {
            if (isPubSubDomain()) {
                return session.createTemporaryQueue();
            } else {
                return session.createTemporaryTopic();
            }
        } else {
            return dest;
        }
    }

    /**
     * Choose a JMS destination given the chooser, a default destination and name
     * @param exchange
     * @param message
     * @param session
     * @param chooser
     * @param defaultDestination
     * @return
     * @throws JMSException
     */
    protected Destination chooseDestination(MessageExchange exchange,
                                            Object message,
                                            Session session,
                                            DestinationChooser chooser,
                                            Object defaultDestination) throws JMSException {
        Object dest = null;
        // Let the replyDestinationChooser a chance to choose the destination
        if (chooser != null) {
            dest = chooser.chooseDestination(exchange, message);
        }
        // Default to defaultDestination properties
        if (dest == null) {
            dest = defaultDestination;
        }
        // Resolve destination if needed
        if (dest instanceof Destination) {
            return (Destination) dest;
        } else if (dest instanceof String) {
            return destinationResolver.resolveDestinationName(session, 
                                                              (String) dest, 
                                                              isPubSubDomain());
        }
        return null;
    }

    /**
     * Start this endpoint.
     *
     * @throws Exception
     */
    public synchronized void activate() throws Exception {
        super.activate();
        if (store == null) {
            if (storeFactory == null) {
                storeFactory = new MemoryStoreFactory();
            }
            store = storeFactory.open(getService().toString() + getEndpoint());
        }
        template = createTemplate();
    }

    protected synchronized void createAndStartListener() throws Exception {
        if (listenerContainer == null) {
            // create the listener container
            listenerContainer = createListenerContainer();
            listenerContainer.start();
        }
    }

    /**
     * Stops this endpoint.
     * 
     * @throws Exception
     */
    public synchronized void deactivate() throws Exception {
        if (listenerContainer != null) {
            listenerContainer.stop();
            listenerContainer.shutdown();
            listenerContainer = null;
        }
        if (store != null) {
            if (storeFactory != null) {
                storeFactory.close(store);
            }
            store = null;
        }
        super.deactivate();
    }

    /**
     * Validate this endpoint.
     * 
     * @throws DeploymentException
     */
    public void validate() throws DeploymentException {
        super.validate();
        if (getService() == null) {
            throw new DeploymentException("service must be set");
        }
        if (getEndpoint() == null) {
            throw new DeploymentException("endpoint must be set");
        }
        if (getConnectionFactory() == null) {
            throw new DeploymentException("connectionFactory is required");
        }
    }

    /**
     * Create the JMS template to be used to send the JMS messages.
     *
     * @return
     */
    protected JmsTemplate createTemplate() {
        JmsTemplate tplt;
        if (isJms102()) {
            tplt = new JmsTemplate102();
        } else {
            tplt = new JmsTemplate();
        }
        tplt.setConnectionFactory(getConnectionFactory());
        if (getDestination() != null) {
            tplt.setDefaultDestination(getDestination());
        } else if (getDestinationName() != null) {
            tplt.setDefaultDestinationName(getDestinationName());
        }
        tplt.setDeliveryMode(getDeliveryMode());
        if (getDestinationResolver() != null) {
            tplt.setDestinationResolver(getDestinationResolver());
        }
        tplt.setExplicitQosEnabled(isExplicitQosEnabled());
        tplt.setMessageIdEnabled(isMessageIdEnabled());
        tplt.setMessageTimestampEnabled(isMessageTimestampEnabled());
        tplt.setPriority(getPriority());
        tplt.setPubSubDomain(isPubSubDomain());
        tplt.setPubSubNoLocal(isPubSubNoLocal());
        tplt.setTimeToLive(getTimeToLive());
        tplt.setReceiveTimeout(getReceiveTimeout());
        tplt.afterPropertiesSet();
        return tplt;
    }

    /**
     * Create the message listener container to receive response messages.
     * 
     * @return
     */
    protected AbstractMessageListenerContainer createListenerContainer() {
        DefaultMessageListenerContainer cont;
        if (isJms102()) {
            cont = new DefaultMessageListenerContainer102();
        } else {
            cont = new DefaultMessageListenerContainer();
        }
        cont.setConnectionFactory(getConnectionFactory());
        if (replyDestination != null) {
            cont.setDestination(replyDestination);
        }
        if (replyDestinationName != null) {
            cont.setDestinationName(replyDestinationName);
        }
        cont.setPubSubDomain(isPubSubDomain());
        cont.setPubSubNoLocal(isPubSubNoLocal());
        cont.setMessageListener(new MessageListener() {
            public void onMessage(Message message) {
                JmsProviderEndpoint.this.onMessage(message);
            }
        });
        cont.setAutoStartup(false);
        cont.afterPropertiesSet();
        return cont;
    }
    
}
