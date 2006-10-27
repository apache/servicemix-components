/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.file;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class PollDirectoryTest extends SpringTestSupport {
    protected String directoryName = "target/pollDirectory";
    protected String dynamicURI = "file:" + directoryName;


    public void testSendToWriterSoItCanBePolled() throws Exception {
        // now lets make a request on this endpoint
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);

        // lets send a request to be written to a file
        // which should then be polled
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("urn:test", "service"));
        NormalizedMessage message = me.getInMessage();

        message.setProperty("name", "cheese");
        message.setContent(new StringSource("<hello>world</hello>"));

        client.sendSync(me);


        Thread.sleep(5000);
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
        return new ClassPathXmlApplicationContext("spring-polling.xml");
    }

}
