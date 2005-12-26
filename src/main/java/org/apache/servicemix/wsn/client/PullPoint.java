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

import java.math.BigInteger;
import java.util.List;

import javax.jbi.JBIException;

import org.oasis_open.docs.wsn.b_1.Destroy;
import org.oasis_open.docs.wsn.b_1.GetMessages;
import org.oasis_open.docs.wsn.b_1.GetMessagesResponse;
import org.oasis_open.docs.wsn.b_1.NotificationMessageHolderType;
import org.apache.servicemix.client.ServiceMixClient;
import org.w3._2005._03.addressing.EndpointReferenceType;

public class PullPoint extends AbstractWSAClient {

	public PullPoint(EndpointReferenceType pullPoint, ServiceMixClient client) {
		super(pullPoint, client);
	}

	public List<NotificationMessageHolderType> getMessages(int max) throws JBIException {
		GetMessages getMessages = new GetMessages();
		getMessages.setMaximumNumber(BigInteger.valueOf(max));
		GetMessagesResponse response = (GetMessagesResponse) request(getMessages);
		return response.getNotificationMessage();
	}
	
	public void destroy() throws JBIException {
		request(new Destroy());
	}
	
}
