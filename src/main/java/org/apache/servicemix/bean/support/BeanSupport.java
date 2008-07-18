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
package org.apache.servicemix.bean.support;

import javax.annotation.Resource;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.management.ObjectName;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.components.util.CopyTransformer;
import org.apache.servicemix.components.util.MessageHelper;
import org.apache.servicemix.components.util.MessageTransformer;
import org.apache.servicemix.jbi.FaultException;
import org.apache.servicemix.jbi.NoInMessageAvailableException;

/**
 * A useful base class for servicemix-bean POJOs
 *
 * @version $$
 */
public abstract class BeanSupport {

    protected Log logger = LogFactory.getLog(getClass());
    
    @Resource
    private ComponentContext context;
    
    @Resource
    private DeliveryChannel channel;
    
    @Resource
    private ServiceEndpoint serviceEndpoint;
    
    private ObjectName extensionMBeanName;
    private MessageExchangeFactory exchangeFactory;
    private MessageTransformer messageTransformer = CopyTransformer.getInstance();
    
    protected BeanSupport() {
    }
    
    // Helper methods
    //-------------------------------------------------------------------------

    /**
     * A helper method to return the body of the message as a POJO which could be a
     * bean or some DOMish model of the body.
     *
     * @param message the message on which to extract the body
     * @return the body of the message as a POJO or DOM object
     * @throws MessagingException
     */
    public Object getBody(NormalizedMessage message) throws MessagingException {
        return MessageHelper.getBody(message);
    }

    /**
     * Sets the body of the message as a POJO
     *
     * @param message the message on which to set the body
     * @param body    the POJO or DOMish model to set
     * @throws MessagingException
     */
    public void setBody(NormalizedMessage message, Object body) throws MessagingException {
        MessageHelper.setBody(message, body);
    }


    // Properties
    //-------------------------------------------------------------------------
    public ObjectName getExtensionMBeanName() {
        return extensionMBeanName;
    }

    public void setExtensionMBeanName(ObjectName extensionMBeanName) {
        this.extensionMBeanName = extensionMBeanName;
    }

    public ComponentContext getContext() {
        return context;
    }
    
    public void setContext(ComponentContext context) {
        this.context = context;
    }

    public ServiceEndpoint getServiceEndpoint() {
        return serviceEndpoint;
    }
    
    public void setServiceEndpoint(ServiceEndpoint serviceEndpoint) {
        this.serviceEndpoint = serviceEndpoint;
    }
    
    /**
     * Provide access to the default message exchange exchangeFactory, lazily creating one.
     */
    public MessageExchangeFactory getExchangeFactory() throws MessagingException {
        if (exchangeFactory == null && context != null) {
            exchangeFactory = getDeliveryChannel().createExchangeFactory();
        }
        return exchangeFactory;
    }

    public DeliveryChannel getDeliveryChannel() throws MessagingException {
        if (channel == null) {
            channel = context.getDeliveryChannel();
        }
        return channel;
    }

    /**
     * A helper method to indicate that the message exchange is complete
     * which will set the status to {@link ExchangeStatus#DONE} and send the message
     * on the delivery channel.
     *
     * @param exchange
     * @throws MessagingException
     */
    public void done(MessageExchange exchange) throws MessagingException {
        exchange.setStatus(ExchangeStatus.DONE);
        getDeliveryChannel().send(exchange);
    }

    public void send(MessageExchange exchange) throws MessagingException {
        getDeliveryChannel().send(exchange);
    }
    
    public boolean sendSync(MessageExchange exchange) throws MessagingException {
        return getDeliveryChannel().sendSync(exchange);
    }

    public boolean sendSync(MessageExchange exchange, long timeMillis) throws MessagingException {
        return getDeliveryChannel().sendSync(exchange, timeMillis);
    }

    /**
     * A helper method to indicate that the message exchange should be
     * continued with the given response and send the message
     * on the delivery channel.
     *
     * @param exchange
     * @throws MessagingException
     */
    public void answer(MessageExchange exchange, NormalizedMessage answer) throws MessagingException {
        exchange.setMessage(answer, "out");
        getDeliveryChannel().send(exchange);
    }

