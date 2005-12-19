package org.servicemix.wsn;

public interface EndpointManager {
	
	Object register(String address, Object service) throws EndpointRegistrationException;
	
	void unregister(Object endpoint) throws EndpointRegistrationException;

}
