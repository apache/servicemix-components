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
package org.apache.servicemix.wsn.spring;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.annotation.Resource;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.w3c.dom.Element;

import org.apache.servicemix.jbi.listener.MessageExchangeListener;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.wsn.client.NotificationBroker;

/**
 * A simple bean acting as a WS-Notification publisher.
 * All messages sent to it will be forwarded to the NotificationBroker as Notify requests.
 * This beans should be used and deployed onto servicemix-bean as a pojo for a bean endpoint. 
 *
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="publisher-proxy"
 */
public class PublisherProxyBean implements MessageExchangeListener {

    private NotificationBroker wsnBroker;

    private String topic;

    //private Publisher publisher;

    @Resource
    private ComponentContext context;

    private SourceTransformer sourceTransformer = new SourceTransformer();


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

    @PostConstruct
    public void start() throws JBIException {
        wsnBroker = new NotificationBroker(context);
        //publisher = wsnBroker.registerPublisher(null, topic, false);
    }

    @PreDestroy
    public void stop() throws JBIException {
        //publisher.destroy();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.MessageExchangeListener#onMessageExchange(javax.jbi.messaging.MessageExchange)
     */
    public void onMessageExchange(MessageExchange exchange) throws MessagingException {
        if (exchange.getStatus() != ExchangeStatus.ACTIVE) {
            return;
        }
        // This is a notification from the WSN broker
        try {
            Element elem = sourceTransformer.toDOMElement(exchange.getMessage("in"));
            wsnBroker.notify(topic, elem);
            exchange.setStatus(ExchangeStatus.DONE);
        } catch (Exception e) {
            exchange.setError(e);
        }
        context.getDeliveryChannel().send(exchange);
    }

}
