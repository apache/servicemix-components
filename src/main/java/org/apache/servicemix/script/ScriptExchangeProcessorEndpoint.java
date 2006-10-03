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
package org.apache.servicemix.script;

import javax.jbi.component.ComponentContext;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;

import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ExchangeProcessor;

/**
 * @org.apache.xbean.XBean element="exchangeProcessor"
 */
public class ScriptExchangeProcessorEndpoint extends Endpoint implements
		ExchangeProcessor {

	private ServiceEndpoint activated;

	private DeliveryChannel channel;

	private MessageExchangeFactory exchangeFactory;

	private ScriptExchangeProcessor script;

	public void activate() throws Exception {
		logger = this.serviceUnit.getComponent().getLogger();
		ComponentContext ctx = getServiceUnit().getComponent()
				.getComponentContext();
		channel = ctx.getDeliveryChannel();
		exchangeFactory = channel.createExchangeFactory();
		activated = ctx.activateEndpoint(service, endpoint);
		start();
	}

	public void deactivate() throws Exception {
		stop();
		ServiceEndpoint ep = activated;
		activated = null;
		ComponentContext ctx = getServiceUnit().getComponent()
				.getComponentContext();
		ctx.deactivateEndpoint(ep);
	}

	protected void done(MessageExchange me) throws MessagingException {
		me.setStatus(ExchangeStatus.DONE);
		send(me);
	}

	protected void fail(MessageExchange me, Exception error)
			throws MessagingException {
		me.setError(error);
		send(me);
	}

	public ExchangeProcessor getProcessor() {
		return this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.servicemix.common.Endpoint#getRole()
	 */
	public Role getRole() {
		return Role.PROVIDER;
	}

	public ScriptExchangeProcessor getScript() {
		return script;
	}

	public void process(MessageExchange exchange) throws Exception {
		if (script != null)
			script.process(exchange);

	}

	protected void send(MessageExchange me) throws MessagingException {
		if (me.getRole() == MessageExchange.Role.CONSUMER
				&& me.getStatus() == ExchangeStatus.ACTIVE) {
			BaseLifeCycle lf = (BaseLifeCycle) getServiceUnit().getComponent()
					.getLifeCycle();
			lf.sendConsumerExchange(me, (Endpoint) this);
		} else {
			channel.send(me);
		}
	}

	public void setScript(ScriptExchangeProcessor script) {
		this.script = script;
		script.setScriptExchangeProcessorEndpoint(this);
	}

	public void start() throws Exception {
		if (script != null)
			script.start();
	}

	public void stop() {
		if (script != null) {
			try {
				script.stop();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public void validate() throws DeploymentException {
	}

}
