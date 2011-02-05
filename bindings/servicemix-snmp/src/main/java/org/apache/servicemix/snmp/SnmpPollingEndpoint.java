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
package org.apache.servicemix.snmp;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.common.endpoints.PollingEndpoint;
import org.apache.servicemix.snmp.marshaler.DefaultSnmpMarshaler;
import org.apache.servicemix.snmp.marshaler.SnmpMarshalerSupport;
import org.apache.servicemix.snmp.util.OIDList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.event.ResponseListener;
import org.snmp4j.mp.MPv3;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.security.SecurityModels;
import org.snmp4j.security.SecurityProtocols;
import org.snmp4j.security.USM;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 * This is the polling endpoint for the snmp component.
 * 
 * @org.apache.xbean.XBean element="poller"
 * @author lhein
 */
public class SnmpPollingEndpoint extends PollingEndpoint implements SnmpEndpointType, ResponseListener {

    private final Logger logger = LoggerFactory.getLogger(SnmpPollingEndpoint.class);

    public static final String DEFAULT_COMMUNITY = "public";
    public static final int DEFAULT_SNMP_VERSION = SnmpConstants.version1;
    public static final int DEFAULT_SNMP_RETRIES = 2;
    public static final int DEFAULT_SNMP_TIMEOUT = 1500;

    private Address targetAddress;
    private TransportMapping transport;
    private Snmp snmp;
    private USM usm;
    private CommunityTarget target;
    private PDU pdu;

    private OIDList oids = new OIDList();
    private String address;
    private int retries = DEFAULT_SNMP_RETRIES;
    private int timeout = DEFAULT_SNMP_TIMEOUT;
    private int snmpVersion = DEFAULT_SNMP_VERSION;
    private String snmpCommunity = DEFAULT_COMMUNITY;

