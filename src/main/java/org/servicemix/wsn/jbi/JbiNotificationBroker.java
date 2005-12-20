package org.servicemix.wsn.jbi;

import javax.jbi.component.ComponentContext;

import org.servicemix.wsn.jms.JmsNotificationBroker;
import org.servicemix.wsn.jms.JmsPublisher;
import org.servicemix.wsn.jms.JmsSubscription;

public class JbiNotificationBroker extends JmsNotificationBroker {

	private ComponentContext context;
	
	public JbiNotificationBroker(String name) {
		super(name);
	}
	
	@Override
	protected JmsSubscription createJmsSubscription(String name) {
		JbiSubscription subscription = new JbiSubscription(name);
		subscription.setContext(context);
		return subscription;
	}

	@Override
	protected JmsPublisher createJmsPublisher(String name) {
		JbiPublisher publisher = new JbiPublisher(name);
		publisher.setContext(context);
		publisher.setNotificationBrokerAddress(address);
		return publisher;
	}

	public ComponentContext getContext() {
		return context;
	}

	public void setContext(ComponentContext context) {
		this.context = context;
	}

}
