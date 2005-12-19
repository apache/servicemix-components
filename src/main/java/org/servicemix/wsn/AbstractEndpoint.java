package org.servicemix.wsn;

public abstract class AbstractEndpoint {

	protected String name;
	protected String address;
	protected EndpointManager manager;
	protected Object endpoint;
	
	public AbstractEndpoint(String name) {
		setName(name);
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		this.address = createAddress();
	}

	public String getAddress() {
		return address;
	}
	
	public void register() throws EndpointRegistrationException {
		endpoint = manager.register(getAddress(), this);
	}
	
	public void unregister() throws EndpointRegistrationException {
		manager.unregister(endpoint);
	}

	public EndpointManager getManager() {
		return manager;
	}

	public void setManager(EndpointManager manager) {
		this.manager = manager;
	}
	
	protected abstract String createAddress();

}
