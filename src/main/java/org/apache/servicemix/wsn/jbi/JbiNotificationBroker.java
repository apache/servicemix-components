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

import org.apache.servicemix.wsn.component.WSNLifeCycle;
import org.apache.servicemix.wsn.jms.JmsNotificationBroker;
import org.apache.servicemix.wsn.jms.JmsPublisher;
import org.apache.servicemix.wsn.jms.JmsSubscription;

public class JbiNotificationBroker extends JmsNotificationBroker {

	private WSNLifeCycle lifeCycle;
	
	public JbiNotificationBroker(String name) {
		super(name);
	}
	
	@Override
	protected JmsSubscription createJmsSubscription(String name) {
		JbiSubscription subscription = new JbiSubscription(name);
		subscription.setLifeCycle(lifeCycle);
		return subscription;
	}

	@Override
	protected JmsPublisher createJmsPublisher(String name) {
		JbiPublisher publisher = new JbiPublisher(name);
		publisher.setLifeCycle(lifeCycle);
		publisher.setNotificationBrokerAddress(address);
		return publisher;
	}

    public WSNLifeCycle getLifeCycle() {
        return lifeCycle;
    }

    public void setLifeCycle(WSNLifeCycle lifeCycle) {
        this.lifeCycle = lifeCycle;
    }

}
