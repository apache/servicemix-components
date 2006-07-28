/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.eip.EIPEndpoint;
import org.apache.servicemix.eip.support.ExchangeTarget;
import org.apache.servicemix.eip.support.MessageUtil;
import org.apache.servicemix.eip.support.RoutingRule;
import org.apache.servicemix.store.Store;

/**
 * ContentBasedRouter can be used for all kind of content-based routing.
 * This component implements the  
 * <a href="http://www.enterpriseintegrationpatterns.com/ContentBasedRouter.html">Content-Based Router</a> 
 * pattern.
 * 
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="content-based-router"
 *                  description="A Content-Based Router"
 */
public class ContentBasedRouter extends EIPEndpoint {

    /**
     * Routing rules that are evaluated to find the target destination
     */
    private RoutingRule[] rules;
    /**
     * The correlation property used by this component
     */
    private String correlation;
    
    /**
     * @return Returns the rules.
     */
    public RoutingRule[] getRules() {
        return rules;
    }

    /**
     * @param rules The rules to set.
     */
    public void setRules(RoutingRule[] rules) {
        this.rules = rules;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.eip.EIPEndpoint#validate()
     */
    public void validate() throws DeploymentException {
        super.validate();
        // Check rules
        if (rules == null || rules.length == 0) {
            throw new IllegalArgumentException("rules should contain at least one RoutingRule");
        }
        // Create correlation property
        correlation = "AbstractContentBasedRouter.Correlation." + getService() + "." + getEndpoint();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.eip.EIPEndpoint#processSync(javax.jbi.messaging.MessageExchange)
     */
    protected void processSync(MessageExchange exchange) throws Exception {
        // Create exchange for target
        MessageExchange tme = exchangeFactory.createExchange(exchange.getPattern());
        // Now copy input to new exchange
        // We need to read the message once for finding routing target
        // so ensure we have a re-readable source
        NormalizedMessage in = MessageUtil.copyIn(exchange);
        MessageUtil.transferToIn(in, tme); 
        // Retrieve target
        ExchangeTarget target = getDestination(tme);
        target.configureTarget(tme, getContext());
        // Send in to target
        sendSync(tme);
        // Send back the result
        if (tme.getStatus() == ExchangeStatus.DONE) {
            done(exchange);
        } else if (tme.getStatus() == ExchangeStatus.ERROR) {
            fail(exchange, tme.getError());
        } else if (tme.getFault() != null) {
            Fault fault = MessageUtil.copyFault(tme);
            done(tme);
            MessageUtil.transferToFault(fault, exchange);
            sendSync(exchange);
        } else if (tme.getMessage("out") != null) {
            NormalizedMessage out = MessageUtil.copyOut(tme);
            done(tme);
            MessageUtil.transferToOut(out, exchange);
            sendSync(exchange);
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
            // Set correlations
            tme.setProperty(correlation, exchange.getExchangeId());
            exchange.setProperty(correlation, tme.getExchangeId());
            // Put exchange to store
            store.store(exchange.getExchangeId(), exchange);
            // Now copy input to new exchange
            // We need to read the message once for finding routing target
            // so ensure we have a re-readable source
            NormalizedMessage in = MessageUtil.copyIn(exchange);
            MessageUtil.transferToIn(in, tme); 
            // Retrieve target
            ExchangeTarget target = getDestination(tme);
            target.configureTarget(tme, getContext());
            // Send in to target
            send(tme);
        // Mimic the exchange on the other side and send to needed listener
        } else {
            String id = (String) exchange.getProperty(correlation);
            if (id == null) {
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
                MessageUtil.transferTo(exchange, org, "fault"); 
                send(org);
            // Reproduce answers to the other side
            } else if (exchange.getMessage("out") != null) {
                store.store(exchange.getExchangeId(), exchange);
                MessageUtil.transferTo(exchange, org, "out"); 
                send(org);
            } else {
                throw new IllegalStateException("Exchange status is " + ExchangeStatus.ACTIVE + " but has no Out nor Fault message");
            }
        }
    }
    
    /**
     * Find the target destination for the given JBI exchange
     * @param exchange
     * @return the target for the given exchange
     * @throws Exception
     */
    protected ExchangeTarget getDestination(MessageExchange exchange) throws Exception {
        for (int i = 0; i < rules.length; i++) {
            if (rules[i].getPredicate() == null ||
                rules[i].getPredicate().matches(exchange)) {
                return rules[i].getTarget();
            }
        }
        throw new MessagingException("No matching rule found for exchange");
    }

}
