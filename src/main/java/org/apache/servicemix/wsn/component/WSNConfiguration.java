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
package org.apache.servicemix.wsn.component;

import org.apache.servicemix.common.PersistentConfiguration;

public class WSNConfiguration extends PersistentConfiguration implements WSNConfigurationMBean {

	private String initialContextFactory;
	private String jndiProviderURL;
	private String jndiConnectionFactoryName = "java:comp/env/jms/wsnotificationCF";
	
	private String brokerName = "Broker";
	
	public String getInitialContextFactory() {
		return initialContextFactory;
	}
	public void setInitialContextFactory(String initialContextFactory) {
		this.initialContextFactory = initialContextFactory;
	}
	public String getJndiConnectionFactoryName() {
		return jndiConnectionFactoryName;
	}
	public void setJndiConnectionFactoryName(String jndiConnectionFactoryName) {
		this.jndiConnectionFactoryName = jndiConnectionFactoryName;
	}
	public String getJndiProviderURL() {
		return jndiProviderURL;
	}
	public void setJndiProviderURL(String jndiProviderURL) {
		this.jndiProviderURL = jndiProviderURL;
	}
	public String getBrokerName() {
		return brokerName;
	}
	public void setBrokerName(String brokerName) {
		this.brokerName = brokerName;
	}
}
