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

import javax.xml.bind.JAXBElement;

import org.apache.servicemix.wsn.AbstractSubscription;
import org.apache.servicemix.wsn.client.AbstractWSAClient;
import org.oasis_open.docs.wsn.b_2.FilterType;
import org.oasis_open.docs.wsn.b_2.QueryExpressionType;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.b_2.TopicExpressionType;
import org.oasis_open.docs.wsn.b_2.UseRaw;
import org.springframework.beans.factory.FactoryBean;

/**
 * 
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="subscribe"
 */
public class SubscribeFactoryBean implements FactoryBean {

    private String consumer;

    private String topic;

    private String xpath;

    private boolean raw;

    /**
     * @return Returns the consumer.
     */
    public String getConsumer() {
        return consumer;
    }

    /**
     * @param consumer
     *            The consumer to set.
     */
    public void setConsumer(String consumer) {
        this.consumer = consumer;
    }

    /**
     * @return Returns the topic.
     */
    public String getTopic() {
        return topic;
    }

    /**
     * @param topic
     *            The topic to set.
     */
    public void setTopic(String topic) {
        this.topic = topic;
    }

    /**
     * @return Returns the xpath.
     */
    public String getXpath() {
        return xpath;
    }

    /**
     * @param xpath
     *            The xpath to set.
     */
    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

    /**
     * @return Returns the raw.
     */
    public boolean isRaw() {
        return raw;
    }

    /**
     * @param raw
     *            The raw to set.
     */
    public void setRaw(boolean raw) {
        this.raw = raw;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.FactoryBean#getObject()
     */
    public Object getObject() throws Exception {
        Subscribe subscribe = new Subscribe();
        subscribe.setConsumerReference(AbstractWSAClient.createWSA(consumer));
        subscribe.setFilter(new FilterType());
        if (topic != null) {
            TopicExpressionType topicExp = new TopicExpressionType();
            topicExp.getContent().add(topic);
            subscribe.getFilter().getAny().add(
                    new JAXBElement<TopicExpressionType>(AbstractSubscription.QNAME_TOPIC_EXPRESSION,
                            TopicExpressionType.class, topicExp));
        }
        if (xpath != null) {
            QueryExpressionType xpathExp = new QueryExpressionType();
            xpathExp.setDialect(AbstractSubscription.XPATH1_URI);
            xpathExp.getContent().add(xpath);
            subscribe.getFilter().getAny().add(
                    new JAXBElement<QueryExpressionType>(AbstractSubscription.QNAME_MESSAGE_CONTENT,
                            QueryExpressionType.class, xpathExp));
        }
        if (raw) {
            subscribe.setSubscriptionPolicy(new Subscribe.SubscriptionPolicy());
            subscribe.getSubscriptionPolicy().getAny().add(new UseRaw());
        }
        return subscribe;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.FactoryBean#getObjectType()
     */
    public Class getObjectType() {
        return Subscribe.class;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.FactoryBean#isSingleton()
     */
    public boolean isSingleton() {
        return false;
    }

}
