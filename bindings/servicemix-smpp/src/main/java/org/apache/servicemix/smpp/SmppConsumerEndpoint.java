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
package org.apache.servicemix.smpp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.common.endpoints.ConsumerEndpoint;
import org.apache.servicemix.smpp.marshaler.DefaultSmppMarshaler;
import org.apache.servicemix.smpp.marshaler.SmppMarshalerSupport;
import org.jsmpp.bean.*;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.MessageReceiverListener;
import org.jsmpp.session.SMPPSession;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.*;
import java.io.IOException;

/**
 * A polling component which bind with jSMPP and receive SMPP messages and sends
 * the SMPPs into the JBI bus as messages.
 *
 * @author jbonofre
 * @author lhein
 * 
 * @org.apache.xbean.XBean element="consumer"
 */
public class SmppConsumerEndpoint extends ConsumerEndpoint implements SmppEndpointType {

    // logging facility
    private final static transient Log log = LogFactory.getLog(SmppConsumerEndpoint.class);

    // default SMPP port
    private static final int SMPP_DEFAULT_PORT = 2775;
    // default system type
    private static final String DEFAULT_SYSTEM_TYPE = "cp";

    // SMPP session and listener
    private SMPPSession session;

    // attributes
    private String host;
    private int port;
    private String systemId;
    private String password;
    private String systemType=DEFAULT_SYSTEM_TYPE;
    private int enquireLinkTimer = 50000;
    private int transactionTimer = 100000;

    private SmppMarshalerSupport marshaler;

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.SimpleEndpoint#start()
     */

    @Override
    public synchronized void start() throws Exception {
        super.start();
        // establish connection
        this.connect();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.SimpleEndpoint#stop()
     */

    @Override
    public synchronized void stop() throws Exception {
        // disconnect now
        this.disconnect();
        super.stop();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.ConsumerEndpoint#validate()
     */

    @Override
    public void validate() throws DeploymentException {
        super.validate();

        // check for valid port number
        if (this.port <= 0) {
            log.warn("Invalid SMPP port specified. Use the default one : " + SMPP_DEFAULT_PORT);
            this.port = SMPP_DEFAULT_PORT;
        }
        // check for valid host
        if (this.host == null || this.host.trim().length() <= 0) {
            throw new IllegalArgumentException("The SMPP host name is mandatory.");
        }
        // check for valid system ID
        if (this.systemId == null || this.systemId.trim().length() <= 0) {
            throw new IllegalArgumentException("The SMPP system ID is mandatory.");
        }
        // check the enquireLinkTimer
        if (this.enquireLinkTimer <= 0) {
            throw new IllegalArgumentException("The enquireLinkTimer value must be greater than 0.");
        }
        // check the transactionTimer
        if (this.transactionTimer <= 0) {
            throw new IllegalArgumentException("The transactionTimer value must be greater than 0.");
        }
        // check the marshaler
        if (this.marshaler == null) {
            this.marshaler = new DefaultSmppMarshaler();
        }
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.servicemix.common.endpoints.AbstractEndpoint#process(javax
     * .jbi.messaging.MessageExchange)
     */

    @Override
    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            // received DONE for a sent message
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            // received ERROR state for a sent message
            // there is no real error handling here for now
            return;
        } else {
            throw new MessagingException("Unsupported exchange received...");
        }
    }

