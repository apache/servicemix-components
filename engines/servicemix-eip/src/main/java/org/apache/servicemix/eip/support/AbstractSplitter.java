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
package org.apache.servicemix.eip.support;

import java.net.URI;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.locks.Lock;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;
import javax.xml.transform.Source;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.eip.EIPEndpoint;
import org.apache.servicemix.common.util.MessageUtil;

/**
 * The AbstractSplitter is an abstract base class for Splitters.
 * This component implements the  
 * <a href="http://www.enterpriseintegrationpatterns.com/Sequencer.html">Splitter</a> 
 * pattern.
 *  
 * @author gnodet
 * @version $Revision: 376451 $
 */
public abstract class AbstractSplitter extends EIPEndpoint {
    
    public static final String SPLITTER_COUNT = "org.apache.servicemix.eip.splitter.count";
    public static final String SPLITTER_INDEX = "org.apache.servicemix.eip.splitter.index";
    public static final String SPLITTER_CORRID = "org.apache.servicemix.eip.splitter.corrid";

    private static final Log LOG = LogFactory.getLog(AbstractSplitter.class);
    /**
     * The address of the target endpoint
     */
    private ExchangeTarget target;
    /**
     * Indicates if faults and errors from splitted parts should be sent
     * back to the consumer.  In such a case, only the first fault or
     * error received will be reported.
     * Note that if the consumer is synchronous, it will be blocked
     * until all parts have been successfully acked, or
     * a fault or error is reported, and the exchange will be kept in the
     * store for recovery. 
     */
    private boolean reportErrors;
    /**
     * Indicates if incoming attachments should be forwarded with the new exchanges.
     */
    private boolean forwardAttachments;
    /**
     * Indicates if properties on the incoming message should be forwarded.
     */
    private boolean forwardProperties;
    /**
     * The correlation property used by this component
     */
    //private String correlation;
    /**
     * Specifies wether exchanges for all parts are sent synchronously or not.
     */
    private boolean synchronous;
    
    /**
     * @return the synchronous
     */
    public boolean isSynchronous() {
        return synchronous;
    }

    /**
     * Specifies wether exchanges for all parts are sent synchronously or not.
     *
     * @param synchronous the synchronous to set
     */
    public void setSynchronous(boolean synchronous) {
        this.synchronous = synchronous;
    }

    /**
     * @return Returns the reportErrors.
     */
    public boolean isReportErrors() {
        return reportErrors;
    }

    /**
     * Indicates if faults and errors from splitted parts should be sent
     * back to the consumer.  In such a case, only the first fault or
     * error received will be reported.
     * Note that if the consumer is synchronous, it will be blocked
     * until all parts have been successfully acked, or
     * a fault or error is reported, and the exchange will be kept in the
     * store for recovery.
     *
     * @param reportErrors The reportErrors to set.
     */
    public void setReportErrors(boolean reportErrors) {
        this.reportErrors = reportErrors;
    }

    /**
     * @return Returns the target.
     */
    public ExchangeTarget getTarget() {
        return target;
    }

    /**
     * The address of the target endpoint.
     *
     * @param target The target to set.
     */
    public void setTarget(ExchangeTarget target) {
        this.target = target;
    }

    /**
     * @return Returns the forwardAttachments.
     */
    public boolean isForwardAttachments() {
        return forwardAttachments;
    }

    /**
     * Indicates if incoming attachments should be forwarded with the new exchanges.
     *
     * @param forwardAttachments The forwardAttachments to set.
     */
    public void setForwardAttachments(boolean forwardAttachments) {
        this.forwardAttachments = forwardAttachments;
    }

    /**
     * @return Returns the forwardProperties.
     */
    public boolean isForwardProperties() {
        return forwardProperties;
    }

