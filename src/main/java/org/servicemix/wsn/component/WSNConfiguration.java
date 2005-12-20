package org.servicemix.wsn.component;

public class WSNConfiguration implements WSNConfigurationMBean {

	private String initialContextFactory;
	private String jndiProviderURL;
	private String jndiConnectionFactoryName;
	
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
