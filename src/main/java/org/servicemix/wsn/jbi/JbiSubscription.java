package org.servicemix.wsn.jbi;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.docs.wsn.b_1.Subscribe;
import org.oasis_open.docs.wsn.b_1.SubscribeCreationFailedFaultType;
import org.servicemix.jbi.jaxp.StringSource;
import org.servicemix.wsn.jaxws.InvalidFilterFault;
import org.servicemix.wsn.jaxws.InvalidMessageContentExpressionFault;
import org.servicemix.wsn.jaxws.InvalidProducerPropertiesExpressionFault;
import org.servicemix.wsn.jaxws.InvalidTopicExpressionFault;
import org.servicemix.wsn.jaxws.InvalidUseRawValueFault;
import org.servicemix.wsn.jaxws.SubscribeCreationFailedFault;
import org.servicemix.wsn.jaxws.TopicExpressionDialectUnknownFault;
import org.servicemix.wsn.jaxws.TopicNotSupportedFault;
import org.servicemix.wsn.jaxws.UnacceptableInitialTerminationTimeFault;
import org.servicemix.wsn.jms.JmsSubscription;

public class JbiSubscription extends JmsSubscription {

	private static Log log = LogFactory.getLog(JbiSubscription.class);
	
	private ComponentContext context;
	private ServiceEndpoint endpoint;
	
	public JbiSubscription(String name) {
		super(name);
	}

	@Override
	protected void validateSubscription(Subscribe subscribeRequest) throws InvalidFilterFault, InvalidMessageContentExpressionFault, InvalidProducerPropertiesExpressionFault, InvalidTopicExpressionFault, InvalidUseRawValueFault, SubscribeCreationFailedFault, TopicExpressionDialectUnknownFault, TopicNotSupportedFault, UnacceptableInitialTerminationTimeFault {
		super.validateSubscription(subscribeRequest);
		String[] parts = split(consumerReference.getAddress().getValue());
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
	protected void doNotify(String notify) {
		try {
			DeliveryChannel channel = context.getDeliveryChannel();
			MessageExchangeFactory factory = channel.createExchangeFactory(endpoint);
			InOnly inonly = factory.createInOnlyExchange();
			NormalizedMessage msg = inonly.createMessage();
			inonly.setInMessage(msg);
			msg.setContent(new StringSource(notify));
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
