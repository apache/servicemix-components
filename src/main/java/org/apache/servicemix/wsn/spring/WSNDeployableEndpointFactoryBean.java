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

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.apache.servicemix.wsn.component.WSNDeployableEndpoint;
import org.apache.servicemix.wsn.component.WSNEndpoint;
import org.apache.servicemix.wsn.component.WSNSubscriptionEndpoint;
import org.apache.servicemix.wsn.component.WSNPullPointEndpoint;
import org.apache.servicemix.wsn.component.WSNPublisherEndpoint;
import org.oasis_open.docs.wsn.brw_2.NotificationBroker;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.b_2.CreatePullPoint;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;

/**
 * @org.apache.xbean.XBean element="endpoint"
 */
public class WSNDeployableEndpointFactoryBean implements FactoryBean, InitializingBean {

    private QName service;
    private String endpoint;
    private Resource resource;
    private Object request;

    public QName getService() {
        return service;
    }

    public void setService(QName service) {
        this.service = service;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public Object getRequest() {
        return request;
    }

    public void setRequest(Object request) {
        this.request = request;
    }

    public void afterPropertiesSet() throws Exception {
        if (request == null && resource == null) {
            throw new IllegalStateException("One of request or resource properties must be set");
        }
    }

    public Object getObject() throws Exception {
        if (request == null && resource != null) {
            JAXBContext context = WSNEndpoint.createJAXBContext(NotificationBroker.class);
            InputStream is = resource.getInputStream();
            try {
                request = context.createUnmarshaller().unmarshal(is);
            } finally {
                is.close();
            }
        }
        if (request instanceof Subscribe) {
            return new WSNSubscriptionEndpoint((Subscribe) request, service, endpoint);
        } else if (request instanceof CreatePullPoint) {
            return new WSNPullPointEndpoint((CreatePullPoint) request, service, endpoint);
        } else if (request instanceof RegisterPublisher) {
            return new WSNPublisherEndpoint((RegisterPublisher) request, service, endpoint);
        } else {
            throw new IllegalStateException("Unrecognized request of type " + request.getClass());
        }
    }

    public Class getObjectType() {
        return WSNDeployableEndpoint.class;
    }

    public boolean isSingleton() {
        return false;
    }
}
