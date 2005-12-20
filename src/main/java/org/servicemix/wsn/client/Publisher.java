package org.servicemix.wsn.client;

import javax.jbi.JBIException;

import org.oasis_open.docs.wsn.br_1.Destroy;
import org.servicemix.client.ServiceMixClient;
import org.w3._2005._03.addressing.EndpointReferenceType;

public class Publisher extends AbstractWSAClient {

	public Publisher(EndpointReferenceType publisherRegistrationReference, ServiceMixClient client) {
		super(publisherRegistrationReference, client);
	}

	public void destroy() throws JBIException {
		request(new Destroy());
	}
	
}