    /**
     * A helper method which fails and completes the given exchange with the specified fault
     */
    public void fail(MessageExchange exchange, Fault fault) throws MessagingException {
        if (exchange instanceof InOnly || fault == null) {
            exchange.setError(new FaultException("Fault occured for in-only exchange", exchange, fault));
        } else {
            exchange.setFault(fault);
        }
        getDeliveryChannel().send(exchange);
    }

    /**
     * A helper method which fails and completes the given exchange with the specified error
     * @throws MessagingException 
     */
    public void fail(MessageExchange exchange, Exception error) throws MessagingException {
        if (exchange instanceof InOnly || !(error instanceof FaultException)) {
            exchange.setError(error);
        } else {
            FaultException faultException = (FaultException) error;
            exchange.setFault(faultException.getFault());
        }
        getDeliveryChannel().send(exchange);
    }


    /**
     * A helper method which will return true if the exchange is capable of both In and Out such as InOut,
     * InOptionalOut etc.
     *
     * @param exchange
     * @return true if the exchange can handle both input and output
     */
    protected boolean isInAndOut(MessageExchange exchange) {
        return exchange instanceof InOut || exchange instanceof InOptionalOut;
    }

    /**
     * Returns the in message or throws an exception if there is no in message.
     */
    protected NormalizedMessage getInMessage(MessageExchange exchange) throws NoInMessageAvailableException {
        NormalizedMessage message = exchange.getMessage("in");
        if (message == null) {
            throw new NoInMessageAvailableException(exchange);
        }
        return message;
    }

    public MessageTransformer getMessageTransformer() {
        return messageTransformer;
    }

    public void setMessageTransformer(MessageTransformer transformer) {
        this.messageTransformer = transformer;
    }

    /**
     * Performs an invocation where the service, operation or interface name could be specified
     *
     * @param exchange
     * @param in
     * @param service
     * @param interfaceName
     * @param operation
     */
    public void invoke(MessageExchange exchange, NormalizedMessage in, 
                       QName service, QName interfaceName, QName operation) throws MessagingException {
        InOnly outExchange = createInOnlyExchange(service, interfaceName, operation);
        forwardToExchange(exchange, outExchange, in, operation);
    }

    /**
     * Creates a new InOnly exchange for the given service, interface and/or operation (any of which can be null).
     */
    public InOnly createInOnlyExchange(QName service, QName interfaceName, QName operation) throws MessagingException {
        MessageExchangeFactory factory = null;
        if (service != null) {
            factory = getDeliveryChannel().createExchangeFactoryForService(service);
        } else if (interfaceName != null) {
            factory = getDeliveryChannel().createExchangeFactory(interfaceName);
        } else {
            factory = getExchangeFactory();
        }
        InOnly outExchange = factory.createInOnlyExchange();
        if (service != null) {
            outExchange.setService(service);
        }
        if (interfaceName != null) {
            outExchange.setInterfaceName(interfaceName);
        }
        if (operation != null) {
            outExchange.setOperation(operation);
        }
        return outExchange;
    }

    public InOnly createInOnlyExchange(QName service, QName interfaceName, 
                                       QName operation, MessageExchange beforeExchange) throws MessagingException {
        InOnly inOnly = createInOnlyExchange(service, interfaceName, operation);
        propagateCorrelationId(beforeExchange, inOnly);
        return inOnly;
    }

    /**
     * Creates a new InOut exchange for the given service, interface and/or operation (any of which can be null).
     */
    public InOut createInOutExchange(QName service, QName interfaceName, QName operation) throws MessagingException {
        MessageExchangeFactory factory = null;
        if (service != null) {
            factory = getDeliveryChannel().createExchangeFactoryForService(service);
        } else if (interfaceName != null) {
            factory = getDeliveryChannel().createExchangeFactory(interfaceName);
        } else {
            factory = getExchangeFactory();
        }
        InOut outExchange = factory.createInOutExchange();
        if (service != null) {
            outExchange.setService(service);
        }
        if (interfaceName != null) {
            outExchange.setInterfaceName(interfaceName);
        }
        if (operation != null) {
            outExchange.setOperation(operation);
        }
        return outExchange;
    }

