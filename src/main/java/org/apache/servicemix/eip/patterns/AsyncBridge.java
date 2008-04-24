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

import java.util.Date;
import java.util.concurrent.TimeoutException;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.eip.EIPEndpoint;
import org.apache.servicemix.eip.support.ExchangeTarget;
import org.apache.servicemix.expression.Expression;
import org.apache.servicemix.expression.PropertyExpression;
import org.apache.servicemix.jbi.util.MessageUtil;
import org.apache.servicemix.timers.Timer;
import org.apache.servicemix.timers.TimerListener;

/**
 * 
 * @author gnodet
 * @org.apache.xbean.XBean element="async-bridge"
 */
public class AsyncBridge extends EIPEndpoint {

    public static final String CORRID = "org.apache.servicemix.eip.asyncbridge.corrid";

    private static final Log LOG = LogFactory.getLog(AsyncBridge.class);

    private Expression requestCorrId = new Expression() {
        public Object evaluate(MessageExchange exchange, NormalizedMessage message) throws MessagingException {
            return exchange.getExchangeId();
        }
    };
    private String responseCorrIdProperty = CORRID;
    private Expression responseCorrId;
    private long timeout;
    private ExchangeTarget target;
    private boolean useRobustInOnly;

    /**
     * @return the timeout
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * @param timeout the timeout to set
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * @return the target
     */
    public ExchangeTarget getTarget() {
        return target;
    }

    /**
     * @param target the target to set
     */
    public void setTarget(ExchangeTarget target) {
        this.target = target;
    }
    
    /**
     * @return the requestCorrId
     */
    public Expression getRequestCorrId() {
        return requestCorrId;
    }

    /**
     * @param requestCorrId the requestCorrId to set
     */
    public void setRequestCorrId(Expression requestCorrId) {
        this.requestCorrId = requestCorrId;
    }

    /**
     * @return the responseCorrIdProperty
     */
    public String getResponseCorrIdProperty() {
        return responseCorrIdProperty;
    }

    /**
     * @param responseCorrIdProperty the responseCorrIdProperty to set
     */
    public void setResponseCorrIdProperty(String responseCorrIdProperty) {
        this.responseCorrIdProperty = responseCorrIdProperty;
    }

    /**
     * @return the responseCorrId
     */
    public Expression getResponseCorrId() {
        return responseCorrId;
    }

    /**
     * @param responseCorrId the responseCorrId to set
     */
    public void setResponseCorrId(Expression responseCorrId) {
        this.responseCorrId = responseCorrId;
    }

    /**
     * @return the useRobustInOnly
     */
    public boolean isUseRobustInOnly() {
        return useRobustInOnly;
    }

    /**
     * @param useRobustInOnly the useRobustInOnly to set
     */
    public void setUseRobustInOnly(boolean useRobustInOnly) {
        this.useRobustInOnly = useRobustInOnly;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.eip.EIPEndpoint#start()
     */
    public void start() throws Exception {
        super.start();
        if (responseCorrId == null) {
            responseCorrId = new PropertyExpression(responseCorrIdProperty);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.eip.EIPEndpoint#processSync(javax.jbi.messaging.MessageExchange)
     */
    protected void processSync(MessageExchange exchange) throws Exception {
        throw new IllegalStateException();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.eip.EIPEndpoint#processAsync(javax.jbi.messaging.MessageExchange)
     */
    protected void processAsync(MessageExchange exchange) throws Exception {
        throw new IllegalStateException();
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.common.ExchangeProcessor#process(javax.jbi.messaging.MessageExchange)
     */
    public void process(MessageExchange exchange) throws Exception {
        // Handle an exchange as a PROVIDER
        if (exchange.getRole() == MessageExchange.Role.PROVIDER) {
            // receive the InOut request
            //   => send the In to the target
            if (exchange instanceof InOut && exchange.getStatus() == ExchangeStatus.ACTIVE) {
                final String correlationId = (String) requestCorrId.evaluate(exchange, exchange.getMessage("in"));
                if (correlationId == null || correlationId.length() == 0) {
                    throw new IllegalArgumentException("Could not retrieve correlation id for incoming exchange");
                }
                store.store(correlationId, exchange);
                MessageExchange tme = useRobustInOnly ? getExchangeFactory().createRobustInOnlyExchange()
                                                      : getExchangeFactory().createInOnlyExchange();
                target.configureTarget(tme, getContext());
                MessageUtil.transferInToIn(exchange, tme);
                tme.setProperty(responseCorrIdProperty, correlationId);
                tme.getMessage("in").setProperty(responseCorrIdProperty, correlationId);
                sendSync(tme);
                // an error
                if (tme.getStatus() == ExchangeStatus.ERROR) {
                    store.load(correlationId);
                    fail(exchange, tme.getError());
                    return;
                // a fault ?
                } else if (tme.getStatus() == ExchangeStatus.ACTIVE) {
                    store.load(correlationId);
                    MessageUtil.transferFaultToFault(tme, exchange);
                    send(tme);
                    done(tme);
                    return;
                // request sent
                } else {
                    Date exchangeTimeout = getTimeout(exchange);
                    if (exchangeTimeout != null) {
                        getTimerManager().schedule(new TimerListener() {
                            public void timerExpired(Timer timer) {
                                AsyncBridge.this.onTimeout(correlationId);
                            }
                        }, exchangeTimeout);
                    }
                }
            // receive the done / error for the InOut request
            } else if (exchange instanceof InOut && exchange.getStatus() != ExchangeStatus.ACTIVE) {
                // ignore these exchanges
            // Receive the response
            } else if (exchange instanceof InOnly || exchange instanceof RobustInOnly) {
                final String correlationId = (String) responseCorrId.evaluate(exchange, exchange.getMessage("in"));
                if (correlationId == null || correlationId.length() == 0) {
                    throw new IllegalArgumentException("Could not retrieve correlation id for incoming exchange");
                }
                MessageExchange request = (MessageExchange) store.load(correlationId);
                // The request is found and has not timed out
                if (request != null) {
                    MessageUtil.transferInToOut(exchange, request);
                    sendSync(request);
                }
                done(exchange);
            } else {
                throw new IllegalStateException();
            }
        // Handle an exchange as a CONSUMER
        } else {
            throw new IllegalStateException();
        }
    }
    
    protected void onTimeout(String correlationId) {
        try {
            MessageExchange request = (MessageExchange) store.load(correlationId);
            if (request != null) {
                fail(request, new TimeoutException());
            }
        } catch (Exception e) {
            LOG.debug("Exception caught when handling timeout", e);
        }
    }
    
    protected Date getTimeout(MessageExchange exchange) {
        if (timeout > 0) {
            return new Date(System.currentTimeMillis() + timeout);
        }
        return null;
    }

}
