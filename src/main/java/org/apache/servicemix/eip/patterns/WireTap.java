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
package org.apache.servicemix.eip.patterns;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.eip.EIPEndpoint;
import org.apache.servicemix.eip.support.ExchangeTarget;
import org.apache.servicemix.jbi.util.MessageUtil;
import org.apache.servicemix.store.Store;

/**
 *
 * A WireTap component can be used to forward a copy of the input message to a listener.
 * This component implements the 
 * <a href="http://www.enterpriseintegrationpatterns.com/WireTap.html">WireTap</a> 
 * pattern.
 * It can handle all 4 standard MEPs, but will only send an In-Only MEP to the listener.
 * In addition, this component is fully asynchronous and uses an exchange store to provide
 * full HA and recovery for clustered / persistent flows. 
 * 
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="wire-tap"
 *                  description="A WireTap"
 */
public class WireTap extends EIPEndpoint {

    /**
     * The main target destination which will receive the exchange
     */
    private ExchangeTarget target;
    /**
     * The listener destination for in messages
     */
    private ExchangeTarget inListener;
    /**
     * The listener destination for out messages
     */
    private ExchangeTarget outListener;
    /**
     * The listener destination for fault messages
     */
    private ExchangeTarget faultListener;
    /**
     * The correlation property used by this component
     */
    private String correlation;
    
    /**
     * @return Returns the target.
     */
    public ExchangeTarget getTarget() {
        return target;
    }

    /**
     * @param target The target to set.
     */
    public void setTarget(ExchangeTarget target) {
        this.target = target;
        this.wsdlExchangeTarget = target;
    }

    /**
     * @return Returns the faultListener.
     */
    public ExchangeTarget getFaultListener() {
        return faultListener;
    }

    /**
     * @param faultListener The faultListener to set.
     */
    public void setFaultListener(ExchangeTarget faultListener) {
        this.faultListener = faultListener;
    }

    /**
     * @return Returns the inListener.
     */
    public ExchangeTarget getInListener() {
        return inListener;
    }

    /**
     * @param inListener The inListener to set.
     */
    public void setInListener(ExchangeTarget inListener) {
        this.inListener = inListener;
    }

    /**
     * @return Returns the outListener.
     */
    public ExchangeTarget getOutListener() {
        return outListener;
    }

