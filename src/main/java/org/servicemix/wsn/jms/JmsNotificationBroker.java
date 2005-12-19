package org.servicemix.wsn.jms;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

import org.oasis_open.docs.wsn.b_1.CreatePullPoint;
import org.oasis_open.docs.wsn.b_1.Subscribe;
import org.oasis_open.docs.wsn.br_1.RegisterPublisher;
import org.servicemix.wsn.AbstractNotificationBroker;
import org.servicemix.wsn.AbstractPublisher;
import org.servicemix.wsn.AbstractPullPoint;
import org.servicemix.wsn.AbstractSubscription;

public abstract class JmsNotificationBroker extends AbstractNotificationBroker {

	private ConnectionFactory connectionFactory;
	private Connection connection;
	
	public JmsNotificationBroker(String name) {
		super(name);
	}

    public void init() throws Exception {
    	if (connection == null) {
    		connection = connectionFactory.createConnection();
			connection.start();
    	}
    	super.init();
    }
	
	@Override
	protected AbstractPublisher createPublisher(String name, RegisterPublisher registerPublisherRequest) {
		JmsPublisher publisher = new JmsPublisher(name);
		publisher.setManager(getManager());
		publisher.setConnection(connection);
		return publisher;
	}

	@Override
	protected AbstractPullPoint createPullPoint(String name, CreatePullPoint createPullPointRequest) {
		JmsPullPoint pullPoint = new JmsPullPoint(name);
		pullPoint.setManager(getManager());
		pullPoint.setConnection(connection);
		return pullPoint;
	}

	@Override
	protected AbstractSubscription createSubcription(String name, Subscribe subscribeRequest) {
		JmsSubscription subscription = createJmsSubscription(name);
		subscription.setManager(getManager());
		subscription.setConnection(connection);
		return subscription;
	}
	
	protected abstract JmsSubscription createJmsSubscription(String name);

	public ConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

}
