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
package org.apache.servicemix.wsn.client;

import java.util.List;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.client.ServiceMixClientFacade;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.resolver.ServiceNameEndpointResolver;
import org.apache.servicemix.wsn.AbstractSubscription;
import org.oasis_open.docs.wsn.b_2.FilterType;
import org.oasis_open.docs.wsn.b_2.GetCurrentMessage;
import org.oasis_open.docs.wsn.b_2.GetCurrentMessageResponse;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.b_2.QueryExpressionType;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.b_2.SubscribeResponse;
import org.oasis_open.docs.wsn.b_2.TopicExpressionType;
import org.oasis_open.docs.wsn.b_2.UseRaw;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;
import org.oasis_open.docs.wsn.br_2.RegisterPublisherResponse;
import org.w3._2005._08.addressing.EndpointReferenceType;

public class NotificationBroker extends AbstractWSAClient {

    
    public static final String WSN_URI = "http://servicemix.org/wsnotification";

    public static final String WSN_SERVICE = "NotificationBroker";

    public static final QName NOTIFICATION_BROKER = new QName(WSN_URI, WSN_SERVICE);

    public NotificationBroker(ComponentContext context) throws JAXBException {
        ServiceMixClientFacade client = new ServiceMixClientFacade(context);
        client.setMarshaler(new JAXBMarshaler(JAXBContext.newInstance(Subscribe.class, RegisterPublisher.class)));
        setClient(client);
        setResolver(new ServiceNameEndpointResolver(NOTIFICATION_BROKER));
    }

    public NotificationBroker(ComponentContext context, String brokerName) throws JAXBException {
        setClient(createJaxbClient(context));
        setEndpoint(createWSA(WSN_URI + "/" + WSN_SERVICE + "/" + brokerName));
        setResolver(resolveWSA(getEndpoint()));
    }

    public NotificationBroker(JBIContainer container) throws JBIException, JAXBException {
        setClient(createJaxbClient(container));
        setResolver(new ServiceNameEndpointResolver(NOTIFICATION_BROKER));
    }

    public NotificationBroker(JBIContainer container, String brokerName) throws JBIException, JAXBException {
        setClient(createJaxbClient(container));
        setEndpoint(createWSA(WSN_URI + "/" + WSN_SERVICE + "/" + brokerName));
        setResolver(resolveWSA(getEndpoint()));
    }

    public NotificationBroker(ServiceMixClient client) {
        setClient(client);
        setResolver(new ServiceNameEndpointResolver(NOTIFICATION_BROKER));
    }

    public NotificationBroker(ServiceMixClient client, String brokerName) {
        setClient(client);
        setEndpoint(createWSA(WSN_URI + "/" + WSN_SERVICE + "/" + brokerName));
        setResolver(resolveWSA(getEndpoint()));
    }

    public void notify(String topic, Object msg) throws JBIException {
        Notify notify = new Notify();
        NotificationMessageHolderType holder = new NotificationMessageHolderType();
        if (topic != null) {
            TopicExpressionType topicExp = new TopicExpressionType();
            topicExp.getContent().add(topic);
            holder.setTopic(topicExp);
        }
        holder.setMessage(new NotificationMessageHolderType.Message());
        holder.getMessage().setAny(msg);
        notify.getNotificationMessage().add(holder);
        send(notify);
    }

    public Subscription subscribe(EndpointReferenceType consumer, String topic) throws JBIException {
        return subscribe(consumer, topic, null, false);
    }

    public Subscription subscribe(EndpointReferenceType consumer, String topic, String xpath) throws JBIException {
        return subscribe(consumer, topic, xpath, false);
    }

    public Subscription subscribe(EndpointReferenceType consumer, String topic, 
                                  String xpath, boolean raw) throws JBIException {

        Subscribe subscribeRequest = new Subscribe();
        subscribeRequest.setConsumerReference(consumer);
        subscribeRequest.setFilter(new FilterType());
        if (topic != null) {
            TopicExpressionType topicExp = new TopicExpressionType();
            topicExp.getContent().add(topic);
            subscribeRequest.getFilter().getAny().add(
                    new JAXBElement<TopicExpressionType>(AbstractSubscription.QNAME_TOPIC_EXPRESSION,
                            TopicExpressionType.class, topicExp));
        }
        if (xpath != null) {
            QueryExpressionType xpathExp = new QueryExpressionType();
            xpathExp.setDialect(AbstractSubscription.XPATH1_URI);
            xpathExp.getContent().add(xpath);
            subscribeRequest.getFilter().getAny().add(
                    new JAXBElement<QueryExpressionType>(AbstractSubscription.QNAME_MESSAGE_CONTENT,
                            QueryExpressionType.class, xpathExp));
        }
        if (raw) {
            subscribeRequest.setSubscriptionPolicy(new Subscribe.SubscriptionPolicy());
            subscribeRequest.getSubscriptionPolicy().getAny().add(new UseRaw());
        }
        SubscribeResponse response = (SubscribeResponse) request(subscribeRequest);
        return new Subscription(response.getSubscriptionReference(), getClient());
    }

    public List<Object> getCurrentMessage(String topic) throws JBIException {
        GetCurrentMessage getCurrentMessageRequest = new GetCurrentMessage();
        if (topic != null) {
            TopicExpressionType topicExp = new TopicExpressionType();
            topicExp.getContent().add(topic);
            getCurrentMessageRequest.setTopic(topicExp);
        }
        GetCurrentMessageResponse response = (GetCurrentMessageResponse) request(getCurrentMessageRequest);
        return response.getAny();
    }

    public Publisher registerPublisher(EndpointReferenceType publisherReference, 
                                       String topic, boolean demand) throws JBIException {

        RegisterPublisher registerPublisherRequest = new RegisterPublisher();
        registerPublisherRequest.setPublisherReference(publisherReference);
        if (topic != null) {
            TopicExpressionType topicExp = new TopicExpressionType();
            topicExp.getContent().add(topic);
            registerPublisherRequest.getTopic().add(topicExp);
        }
        registerPublisherRequest.setDemand(Boolean.valueOf(demand));
        RegisterPublisherResponse response = (RegisterPublisherResponse) request(registerPublisherRequest);
        return new Publisher(response.getPublisherRegistrationReference(), getClient());
    }

}
