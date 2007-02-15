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

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.jms.JmsEndpointType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.JmsTemplate102;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.support.destination.DestinationResolver;

/**
 * 
 * @author gnodet
 * @org.apache.xbean.XBean element="provider"
 * @since 3.2
 */
public class JmsProviderEndpoint extends ProviderEndpoint implements JmsEndpointType {

    private JmsProviderMarshaler marshaler = new DefaultProviderMarshaler();
    private DestinationChooser destinationChooser = new SimpleDestinationChooser();
    private JmsTemplate template;

    private boolean jms102 = false;
    private ConnectionFactory connectionFactory;
    private boolean pubSubDomain = false;
    private DestinationResolver destinationResolver;
    private Destination destination;
    private String destinationName;
    private boolean messageIdEnabled = true;
    private boolean messageTimestampEnabled = true;
    private boolean pubSubNoLocal = false;
    private long receiveTimeout = JmsTemplate.DEFAULT_RECEIVE_TIMEOUT;
    private boolean explicitQosEnabled = false;
    private int deliveryMode = Message.DEFAULT_DELIVERY_MODE;
    private int priority = Message.DEFAULT_PRIORITY;
    private long timeToLive = Message.DEFAULT_TIME_TO_LIVE;

    /**
     * @return the destination
     */
    public Destination getDestination() {
        return destination;
    }

    /**
     * @param destination the destination to set
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
     */
    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
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
     * @return the deliveryMode
     */
    public int getDeliveryMode() {
        return deliveryMode;
    }

    /**
     * @param deliveryMode the deliveryMode to set
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
     * @param destinationChooser the destinationChooser to set
     */
    public void setDestinationChooser(DestinationChooser destinationChooser) {
        if (destinationChooser == null) {
            throw new NullPointerException("destinationChooser is null");
        }
        this.destinationChooser = destinationChooser;
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
     * @return the explicitQosEnabled
     */
    public boolean isExplicitQosEnabled() {
        return explicitQosEnabled;
    }

    /**
     * @param explicitQosEnabled the explicitQosEnabled to set
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
     * @param marshaler the marshaler to set
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
     * @param messageIdEnabled the messageIdEnabled to set
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
     * @param messageTimestampEnabled the messageTimestampEnabled to set
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
     * @param priority the priority to set
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
     * @param pubSubDomain the pubSubDomain to set
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
     * @param pubSubNoLocal the pubSubNoLocal to set
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
     * @param timeToLive the timeToLive to set
     */
    public void setTimeToLive(long timeToLive) {
        this.timeToLive = timeToLive;
    }

    protected void processInOnly(final MessageExchange exchange, final NormalizedMessage in) throws Exception {
        MessageCreator creator = new MessageCreator() {
            public Message createMessage(Session session) throws JMSException {
                try {
                    Message message = marshaler.createMessage(exchange, in, session);
                    if (logger.isTraceEnabled()) {
                        logger.trace("Sending message to: " + template.getDefaultDestinationName() + " message: " + message);
                    }
                    return message;
                }
                catch (Exception e) {
                    JMSException jmsEx =  new JMSException("Failed to create JMS Message: " + e);
                    jmsEx.setLinkedException(e);
                    jmsEx.initCause(e);
                    throw jmsEx;
                }
            }
        };
        Object dest = destinationChooser.chooseDestination(exchange, in);
        if (dest instanceof Destination) {
            template.send((Destination) dest, creator);
        } else if (dest instanceof String) {
            template.send((String) dest, creator);
        } else {
            template.send(creator);
        }
    }

    protected void processInOut(MessageExchange exchange, NormalizedMessage in) throws Exception {
        
    }
    
    public synchronized void start() throws Exception {
        template = createTemplate();
        super.start();
    }
    
    public synchronized void stop() throws Exception {
        super.stop();
    }
    
    public void validate() throws DeploymentException {
        // TODO: check service, endpoint
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

    protected JmsTemplate createTemplate() {
        JmsTemplate template;
        if (isJms102()) {
            template = new JmsTemplate102();
        } else {
            template = new JmsTemplate();
        }
        template.setConnectionFactory(getConnectionFactory());
        if (getDestination() != null) {
            template.setDefaultDestination(getDestination());
        } else if (getDestinationName() != null) {
            template.setDefaultDestinationName(getDestinationName());
        }
        template.setDeliveryMode(getDeliveryMode());
        if (getDestinationResolver() != null) {
            template.setDestinationResolver(getDestinationResolver());
        }
        template.setExplicitQosEnabled(isExplicitQosEnabled());
        template.setMessageIdEnabled(isMessageIdEnabled());
        template.setMessageTimestampEnabled(isMessageTimestampEnabled());
        template.setPriority(getPriority());
        template.setPubSubDomain(isPubSubDomain());
        template.setPubSubNoLocal(isPubSubNoLocal());
        template.setTimeToLive(getTimeToLive());
        template.setReceiveTimeout(getReceiveTimeout());
        return template;
    }
}
