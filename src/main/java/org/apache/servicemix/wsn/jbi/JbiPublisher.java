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
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.servicemix.wsn.ComponentContextAware;
import org.apache.servicemix.wsn.client.AbstractWSAClient;
import org.apache.servicemix.wsn.client.NotificationBroker;
import org.apache.servicemix.wsn.client.Subscription;
import org.apache.servicemix.wsn.jms.JmsPublisher;
import org.oasis_open.docs.wsn.br_2.PublisherRegistrationFailedFaultType;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationFailedFault;
import org.oasis_open.docs.wsn.brw_2.PublisherRegistrationRejectedFault;
import org.oasis_open.docs.wsn.bw_2.InvalidTopicExpressionFault;
import org.oasis_open.docs.wsn.bw_2.TopicNotSupportedFault;
import org.oasis_open.docs.wsrf.rw_2.ResourceUnknownFault;

public class JbiPublisher extends JmsPublisher implements ComponentContextAware {

    private ComponentContext context;

    private ServiceEndpoint endpoint;

    private String notificationBrokerAddress;

    public JbiPublisher(String name) {
        super(name);
    }

    public String getNotificationBrokerAddress() {
        return notificationBrokerAddress;
    }

    public void setNotificationBrokerAddress(String notificationBrokerAddress) {
        this.notificationBrokerAddress = notificationBrokerAddress;
    }

    @Override
    protected Object startSubscription() {
        Subscription subscription = null;
        try {
            NotificationBroker broker = new NotificationBroker(getContext());
            broker.setResolver(AbstractWSAClient.resolveWSA(publisherReference));
            subscription = broker.subscribe(AbstractWSAClient.createWSA(notificationBrokerAddress), "noTopic", null);
        } catch (JBIException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return subscription;
    }

    @Override
    protected void destroySubscription(Object subscription) {
        try {
            ((Subscription) subscription).unsubscribe();
        } catch (JBIException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void validatePublisher(RegisterPublisher registerPublisherRequest) throws InvalidTopicExpressionFault,
            PublisherRegistrationFailedFault, PublisherRegistrationRejectedFault, ResourceUnknownFault,
            TopicNotSupportedFault {
        super.validatePublisher(registerPublisherRequest);
        String[] parts = split(AbstractWSAClient.getWSAAddress(publisherReference));
        endpoint = getContext().getEndpoint(new QName(parts[0], parts[1]), parts[2]);
        if (endpoint == null) {
            PublisherRegistrationFailedFaultType fault = new PublisherRegistrationFailedFaultType();
            throw new PublisherRegistrationFailedFault("Unable to resolve consumer reference endpoint", fault);
        }
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

    public ComponentContext getContext() {
        return context;
    }

    public void setContext(ComponentContext context) {
        this.context = context;
    }
}