    /**
     * @param outListener The outListener to set.
     */
    public void setOutListener(ExchangeTarget outListener) {
        this.outListener = outListener;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.eip.EIPEndpoint#validate()
     */
    public void validate() throws DeploymentException {
        super.validate();
        // Check target
        if (target == null) {
            throw new IllegalArgumentException("target should be set to a valid ExchangeTarget");
        }
        // Create correlation property
        correlation = "WireTap.Correlation." + getService() + "." + getEndpoint();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.eip.EIPEndpoint#processSync(javax.jbi.messaging.MessageExchange)
     */
    protected void processSync(MessageExchange exchange) throws Exception {
        // Create exchange for target
        MessageExchange tme = exchangeFactory.createExchange(exchange.getPattern());
        target.configureTarget(tme, getContext());
        sendSyncToListenerAndTarget(exchange, tme, inListener, "in");
        if (tme.getStatus() == ExchangeStatus.DONE) {
            done(exchange);
        } else if (tme.getStatus() == ExchangeStatus.ERROR) {
            fail(exchange, tme.getError());
        } else if (tme.getFault() != null) {
            sendSyncToListenerAndTarget(tme, exchange, faultListener, "fault");
            done(tme);
        } else if (tme.getMessage("out") != null) {
            sendSyncToListenerAndTarget(tme, exchange, outListener, "out");
            done(tme);
        } else {
            done(tme);
            throw new IllegalStateException("Exchange status is " + ExchangeStatus.ACTIVE + " but has no Out nor Fault message");
        }
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.eip.EIPEndpoint#processAsync(javax.jbi.messaging.MessageExchange)
     */
    protected void processAsync(MessageExchange exchange) throws Exception {
        if (exchange.getRole() == MessageExchange.Role.PROVIDER &&
            exchange.getProperty(correlation) == null) {
            // Create exchange for target
            MessageExchange tme = exchangeFactory.createExchange(exchange.getPattern());
            if (store.hasFeature(Store.CLUSTERED)) {
                exchange.setProperty(JbiConstants.STATELESS_PROVIDER, Boolean.TRUE);
                tme.setProperty(JbiConstants.STATELESS_CONSUMER, Boolean.TRUE);
            }
            target.configureTarget(tme, getContext());
            // Set correlations
            exchange.setProperty(correlation, tme.getExchangeId());
            tme.setProperty(correlation, exchange.getExchangeId());
            // Put exchange to store
            store.store(exchange.getExchangeId(), exchange);
            // Send in to listener and target
            sendToListenerAndTarget(exchange, tme, inListener, "in");
        // Mimic the exchange on the other side and send to needed listener
        } else {
            String id = (String) exchange.getProperty(correlation);
            if (id == null) {
                if (exchange.getRole() == MessageExchange.Role.CONSUMER &&
                    exchange.getStatus() != ExchangeStatus.ACTIVE) {
                    // This must be a listener status, so ignore
                    return;
                }
                throw new IllegalStateException(correlation + " property not found");
            }
            MessageExchange org = (MessageExchange) store.load(id);
            if (org == null) {
                throw new IllegalStateException("Could not load original exchange with id " + id);
            }
            // Reproduce DONE status to the other side
            if (exchange.getStatus() == ExchangeStatus.DONE) {
                done(org);
            // Reproduce ERROR status to the other side
            } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
                fail(org, exchange.getError());
            // Reproduce faults to the other side and listeners
            } else if (exchange.getFault() != null) {
                store.store(exchange.getExchangeId(), exchange);
                sendToListenerAndTarget(exchange, org, faultListener, "fault");
            // Reproduce answers to the other side
            } else if (exchange.getMessage("out") != null) {
                store.store(exchange.getExchangeId(), exchange);
                sendToListenerAndTarget(exchange, org, outListener, "out");
            } else {
                throw new IllegalStateException("Exchange status is " + ExchangeStatus.ACTIVE + " but has no Out nor Fault message");
            }
        }
    }
    
    private void sendToListenerAndTarget(MessageExchange source, 
                                         MessageExchange dest, 
                                         ExchangeTarget listener,
                                         String message) throws Exception {
        if (listener != null) {
            NormalizedMessage msg = MessageUtil.copy(source.getMessage(message));
            InOnly lme = exchangeFactory.createInOnlyExchange();
            if (store.hasFeature(Store.CLUSTERED)) {
                lme.setProperty(JbiConstants.STATELESS_CONSUMER, Boolean.TRUE);
            }
            listener.configureTarget(lme, getContext());
            MessageUtil.transferToIn(msg, lme);
            send(lme);
            MessageUtil.transferTo(msg, dest, message);
            send(dest);
        } else {
            MessageUtil.transferTo(source, dest, message);
            send(dest);
        }
    }

    private void sendSyncToListenerAndTarget(MessageExchange source, 
                                             MessageExchange dest, 
                                             ExchangeTarget listener,
                                             String message) throws Exception {
        if (listener != null) {
            NormalizedMessage msg = MessageUtil.copy(source.getMessage(message));
            InOnly lme = exchangeFactory.createInOnlyExchange();
            if (store.hasFeature(Store.CLUSTERED)) {
                lme.setProperty(JbiConstants.STATELESS_CONSUMER, Boolean.TRUE);
            }
            listener.configureTarget(lme, getContext());
            MessageUtil.transferToIn(msg, lme);
            sendSync(lme);
            MessageUtil.transferTo(msg, dest, message);
            sendSync(dest);
        } else {
            MessageUtil.transferTo(source, dest, message);
            sendSync(dest);
        }
    }
    
}
