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
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.apache.servicemix.eip.patterns.ContentEnricher;
import org.apache.servicemix.jbi.util.DOMUtil;
import org.apache.servicemix.tck.ReceiverComponent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ContentEnricherTest extends AbstractEIPTest {

    protected ContentEnricher enricher;

    protected void setUp() throws Exception {
        super.setUp();

        enricher = new ContentEnricher();
        enricher.setEnricherTarget(createServiceExchangeTarget(new QName(
                "enricherTarget")));
        enricher.setTarget(createServiceExchangeTarget(new QName("target")));

        configurePattern(enricher);
        activateComponent(enricher, "enricher");
    }

    public void testInOnly() throws Exception {

        activateComponent(new ReturnMockComponent("<halloMock/>"),
                "enricherTarget");

        ReceiverComponent rec = activateReceiver("target");

        InOnly me = client.createInOnlyExchange();

        me.setService(new QName("enricher"));
        me.getInMessage().setContent(createSource("<hello/>"));
        client.sendSync(me);

        assertEquals(ExchangeStatus.DONE, me.getStatus());

        assertEquals(1, rec.getMessageList().getMessageCount());

        NormalizedMessage object = (NormalizedMessage) rec.getMessageList()
                .getMessages().get(0);

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
}
