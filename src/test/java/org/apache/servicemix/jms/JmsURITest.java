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
package org.apache.servicemix.jms;

import java.util.List;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.messaging.MessageExchangeSupport;
import org.apache.servicemix.jbi.resolver.URIResolver;
import org.apache.servicemix.tck.ReceiverComponent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

public class JmsURITest extends AbstractJmsTestCase {

    private static Log logger =  LogFactory.getLog(JmsURITest.class);

    public void testResolveEndpoint() throws Exception {
        JmsComponent jms = new JmsComponent();
        jms.getConfiguration().setConnectionFactory(connectionFactory);
        JmsEndpoint ep = new JmsEndpoint();
        ep.setRole(MessageExchange.Role.CONSUMER);
        ep.setService(ReceiverComponent.SERVICE);
        ep.setEndpoint(ReceiverComponent.ENDPOINT);
        ep.setDefaultMep(MessageExchangeSupport.IN_ONLY);
        ep.setJmsProviderDestinationName("foo.bar.myqueue");
        ep.setDestinationStyle(AbstractJmsProcessor.STYLE_QUEUE);
        jms.setEndpoints(new JmsEndpoint[] { ep });
        container.activateComponent(jms, "servicemix-jms");

        ReceiverComponent receiver = new ReceiverComponent();
        container.activateComponent(receiver, "receiver");

        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        DocumentFragment epr = URIResolver.createWSAEPR("jms://queue/foo.bar.myqueue?jms.soap=true");
        ServiceEndpoint se = client.getContext().resolveEndpointReference(epr);
        assertNotNull(se);

        InOnly inonly = client.createInOnlyExchange();
        inonly.setEndpoint(se);
        inonly.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        client.sendSync(inonly);

        assertEquals(ExchangeStatus.DONE, inonly.getStatus());
        receiver.getMessageList().assertMessagesReceived(1);
        List msgs = receiver.getMessageList().flushMessages();
        NormalizedMessage msg = (NormalizedMessage) msgs.get(0);
        Element elem = new SourceTransformer().toDOMElement(msg);
        assertEquals("http://www.w3.org/2003/05/soap-envelope", elem.getNamespaceURI());
        assertEquals("env:Envelope", elem.getNodeName());
        logger.info(new SourceTransformer().contentToString(msg));

        // Wait for DONE status
        Thread.sleep(50);
    }

}
