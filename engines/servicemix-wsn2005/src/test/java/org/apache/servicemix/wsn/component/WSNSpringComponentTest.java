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
package org.apache.servicemix.wsn.component;

import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class WSNSpringComponentTest extends SpringTestSupport {

    protected AbstractXmlApplicationContext createBeanFactory() {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "org/apache/servicemix/wsn/spring.xml" }, false);
        context.setValidating(false);
        context.refresh();
        return context;
    }

    public void tearDown() throws Exception {
        Thread.sleep(5000);
        super.tearDown();
    }
    
    public void test() throws Exception {
        // Wait for the publisher to be registered
        Thread.sleep(1000);

        Receiver receiver = (Receiver) jbi.getBean("receiver");
        receiver.getMessageList().assertMessagesReceived(1);
    }
    
}