    /**
     * Indicates if properties on the incoming message should be forwarded.
     * 
     * @param forwardProperties The forwardProperties to set.
     */
    public void setForwardProperties(boolean forwardProperties) {
        this.forwardProperties = forwardProperties;
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
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.eip.EIPEndpoint#start()
     */
    public void start() throws Exception {
        super.start();
        // Create correlation property
        //correlation = "Splitter.Correlation." + getContext().getComponentName();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.eip.EIPEndpoint#processSync(javax.jbi.messaging.MessageExchange)
     */
    protected void processSync(MessageExchange exchange) throws Exception {
        if (!(exchange instanceof InOnly)
            && !(exchange instanceof RobustInOnly)) {
            fail(exchange, new UnsupportedOperationException("Use an InOnly or RobustInOnly MEP"));
            return;
        }
        MessageExchange[] parts = createParts(exchange);
        for (int i = 0; i < parts.length; i++) {
            target.configureTarget(parts[i], getContext());
            if (reportErrors || isSynchronous()) {
                sendSync(parts[i]);
                if (parts[i].getStatus() == ExchangeStatus.DONE) {
                    // nothing to do
                } else if (parts[i].getStatus() == ExchangeStatus.ERROR) {
                    if (reportErrors) {
                        fail(exchange, parts[i].getError());
                        return;
                    }
                } else if (parts[i].getFault() != null) {
                    if (reportErrors) {
                        MessageUtil.transferToFault(MessageUtil.copyFault(parts[i]), exchange);
                        done(parts[i]);
                        sendSync(exchange);
                        return;
                    } else {
                        done(parts[i]);
                    }
                } else {
                    throw new IllegalStateException("Exchange status is " + ExchangeStatus.ACTIVE
                            + " but has no Fault message");
                }
            } else {
                sendSync(parts[i]);
            }
        }
        done(exchange);
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.eip.EIPEndpoint#processAsync(javax.jbi.messaging.MessageExchange)
     */
    protected void processAsync(MessageExchange exchange) throws Exception {
        if (exchange.getRole() == MessageExchange.Role.CONSUMER) {
            String corrId = (String) exchange.getMessage("in").getProperty(SPLITTER_CORRID);
            int count = (Integer) exchange.getMessage("in").getProperty(SPLITTER_COUNT);
            Integer acks = null;
            Lock lock = lockManager.getLock(corrId);
            lock.lock();
            boolean removeLock = true;
            try {
                acks = (Integer) store.load(corrId + ".acks");
                if (exchange.getStatus() == ExchangeStatus.DONE) {
                    // If the acks integer is not here anymore, the message response has been sent already
                    if (acks != null) {
                        acks++;
                        if (acks < count) {
                            MessageExchange me = (MessageExchange) store.load(corrId);
                            done(me);
                        } else {
                            store.store(corrId + ".acks", Integer.valueOf(acks));
                            removeLock = false;
                        }
                    }
                } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
                    // If the acks integer is not here anymore, the message response has been sent already
                    if (acks != null) {
                        acks++;
                        if (reportErrors) {
                            MessageExchange me = (MessageExchange) store.load(corrId);
                            fail(me, exchange.getError());
                        } else  if (acks < count) {
                            MessageExchange me = (MessageExchange) store.load(corrId);
                            done(me);
                        } else {
                            store.store(corrId + ".acks", Integer.valueOf(acks));
                            removeLock = false;
                        }
                    }
                } else if (exchange.getFault() != null) {
                    // If the acks integer is not here anymore, the message response has been sent already
                    if (acks != null) {
                        acks++;
                        if (reportErrors) {
                            MessageExchange me = (MessageExchange) store.load(corrId);
                            MessageUtil.transferToFault(MessageUtil.copyFault(exchange), me);
                            send(me);
                            done(exchange);
                        } else  if (acks < count) {
                            MessageExchange me = (MessageExchange) store.load(corrId);
                            done(me);
                        } else {
                            store.store(corrId + ".acks", Integer.valueOf(acks));
                            removeLock = false;
                        }
                    } else {
                        done(exchange);
                    }
                }
            } finally {
                try {
                    lock.unlock();
                } catch (Exception ex) {
                    LOG.info("Caught exception while attempting to release lock", ex);
                }
                if (removeLock) {
                    lockManager.removeLock(corrId);
                }
            }
        } else {
            if (exchange.getStatus() == ExchangeStatus.DONE) {
                return;
            } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
                return;
            } else if (!(exchange instanceof InOnly) && !(exchange instanceof RobustInOnly)) {
                fail(exchange, new UnsupportedOperationException("Use an InOnly or RobustInOnly MEP"));
            } else if (exchange.getFault() != null) {
                done(exchange);
            } else {
                store.store(exchange.getExchangeId(), exchange);
                MessageExchange[] parts = createParts(exchange);
                store.store(exchange.getExchangeId() + ".acks", Integer.valueOf(0));
                for (int i = 0; i < parts.length; i++) {
                    target.configureTarget(parts[i], getContext());
                    send(parts[i]);
                }
                // do not done the exchange on provider side!
                //done(exchange);
            }
        }
    }
    
    protected MessageExchange[] createParts(MessageExchange exchange) throws Exception {
        NormalizedMessage in = MessageUtil.copyIn(exchange);
        Source[] srcParts = split(in.getContent());
        MessageExchange[] parts = new MessageExchange[srcParts.length];
        for (int i = 0; i < srcParts.length; i++) {
            parts[i] = createPart(exchange.getPattern(), in, srcParts[i]);
            NormalizedMessage msg = parts[i].getMessage("in");
            msg.setProperty(SPLITTER_COUNT, new Integer(srcParts.length));
            msg.setProperty(SPLITTER_INDEX, new Integer(i));
            msg.setProperty(SPLITTER_CORRID, exchange.getExchangeId());
        }
        return parts;
    }
    
    protected MessageExchange createPart(URI pattern,
                                         NormalizedMessage srcMessage, 
                                         Source content) throws Exception {
        MessageExchange me = getExchangeFactory().createExchange(pattern);
        NormalizedMessage in = me.createMessage();
        in.setContent(content);
        me.setMessage(in, "in");
        if (forwardAttachments) {
            Set names = srcMessage.getAttachmentNames();
            for (Iterator iter = names.iterator(); iter.hasNext();) {
                String name = (String) iter.next();
                in.addAttachment(name, srcMessage.getAttachment(name));
            }
        }
        if (forwardProperties) {
            Set names  = srcMessage.getPropertyNames();
            for (Iterator iter = names.iterator(); iter.hasNext();) {
                String name = (String) iter.next();
                in.setProperty(name, srcMessage.getProperty(name));
            }
        }
        return me;
    }

    protected abstract Source[] split(Source main) throws Exception;

}