    /**
     * Connect to the SMPP server
     */
    private void connect() {
        // create the SMPP session
        session = new SMPPSession();
        // define the enquireLinkTimer
        session.setEnquireLinkTimer(this.enquireLinkTimer);
        // define the transactionTimer
        session.setTransactionTimer(this.transactionTimer);
        // define a message receiver listener on the SMPP connection
        session.setMessageReceiverListener(new MessageReceiverListener() {
            /*
             * (non-Javadoc)
             * @see
             * org.jsmpp.session.MessageReceiverListener#onAcceptAlertNotification
             * (org.jsmpp.bean.AlertNotification)
             */
            public void onAcceptAlertNotification(AlertNotification alertNotification) {
                // nothing to do
            }

            /*
             * (non-Javadoc)
             * @see
             * org.jsmpp.session.MessageReceiverListener#onAcceptDeliverSm(org
             * .jsmpp.bean.DeliverSm)
             */
            public void onAcceptDeliverSm(DeliverSm deliverSm) {
                try {
                    InOnly exchange = getExchangeFactory().createInOnlyExchange();
                    NormalizedMessage in = exchange.createMessage();
                    exchange.setInMessage(in);
                    marshaler.toNMS(in, deliverSm);
                    send(exchange);
                } catch (MessagingException messagingException) {
                    log.error("Unable to send the received SMS to the NMR", messagingException);
                }
            }

        });

        // connect and bind to the SMPP server
        try {
            session.connectAndBind(this.host, this.port, new BindParameter(BindType.BIND_RX, this.systemId,
                    this.password, this.systemType,
                    TypeOfNumber.UNKNOWN,
                    NumberingPlanIndicator.UNKNOWN,
                    null));
        } catch (IOException ioException) {
            log.error("Error connecting to the SMPP server", ioException);
            return;
        }
    }

    /**
     * Disconnect from the SMPP server
     */
    private void disconnect() {
        if (this.session == null) {
            // seems to not be opened at all
            return;
        }
        this.session.unbindAndClose();
    }

    public int getPort() {
        return port;
    }

    /**
     * <p/>
     * This attribute specifies the port number to use for connecting to the
     * server.<br/>
     * <p/>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>2775</b></i>
     *
     * @param port a <code>int</code> value representing the port number
     */
    public void setPort(int port) {
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    /**
     * <p/>
     * This attribute specifies the host name to use for connecting to the
     * server.<br/>
     * <p/>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>null</b></i>
     *
     * @param host a <code>String</code> value representing the host name
     */
    public void setHost(String host) {
        this.host = host;
    }

    public String getSystemId() {
        return systemId;
    }

    /**
     * <p/>
     * This attribute specifies the system id to use for connecting to the
     * server.<br/>
     * <p/>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>null</b></i>
     *
     * @param systemId a <code>String</code> value representing the system id
     */
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public String getPassword() {
        return password;
    }

    /**
     * <p/>
     * This attribute specifies the password to use for connecting to the
     * server.<br/>
     * <p/>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>null</b></i>
     *
     * @param password a <code>String</code> value representing the password
     */
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getSystemType() {
        return systemType;
    }

    /**
     * <p />
     * This attribute specifies the system type that will be used for connecting to the server. <p />
     * <i/> The default value is <b>cp</b>
     * @param systemType 
     */
    public void setSystemType(String systemType) {
        this.systemType = systemType;
    }

    public SmppMarshalerSupport getMarshaler() {
        return marshaler;
    }

    /**
     * <p>
     * With this method you can specify a marshaler class which provides the
     * logic for converting a sms message into a normalized message. This class
     * has to implement the interface class <code>SmppMarshaler</code>. If you
     * don't specify a marshaler, the <code>DefaultSmppMarshaler</code> will be
     * used.
     * </p>
     *
     * @param marshaler a <code>SmppMarshaler</code> class representing the
     *                  marshaler
     */
    public void setMarshaler(SmppMarshalerSupport marshaler) {
        this.marshaler = marshaler;
    }

    public int getEnquireLinkTimer() {
        return enquireLinkTimer;
    }

    /**
     * <p>
     * This attribute specifies the enquire link timer defining the SMSC time
     * interval.<br/>
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>50000</b></i>
     *
     * @param enquireLinkTimer a <code>int</code> value representing the enquire
     *                         link timer
     */
    public void setEnquireLinkTimer(int enquireLinkTimer) {
        this.enquireLinkTimer = enquireLinkTimer;
    }

    public int getTransactionTimer() {
        return transactionTimer;
    }

    /**
     * <p>
     * This attribute specifies the transaction timer defining the SMSC timeout.
     * <br/>
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>100000</b></i>
     *
     * @param transactionTimer a <code>int</code> value representing the
     *                         transaction timer (timeout)
     */
    public void setTransactionTimer(int transactionTimer) {
        this.transactionTimer = transactionTimer;
    }
}
