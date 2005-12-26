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

import org.oasis_open.docs.wsn.br_1.Destroy;
import org.apache.servicemix.client.ServiceMixClient;
import org.w3._2005._03.addressing.EndpointReferenceType;

public class Publisher extends AbstractWSAClient {

	public Publisher(EndpointReferenceType publisherRegistrationReference, ServiceMixClient client) {
		super(publisherRegistrationReference, client);
	}

	public void destroy() throws JBIException {
		request(new Destroy());
	}
	
}