    public InOut createInOutExchange(QName service, QName interfaceName, 
                                    QName operation, MessageExchange srcExchange) throws MessagingException {
        InOut inOut = createInOutExchange(service, interfaceName, operation);
        propagateCorrelationId(srcExchange, inOut);
        return inOut;
    }

    /**
     * Creates an InOnly exchange and propagates the correlation id from the given exchange
     * to the newly created exchange
     * @param srcExchange
     * @return InOnly
     * @throws MessagingException
     */
    public InOnly createInOnlyExchange(MessageExchange srcExchange) throws MessagingException {
        MessageExchangeFactory factory = getExchangeFactory();
        InOnly inOnly = factory.createInOnlyExchange();

        propagateCorrelationId(srcExchange, inOnly);

        return inOnly;
    }

    /**
     * Creates an InOptionalOut exchange and propagates the correlation id from the given exchange
     * to the newly created exchange
     * @param srcExchange
     * @return InOptionalOut
     * @throws MessagingException
     */
    public InOptionalOut createInOptionalOutExchange(MessageExchange srcExchange) throws MessagingException {
        MessageExchangeFactory factory = getExchangeFactory();
        InOptionalOut inOptionalOut = factory.createInOptionalOutExchange();

        propagateCorrelationId(srcExchange, inOptionalOut);

        return inOptionalOut;
    }

    /**
     * Creates an InOut exchange and propagates the correlation id from the given exchange
     * to the newly created exchange
     * @param srcExchange
     * @return InOut
     * @throws MessagingException
     */
    public InOut createInOutExchange(MessageExchange srcExchange) throws MessagingException {
        MessageExchangeFactory factory = getExchangeFactory();
        InOut inOut = factory.createInOutExchange();

        propagateCorrelationId(srcExchange, inOut);

        return inOut;
    }

    /**
     * Creates an RobustInOnly exchange and propagates the correlation id from the given exchange
     * to the newly created exchange
     * @param srcExchange
     * @return RobustInOnly the created exchange
     * @throws MessagingException
     */
    public RobustInOnly createRobustInOnlyExchange(MessageExchange srcExchange) throws MessagingException {
        MessageExchangeFactory factory = getExchangeFactory();
        RobustInOnly robustInOnly = factory.createRobustInOnlyExchange();

        propagateCorrelationId(srcExchange, robustInOnly);

        return robustInOnly;
    }

    /**
     * Propagates the correlation id from an exchange to a newly created exchange
     * @param source Exchange which already exists
     * @param dest Newly created exchange which should get the correlation id
     */
    public void propagateCorrelationId(MessageExchange source, MessageExchange dest) {
        if (source == null || dest == null) {
            return;
        }
        String correlationId = (String) source.getProperty(JbiConstants.CORRELATION_ID);
        if (correlationId != null) {
            dest.setProperty(JbiConstants.CORRELATION_ID, correlationId);
        } else {
            dest.setProperty(JbiConstants.CORRELATION_ID, source.getExchangeId());
        }
    }

    protected void forwardToExchange(MessageExchange exchange, InOnly outExchange, 
                                     NormalizedMessage in, QName operationName) throws MessagingException {
        if (operationName != null) {
            exchange.setOperation(operationName);
        }
        forwardToExchange(exchange, outExchange, in);
    }

    protected void forwardToExchange(MessageExchange exchange, InOnly outExchange, NormalizedMessage in) throws MessagingException {
        NormalizedMessage out = outExchange.createMessage();
        outExchange.setInMessage(out);
        getMessageTransformer().transform(exchange, in, out);
        getDeliveryChannel().send(outExchange);
    }
    
    protected QName getService() {
        QName service = null;
        if (serviceEndpoint != null) {
            service = serviceEndpoint.getServiceName();
        }
        return service;
    }

    protected String getEndpoint() {
        String endpoint = null;
        if (serviceEndpoint != null) {
            endpoint = serviceEndpoint.getEndpointName();
        }
        return endpoint;
    }

}
