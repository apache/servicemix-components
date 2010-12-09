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

import java.io.IOException;

import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.snmp4j.CommunityTarget;
import org.snmp4j.PDU;
import org.snmp4j.PDUv1;
import org.snmp4j.Snmp;
import org.snmp4j.TransportMapping;
import org.snmp4j.smi.Address;
import org.snmp4j.smi.GenericAddress;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.context.support.AbstractXmlApplicationContext;

/**
 * @author gperdiguero
 */
public class SnmpTrapConsumerEndpointTest extends SpringTestSupport {
    
    private static final long TIMEOUT = 15000;
    private static final String VAR_BIND_UDP_LOCAL = "1.3.6.1.2.1.7.5.1";
    private static final String VAR_BIND_UDP_LOCAL_ADDRESS = ".1";
    private static final String VAR_BIND_UDP_LOCAL_PORT = ".2";
    private static final String address = "udp:127.0.0.1/1662";
    
    /**
     * Sets up a {@link SnmpTrapConsumerEndpoint}, sends a v1 trap
     * and waits until the receiver gets the pdu.
     * @throws Exception
     */
    public void testSendAndReceiveTrap() throws Exception {
        
        // send a v1 trap and an inform trap
        sendV1Trap();
        long waitTime = System.currentTimeMillis();
        
        Receiver receiver = (Receiver) getBean("receiver");
       
        while (!receiver.getMessageList().hasReceivedMessage()) {
            try {
                // wait for traps
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                // ignore
            }
            
            if (System.currentTimeMillis() - waitTime > TIMEOUT) {
                // don't block the build too long
                break;
            }
        }
        
        if (receiver.getMessageList().getMessageCount() > 0) {
            for (int i = 0 ; i < receiver.getMessageList().getMessageCount() ; i++) {
                NormalizedMessage msg = (NormalizedMessage) receiver.getMessageList().getMessages().get(i);
                // check if valid
                SourceTransformer st = new SourceTransformer();
                try {
                    st.toDOMDocument(msg);
                } catch (Exception e) {
                    fail("Unable to parse the snmp trap result.\n" + st.contentToString(msg));
                }
            }
        } else {
            fail("There wasn't a PDU for " + TIMEOUT + " millis...");
        }
    }
    
    /**
     * Sends a v1Trap PDU to the address specified in 
     * the {@link SnmpTrapConsumerEndpoint}.
     */
    private void sendV1Trap() {
        // Setting up snmp and listening address
        Snmp snmp = null;
        Address listenGenericAddress = GenericAddress.parse(address);
        try {
            TransportMapping transport = new DefaultUdpTransportMapping();
            snmp = new Snmp(transport);
            transport.listen();
        } catch (IOException ioe) {
            // ignore
        }
        
        // setting up target
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString("private"));
        target.setAddress(listenGenericAddress);
        target.setRetries(2);
        target.setTimeout(1500);
        target.setVersion(0);
        
        // creating PDU. Since we are sending a V1TRAP, we have to use 
        // a PDUv1 protocol data unit. Otherwise the trap won't be sent.
        PDUv1 pdu = new PDUv1();
        pdu.setType(PDU.V1TRAP);
        pdu.setRequestID(new Integer32());
        pdu.setTimestamp(0);
        
        // add a few variable bindings
        String var_binding_name = VAR_BIND_UDP_LOCAL + VAR_BIND_UDP_LOCAL_ADDRESS + ".0.0.0.0.67";
        pdu.add(new VariableBinding(new OID(var_binding_name), new OctetString("0.0.0.0")));
        var_binding_name = VAR_BIND_UDP_LOCAL + VAR_BIND_UDP_LOCAL_PORT + ".0.0.0.0.67";
        pdu.add(new VariableBinding(new OID(var_binding_name), new OctetString("67")));
        
        try {
            snmp.send(pdu, target);
        } catch (IOException ioe) {
            // ignore
        }
    }

    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "spring-trap-consumer.xml" }, false);
        context.setValidating(false);
        context.refresh();
        return context;
    }

}
