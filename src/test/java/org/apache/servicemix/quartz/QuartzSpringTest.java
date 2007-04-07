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
package org.apache.servicemix.quartz;

import javax.jbi.messaging.NormalizedMessage;

import org.w3c.dom.Element;

import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class QuartzSpringTest extends SpringTestSupport {

    public void test() throws Exception {
        Receiver r1 = (Receiver) getBean("receiver1");
        Receiver r2 = (Receiver) getBean("receiver2");
        Receiver r3 = (Receiver) getBean("receiver3");
        r1.getMessageList().assertMessagesReceived(1);
        r2.getMessageList().assertMessagesReceived(1);
        r3.getMessageList().assertMessagesReceived(1);
        NormalizedMessage nm = (NormalizedMessage) r3.getMessageList().flushMessages().get(0);
        Element e = new SourceTransformer().toDOMElement(nm);
        System.err.println(new SourceTransformer().contentToString(nm));
        assertEquals("hello", e.getNodeName());
    }
    
    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("org/apache/servicemix/quartz/spring.xml");
    }

}
