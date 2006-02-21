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

import java.math.BigInteger;
import java.util.List;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.docs.wsn.b_2.CreatePullPoint;
import org.oasis_open.docs.wsn.b_2.DestroyPullPoint;
import org.oasis_open.docs.wsn.b_2.DestroyPullPointResponse;
import org.oasis_open.docs.wsn.b_2.GetMessages;
import org.oasis_open.docs.wsn.b_2.GetMessagesResponse;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.b_2.UnableToDestroyPullPointFaultType;
import org.apache.servicemix.wsn.jaxws.NotificationConsumer;
import org.apache.servicemix.wsn.jaxws.PullPoint;
import org.apache.servicemix.wsn.jaxws.ResourceUnknownFault;
import org.apache.servicemix.wsn.jaxws.UnableToCreatePullPointFault;
import org.apache.servicemix.wsn.jaxws.UnableToDestroyPullPointFault;

@WebService(endpointInterface = "org.apache.servicemix.wsn.PullPointConsumer")
public abstract class AbstractPullPoint extends AbstractEndpoint 
										implements PullPoint, NotificationConsumer {

	private static Log log = LogFactory.getLog(AbstractPullPoint.class);
	
    protected AbstractCreatePullPoint createPullPoint;
    
	public AbstractPullPoint(String name) {
		super(name);
	}
	
    /**
     * 
     * @param notify
     */
    @WebMethod(operationName = "Notify")
    @Oneway
    public void notify(
        @WebParam(name = "Notify", targetNamespace = "http://docs.oasis-open.org/wsn/b-1", partName = "Notify")
        Notify notify) {
    
    	log.debug("Notify");
    	for (NotificationMessageHolderType messageHolder : notify.getNotificationMessage()) {
    		store(messageHolder);
    	}
    }
    
    /**
     * 
     * @param getMessagesRequest
     * @return
     *     returns org.oasis_open.docs.wsn.b_1.GetMessagesResponse
     * @throws ResourceUnknownFault
     */
    @WebMethod(operationName = "GetMessages")
    @WebResult(name = "GetMessagesResponse", targetNamespace = "http://docs.oasis-open.org/wsn/b-1", partName = "GetMessagesResponse")
    public GetMessagesResponse getMessages(
        @WebParam(name = "GetMessages", targetNamespace = "http://docs.oasis-open.org/wsn/b-1", partName = "GetMessagesRequest")
        GetMessages getMessagesRequest)
        throws ResourceUnknownFault {
    	
    	log.debug("GetMessages");
    	BigInteger max = getMessagesRequest.getMaximumNumber();
    	List<NotificationMessageHolderType> messages = getMessages(max != null ? max.intValue() : 0);
    	GetMessagesResponse response = new GetMessagesResponse();
    	response.getNotificationMessage().addAll(messages);
    	return response;
    }
    
    /**
     * 
     * @param destroyRequest
     * @return
     *     returns org.oasis_open.docs.wsn.b_1.DestroyResponse
     * @throws UnableToDestroyPullPoint
     */
    @WebMethod(operationName = "DestroyPullPoint")
    @WebResult(name = "DestroyPullPointResponse", targetNamespace = "http://docs.oasis-open.org/wsn/b-2", partName = "DestroyPullPointResponse")
    public DestroyPullPointResponse destroyPullPoint(
        @WebParam(name = "DestroyPullPoint", targetNamespace = "http://docs.oasis-open.org/wsn/b-2", partName = "DestroyPullPointRequest")
        DestroyPullPoint destroyPullPointRequest)
        throws UnableToDestroyPullPointFault {
    	
    	log.debug("Destroy");
        createPullPoint.destroyPullPoint(getAddress());
    	return new DestroyPullPointResponse();
    }
    
    public void create(CreatePullPoint createPullPointRequest) throws UnableToCreatePullPointFault {
    }
    
	protected abstract void store(NotificationMessageHolderType messageHolder);

    protected abstract List<NotificationMessageHolderType> getMessages(int max) throws ResourceUnknownFault;

    protected void destroy() throws UnableToDestroyPullPointFault {
    	try {
    		unregister();
    	} catch (EndpointRegistrationException e) {
    		UnableToDestroyPullPointFaultType fault = new UnableToDestroyPullPointFaultType();
    		throw new UnableToDestroyPullPointFault("Error unregistering endpoint", fault, e);
    	}
    }

	protected String createAddress() {
		return "http://servicemix.org/wsnotification/PullPoint/" + getName();
	}

    public AbstractCreatePullPoint getCreatePullPoint() {
        return createPullPoint;
    }

    public void setCreatePullPoint(AbstractCreatePullPoint createPullPoint) {
        this.createPullPoint = createPullPoint;
    }
}
