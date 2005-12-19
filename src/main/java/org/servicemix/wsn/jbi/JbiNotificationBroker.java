package org.servicemix.wsn.jbi;

import javax.jbi.component.ComponentContext;

import org.servicemix.wsn.jms.JmsNotificationBroker;
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

	public ComponentContext getContext() {
		return context;
	}

	public void setContext(ComponentContext context) {
		this.context = context;
	}

}
