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
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.eip.EIPEndpoint;
import org.apache.servicemix.eip.support.ExchangeTarget;
import org.apache.servicemix.eip.support.MessageUtil;
import org.apache.servicemix.eip.support.Predicate;

/**
 * MessageFilter allows filtering incoming JBI exchanges.
 * This component implements the  
 * <a href="http://www.enterpriseintegrationpatterns.com/Filter.html">Message Filter</a> 
 * pattern.
 *  
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="message-filter"
 *                  description="A Message Filter"
 */
public class MessageFilter extends EIPEndpoint {

    private static final Log log = LogFactory.getLog(MessageFilter.class);
    
    /**
     * The main target destination which will receive the exchange
     */
    private ExchangeTarget target;
    /**
     * The filter to use on incoming messages
     */
    private Predicate filter;
    /**
     * The correlation property used by this component
     */
    private String correlation;
    /**
     * Indicates if faults and errors from recipients should be sent
     * back to the consumer.  In such a case, only the first fault or
     * error received will be reported.
     * Note that if the consumer is synchronous, it will be blocked
     * until all recipients successfully acked the exchange, or
     * a fault or error is reported, and the exchange will be kept in the
     * store for recovery. 
     */
    private boolean reportErrors;
    
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
    }

    /**
     * @return Returns the filter.
     */
    public Predicate getFilter() {
        return filter;
    }

    /**
     * @param filter The filter to set.
     */
    public void setFilter(Predicate filter) {
        this.filter = filter;
    }

    /**
     * @return Returns the reportErrors.
     */
    public boolean isReportErrors() {
        return reportErrors;
    }

    /**
     * @param reportErrors The reportErrors to set.
     */
    public void setReportErrors(boolean reportErrors) {
        this.reportErrors = reportErrors;
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
        // Check filter
        if (filter == null) {
            throw new IllegalArgumentException("filter property should be set");
        }
        // Create correlation property
        correlation = "MessageFilter.Correlation." + getService() + "." + getEndpoint();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.ExchangeProcessor#process(javax.jbi.messaging.MessageExchange)
     */
    public void process(MessageExchange exchange) throws Exception {
        try {
            // If we need to report errors, the behavior is really different,
            // as we need to keep the incoming exchange in the store until
            // all acks have been received
            if (reportErrors) {
                // TODO: implement this
                throw new UnsupportedOperationException("Not implemented");
            // We are in a simple fire-and-forget behaviour.
            // This implementation is really efficient as we do not use
            // the store at all.
            } else {
                if (exchange.getStatus() == ExchangeStatus.DONE) {
                    return;
                } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
                    return;
                } else if (exchange instanceof InOnly == false &&
                           exchange instanceof RobustInOnly == false) {
                    fail(exchange, new UnsupportedOperationException("Use an InOnly or RobustInOnly MEP"));
                } else if (exchange.getFault() != null) {
                    done(exchange);
                } else {
                    NormalizedMessage in = MessageUtil.copyIn(exchange);
                    MessageExchange me = exchangeFactory.createExchange(exchange.getPattern());
                    target.configureTarget(me, getContext());
                    MessageUtil.transferToIn(in, me);
                    if (filter.matches(me)) {
                        send(me);
                    }
                    done(exchange);
                }
            }
        // If an error occurs, log it and report the error back to the sender
        // if the exchange is still ACTIVE 
        } catch (Exception e) {
            log.error("An exception occured while processing exchange", e);
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                fail(exchange, e);
            }
        }
    }

}
