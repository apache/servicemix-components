package org.servicemix.wsn.component;

import java.util.Hashtable;

import javax.jms.ConnectionFactory;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.servicemix.common.BaseComponent;
import org.servicemix.common.BaseLifeCycle;
import org.servicemix.common.ServiceUnit;
import org.servicemix.wsn.EndpointManager;
import org.servicemix.wsn.EndpointRegistrationException;
import org.servicemix.wsn.jbi.JbiNotificationBroker;

public class WSNLifeCycle extends BaseLifeCycle {

	private JbiNotificationBroker notificationBroker;
	private WSNConfiguration configuration;
	private ConnectionFactory connectionFactory;
	private ServiceUnit serviceUnit;
	
	public WSNLifeCycle(BaseComponent component) {
		super(component);
		configuration = new WSNConfiguration();
		serviceUnit = new ServiceUnit();
		serviceUnit.setComponent(component);
	}

    protected Object getExtensionMBean() throws Exception {
        return configuration;
    }
    
	@Override
	protected void doInit() throws Exception {
		super.doInit();
		notificationBroker = new JbiNotificationBroker(configuration.getBrokerName());
		notificationBroker.setContext(context);
		notificationBroker.setManager(new WSNEndpointManager());
		if (connectionFactory == null) {
			connectionFactory = lookupConnectionFactory();
		}
		notificationBroker.setConnectionFactory(connectionFactory);
		notificationBroker.init();
	}

	@Override
	protected void doShutDown() throws Exception {
		// TODO Auto-generated method stub
		super.doShutDown();
	}

	@Override
	protected void doStart() throws Exception {
		super.doStart();
	}

	@Override
	protected void doStop() throws Exception {
		// TODO Auto-generated method stub
		super.doStop();
	}

	public WSNConfiguration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(WSNConfiguration configuration) {
		this.configuration = configuration;
	}

	public ConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}
	
	protected ConnectionFactory lookupConnectionFactory() throws NamingException {
		Hashtable<String,String> props = new Hashtable<String,String>();
		if (configuration.getInitialContextFactory() != null && configuration.getJndiProviderURL() != null) {
			props.put(Context.INITIAL_CONTEXT_FACTORY, configuration.getInitialContextFactory());
			props.put(Context.PROVIDER_URL, configuration.getJndiProviderURL());
		}
		InitialContext ctx = new InitialContext(props);
		ConnectionFactory connectionFactory = (ConnectionFactory) ctx.lookup(configuration.getJndiConnectionFactoryName());
		return connectionFactory;
	}
	
	public class WSNEndpointManager implements EndpointManager {

		public Object register(String address, Object service) throws EndpointRegistrationException {
			component.getRegistry().unregisterServiceUnit(serviceUnit);
			try {
				WSNEndpoint endpoint = new WSNEndpoint(address, service);
				endpoint.setServiceUnit(serviceUnit);
				endpoint.activate();
				serviceUnit.addEndpoint(endpoint);
				return endpoint;
			} catch (Exception e) {
				throw new EndpointRegistrationException("Unable to activate endpoint", e);
			} finally {
				component.getRegistry().registerServiceUnit(serviceUnit);
			}
		}

		public void unregister(Object endpoint) throws EndpointRegistrationException {
			component.getRegistry().unregisterServiceUnit(serviceUnit);
			try {
				((WSNEndpoint) endpoint).deactivate();
			} catch (Exception e) {
				throw new EndpointRegistrationException("Unable to activate endpoint", e);
			} finally {
				serviceUnit.getEndpoints().remove(endpoint);
				component.getRegistry().registerServiceUnit(serviceUnit);
			}
		}

	}

}
