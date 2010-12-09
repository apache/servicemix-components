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

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.common.EndpointSupport;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.executors.Executor;

/**
 * @org.apache.xbean.XBean element="endpoint"
 * 
 * @author lhe
 */
public class OSWorkflowEndpoint extends ProviderEndpoint {

    private static final long TIME_OUT = 30000;

    private String workflowName;

    private String caller;

    private int action;

    private Executor executor;

    private SourceTransformer sourceTransformer = new SourceTransformer();

    public void start() throws Exception {
        super.start();
        OSWorkflowComponent component = (OSWorkflowComponent) getServiceUnit().getComponent();
        executor = component.getExecutorFactory().createExecutor("component." + component.getComponentName() + "." + EndpointSupport.getKey(this));
    }

    public void stop() throws Exception {
        executor.shutdown();
        super.stop();
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
            throw new MessagingException("OSWorkflowEndpoint.onMessageExchange(): Unknown role: "
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
            done(exchange);
        } else if (exchange.getFault() != null) {
            //Fault message
            done(exchange);
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
            done(exchange);
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

                executor.execute(osWorkflow);
            }
        }
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
        InOnly inOnly = getChannel().createExchangeFactoryForService(service).createInOnlyExchange();
        NormalizedMessage msg = inOnly.createMessage();
        msg.setContent(source);
        inOnly.setInMessage(msg);
        if (getChannel().sendSync(inOnly)) {
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
        InOut inOut = getChannel().createExchangeFactoryForService(service).createInOutExchange();
        NormalizedMessage msg = inOut.createMessage();
        msg.setContent(source);
        inOut.setInMessage(msg);

        if (getChannel().sendSync(inOut)) {
            try {
                Source result = sourceTransformer.toDOMSource(inOut.getOutMessage().getContent());
                done(inOut);
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
        InOut inOut = getChannel().createExchangeFactoryForService(service).createInOutExchange();
        NormalizedMessage msg = inOut.createMessage();
        msg.setContent(source);
        inOut.setInMessage(msg);
        if (getChannel().sendSync(inOut)) {
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
            exchange = getChannel().createExchangeFactoryForService(qname).createInOutExchange();
        } else {
            exchange = getChannel().createExchangeFactoryForService(qname).createInOnlyExchange();
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
        super.done(ex);
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
            getChannel().sendSync(ex, TIME_OUT);
        } else {
            getChannel().send(ex);
        }
    }

    /**
     * sends a error to the channel
     *
     * @param ex
     * @throws MessagingException
     */
    public void fail(MessageExchange ex) throws MessagingException {
        super.fail(ex, new Exception("Failure"));
    }

    /**
     * @return the workflowName
     */
    public String getWorkflowName() {
        return this.workflowName;
    }

    /**
     * The name of the workflow to be used for handling the exchange.
     *
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
     * The caller user name to be used when executing the workflow.
     *
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
     * The initial action to trigger in the workflow.
     *
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
