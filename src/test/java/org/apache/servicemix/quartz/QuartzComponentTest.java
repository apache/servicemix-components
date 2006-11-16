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

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.tck.ReceiverComponent;
import org.quartz.Trigger;
import org.springframework.scheduling.quartz.SimpleTriggerBean;

public class QuartzComponentTest extends TestCase {

    public void test() throws Exception {
        JBIContainer jbi = new JBIContainer();
        jbi.setEmbedded(true);
        jbi.init();
        
        QuartzComponent quartz = new QuartzComponent();
        QuartzEndpoint endpoint = new QuartzEndpoint();
        endpoint.setService(new QName("quartz"));
        endpoint.setEndpoint("endpoint");
        endpoint.setTargetService(new QName("receiver"));
        SimpleTriggerBean trigger = new SimpleTriggerBean();
        trigger.setRepeatInterval(100);
        trigger.setName("trigger");
        trigger.afterPropertiesSet();
        endpoint.setTrigger(trigger);
        quartz.setEndpoints(new QuartzEndpoint[] { endpoint });
        jbi.activateComponent(quartz, "servicemix-quartz");
        
        ReceiverComponent receiver = new ReceiverComponent(new QName("receiver"), "endpoint");
        jbi.activateComponent(receiver, "receiver");
        
        jbi.start();

        Thread.sleep(200);
        assertTrue(receiver.getMessageList().flushMessages().size() > 0);
        
        quartz.stop();
        receiver.getMessageList().flushMessages();
        Thread.sleep(200);
        assertEquals(0, receiver.getMessageList().flushMessages().size());
        
        quartz.start();
        Thread.sleep(200);
        assertTrue(receiver.getMessageList().flushMessages().size() > 0);

        jbi.shutDown();
    }
    
}
