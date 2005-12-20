package org.servicemix.wsn.client;

import javax.jbi.JBIException;

import org.oasis_open.docs.wsn.b_1.PauseSubscription;
import org.oasis_open.docs.wsn.b_1.ResumeSubscription;
import org.oasis_open.docs.wsn.b_1.Unsubscribe;
import org.servicemix.client.ServiceMixClient;
import org.w3._2005._03.addressing.EndpointReferenceType;

public class Subscription extends AbstractWSAClient {

	public Subscription(EndpointReferenceType subscriptionReference, ServiceMixClient client) {
		super(subscriptionReference, client);
	}
	
	public void pause() throws JBIException {
		request(new PauseSubscription());
	}
	
	public void resume() throws JBIException {
		request(new ResumeSubscription());
	}
	
	public void unsubscribe() throws JBIException {
		request(new Unsubscribe());
	}
}
