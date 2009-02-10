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

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.servicemix.components.util.TransformComponentSupport;
import org.apache.servicemix.eip.patterns.ContentEnricher;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.util.DOMUtil;
import org.apache.servicemix.tck.ReceiverComponent;

public class ContentEnricherTest extends AbstractEIPTest {

    private static final Logger ROOT_LOGGER = Logger.getRootLogger();
    protected ContentEnricher enricher;

    protected void setUp() throws Exception {
        super.setUp();

        enricher = new ContentEnricher();
        enricher.setEnricherTarget(createServiceExchangeTarget(new QName("enricherTarget")));
        enricher.setTarget(createServiceExchangeTarget(new QName("target")));

        configurePattern(enricher);
        activateComponent(enricher, "enricher");
    }

    public void testInOnly() throws Exception {
        activateComponent(new ReturnMockComponent("<halloMock/>"), "enricherTarget");
        sendAndAssertInOnly();
    }
    
    public void testInOnlyEnricherTargetConsumerStreamSource() throws Exception {
        //disable debug level to avoid StreamSource conversion
        Level original = ROOT_LOGGER.getLevel();
        ROOT_LOGGER.setLevel(Level.INFO);
        try {
            activateComponent(new TransformComponentSupport() {
                
                private final SourceTransformer transformer = new SourceTransformer();
                
                public boolean transform(MessageExchange exchange, NormalizedMessage in, NormalizedMessage out) throws Exception {
                    //let's consume the in message's content stream and set the out message's content to another stream
                    transformer.toString(in.getContent());
                    out.setContent(createSource("<halloMock/>"));
                    return true;
                }
                
            }, "enricherTarget");
            sendAndAssertInOnly();
        } finally {
            //restore the original log level
            ROOT_LOGGER.setLevel(original);
        }
    }

    private void sendAndAssertInOnly() throws Exception {
        ReceiverComponent rec = activateReceiver("target");

        InOnly me = client.createInOnlyExchange();

        me.setService(new QName("enricher"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);

        assertEquals(ExchangeStatus.DONE, me.getStatus());

        assertEquals(1, rec.getMessageList().getMessageCount());

        NormalizedMessage object = (NormalizedMessage) rec.getMessageList().getMessages().get(0);

        DOMSource domSource = (DOMSource) object.getContent();
        Document doc = (Document) domSource.getNode();
        
        Element e = doc.getDocumentElement();
        assertEquals("enricher", e.getNodeName());
        Element r = DOMUtil.getFirstChildElement(e);
        assertEquals("request", r.getNodeName());
        assertEquals("hello", DOMUtil.getFirstChildElement(r).getNodeName());
        r = DOMUtil.getNextSiblingElement(r);
        assertEquals("result", r.getNodeName());
        assertEquals("halloMock", DOMUtil.getFirstChildElement(r).getNodeName());
    }

    public void testInOut() throws Exception {

        activateComponent(new ReturnMockComponent("<halloMock/>"),
                "enricherTarget");

        InOut me = client.createInOutExchange();

        me.setService(new QName("enricher"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);

        assertEquals(ExchangeStatus.ERROR, me.getStatus());

    }

    public void testMsgProperties() throws Exception {

        String propName1 = "propName1";
        String propName2 = "propName2";
        String propVal1 = "propVal1";
        String propVal2 = "propVal2";

        enricher.setCopyProperties(true);
        
        activateComponent(new ReturnMockComponent("<helloMock/>"), "enricherTarget");

        ReceiverComponent rec = activateReceiver("target");

        InOnly me = client.createInOnlyExchange();

        me.setService(new QName("enricher"));
        me.getInMessage().setContent(createSource("<hello/>"));
        me.getInMessage().setProperty(propName1, propVal1);
        me.getInMessage().setProperty(propName2, propVal2);
        client.sendSync(me);
                
        NormalizedMessage msg = (NormalizedMessage) rec.getMessageList()
            .getMessages().get(0);

        //assertions
        assertEquals(ExchangeStatus.DONE, me.getStatus());
        assertEquals(1, rec.getMessageList().getMessageCount());
        assertEquals(propVal1, msg.getProperty(propName1));
        assertEquals(propVal2, msg.getProperty(propName2));
        assertEquals(ReturnMockComponent.PROPERTY_VALUE, msg.getProperty(ReturnMockComponent.PROPERTY_NAME));
    }
}
