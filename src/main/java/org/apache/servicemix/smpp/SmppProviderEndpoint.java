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

import java.io.IOException;
import java.util.Date;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.jbi.helper.MessageUtil;
import org.apache.servicemix.smpp.marshaler.DefaultSmppMarshaler;
import org.apache.servicemix.smpp.marshaler.SmppMarshalerSupport;
import org.jsmpp.InvalidResponseException;
import org.jsmpp.PDUException;
import org.jsmpp.bean.Alphabet;
import org.jsmpp.bean.BindType;
import org.jsmpp.bean.ESMClass;
import org.jsmpp.bean.GeneralDataCoding;
import org.jsmpp.bean.MessageClass;
import org.jsmpp.bean.MessageRequest;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.RegisteredDelivery;
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.TypeOfNumber;
import org.jsmpp.extra.NegativeResponseException;
import org.jsmpp.extra.ResponseTimeoutException;
import org.jsmpp.session.BindParameter;
import org.jsmpp.session.SMPPSession;
import org.jsmpp.util.AbsoluteTimeFormatter;
import org.jsmpp.util.TimeFormatter;

/**
 * A provider component receives XML message from the NMR and converts into SMPP
 * packet and sends it to SMS.
 * 
 * @org.apache.xbean.XBean element="provider"
 * @author jbonofre
 * @author lhein
 */
public class SmppProviderEndpoint extends ProviderEndpoint implements SmppEndpointType {

    // logging facility
    private final static transient Log log = LogFactory.getLog(SmppProviderEndpoint.class);

    // SMPP default port number
    private final static int SMPP_DEFAULT_PORT = 2775;
    // SMPP system type
    private final static String SYSTEM_TYPE = "cp";

    private SMPPSession session;
    private static TimeFormatter timeFormatter = new AbsoluteTimeFormatter();

    private String host;
    private int port;
    private String systemId;
    private String password;
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
        // connect to the SMPP server
        this.connect();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.SimpleEndpoint#stop()
     */
    @Override
    public synchronized void stop() throws Exception {
        super.stop();
        // disconnect from the SMPP server
        this.disconnect();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.AbstractEndpoint#validate()
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
		// check the marshaler
		if (this.marshaler == null) {
			this.marshaler = new DefaultSmppMarshaler();
		}
		// check the enquire link timer
		if (this.enquireLinkTimer <= 0) {
		    throw new IllegalArgumentException("The enquireLinkTimer value must be greater than 0.");
		}
		// check the transaction timer
		if (this.transactionTimer <= 0) {
		    throw new IllegalArgumentException("The transactionTimer value must be greater than 0.");
		}
    }

    /**
     * Connect to the SMPP server and bind the SMPP session
     */
    private void connect() {
        // create the SMPPSession
        session = new SMPPSession();
        // define the enquireLinkTimer
        session.setEnquireLinkTimer(this.enquireLinkTimer);
        // define the transationTimer
        session.setTransactionTimer(this.transactionTimer);
        // connect and bind to the SMPP server
        try {
            session.connectAndBind(this.host, this.port, new BindParameter(BindType.BIND_TX, this.systemId,
                                                                           this.password, SYSTEM_TYPE,
                                                                           TypeOfNumber.UNKNOWN,
                                                                           NumberingPlanIndicator.UNKNOWN,
                                                                           null));
        } catch (IOException ioException) {
            log.error("Error connecting to the SMPP server", ioException);
            return;
        }
    }

