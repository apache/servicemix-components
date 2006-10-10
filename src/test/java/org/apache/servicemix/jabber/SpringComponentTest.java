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
package org.apache.servicemix.jabber;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import java.util.Map;
import java.util.Set;
import java.net.InetAddress;

public class SpringComponentTest extends SpringTestSupport {

    public static void main(String[] args) {
        Set<Map.Entry<Object,Object>> entries = System.getProperties().entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }

        try {
            SpringComponentTest test = new SpringComponentTest();
            test.setUp();
            test.testSendingToStaticEndpoint();

            System.out.println();
            System.out.println("Waiting for incoming messages");
            System.out.println("Press any key to terminate");
            System.out.println();

            System.in.read();

            test.tearDown();
        }
        catch (Exception e) {
            System.err.println("Caught: " + e);
            e.printStackTrace();
        }

    }
    public void testSendingToStaticEndpoint() throws Exception {
        ServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("urn:test", "service"));
        NormalizedMessage message = me.getInMessage();

        message.setProperty("name", "cheese");
        message.setContent(new StringSource("<hello>world</hello>"));

        client.sendSync(me);
        assertExchangeWorked(me);
    }


    protected void setUp() throws Exception {
        String host = InetAddress.getLocalHost().getHostName();
        System.setProperty("host", host);
        super.setUp();
    }

    protected void assertExchangeWorked(MessageExchange me) throws Exception {
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getError() != null) {
                throw me.getError();
            }
            else {
                fail("Received ERROR status");
            }
        }
        else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        }
    }

    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("spring.xml");
    }

}
