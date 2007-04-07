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
package org.apache.servicemix.eip;

import javax.jbi.messaging.InOnly;
import javax.xml.namespace.QName;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class SpringConfigurationTest extends SpringTestSupport {

    public void testConfig() throws Exception {
        ActivationSpec as = new ActivationSpec();
        as.setComponentName("client");
        ServiceMixClient client = new DefaultServiceMixClient(jbi, as);
        
        int nbMsgs = 10;
        for (int i = 0; i < nbMsgs; i++) {
            InOnly me = client.createInOnlyExchange();
            me.setService(new QName("http://test", "entryPoint"));
            me.getInMessage().setContent(new StringSource(
                    "<test xmlns=\"http://test\"><echo/><world/><earth/></test>"));
            client.sendSync(me);
        }        
        ((Receiver) getBean("trace1")).getMessageList().assertMessagesReceived(1 * nbMsgs);
        ((Receiver) getBean("trace2")).getMessageList().assertMessagesReceived(1 * nbMsgs);
        ((Receiver) getBean("trace3")).getMessageList().assertMessagesReceived(1 * nbMsgs);
        ((Receiver) getBean("trace4")).getMessageList().assertMessagesReceived(2 * nbMsgs);
        ((Receiver) getBean("trace5")).getMessageList().assertMessagesReceived(1 * nbMsgs);
        
        // Wait for all messages to be processed
        Thread.sleep(50);
    }

    public void testConfigAsync() throws Exception {
        ActivationSpec as = new ActivationSpec();
        as.setComponentName("client");
        ServiceMixClient client = new DefaultServiceMixClient(jbi, as);
        
        int nbMsgs = 100;
        for (int i = 0; i < nbMsgs; i++) {
            InOnly me = client.createInOnlyExchange();
            me.setService(new QName("http://test", "entryPoint"));
            me.getInMessage().setContent(new StringSource(
                    "<test xmlns=\"http://test\"><echo/><world/><earth/></test>"));
            client.send(me);
        }
        for (int i = 0; i < nbMsgs; i++) {
            client.receive();
        }
        ((Receiver) getBean("trace1")).getMessageList().assertMessagesReceived(1 * nbMsgs);
        ((Receiver) getBean("trace2")).getMessageList().assertMessagesReceived(1 * nbMsgs);
        ((Receiver) getBean("trace3")).getMessageList().assertMessagesReceived(1 * nbMsgs);
        ((Receiver) getBean("trace4")).getMessageList().assertMessagesReceived(2 * nbMsgs);
        ((Receiver) getBean("trace5")).getMessageList().assertMessagesReceived(1 * nbMsgs);
        
        // Wait for all messages to be processed
        Thread.sleep(50);
    }

    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("org/apache/servicemix/eip/spring.xml");
    }

}
