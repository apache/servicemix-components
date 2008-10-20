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

import javax.jbi.messaging.NormalizedMessage;

import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

/**
 * @author lhein
 */
public class SnmpPollingEndpointTest extends SpringTestSupport {

    private static final long TIMEOUT = 30000;
    
    public void testDummy() {
        // just that maven doesn't complain about non-existing tests
    }
    
    /**
     * sets up an endpoint and waits 30 seconds for a incoming snmp message
     * @throws Exception
     */
    public void xtestPolling() throws Exception {
        long waitTime = System.currentTimeMillis();
        
        Receiver receiver = (Receiver) getBean("receiver");
        
        while (!receiver.getMessageList().hasReceivedMessage()) {
            try {
                // wait for a polled state
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
            
            if (System.currentTimeMillis() - waitTime > TIMEOUT) {
                // don't block the build too long
                break;
            }
        }

        if (receiver.getMessageList().getMessageCount()>0) {
            NormalizedMessage msg = (NormalizedMessage)receiver.getMessageList().getMessages().get(0);
            // now check if valid
            SourceTransformer st = new SourceTransformer();
            try {
                st.toDOMDocument(msg);
            } catch (Exception e) {
                fail("Unable to parse the snmp poll result.\n" + st.contentToString(msg));
            }            
        } else {
            fail("There wasn'a a response for " + TIMEOUT + " millis...");
        }            
    }

    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("spring-polling.xml");
    }
}
