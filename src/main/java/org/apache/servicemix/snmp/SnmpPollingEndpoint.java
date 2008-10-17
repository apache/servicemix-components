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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.common.endpoints.PollingEndpoint;
import org.apache.servicemix.snmp.marshaler.DefaultSnmpMarshaler;
import org.apache.servicemix.snmp.marshaler.SnmpMarshalerSupport;
import org.apache.servicemix.snmp.util.OIDList;
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
    private static final transient Log LOG = LogFactory.getLog(SnmpPollingEndpoint.class);

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
            LOG.debug("Received invalid snmp event. Request: " + event.getRequest() + " / Response: "
                      + event.getResponse());
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
            LOG.error("Error while trying to send the snmp event to the jbi bus", ex);
        }
    }

    /**
     * * @return Returns the address.
     */
    public String getAddress() {
        return this.address;
    }

    /**
     * @param address The address to set.
     */
    public void setAddress(String address) {
        this.address = address;
    }

    /**
     * @return Returns the retries.
     */
    public int getRetries() {
        return this.retries;
    }

    /**
     * @param retries The retries to set.
     */
    public void setRetries(int retries) {
        this.retries = retries;
    }

    /**
     * @return Returns the timeout.
     */
    public int getTimeout() {
        return this.timeout;
    }

    /**
     * @param timeout The timeout to set.
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * @return Returns the snmpVersion.
     */
    public int getSnmpVersion() {
        return this.snmpVersion;
    }

    /**
     * @param snmpVersion The snmpVersion to set.
     */
    public void setSnmpVersion(int snmpVersion) {
        this.snmpVersion = snmpVersion;
    }

    /**
     * @return Returns the snmpCommunity.
     */
    public String getSnmpCommunity() {
        return this.snmpCommunity;
    }

    /**
     * @param snmpCommunity The snmpCommunity to set.
     */
    public void setSnmpCommunity(String snmpCommunity) {
        this.snmpCommunity = snmpCommunity;
    }

    /**
     * @return Returns the marshaler.
     */
    public SnmpMarshalerSupport getMarshaler() {
        return this.marshaler;
    }

    /**
     * @param marshaler The marshaler to set.
     */
    public void setMarshaler(SnmpMarshalerSupport marshaler) {
        this.marshaler = marshaler;
    }

    /** * @return Returns the oids.
     */
    public OIDList getOids() {
        return this.oids;
    }

    /**
     * @param oids The oids to set.
     */
    public void setOids(OIDList oids) {
        this.oids = oids;
    }
}
