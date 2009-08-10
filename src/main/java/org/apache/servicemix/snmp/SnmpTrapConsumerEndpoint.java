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

import org.apache.servicemix.common.endpoints.ConsumerEndpoint;
import org.apache.servicemix.snmp.marshaler.DefaultSnmpMarshaler;
import org.apache.servicemix.snmp.marshaler.SnmpMarshalerSupport;
import org.snmp4j.CommandResponder;
import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.UdpAddress;
import org.snmp4j.transport.DefaultUdpTransportMapping;

/**
 * This is the trap endpoint for the snmp component.
 * 
 * This endpoint receives and process trap PDUs from
 * a device. Then it sends an exchange to the target service 
 * containing the processed content.
 * 
 * @org.apache.xbean.XBean element="trap-consumer"
 * @author gperdiguero
 * @author jbonofre
 */
public class SnmpTrapConsumerEndpoint extends ConsumerEndpoint implements SnmpEndpointType, CommandResponder {
    
    public static final boolean DEFAULT_ENABLED_VALUE = true;
    
    private Address listenGenericAddress;
    private Snmp snmp;
    private TransportMapping transport;

    private String listenAddress;
    private boolean enabled = DEFAULT_ENABLED_VALUE;
    
    private SnmpMarshalerSupport marshaler = new DefaultSnmpMarshaler();

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.ConsumerEndpoint#activate()
     */
    @Override
    public synchronized void activate() throws Exception {
        super.activate();
        // load connection data only if the endpoint is enabled
        if (isEnabled()) {
            logger.debug("Activating endpoint");
            this.listenGenericAddress = GenericAddress.parse(this.listenAddress);
            this.transport = new DefaultUdpTransportMapping((UdpAddress) this.listenGenericAddress);
            this.snmp = new Snmp(transport);
            snmp.addCommandResponder(this);
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.PollingEndpoint#start()
     */
    @Override
    public synchronized void start() throws Exception {
        super.start();
        
        // listening is only allowed if the endpoint was initialized
        if (isEnabled()) {
            // listen to the transport
            this.transport.listen();
        }
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.common.endpoints.PollingEndpoint#stop()
     */
    @Override
    public synchronized void stop() throws Exception {
        // stop listening only if the endpoint was initialized
        if (isEnabled()) {
            // stop listening to the transport
            if (this.transport.isListening()) {
                this.transport.close();
            }
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
        
        // check listen address not null
        if (this.listenAddress == null) {
            throw new DeploymentException("The listen address attribute has to be specified!");
        }
        
        // check if address is valid
        try {
            if (GenericAddress.parse(this.listenAddress) == null) {
                throw new DeploymentException("The specified address " + listenAddress + " is not valid!");
            }
        } catch (IllegalArgumentException ex) {
            throw new DeploymentException("The specified address " + listenAddress + " is not valid!");
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
        // only DONE and ERROR states will be received here and this
        // endpoint is not interested in such messages at all
    }

    /*
     * (non-Javadoc)
     * @see
     * org.snmp4j.CommandResponder#processPdu(org.snmp4j.CommandResponderEvent) 
     */
    public void processPdu(CommandResponderEvent event) {
        PDU pdu = event.getPDU();
        // check PDU not null
        if (pdu != null) {
            sendSnmpTrapMessage(pdu);
        } else {
            logger.debug("Received invalid trap PDU: " + pdu);
        }
    }

    /**
     * Sends the message to the bus 
     * @param pdu the trap received
     */
    private void sendSnmpTrapMessage(PDU pdu) {
        try {
            // create an inOnly exchange
            InOnly io = getExchangeFactory().createInOnlyExchange();
            
            // configure the exchange target
            configureExchangeTarget(io);
            
            // create the in message
            NormalizedMessage inMsg = io.createMessage();
            
            // now let the marshaler convert the snmp data into 
            // a normalized message to send to jbi bus
            this.marshaler.convertToJBI(io, inMsg, null, pdu);
            
            // put the in message into the inOnly exchange
            io.setInMessage(inMsg);
            
            // send the exchange
            getChannel().send(io);
        } catch (MessagingException ex) {
            logger.error("Error while trying to send the snmp trap PDU to the jbi bus", ex);
        }
    }

    public Snmp getSnmp() {
        return this.snmp;
    }

    public String getListenAddress() {
        return this.listenAddress;
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
     *      <td width="40%" align="left"><b><u>Name</u></b></td>
     *      <td width="60%" align="left"><b><u>Description</u></b></td>
     * </tr>
     * <tr>
     *      <td>protocol</td>
     *      <td>the protocol to use (udp or tcp)</td>
     * </tr>
     * <tr>
     *      <td>host</td>
     *      <td>the name or ip address of the snmp capable device</td>
     * </tr>
     * <tr>
     *      <td>port</td>
     *      <td>the port number to use</td>
     * </tr>
     * </table>
     * <br/>
     * <b><u>Example:</u></b><br />
     * &nbsp;&nbsp;&nbsp;<i>udp:192.168.2.122/162</i></p>
     * <i>&nbsp;&nbsp;&nbsp;The default value is <b>null</b></i><br/><br/>
     * 
     * @param listenAddress 
     *              a <code>String</code> value containing the connection details
     */
    public void setListenAddress(String listenAddress) {
        this.listenAddress = listenAddress;
    }

    public SnmpMarshalerSupport getMarshaler() {
        return this.marshaler;
    }

    /**
     * <p>Specifies a marshaler class which provides the logic for converting 
     * a snmp trap into a normalized message. This class has to implement 
     * the <code>SnmpMarshalerSupport</code> interface. If you don't specify a 
     * marshaler, the <code>DefaultSnmpMarshaler</code> will be used.</p>
     * 
     * @param marshaler 
     *              a class which implements <code>SnmpMarshalerSupport</code>
     */
    public void setMarshaler(SnmpMarshalerSupport marshaler) {
        this.marshaler = marshaler;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /**
     * <p>Specifies wether the endpoint is enabled or not.
     * If its value is set to true, the connection data will be
     * setted and trap PDUs will be processed. Otherwise, 
     * the endpoint won't do anything.</p> 
     * @param enabled the enabled to set
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
