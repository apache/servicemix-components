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
package org.apache.servicemix.wsn.spring;

import java.io.StringWriter;

import javax.jbi.JBIException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.transform.Source;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.MessageExchangeListener;
import org.apache.servicemix.components.util.ComponentSupport;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.wsn.client.AbstractWSAClient;
import org.apache.servicemix.wsn.client.NotificationBroker;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.b_2.SubscribeResponse;
import org.oasis_open.docs.wsn.b_2.Unsubscribe;
import org.oasis_open.docs.wsn.b_2.UnsubscribeResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * 
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="publisher"
 */
public class PublisherComponent extends ComponentSupport implements MessageExchangeListener {

    private static final Log log = LogFactory.getLog(PublisherComponent.class);
    
    private NotificationBroker wsnBroker;
    private String topic;
    private boolean demand;
    private String subscriptionEndpoint = "subscription";
    private Subscribe subscription;
    
    /**
     * @return Returns the demand.
     */
    public boolean getDemand() {
        return demand;
    }

    /**
     * @param demand The demand to set.
     */
    public void setDemand(boolean demand) {
        this.demand = demand;
    }

    /**
     * @return Returns the topic.
     */
    public String getTopic() {
        return topic;
    }

    /**
     * @param topic The topic to set.
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * @return Returns the subscription.
     */
    public Subscribe getSubscription() {
        return subscription;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.jbi.management.BaseLifeCycle#init()
     */
    public void init() throws JBIException {
        super.init();
        getContext().activateEndpoint(getService(), subscriptionEndpoint);
    }
    
    /* (non-Javadoc)
     * @see javax.jbi.management.LifeCycleMBean#start()
     */
    public void start() throws JBIException {
        new Thread() {
            public void run() {
                try {
                    wsnBroker = new NotificationBroker(getContext());
                    String wsaAddress = getService().getNamespaceURI() + "/" + getService().getLocalPart() + "/" + subscriptionEndpoint;
                    wsnBroker.registerPublisher(AbstractWSAClient.createWSA(wsaAddress),
                                                topic,
                                                demand);
                } catch (Exception e) {
                    log.error("Could not create wsn client", e);
                }
            }
        }.start();
    }
    
    /* (non-Javadoc)
     * @see javax.jbi.management.LifeCycleMBean#shutDown()
     */
    public void shutDown() throws JBIException {
        super.shutDown();
    }
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.MessageExchangeListener#onMessageExchange(javax.jbi.messaging.MessageExchange)
     */
    public void onMessageExchange(MessageExchange exchange) throws MessagingException {
        if (exchange.getStatus() != ExchangeStatus.ACTIVE) {
            return;
        }
        // This is a notification from the WSN broker
        if (exchange.getEndpoint().getEndpointName().equals(subscriptionEndpoint)) {
            try {
                JAXBContext jaxbContext = JAXBContext.newInstance(Subscribe.class);
                Source src = exchange.getMessage("in").getContent();
                Object input = jaxbContext.createUnmarshaller().unmarshal(src);
                if (input instanceof Subscribe) {
                    subscription = (Subscribe) input;
                    SubscribeResponse response = new SubscribeResponse();
                    String wsaAddress = getService().getNamespaceURI() + "/" + getService().getLocalPart() + "/" + subscriptionEndpoint;
                    response.setSubscriptionReference(AbstractWSAClient.createWSA(wsaAddress));
                    StringWriter writer = new StringWriter();
                    jaxbContext.createMarshaller().marshal(response, writer);
                    NormalizedMessage out = exchange.createMessage();
                    out.setContent(new StringSource(writer.toString()));
                    exchange.setMessage(out, "out");
                    send(exchange);
                } else if (input instanceof Unsubscribe) {
                    subscription = null;
                    UnsubscribeResponse response = new UnsubscribeResponse();
                    StringWriter writer = new StringWriter();
                    jaxbContext.createMarshaller().marshal(response, writer);
                    NormalizedMessage out = exchange.createMessage();
                    out.setContent(new StringSource(writer.toString()));
                    exchange.setMessage(out, "out");
                    send(exchange);
                } else {
                    throw new Exception("Unkown request");
                }
            } catch (Exception e) {
                fail(exchange, e);
            }
        // This is a notification to publish
        } else {
            try {
                if (!demand || subscription != null) {
                    Node n = new SourceTransformer().toDOMNode(exchange.getMessage("in"));
                    if (n instanceof Document) {
                        n = ((Document) n).getDocumentElement();
                    }
                    wsnBroker.notify(topic, n);
                    done(exchange);
                } else {
                    log.info("Ingore notification as the publisher is no subscribers");
                }
            } catch (Exception e) {
                fail(exchange, e);
            }
        }
    }

}
