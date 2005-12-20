package org.servicemix.wsn.client;

import javax.jbi.JBIException;
import javax.xml.namespace.QName;

import org.servicemix.client.ServiceMixClient;
import org.servicemix.jbi.resolver.EndpointResolver;
import org.servicemix.jbi.resolver.ServiceAndEndpointNameResolver;
import org.w3._2005._03.addressing.EndpointReferenceType;

public abstract class AbstractWSAClient {

	private EndpointReferenceType endpoint;
	private EndpointResolver resolver;
	private ServiceMixClient client;
	
	public AbstractWSAClient() {
	}
	
	public AbstractWSAClient(EndpointReferenceType endpoint, ServiceMixClient client) {
		this.endpoint = endpoint;
		this.resolver = resolveWSA(endpoint);
		this.client = client;
	}

	protected EndpointResolver resolveWSA(EndpointReferenceType ref) {
		String[] parts = split(ref.getAddress().getValue());
		return new ServiceAndEndpointNameResolver(new QName(parts[0], parts[1]), parts[2]);
	}

    protected String[] split(String uri) {
		char sep;
		if (uri.indexOf('/') > 0) {
			sep = '/';
		} else {
			sep = ':';
		}
		int idx1 = uri.lastIndexOf(sep);
		int idx2 = uri.lastIndexOf(sep, idx1 - 1);
		String epName = uri.substring(idx1 + 1);
		String svcName = uri.substring(idx2 + 1, idx1);
		String nsUri   = uri.substring(0, idx2);
    	return new String[] { nsUri, svcName, epName };
    }

	public EndpointReferenceType getEndpoint() {
		return endpoint;
	}

	public void setEndpoint(EndpointReferenceType endpoint) {
		this.endpoint = endpoint;
	}

	public EndpointResolver getResolver() {
		return resolver;
	}

	public void setResolver(EndpointResolver resolver) {
		this.resolver = resolver;
	}
	
	public ServiceMixClient getClient() {
		return client;
	}

	public void setClient(ServiceMixClient client) {
		this.client = client;
	}
	protected Object request(Object request) throws JBIException {
		return client.request(resolver, null, null, request);
	}

}
