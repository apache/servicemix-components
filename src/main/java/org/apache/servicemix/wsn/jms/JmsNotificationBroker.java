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
package org.apache.servicemix.wsn.jms;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

import org.apache.servicemix.wsn.AbstractNotificationBroker;
import org.apache.servicemix.wsn.AbstractPublisher;
import org.apache.servicemix.wsn.AbstractSubscription;

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
	protected AbstractPublisher createPublisher(String name) {
		JmsPublisher publisher = createJmsPublisher(name);
		publisher.setManager(getManager());
		publisher.setConnection(connection);
		return publisher;
	}

	@Override
	protected AbstractSubscription createSubcription(String name) {
		JmsSubscription subscription = createJmsSubscription(name);
		subscription.setManager(getManager());
		subscription.setConnection(connection);
		return subscription;
	}
	
	protected abstract JmsSubscription createJmsSubscription(String name);

	protected abstract JmsPublisher createJmsPublisher(String name);

	public ConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

}
