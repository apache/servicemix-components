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
package org.apache.servicemix.wsn.client;

import javax.jbi.JBIException;

import org.oasis_open.docs.wsn.b_2.PauseSubscription;
import org.oasis_open.docs.wsn.b_2.ResumeSubscription;
import org.oasis_open.docs.wsn.b_2.Unsubscribe;
import org.apache.servicemix.client.ServiceMixClient;
import org.w3._2005._08.addressing.EndpointReferenceType;

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
