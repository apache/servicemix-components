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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import org.apache.activemq.util.IdGenerator;
import org.apache.servicemix.eip.patterns.MessageFilter;
import org.apache.servicemix.eip.support.XPathPredicate;
import org.apache.servicemix.store.memory.MemoryStore;
import org.apache.servicemix.tck.ReceiverComponent;

public class MessageFilterTest extends AbstractEIPTest {

    protected MessageFilter messageFilter;
    
    protected void setUp() throws Exception {
        super.setUp();

        messageFilter = new MessageFilter();
        messageFilter.setFilter(new XPathPredicate("/hello/@id = '1'"));
        messageFilter.setTarget(createServiceExchangeTarget(new QName("target")));
        configureMessageFilter();
        activateComponent(messageFilter, "messageFilter");
    }
    
    protected void configureMessageFilter() throws Exception {
        messageFilter.setStore(new MemoryStore(new IdGenerator()) {
            public void store(String id, Object exchange) throws IOException {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                new ObjectOutputStream(baos).writeObject(exchange);
                super.store(id, exchange);
            }
        });
    }

    public void testInOnly() throws Exception {
        ReceiverComponent rec = activateReceiver("target");
        
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("messageFilter"));
        me.getInMessage().setContent(createSource("<hello><one/><two/><three/></hello>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        rec.getMessageList().assertMessagesReceived(0); 

        me = client.createInOnlyExchange();
        me.setService(new QName("messageFilter"));
        me.getInMessage().setContent(createSource("<hello id='1'><one/><two/><three/></hello>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        rec.getMessageList().assertMessagesReceived(1); 
    }

    public void testInOut() throws Exception {
        InOut me = client.createInOutExchange();
        me.setService(new QName("messageFilter"));
        me.getInMessage().setContent(createSource("<hello><one/><two/><three/></hello>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
    }

}
