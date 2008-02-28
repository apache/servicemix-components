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
package org.apache.servicemix.osworkflow;

import java.util.HashMap;

import javax.jbi.component.ComponentContext;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;

/**
 * @org.apache.xbean.XBean element="endpoint"
 * 
 * @author lhe
 */
public class OSWorkflowEndpoint extends Endpoint implements ExchangeProcessor {
    private static final long TIME_OUT = 30000;

    private ServiceEndpoint activated;

    private DeliveryChannel channel;

    private MessageExchangeFactory exchangeFactory;

    private String workflowName;

    private String caller;

    private int action;

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.Endpoint#getRole()
     */
    public Role getRole() {
        return Role.PROVIDER;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.Endpoint#activate()
     */
    public void activate() throws Exception {
        logger = this.serviceUnit.getComponent().getLogger();
        ComponentContext ctx = getServiceUnit().getComponent()
                .getComponentContext();
        channel = ctx.getDeliveryChannel();
        exchangeFactory = channel.createExchangeFactory();
        activated = ctx.activateEndpoint(service, endpoint);
        start();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.Endpoint#deactivate()
     */
    public void deactivate() throws Exception {
        stop();
        ServiceEndpoint ep = activated;
        activated = null;
        ComponentContext ctx = getServiceUnit().getComponent()
                .getComponentContext();
        ctx.deactivateEndpoint(ep);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.Endpoint#getProcessor()
     */
    public ExchangeProcessor getProcessor() {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.Endpoint#validate()
     */
    public void validate() throws DeploymentException {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.ExchangeProcessor#start()
     */
    public void start() throws Exception {
        // initialize the workflow manager
        WorkflowManager.getInstance();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.ExchangeProcessor#stop()
     */
    public void stop() {
        // shut down first finishing running threads
        WorkflowManager.getInstance().prepareShutdown(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.ExchangeProcessor#process(javax.jbi.messaging.MessageExchange)
     */
    public void process(MessageExchange exchange) throws Exception {
        if (exchange == null) {
            return;
        }

        // The component acts as a consumer, this means this exchange is
        // received because
        // we sent it to another component. As it is active, this is either an
        // out or a fault
        // If this component does not create / send exchanges, you may just
        // throw an
        // UnsupportedOperationException
        if (exchange.getRole() == Role.CONSUMER) {
            onConsumerExchange(exchange);
        } else if (exchange.getRole() == MessageExchange.Role.PROVIDER) {
//          The component acts as a provider, this means that another component
            // has requested our
            // service
            // As this exchange is active, this is either an in or a fault (out are
            // send by this
            // component)
            onProviderExchange(exchange);
        } else {
            // Unknown role
            throw new MessagingException(
                    "OSWorkflowEndpoint.onMessageExchange(): Unknown role: "
                            + exchange.getRole());
        }
    }

    /**
     * handles the incoming consumer messages
     * 
     * @param exchange
     * @throws MessagingException
     */
    protected void onConsumerExchange(MessageExchange exchange)
        throws MessagingException {
        // Out message
        if (exchange.getMessage("out") != null) {
            exchange.setStatus(ExchangeStatus.DONE);
            channel.send(exchange);
        } else if (exchange.getFault() != null) {
            //Fault message
            exchange.setStatus(ExchangeStatus.DONE);
            channel.send(exchange);
        } else {
            //This is not compliant with the default MEPs
            throw new MessagingException(
                    "OSWorkflowEndpoint.onConsumerExchange(): Consumer exchange is ACTIVE, but no out or fault is provided");
        }
    }

    /**
     * handles the incoming provider messages
     * 
     * @param exchange
     * @throws MessagingException
     */
    protected void onProviderExchange(MessageExchange exchange)
        throws MessagingException {
        
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            //Exchange is finished
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            //Exchange has been aborted with an exception
            return;
        } else if (exchange.getFault() != null) {
            //Fault message
            exchange.setStatus(ExchangeStatus.DONE);
            channel.send(exchange);
        } else {
            NormalizedMessage in = exchange.getMessage("in");

            if (in == null) {
                // no in message - strange
                throw new MessagingException(
                        "OSWorkflowEndpoint.onProviderExchange(): Exchange has no IN message");
            } else {
                // create a new workflow object
                OSWorkflow osWorkflow = new OSWorkflow(this, this.workflowName,
                        this.action, new HashMap(), this.caller, exchange);

                if (exchange instanceof InOnly
                        || exchange instanceof RobustInOnly) {
                    // do start the workflow in separate thread
                    try {
                        WorkflowManager.getInstance().executeWorkflow(
                                osWorkflow);
                    } catch (Exception ex) {
                        logger.error(ex);
                    }
                } else {
                    // synchronous processing, keep state ACTIVE
                    // do start the workflow and join the thread
                    try {
                        osWorkflow.start();
                        osWorkflow.join();
                    } catch (Exception ex) {
                        logger.error(ex);
                    }
                }
            }
        }
    }

    /**
     * returns the delivery channel for the endpoint
     * 
     * @return the delivery channel
     */
    public DeliveryChannel getChannel() {

        return this.channel;
    }

    /**
     * returns the message exchange factory
     * 
     * @return the message exchange factory
     */
    public MessageExchangeFactory getMessageExchangeFactory() {
        return this.exchangeFactory;
    }

    /**
     * sends the given DOMSource as message to the given service (inOnly)
     * 
     * @param service
     *            the service name to send the message to
     * @param source
     *            the source to put in the in message content
     * @return true on sucessful delivering or false on failure
     * @throws MessagingException
     *             on any messaging exception
     */
    public boolean sendMessage(QName service, Source source)
        throws MessagingException {
        InOnly inOnly = channel.createExchangeFactoryForService(service)
                .createInOnlyExchange();
        NormalizedMessage msg = inOnly.createMessage();
        msg.setContent(source);
        inOnly.setInMessage(msg);
        if (channel.sendSync(inOnly)) {
            return inOnly.getStatus() == ExchangeStatus.DONE;
        } else {
            return false;
        }
    }

    /**
     * sends the given DOMSource as message to the given service (inOut)
     * 
     * @param service
     *            the service name to send the message to
     * @param source
     *            the source to put in the in message content
     * @return the DOMSource of the out message or null
     * @throws MessagingException
     *             on any messaging exception
     */
    public Source sendRequest(QName service, Source source)
        throws MessagingException {
        InOut inOut = channel.createExchangeFactoryForService(service)
                .createInOutExchange();
        NormalizedMessage msg = inOut.createMessage();
        msg.setContent(source);
        inOut.setInMessage(msg);

        if (channel.sendSync(inOut)) {
            SourceTransformer sourceTransformer = new SourceTransformer();

            try {
                Source result = sourceTransformer.toDOMSource(inOut
                        .getOutMessage().getContent());

                inOut.setStatus(ExchangeStatus.DONE);
                channel.send(inOut);

                return result;
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * sends the given DOMSource as message to the given service (inOut)
     * 
     * @param service
     *            the service name to send the message to
     * @param source
     *            the source to put in the in message content
     * @return the DOMSource of the out message or null
     * @throws MessagingException
     *             on any messaging exception
     */
    public MessageExchange sendRawInOutRequest(QName service, Source source)
        throws MessagingException {
        InOut inOut = channel.createExchangeFactoryForService(service)
                .createInOutExchange();
        NormalizedMessage msg = inOut.createMessage();
        msg.setContent(source);
        inOut.setInMessage(msg);
        if (channel.sendSync(inOut)) {
            return inOut;
        } else {
            return null;
        }
    }

    /**
     * creates a msg object
     * 
     * @param qname
     *            the service which will be the receiver
     * @param inOut
     *            should it be inOut or InOnly
     * @return the created exchange
     * @throws MessagingException
     */
    public MessageExchange getNewExchange(QName qname, boolean inOut)
        throws MessagingException {
        MessageExchange exchange = null;

        if (inOut) {
            exchange = channel.createExchangeFactoryForService(qname)
                    .createInOutExchange();
        } else {
            exchange = channel.createExchangeFactoryForService(qname)
                    .createInOnlyExchange();
        }

        return exchange;
    }

    /**
     * sends a done to the channel
     * 
     * @param ex
     * @throws MessagingException
     */
    public void done(MessageExchange ex) throws MessagingException {
        ex.setStatus(ExchangeStatus.DONE);
        channel.send(ex);
    }

    /**
     * sends a msg to the channel
     * 
     * @param ex
     * @param sync
     * @throws MessagingException
     */
    public void send(MessageExchange ex, boolean sync)
        throws MessagingException {
        if (sync) {
            channel.sendSync(ex, TIME_OUT);
        } else {
            channel.send(ex);
        }
    }

    /**
     * sends a error to the channel
     * 
     * @param ex
     * @throws MessagingException
     */
    public void fail(MessageExchange ex) throws MessagingException {
        ex.setStatus(ExchangeStatus.ERROR);
        channel.send(ex);
    }

    /**
     * @return the workflowName
     */
    public String getWorkflowName() {
        return this.workflowName;
    }

    /**
     * @param workflowName
     *            the workflowName to set
     */
    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    /**
     * @return the caller
     */
    public String getCaller() {
        return this.caller;
    }

    /**
     * @param caller
     *            the caller to set
     */
    public void setCaller(String caller) {
        this.caller = caller;
    }

    /**
     * @return the action
     */
    public int getAction() {
        return this.action;
    }

    /**
     * @param action
     *            the action to set
     */
    public void setAction(int action) {
        this.action = action;
    }

    /**
     * init actions
     */
    public void preWorkflow() {
        // nothing for now
    }

    /**
     * cleanup action
     */
    public void postWorkflow() {
        // nothing for now
    }
}
