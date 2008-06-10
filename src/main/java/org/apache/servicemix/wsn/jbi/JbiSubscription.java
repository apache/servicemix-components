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
package org.apache.servicemix.wsn.jbi;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.wsn.ComponentContextAware;
import org.apache.servicemix.wsn.client.AbstractWSAClient;
import org.apache.servicemix.wsn.jms.JmsSubscription;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.b_2.SubscribeCreationFailedFaultType;
import org.oasis_open.docs.wsn.bw_2.InvalidFilterFault;
import org.oasis_open.docs.wsn.bw_2.InvalidMessageContentExpressionFault;
import org.oasis_open.docs.wsn.bw_2.InvalidProducerPropertiesExpressionFault;
import org.oasis_open.docs.wsn.bw_2.InvalidTopicExpressionFault;
import org.oasis_open.docs.wsn.bw_2.SubscribeCreationFailedFault;
import org.oasis_open.docs.wsn.bw_2.TopicExpressionDialectUnknownFault;
import org.oasis_open.docs.wsn.bw_2.TopicNotSupportedFault;
import org.oasis_open.docs.wsn.bw_2.UnacceptableInitialTerminationTimeFault;

public class JbiSubscription extends JmsSubscription implements ComponentContextAware {

    private static Log log = LogFactory.getLog(JbiSubscription.class);

    private ComponentContext context;

    private ServiceEndpoint endpoint;

    public JbiSubscription(String name) {
        super(name);
    }

    @Override
    protected void start() throws SubscribeCreationFailedFault {
        super.start();
    }

    @Override
    protected void validateSubscription(Subscribe subscribeRequest) throws InvalidFilterFault,
            InvalidMessageContentExpressionFault, InvalidProducerPropertiesExpressionFault,
            InvalidTopicExpressionFault, SubscribeCreationFailedFault, TopicExpressionDialectUnknownFault,
            TopicNotSupportedFault, UnacceptableInitialTerminationTimeFault {
        super.validateSubscription(subscribeRequest);
        try {
            endpoint = resolveConsumer(subscribeRequest);
        } catch (Exception e) {
            SubscribeCreationFailedFaultType fault = new SubscribeCreationFailedFaultType();
            throw new SubscribeCreationFailedFault("Unable to resolve consumer reference endpoint", fault, e);
        }
        if (endpoint == null) {
            SubscribeCreationFailedFaultType fault = new SubscribeCreationFailedFaultType();
            throw new SubscribeCreationFailedFault("Unable to resolve consumer reference endpoint", fault);
        }
    }

    protected ServiceEndpoint resolveConsumer(Subscribe subscribeRequest) throws Exception {
        // Try to resolve the WSA endpoint
        JAXBContext ctx = JAXBContext.newInstance(Subscribe.class);
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();
        ctx.createMarshaller().marshal(subscribeRequest, doc);
        NodeList nl = doc.getDocumentElement().getElementsByTagNameNS("http://docs.oasis-open.org/wsn/b-2",
                "ConsumerReference");
        if (nl.getLength() != 1) {
            throw new Exception("Subscribe request must have exactly one ConsumerReference node");
        }
        Element el = (Element) nl.item(0);
        DocumentFragment epr = doc.createDocumentFragment();
        epr.appendChild(el);
        ServiceEndpoint ep = getContext().resolveEndpointReference(epr);
        if (ep == null) {
            String[] parts = split(AbstractWSAClient.getWSAAddress(subscribeRequest.getConsumerReference()));
            ep = getContext().getEndpoint(new QName(parts[0], parts[1]), parts[2]);
        }
        return ep;
    }

    protected String[] split(String uri) {
        char sep;
        if (uri.indexOf('/') > 0) {
            sep = '/';
        } else {
            sep = ':';
        }
        int idx1 = uri.lastIndexOf(sep);
        int idx2 = uri.lastIndexOf(sep, idx1 - 1);
        String epName = uri.substring(idx1 + 1);
        String svcName = uri.substring(idx2 + 1, idx1);
        String nsUri = uri.substring(0, idx2);
        return new String[] {nsUri, svcName, epName };
    }

    @Override
    protected void doNotify(final Element content) {
        try {
            MessageExchangeFactory factory = context.getDeliveryChannel().createExchangeFactory(endpoint);
            InOnly inonly = factory.createInOnlyExchange();
            NormalizedMessage msg = inonly.createMessage();
            inonly.setInMessage(msg);
            msg.setContent(new DOMSource(content));
            context.getDeliveryChannel().send(inonly);
        } catch (JBIException e) {
            log.warn("Could not deliver notification", e);
        }
    }

    public ComponentContext getContext() {
        return context;
    }

    public void setContext(ComponentContext context) {
        this.context = context;
    }
}
