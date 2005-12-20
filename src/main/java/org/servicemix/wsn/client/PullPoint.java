package org.servicemix.wsn.client;

import java.math.BigInteger;
import java.util.List;

import javax.jbi.JBIException;

import org.oasis_open.docs.wsn.b_1.Destroy;
import org.oasis_open.docs.wsn.b_1.GetMessages;
import org.oasis_open.docs.wsn.b_1.GetMessagesResponse;
import org.oasis_open.docs.wsn.b_1.NotificationMessageHolderType;
import org.servicemix.client.ServiceMixClient;
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
