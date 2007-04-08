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
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
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
import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.wsn.component.WSNLifeCycle;
import org.apache.servicemix.wsn.jaxws.InvalidFilterFault;
import org.apache.servicemix.wsn.jaxws.InvalidMessageContentExpressionFault;
import org.apache.servicemix.wsn.jaxws.InvalidProducerPropertiesExpressionFault;
import org.apache.servicemix.wsn.jaxws.InvalidTopicExpressionFault;
import org.apache.servicemix.wsn.jaxws.SubscribeCreationFailedFault;
import org.apache.servicemix.wsn.jaxws.TopicExpressionDialectUnknownFault;
import org.apache.servicemix.wsn.jaxws.TopicNotSupportedFault;
import org.apache.servicemix.wsn.jaxws.UnacceptableInitialTerminationTimeFault;
import org.apache.servicemix.wsn.jms.JmsSubscription;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.b_2.SubscribeCreationFailedFaultType;

public class JbiSubscription extends JmsSubscription {

    private static Log log = LogFactory.getLog(JbiSubscription.class);

    private WSNLifeCycle lifeCycle;

    private ServiceEndpoint endpoint;

    private ExchangeProcessor processor;

    public JbiSubscription(String name) {
        super(name);
        processor = new NoOpProcessor();
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
            String[] parts = split(subscribeRequest.getConsumerReference().getAddress().getValue().trim());
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
            DeliveryChannel channel = getContext().getDeliveryChannel();
            MessageExchangeFactory factory = channel.createExchangeFactory(endpoint);
            InOnly inonly = factory.createInOnlyExchange();
            NormalizedMessage msg = inonly.createMessage();
            inonly.setInMessage(msg);
            msg.setContent(new DOMSource(content));
            getLifeCycle().sendConsumerExchange(inonly, processor);
        } catch (JBIException e) {
            log.warn("Could not deliver notification", e);
        }
    }

    public ComponentContext getContext() {
        return lifeCycle.getContext();
    }

    public WSNLifeCycle getLifeCycle() {
        return lifeCycle;
    }

    public void setLifeCycle(WSNLifeCycle lifeCycle) {
        this.lifeCycle = lifeCycle;
    }

    protected class NoOpProcessor implements ExchangeProcessor {

        public void process(MessageExchange exchange) throws Exception {
        }

        public void start() throws Exception {
        }

        public void stop() throws Exception {
        }
    }

}
