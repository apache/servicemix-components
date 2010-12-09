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
import org.apache.servicemix.common.util.MessageUtil;
import org.apache.servicemix.eip.EIPEndpoint;
import org.apache.servicemix.eip.support.ExchangeTarget;
import org.apache.servicemix.expression.Expression;
import org.apache.servicemix.expression.PropertyExpression;
import org.apache.servicemix.timers.Timer;
import org.apache.servicemix.timers.TimerListener;

/**
 * The async bridge pattern is used to bridge an In-Out exchange with two In-Only
 * (or Robust-In-Only) exchanges. This pattern is the opposite of the {@link Pipeline}.
  <br/>
 * The AsyncBridge uses a correlation identifier to be able to correlate the received
 * In-Out exchange, the In-Only sent as the request and the In-Only received as the response.
 * Defaults values are provided to configure those correlation ids.  The default behavior
 * is to use the exchange id of the incoming In-Out exchange as the correlation id and set
 * it on the request exchange.  The same property with the same value should be present on the
 * response exchange in order for the AsyncBridge to work.  ServiceMix components usually take
 * care of propagating such properties, but failing to propagate it will result in errors.
 *
 * @author gnodet
 * @see Pipeline
 *
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
     * The timeout property controls the amount of time that the async bridge will wait for the response
     * after having sent the request.  The default value is 0 which means that no timeout apply.  If set
     * to a non zero value, a timer will be started when after the request is sent.  When the timer
     * expires, the In-Out exchange will be sent back with an error status and a
     * {@link java.util.concurrent.TimeoutException} as the cause of the error.
     * The value represents the number of milliseconds to wait.
     *
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
     * The target which will be used to send an In-Only or Robust-In-Only exchange to.
     * When receiving an In-Out exchange, the async bridge will create an In-Only request
     * and send it to the specified target.  It then expects another In-Only exchange to
     * come back as the response, which will be set as the Out message on the In-Out exchange.
     * This property is mandatory and must be set to a valid target.
     *
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
     * The expression used to compute the correlation id used to correlate the response and
     * the request.  The default behavior is to use the exchange id of the incoming In-Out
     * exchange as the correlation id.
     *
     * @param requestCorrId the requestCorrId to set
     * @see #setResponseCorrId(org.apache.servicemix.expression.Expression)
     * @see #setResponseCorrIdProperty(String)
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
     * Name of the property used by default to compute the correlation id on the response
     * exchange.
     *
     * @param responseCorrIdProperty the responseCorrIdProperty to set
     * @see #setRequestCorrId(org.apache.servicemix.expression.Expression)
     * @see #setResponseCorrId(org.apache.servicemix.expression.Expression)
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
     * The expression used to compute the correlation id from the response exchange.
     * The value computed by this expression must match the one from the {@link #setRequestCorrId}
     * expression.  The default value is null, but if no specific expression is configured,
     * an expression will be created which will extract the response correlation id from the
     * {@link #setResponseCorrIdProperty(String)} property on the exchange.
     *
     * @param responseCorrId the responseCorrId to set
     * @see #setResponseCorrIdProperty(String)
     * @see #setRequestCorrId(org.apache.servicemix.expression.Expression)
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
     * Boolean flag to control if In-Only or Robust-In-Only exchange should be used
     * when sending the request.  The default value is <code>false</code> which means
     * that an In-Only exchange will be used.  When using a Robust-In-Only exchange and
     * when a fault is received, this fault will be sent back to the consumer on the In-Out
     * exchange and the response exchange (if any) would be discarded.
     * For both In-Only and Robust-In-Only, if the request exchange comes back with an Error
     * status, this error will be conveyed back to the consumer in the same way.
     *
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
        // Three exchanges are involved: the first InOut will be called t0,
        // the InOnly send will be called t1 and the InOnly received will be called t2

        if (exchange.getRole() == MessageExchange.Role.PROVIDER) {
            // Step1: receive t0 as the first message
            if (exchange instanceof InOut && exchange.getStatus() == ExchangeStatus.ACTIVE) {
                MessageExchange t0 = exchange;
                MessageExchange t1;
                final String correlationId = (String) requestCorrId.evaluate(t0, t0.getMessage("in"));
                if (correlationId == null || correlationId.length() == 0) {
                    throw new IllegalArgumentException("Could not retrieve correlation id for incoming exchange");
                }
                store.store(correlationId + ".t0", t0);
                t1 = useRobustInOnly ? getExchangeFactory().createRobustInOnlyExchange()
                                     : getExchangeFactory().createInOnlyExchange();
                target.configureTarget(t1, getContext());
                MessageUtil.transferInToIn(t0, t1);
                t1.setProperty(responseCorrIdProperty, correlationId);
                t1.getMessage("in").setProperty(responseCorrIdProperty, correlationId);
                send(t1);
            // Receive the done / error from t0
            } else if (exchange instanceof InOut && exchange.getStatus() != ExchangeStatus.ACTIVE) {
                MessageExchange t0 = exchange;
                MessageExchange t1;
                MessageExchange t2;
                final String correlationId = (String) requestCorrId.evaluate(t0, t0.getMessage("in"));
                t1 = (MessageExchange) store.load(correlationId + ".t1");
                t2 = (MessageExchange) store.load(correlationId + ".t2");
                if (t1 != null) {
                    done(t1);
                }
                if (t2 != null) {
                    done(t2);
                }
            // Receive the response from t2
            } else if ((exchange instanceof InOnly || exchange instanceof RobustInOnly) && exchange.getStatus() == ExchangeStatus.ACTIVE) {
                MessageExchange t0;
                MessageExchange t2 = exchange;
                final String correlationId = (String) responseCorrId.evaluate(t2, t2.getMessage("in"));
                if (correlationId == null || correlationId.length() == 0) {
                    throw new IllegalArgumentException("Could not retrieve correlation id for incoming exchange");
                }
                t0 = (MessageExchange) store.load(correlationId + ".t0");
                store.store(correlationId + ".t2", t2);
                // The request is found and has not timed out
                if (t0 != null) {
                    MessageUtil.transferInToOut(t2, t0);
                    send(t0);
                }
            } else {
                throw new IllegalStateException();
            }
        // Handle an exchange as a CONSUMER
        } else {
            // Step 2: receive t1 response
            // If this is an error or a fault, transfer it from t1 to t0 and send,
            // else, start a timeout to wait for t2
            MessageExchange t1 = exchange;
            // an error
            final String correlationId = (String) t1.getProperty(responseCorrIdProperty);
            if (t1.getStatus() == ExchangeStatus.ERROR) {
                MessageExchange t0 = (MessageExchange) store.load(correlationId + ".t0");
                // t1 response may come after t0, so in case this happens, we need to discard t1
                if (t0 != null) {
                    fail(t0, t1.getError());
                }
            // a fault ?
            } else if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                MessageExchange t0 = (MessageExchange) store.load(correlationId + ".t0");
                // t1 response may come after t0, so in case this happens, we need to discard t1
                if (t0 != null) {
                    store.store(correlationId + ".t1", t1);
                    MessageUtil.transferFaultToFault(t1, t0);
                    send(t0);
                }
            // request sent successfully, start the timeout
            } else {
                Date exchangeTimeout = getTimeout(t1);
                if (exchangeTimeout != null) {
                    getTimerManager().schedule(new TimerListener() {
                        public void timerExpired(Timer timer) {
                            AsyncBridge.this.onTimeout(correlationId);
                        }
                    }, exchangeTimeout);
                }
            }
        }
    }
    
    protected void onTimeout(String correlationId) {
        try {
            MessageExchange t0 = (MessageExchange) store.load(correlationId + ".t0");
            if (t0 != null) {
                fail(t0, new TimeoutException());
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
