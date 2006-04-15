/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class SpringConfigurationTest extends SpringTestSupport {

    public void testConfig() throws Exception {
        ServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("http://test", "wireTap"));
        me.getInMessage().setContent(new StringSource("<test><echo/><world/></test>"));
        client.sendSync(me);
        
        ((Receiver) getBean("trace1")).getMessageList().assertMessagesReceived(1);
        ((Receiver) getBean("trace2")).getMessageList().assertMessagesReceived(1);
        ((Receiver) getBean("trace3")).getMessageList().assertMessagesReceived(1);
        ((Receiver) getBean("trace4")).getMessageList().assertMessagesReceived(1);
        
        // Wait for all messages to be processed
        Thread.sleep(50);
    }

    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("org/apache/servicemix/eip/spring.xml");
    }

}
