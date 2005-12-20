package org.servicemix.wsn.jbi;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.oasis_open.docs.wsn.br_1.PublisherRegistrationFailedFaultType;
import org.oasis_open.docs.wsn.br_1.RegisterPublisher;
import org.servicemix.wsn.client.AbstractWSAClient;
import org.servicemix.wsn.client.NotificationBroker;
import org.servicemix.wsn.client.Subscription;
import org.servicemix.wsn.jaxws.InvalidTopicExpressionFault;
import org.servicemix.wsn.jaxws.PublisherRegistrationFailedFault;
import org.servicemix.wsn.jaxws.PublisherRegistrationRejectedFault;
import org.servicemix.wsn.jaxws.ResourceUnknownFault;
import org.servicemix.wsn.jaxws.TopicNotSupportedFault;
import org.servicemix.wsn.jms.JmsPublisher;

public class JbiPublisher extends JmsPublisher {

	private ComponentContext context;
	private ServiceEndpoint endpoint;
	private String notificationBrokerAddress;
	
	public JbiPublisher(String name) {
		super(name);
	}

	public void setContext(ComponentContext context) {
		this.context = context;
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
			NotificationBroker broker = new NotificationBroker(context);
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
		endpoint = context.getEndpoint(new QName(parts[0], parts[1]), parts[2]);
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

}
