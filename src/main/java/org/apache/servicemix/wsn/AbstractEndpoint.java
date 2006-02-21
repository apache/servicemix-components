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
package org.apache.servicemix.wsn;

import org.w3._2005._08.addressing.AttributedURIType;
import org.w3._2005._08.addressing.EndpointReferenceType;

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
    
    public void setAddress(String address) {
        this.address = address;
    }
	
	public void register() throws EndpointRegistrationException {
		endpoint = manager.register(getAddress(), this);
	}
	
	public void unregister() throws EndpointRegistrationException {
		if (endpoint != null) {
			manager.unregister(endpoint);
		}
	}

	public EndpointManager getManager() {
		return manager;
	}

	public void setManager(EndpointManager manager) {
		this.manager = manager;
	}
	
	protected abstract String createAddress();

    public static EndpointReferenceType createEndpointReference(String address) {
        EndpointReferenceType epr = new EndpointReferenceType();
        AttributedURIType addressUri = new AttributedURIType();
        addressUri.setValue(address);
        epr.setAddress(addressUri);
        return epr;
    }

}
