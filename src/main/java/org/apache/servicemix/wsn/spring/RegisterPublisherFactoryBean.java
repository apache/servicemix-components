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

import org.apache.servicemix.wsn.client.AbstractWSAClient;
import org.oasis_open.docs.wsn.b_2.TopicExpressionType;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;
import org.springframework.beans.factory.FactoryBean;

/**
 * 
 * @author gnodet
 * @version $Revision: 376451 $
 * @org.apache.xbean.XBean element="register-publisher"
 */
public class RegisterPublisherFactoryBean implements FactoryBean {

    private String publisher;
    private String topic;
    private boolean demand;
    
    /**
     * @return Returns the demand.
     */
    public boolean isDemand() {
        return demand;
    }

    /**
     * @param demand The demand to set.
     */
    public void setDemand(boolean demand) {
        this.demand = demand;
    }

    /**
     * @return Returns the publisher.
     */
    public String getPublisher() {
        return publisher;
    }

    /**
     * @param publisher The publisher to set.
     */
    public void setPublisher(String publisher) {
        this.publisher = publisher;
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

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.FactoryBean#getObject()
     */
    public Object getObject() throws Exception {
        RegisterPublisher registerPublisher = new RegisterPublisher();
        registerPublisher.setPublisherReference(AbstractWSAClient.createWSA(publisher));
        if (topic != null) {
            TopicExpressionType topicExp = new TopicExpressionType();
            topicExp.getContent().add(topic);
            registerPublisher.getTopic().add(topicExp);
        }
        registerPublisher.setDemand(new Boolean(demand));
        return registerPublisher;
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.FactoryBean#getObjectType()
     */
    public Class getObjectType() {
        return RegisterPublisher.class;
    }

    /* (non-Javadoc)
     * @see org.springframework.beans.factory.FactoryBean#isSingleton()
     */
    public boolean isSingleton() {
        return false;
    }

}