    private SnmpMarshalerSupport marshaler = new DefaultSnmpMarshaler();

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.ConsumerEndpoint#activate()
     */
    @Override
    public synchronized void activate() throws Exception {
        super.activate();

        this.targetAddress = GenericAddress.parse(this.address);
        this.transport = new DefaultUdpTransportMapping();
        this.snmp = new Snmp(transport);
        this.usm = new USM(SecurityProtocols.getInstance(), new OctetString(MPv3.createLocalEngineID()), 0);
        SecurityModels.getInstance().addSecurityModel(usm);

        // setting up target
        target = new CommunityTarget();
        target.setCommunity(new OctetString(this.snmpCommunity));
        target.setAddress(targetAddress);
        target.setRetries(this.retries);
        target.setTimeout(this.timeout);
        target.setVersion(this.snmpVersion);

        // creating PDU
        this.pdu = new PDU();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.PollingEndpoint#start()
     */
    @Override
    public synchronized void start() throws Exception {
        super.start();

        // again listen to the transport
        this.transport.listen();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.PollingEndpoint#stop()
     */
    @Override
    public synchronized void stop() throws Exception {
        // stop listening to the transport
        if (this.transport.isListening()) {
            this.transport.close();
        }

        super.stop();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.ConsumerEndpoint#validate()
     */
    @Override
    public void validate() throws DeploymentException {
        super.validate();

        // check address not null
        if (this.address == null) {
            throw new DeploymentException("The address attribute has to be specified!");
        }

        // check if address is valid
        try {
            if (GenericAddress.parse(this.address) == null) {
                throw new DeploymentException("The specified address " + address + " is not valid!");
            }
        } catch (IllegalArgumentException ex) {
            throw new DeploymentException("The specified address " + address + " is not valid!");
        }
        
        // finally check if the oid vector contains values
        if (this.oids == null || this.oids.size()<=0) {
            // the poller would be unemployed
            throw new DeploymentException("There are no OIDs defined to be polled. Check your oids attribute.");
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.PollingEndpoint#poll()
     */
    @Override
    public void poll() throws Exception {
        this.pdu.clear();
        this.pdu.setType(PDU.GET);

        // prepare the request items
        for (OID oid : oids) {
            this.pdu.add(new VariableBinding(oid));
        }

        // send the request
        snmp.send(pdu, target, null, this);
    }

    /*
     * (non-Javadoc)
     * @see
     * org.apache.servicemix.common.endpoints.AbstractEndpoint#process(javax
     * .jbi.messaging.MessageExchange)
     */
    @Override
    public void process(MessageExchange exchange) throws Exception {
        // only DONE and ERROR states will be received here and this
        // endpoint is not interested in such messages at all
    }

    /*
     * (non-Javadoc)
     * @see
     * org.snmp4j.event.ResponseListener#onResponse(org.snmp4j.event.ResponseEvent
     * )
     */
    public void onResponse(ResponseEvent event) {
        // Always cancel async request when response has been received
        // otherwise a memory leak is created! Not canceling a request
        // immediately can be useful when sending a request to a broadcast
        // address.
        ((Snmp)event.getSource()).cancel(event.getRequest(), this);

        // check for valid response
        if (event.getRequest() == null || event.getResponse() == null) {
            // ignore null requests/responses
            logger.debug("Received invalid snmp event. Request: {} / Response: {}", event.getRequest(),
                      event.getResponse());
            return;
        }

        // now prepare the message and send it
        sendSnmpDataMessage(event.getRequest(), event.getResponse());
    }

    /**
     * sends the message to the bus
     * 
     * @param request the request PDU
     * @param response the response PDU
     */
    private void sendSnmpDataMessage(PDU request, PDU response) {
        try {
            // create a inOnly exchange
            InOnly io = getExchangeFactory().createInOnlyExchange();

            // configure the exchange target
            configureExchangeTarget(io);

            // create the in message
            NormalizedMessage inMsg = io.createMessage();

            // now let the marshaller convert the snmp data into a normalized
            // message to send to jbi bus
            this.marshaler.convertToJBI(io, inMsg, request, response);

            // then put the in message into the inOnly exchange
            io.setInMessage(inMsg);

            // and use send to deliver it
            getChannel().send(io);
        } catch (MessagingException ex) {
            logger.error("Error while trying to send the snmp event to the jbi bus", ex);
        }
    }

    public String getAddress() {
        return this.address;
    }

    /**
     * <p>Specifies the connection URI used to connect to a snmp capable device.
     * <br /><br />
     * <b><u>Template:</u></b> <br />
     *     &nbsp;&nbsp;&nbsp;<i>&lt;protocol&gt;:&lt;host&gt;/&lt;port&gt;</i>
     * <br /><br />
     * <b><u>Details:</u></b><br /><br/>
     * <table border="0" cellpadding="0" cellspacing="0">
     * <tr>
     * 		<td width="40%" align="left"><b><u>Name</u></b></td>
     * 		<td width="60%" align="left"><b><u>Description</u></b></td>
     * </tr>
     * <tr>
     * 		<td>protocol</td>
     * 		<td>the protocol to use (udp or tcp)</td>
     * </tr>
     * <tr>
     * 		<td>host</td>
     * 		<td>the name or ip address of the snmp capable device</td>
     * </tr>
     * <tr>
     * 		<td>port</td>
     * 		<td>the port number to use</td>
     * </tr>
     * </table>
     * <br/>
     * <b><u>Example:</u></b><br />
     * &nbsp;&nbsp;&nbsp;<i>udp:192.168.2.122/161</i></p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>null</b></i><br/><br/>
     * 
     * @param address 
     * 				a <code>String</code> value containing the connection details
     */
    public void setAddress(String address) {
        this.address = address;
    }

    public int getRetries() {
        return this.retries;
    }

    /**
     * <p>Specifies the connection retries.</p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>2</b></i><br/><br/>
     * 
     * @param retries 
     * 				a <code>int</code> value containing the retry count
     */
    public void setRetries(int retries) {
        this.retries = retries;
    }

    public int getTimeout() {
        return this.timeout;
    }

    /**
     * <p>Specifies the connection time out in milliseconds.</p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>1500</b></i><br/><br/>
     * 
     * @param timeout 
     * 				a <code>int</code> value containing the time out in millis
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getSnmpVersion() {
        return this.snmpVersion;
    }

    /**
     * <p>Specifies the snmp protocol version to use.</p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>0 (version 1)</b></i><br/><br/>
     * 
     * @param snmpVersion 
     * 				a <code>int</code> value containing the snmp version
     */
    public void setSnmpVersion(int snmpVersion) {
        this.snmpVersion = snmpVersion;
    }

    public String getSnmpCommunity() {
        return this.snmpCommunity;
    }

    /**
     * <p>Specifies the snmp community to use.</p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>"public"</b></i><br/><br/>
     * 
     * @param snmpCommunity 
     * 				a <code>String</code> value containing the snmp community
     */
    public void setSnmpCommunity(String snmpCommunity) {
        this.snmpCommunity = snmpCommunity;
    }

    public SnmpMarshalerSupport getMarshaler() {
        return this.marshaler;
    }

    /**
     * <p>Specifies a marshaler class which provides the logic for converting 
     * a snmp response into a normalized message. This class has to implement 
     * the <code>SnmpMarshalerSupport</code> interface. If you don't specify a 
     * marshaler, the <code>DefaultSnmpMarshaler</code> will be used.</p>
     * 
     * @param marshaler 
     * 				a class which implements <code>SnmpMarshalerSupport</code>
     */
    public void setMarshaler(SnmpMarshalerSupport marshaler) {
        this.marshaler = marshaler;
    }

    public OIDList getOids() {
        return this.oids;
    }

    /**
     * <p>Specifies a reference to a list of OID values which will be used for 
     * the snmp request. You have two possibilities how to specify the value:
     * <br /><br />
     * &nbsp;<i>a) referencing to a file containing a list of OID values separated by a line feed
     * <br/>&nbsp;&nbsp;&nbsp;&nbsp;or<br/>
     * &nbsp;<i>b) defining a coma (<b>,</b>) separated list of OID values 
     * <br /><br />
     * <b><u>Examples:</u></b><br />
     * &nbsp;&nbsp;&nbsp;<i>a) oids="classpath:myOids.txt"<br />
     * &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;oids="file:/home/lhein/snmp/device_a/oids.txt"<br/>
     * <br />
     * &nbsp;&nbsp;&nbsp;<i>b) oids="1.3.6.1.2.1.1.3.0 , 1.3.6.1.2.1.25.3.2.1.5.1 , 1.3.6.1.2.1.25.3.5.1.1.1 , 1.3.6.1.2.1.43.5.1.1.11.1"</i></p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>null</b></i><br/><br/>
     * 
     * @param oids 
     * 				a <code>OIDList</code> containing the OID values for the request
     */
    public void setOids(OIDList oids) {
        this.oids = oids;
    }
}
