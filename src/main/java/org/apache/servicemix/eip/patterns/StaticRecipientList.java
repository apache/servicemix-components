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
import javax.jbi.messaging.RobustInOnly;

import org.apache.servicemix.eip.EIPEndpoint;
import org.apache.servicemix.eip.support.ExchangeTarget;
import org.apache.servicemix.jbi.util.MessageUtil;

/**
 * The StaticRecipientList component will forward an input In-Only or Robust-In-Only
 * exchange to a list of known recipients.
 * This component implements the  
 * <a href="http://www.enterpriseintegrationpatterns.com/RecipientList.html">Recipient List</a> 
 * pattern, with the limitation that the recipient list is static.
 * 
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="static-recipient-list"
 *                  description="A static Recipient List"
 */
public class StaticRecipientList extends EIPEndpoint {

    /**
     * List of recipients
     */
    private ExchangeTarget[] recipients;
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
     * The correlation property used by this component
     */
    private String correlation;

    /**
     * @return Returns the recipients.
     */
    public ExchangeTarget[] getRecipients() {
        return recipients;
    }

    /**
     * @param recipients The recipients to set.
     */
    public void setRecipients(ExchangeTarget[] recipients) {
        this.recipients = recipients;
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
        // Check recipients
        if (recipients == null || recipients.length == 0) {
            throw new IllegalArgumentException("recipients should contain at least one ExchangeTarget");
        }
        // Create correlation property
        correlation = "StaticRecipientList.Correlation." + getService() + "." + getEndpoint();
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.eip.EIPEndpoint#processSync(javax.jbi.messaging.MessageExchange)
     */
    protected void processSync(MessageExchange exchange) throws Exception {
        if (exchange instanceof InOnly == false &&
            exchange instanceof RobustInOnly == false) {
            fail(exchange, new UnsupportedOperationException("Use an InOnly or RobustInOnly MEP"));
            return;
        }
        NormalizedMessage in = MessageUtil.copyIn(exchange);
        for (int i = 0; i < recipients.length; i++) {
            MessageExchange me = getExchangeFactory().createExchange(exchange.getPattern());
            recipients[i].configureTarget(me, getContext());
            MessageUtil.transferToIn(in, me);
            sendSync(me);
            if (me.getStatus() == ExchangeStatus.ERROR && reportErrors) {
                fail(exchange, me.getError());
                return;
            }
        }
        done(exchange);
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.eip.EIPEndpoint#processAsync(javax.jbi.messaging.MessageExchange)
     */
    protected void processAsync(MessageExchange exchange) throws Exception {
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
                for (int i = 0; i < recipients.length; i++) {
                    MessageExchange me = getExchangeFactory().createExchange(exchange.getPattern());
                    recipients[i].configureTarget(me, getContext());
                    MessageUtil.transferToIn(in, me);
                    send(me);
                }
                done(exchange);
            }
        }
    }

}
