package org.servicemix.wsn.component;

public interface WSNConfigurationMBean {

	String getInitialContextFactory();
	void setInitialContextFactory(String initialContextFactory);
	
	String getJndiProviderURL();
	void setJndiProviderURL(String jndiProviderURL);
	
	String getJndiConnectionFactoryName();
	void setJndiConnectionFactoryName(String jndiConnectionFactoryName);
	
	String getBrokerName();
	void setBrokerName(String brokerName);
}
