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

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import org.apache.servicemix.eip.patterns.ContentBasedRouter;
import org.apache.servicemix.eip.support.RoutingRule;
import org.apache.servicemix.eip.support.XPathPredicate;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.tck.ReceiverComponent;
import org.w3c.dom.Node;

public class ContentBasedRouterTxTest extends AbstractEIPTransactionalTest {

    private ContentBasedRouter router;

    protected void setUp() throws Exception {
        super.setUp();

        router = new ContentBasedRouter();
        router.setRules(new RoutingRule[] {
                new RoutingRule(
                        new XPathPredicate("/hello/@id = '1'"),
                        createServiceExchangeTarget(new QName("target1"))),
                new RoutingRule(
                        new XPathPredicate("/hello/@id = '2'"),
                        createServiceExchangeTarget(new QName("target2"))),
                new RoutingRule(
                        null,
                        createServiceExchangeTarget(new QName("target3")))
        });
        configurePattern(router);
        activateComponent(router, "router");
    }
    
    public void testInOnlySync() throws Exception {
        ReceiverComponent rec1 = activateReceiver("target1");
        ReceiverComponent rec2 = activateReceiver("target2");
        ReceiverComponent rec3 = activateReceiver("target3");
        
        tm.begin();
        
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("router"));
        me.getInMessage().setContent(createSource("<hello id='1' />"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        rec1.getMessageList().assertMessagesReceived(1); 
        rec2.getMessageList().assertMessagesReceived(0);
        rec3.getMessageList().assertMessagesReceived(0);

        rec1.getMessageList().flushMessages();
        rec2.getMessageList().flushMessages();
        rec3.getMessageList().flushMessages();
        
        me = client.createInOnlyExchange();
        me.setService(new QName("router"));
        me.getInMessage().setContent(createSource("<hello id='2' />"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        rec1.getMessageList().assertMessagesReceived(0);
        rec2.getMessageList().assertMessagesReceived(1);
        rec3.getMessageList().assertMessagesReceived(0);

        rec1.getMessageList().flushMessages();
        rec2.getMessageList().flushMessages();
        rec3.getMessageList().flushMessages();

        me = client.createInOnlyExchange();
        me.setService(new QName("router"));
        me.getInMessage().setContent(createSource("<hello id='3' />"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        rec1.getMessageList().assertMessagesReceived(0);
        rec2.getMessageList().assertMessagesReceived(0);
        rec3.getMessageList().assertMessagesReceived(1);

        tm.commit();
    }

    public void testInOnlyAsync() throws Exception {
        ReceiverComponent rec1 = activateReceiver("target1");
        ReceiverComponent rec2 = activateReceiver("target2");
        ReceiverComponent rec3 = activateReceiver("target3");
        
        tm.begin();
        
        InOnly me = client.createInOnlyExchange();
        me.setService(new QName("router"));
        me.getInMessage().setContent(createSource("<hello id='1' />"));
        client.send(me);
        
        me = client.createInOnlyExchange();
        me.setService(new QName("router"));
        me.getInMessage().setContent(createSource("<hello id='2' />"));
        client.send(me);
        
        me = client.createInOnlyExchange();
        me.setService(new QName("router"));
        me.getInMessage().setContent(createSource("<hello id='3' />"));
        client.send(me);
        
        tm.commit();
        
        me = (InOnly) client.receive();
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        me = (InOnly) client.receive();
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        me = (InOnly) client.receive();
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        
        rec1.getMessageList().assertMessagesReceived(1); 
        rec2.getMessageList().assertMessagesReceived(1);
        rec3.getMessageList().assertMessagesReceived(1);
    }

    public void testInOutSync() throws Exception {
        tm.begin();
        
        activateComponent(new ReturnMockComponent("<from1/>"), "target1");
        activateComponent(new ReturnMockComponent("<from2/>"), "target2");
        activateComponent(new ReturnMockComponent("<from3/>"), "target3");
        
        InOut me = client.createInOutExchange();
        me.setService(new QName("router"));
        me.getInMessage().setContent(createSource("<hello id='1' />"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        Node node = new SourceTransformer().toDOMNode(me.getOutMessage());
        assertEquals("from1", node.getFirstChild().getNodeName());
        client.done(me);
        
        me = client.createInOutExchange();
        me.setService(new QName("router"));
        me.getInMessage().setContent(createSource("<hello id='2' />"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        node = new SourceTransformer().toDOMNode(me.getOutMessage());
        assertEquals("from2", node.getFirstChild().getNodeName());
        client.done(me);
        
        me = client.createInOutExchange();
        me.setService(new QName("router"));
        me.getInMessage().setContent(createSource("<hello id='3' />"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        node = new SourceTransformer().toDOMNode(me.getOutMessage());
        assertEquals("from3", node.getFirstChild().getNodeName());
        client.done(me);
        
        tm.commit();
    }

    public void testInOutAsync() throws Exception {
        activateComponent(new ReturnMockComponent("<from1/>"), "target1");
        activateComponent(new ReturnMockComponent("<from2/>"), "target2");
        activateComponent(new ReturnMockComponent("<from3/>"), "target3");
        
        tm.begin();
        
        InOut me = client.createInOutExchange();
        me.setService(new QName("router"));
        me.getInMessage().setContent(createSource("<hello id='1' />"));
        client.send(me);
        
        tm.commit();
        
        me = (InOut) client.receive();
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        Node node = new SourceTransformer().toDOMNode(me.getOutMessage());
        assertEquals("from1", node.getFirstChild().getNodeName());
        client.done(me);
        
        tm.begin();
        
        me = client.createInOutExchange();
        me.setService(new QName("router"));
        me.getInMessage().setContent(createSource("<hello id='2' />"));
        client.send(me);
        
        tm.commit();
        
        me = (InOut) client.receive();
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        node = new SourceTransformer().toDOMNode(me.getOutMessage());
        assertEquals("from2", node.getFirstChild().getNodeName());
        client.done(me);
        
        tm.begin();
        
        me = client.createInOutExchange();
        me.setService(new QName("router"));
        me.getInMessage().setContent(createSource("<hello id='3' />"));
        client.send(me);
        
        tm.commit();
        
        me = (InOut) client.receive();
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        node = new SourceTransformer().toDOMNode(me.getOutMessage());
        assertEquals("from3", node.getFirstChild().getNodeName());
        client.done(me);
    }

}
