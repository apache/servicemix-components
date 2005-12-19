package org.servicemix.wsn.component;

import javax.jms.ConnectionFactory;

import org.servicemix.common.BaseComponent;
import org.servicemix.common.BaseLifeCycle;

public class WSNComponent extends BaseComponent {

	@Override
	protected BaseLifeCycle createLifeCycle() {
		return new WSNLifeCycle(this);
	}

	public ConnectionFactory getConnectionFactory() {
		return ((WSNLifeCycle) lifeCycle).getConnectionFactory();
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		((WSNLifeCycle) lifeCycle).setConnectionFactory(connectionFactory);
	}
	
}
