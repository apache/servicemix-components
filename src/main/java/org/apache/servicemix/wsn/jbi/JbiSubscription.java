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
package org.apache.servicemix.wsn.jbi;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.w3c.dom.Element;

public class JbiSubscription extends JmsSubscription {

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
	protected void validateSubscription(Subscribe subscribeRequest) throws InvalidFilterFault, InvalidMessageContentExpressionFault, InvalidProducerPropertiesExpressionFault, InvalidTopicExpressionFault, SubscribeCreationFailedFault, TopicExpressionDialectUnknownFault, TopicNotSupportedFault, UnacceptableInitialTerminationTimeFault {
		super.validateSubscription(subscribeRequest);
        String[] parts = split(consumerReference.getAddress().getValue().trim());
        endpoint = context.getEndpoint(new QName(parts[0], parts[1]), parts[2]);
        if (endpoint == null) {
            SubscribeCreationFailedFaultType fault = new SubscribeCreationFailedFaultType();
            throw new SubscribeCreationFailedFault("Unable to resolve consumer reference endpoint", fault);
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
	
	@Override
	protected void doNotify(Element content) {
		try {
			DeliveryChannel channel = context.getDeliveryChannel();
			MessageExchangeFactory factory = channel.createExchangeFactory(endpoint);
			InOnly inonly = factory.createInOnlyExchange();
			NormalizedMessage msg = inonly.createMessage();
			inonly.setInMessage(msg);
			msg.setContent(new DOMSource(content));
			if (!channel.sendSync(inonly)) {
				log.warn("Notification was aborted");
			}
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
