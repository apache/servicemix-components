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

import org.apache.servicemix.wsn.client.AbstractWSAClient;
import org.apache.servicemix.wsn.client.NotificationBroker;
import org.apache.servicemix.wsn.client.Subscription;
import org.apache.servicemix.wsn.component.WSNLifeCycle;
import org.apache.servicemix.wsn.jaxws.InvalidTopicExpressionFault;
import org.apache.servicemix.wsn.jaxws.PublisherRegistrationFailedFault;
import org.apache.servicemix.wsn.jaxws.PublisherRegistrationRejectedFault;
import org.apache.servicemix.wsn.jaxws.ResourceUnknownFault;
import org.apache.servicemix.wsn.jaxws.TopicNotSupportedFault;
import org.apache.servicemix.wsn.jms.JmsPublisher;
import org.oasis_open.docs.wsn.br_2.PublisherRegistrationFailedFaultType;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;

public class JbiPublisher extends JmsPublisher {

    private WSNLifeCycle lifeCycle;
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
			subscription = broker.subscribe(AbstractWSAClient.createWSA(notificationBrokerAddress), 
														 "noTopic", null);
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
	protected void validatePublisher(RegisterPublisher registerPublisherRequest) throws InvalidTopicExpressionFault, PublisherRegistrationFailedFault, PublisherRegistrationRejectedFault, ResourceUnknownFault, TopicNotSupportedFault {
		super.validatePublisher(registerPublisherRequest);
		String[] parts = split(publisherReference.getAddress().getValue());
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
		String nsUri   = uri.substring(0, idx2);
    	return new String[] { nsUri, svcName, epName };
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

}