    /**
     * Unbind the SMPP session and close the connection to the SMPP server
     */
    private void disconnect() {
        if (this.session == null) {
            // seems to not be opened at all
            return;
        }
        this.session.unbindAndClose();
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.servicemix.common.endpoints.ProviderEndpoint#processInOnly
     * (javax.jbi.messaging.MessageExchange,
     * javax.jbi.messaging.NormalizedMessage)
     */
    @Override
    protected void processInOnly(MessageExchange exchange, NormalizedMessage inMsg) throws Exception {
        process(exchange, inMsg);
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.servicemix.common.endpoints.ProviderEndpoint#processInOut(
     * javax.jbi.messaging.MessageExchange,
     * javax.jbi.messaging.NormalizedMessage,
     * javax.jbi.messaging.NormalizedMessage)
     */
    @Override
    protected void processInOut(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out)
        throws Exception {
        // we are reading the source of the NormalizedMessage multiple times
        // (else we receive a IOException: Stream closed)
        MessageUtil.enableContentRereadability(in);

        process(exchange, in);

        // message was delivered, simply copy the in message with properties and
        // attachements to out
        MessageUtil.transfer(in, out);
    }

    /**
     * process the incoming exchange
     * 
     * @param exchange the message exchange
     * @param in the in message
     * @throws TransformerException on transformation errors
     * @throws MessagingException on messaging errors
     */
    private void process(MessageExchange exchange, NormalizedMessage in) throws TransformerException,
        MessagingException {
        // let the marshaler create a SM content
        MessageRequest sm = marshaler.fromNMS(exchange, in);

        try {
            log.debug("Submiting request: " + sm);
            String messageId = session
                .submitShortMessage("CMT", TypeOfNumber.valueOf(sm.getSourceAddrTon()),
                                    NumberingPlanIndicator.valueOf(sm.getSourceAddrNpi()),
                                    sm.getSourceAddr(), TypeOfNumber.valueOf(sm.getDestAddrTon()),
                                    NumberingPlanIndicator.valueOf(sm.getDestAddrNpi()), sm.getDestAddress(),
                                    new ESMClass(), (byte)0, (byte)1, timeFormatter.format(new Date()), null,
                                    new RegisteredDelivery(SMSCDeliveryReceipt.DEFAULT), (byte)0,
                                    new GeneralDataCoding(false, false, MessageClass.CLASS1,
                                                          Alphabet.ALPHA_DEFAULT), (byte)0, sm
                                        .getShortMessage());

            log.debug("Message sent with ID " + messageId);
        } catch (PDUException pduException) {
            log.error("Invalid PDU parameter", pduException);
            fail(exchange, new Exception("Invalid PDU parameter", pduException));
        } catch (ResponseTimeoutException responseTimeoutException) {
            log.error("Response timeout");
            fail(exchange, new Exception("Response timeout", responseTimeoutException));
        } catch (InvalidResponseException invalidResponseException) {
            log.error("Invalid response");
            fail(exchange, new Exception("Invalid response", invalidResponseException));
        } catch (NegativeResponseException negativeResponseException) {
            log.error("Negative response");
            fail(exchange, new Exception("Negative response", negativeResponseException));
        } catch (IOException ioException) {
            log.error("IO error during message send");
            fail(exchange, new Exception("IO error during message send", ioException));
        }
    }

    public String getHost() {
        return host;
    }

    /**
     * <p>
     * This attribute specifies the host name to use for connecting to the
     * server.<br/>
     * <p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>null</b></i>
     * 
     * @param host a <code>String</code> value representing the host name
     */
    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getSystemId() {
        return systemId;
    }

    /**
     * <p>
     * This attribute specifies the system id to use for connecting to the
     * server.<br/>
     * <p>
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
     * <p>
     * This attribute specifies the password to use for connecting to the
     * server.<br/>
     * <p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>null</b></i>
     * 
     * @param password a <code>String</code> value representing the password
     */
    public void setPassword(String password) {
        this.password = password;
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
     *            marshaler
     */
    public void setMarshaler(SmppMarshalerSupport marshaler) {
        this.marshaler = marshaler;
    }

    public int getEnquireLinkTimer() {
        return enquireLinkTimer;
    }

    /**
     * <p>
     * This attribute specifies the enquire link timer defining the resend time
     * interval.<br/>
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>50000</b> milliseconds</i>
     * 
     * @param enquireLinkTimer a <code>int</code> value representing the enquire
     *            link timer
     */
    public void setEnquireLinkTimer(int enquireLinkTimer) {
        this.enquireLinkTimer = enquireLinkTimer;
    }

    public int getTransactionTimer() {
        return transactionTimer;
    }

    /**
     * <p>
     * This attribute specifies the transaction timer defining the maximum
     * lifetime of a message.<br/>
     * </p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>100000</b> milliseconds</i>
     * 
     * @param transactionTimer a <code>int</code> value representing the
     *            transaction timer
     */
    public void setTransactionTimer(int transactionTimer) {
        this.transactionTimer = transactionTimer;
    }   
}