package org.servicemix.wsn;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import org.oasis_open.docs.wsn.b_1.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.br_1.Destroy;
import org.oasis_open.docs.wsn.br_1.DestroyResponse;
import org.oasis_open.docs.wsn.br_1.ResourceNotDestroyedFaultType;
import org.servicemix.wsn.jaxws.PublisherRegistrationManager;
import org.servicemix.wsn.jaxws.ResourceNotDestroyedFault;
import org.servicemix.wsn.jaxws.ResourceUnknownFault;

@WebService(endpointInterface = "org.servicemix.wsn.jaxws.PublisherRegistrationManager")
public abstract class AbstractPublisher extends AbstractEndpoint 
									    implements PublisherRegistrationManager {

	public AbstractPublisher(String name) {
		super(name);
	}
	
    /**
     * 
     * @param destroyRequest
     * @return
     *     returns org.oasis_open.docs.wsn.br_1.DestroyResponse
     * @throws ResourceNotDestroyedFault
     * @throws ResourceUnknownFault
     */
    @WebMethod(operationName = "Destroy")
    @WebResult(name = "DestroyResponse", targetNamespace = "http://docs.oasis-open.org/wsn/br-1", partName = "DestroyResponse")
    public DestroyResponse destroy(
        @WebParam(name = "Destroy", targetNamespace = "http://docs.oasis-open.org/wsn/br-1", partName = "DestroyRequest")
        Destroy destroyRequest)
        throws ResourceNotDestroyedFault, ResourceUnknownFault {
    	
    	destroy();
    	return new DestroyResponse();
    }
    
    public abstract void notify(NotificationMessageHolderType messageHolder);

    protected void destroy() throws ResourceNotDestroyedFault {
    	try {
    		unregister();
    	} catch (EndpointRegistrationException e) {
    		ResourceNotDestroyedFaultType fault = new ResourceNotDestroyedFaultType();
    		throw new ResourceNotDestroyedFault("Error unregistering endpoint", fault, e);
    	}
    }

	protected String createAddress() {
		return "http://servicemix.org/wsnotification/Publisher/" + getName();
	}
}
