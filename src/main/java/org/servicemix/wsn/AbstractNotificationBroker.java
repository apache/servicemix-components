package org.servicemix.wsn;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import org.activemq.util.IdGenerator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.docs.wsn.b_1.CreatePullPoint;
import org.oasis_open.docs.wsn.b_1.CreatePullPointResponse;
import org.oasis_open.docs.wsn.b_1.GetCurrentMessage;
import org.oasis_open.docs.wsn.b_1.GetCurrentMessageResponse;
import org.oasis_open.docs.wsn.b_1.NoCurrentMessageOnTopicFaultType;
import org.oasis_open.docs.wsn.b_1.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_1.Notify;
import org.oasis_open.docs.wsn.b_1.Subscribe;
import org.oasis_open.docs.wsn.b_1.SubscribeCreationFailedFaultType;
import org.oasis_open.docs.wsn.b_1.SubscribeResponse;
import org.oasis_open.docs.wsn.b_1.UnableToCreatePullPointType;
import org.oasis_open.docs.wsn.br_1.PublisherRegistrationFailedFaultType;
import org.oasis_open.docs.wsn.br_1.RegisterPublisher;
import org.oasis_open.docs.wsn.br_1.RegisterPublisherResponse;
import org.servicemix.wsn.jaxws.InvalidFilterFault;
import org.servicemix.wsn.jaxws.InvalidMessageContentExpressionFault;
import org.servicemix.wsn.jaxws.InvalidProducerPropertiesExpressionFault;
import org.servicemix.wsn.jaxws.InvalidTopicExpressionFault;
import org.servicemix.wsn.jaxws.InvalidUseRawValueFault;
import org.servicemix.wsn.jaxws.MultipleTopicsSpecifiedFault;
import org.servicemix.wsn.jaxws.NoCurrentMessageOnTopicFault;
import org.servicemix.wsn.jaxws.NotificationBroker;
import org.servicemix.wsn.jaxws.NotificationConsumer;
import org.servicemix.wsn.jaxws.PublisherRegistrationFailedFault;
import org.servicemix.wsn.jaxws.PublisherRegistrationRejectedFault;
import org.servicemix.wsn.jaxws.PullNotificationNotSupportedFault;
import org.servicemix.wsn.jaxws.ResourceNotDestroyedFault;
import org.servicemix.wsn.jaxws.ResourceUnknownFault;
import org.servicemix.wsn.jaxws.SubscribeCreationFailedFault;
import org.servicemix.wsn.jaxws.TopicExpressionDialectUnknownFault;
import org.servicemix.wsn.jaxws.TopicNotSupportedFault;
import org.servicemix.wsn.jaxws.UnableToCreatePullPoint;
import org.servicemix.wsn.jaxws.UnableToDestroyPullPoint;
import org.servicemix.wsn.jaxws.UnableToDestroySubscriptionFault;
import org.servicemix.wsn.jaxws.UnacceptableInitialTerminationTimeFault;
import org.w3._2005._03.addressing.AttributedURIType;
import org.w3._2005._03.addressing.EndpointReferenceType;

@WebService(endpointInterface = "org.servicemix.wsn.jaxws.NotificationBroker")
public abstract class AbstractNotificationBroker extends AbstractEndpoint implements NotificationBroker, NotificationConsumer {

	private static Log log = LogFactory.getLog(AbstractPullPoint.class);
	
    private IdGenerator idGenerator;
    private AbstractPublisher anonymousPublisher;
    private Map<String,AbstractPublisher> publishers;
    private Map<String,AbstractPullPoint> pullPoints;
    private Map<String,AbstractSubscription> subscriptions;

	public AbstractNotificationBroker(String name) {
		super(name);
        idGenerator = new IdGenerator();
        subscriptions = new ConcurrentHashMap<String,AbstractSubscription>();
        publishers = new ConcurrentHashMap<String, AbstractPublisher>();
        pullPoints = new ConcurrentHashMap<String, AbstractPullPoint>();
	}

    public void init() throws Exception {
        register();
        anonymousPublisher = createPublisher("Anonymous");
        anonymousPublisher.register();
    }
    
	protected String createAddress() {
		return "http://servicemix.org/wsnotification/NotificationBroker/" + getName();
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
    	handleNotify(notify);
    }
    
	protected void handleNotify(Notify notify) {
		for (NotificationMessageHolderType messageHolder : notify.getNotificationMessage()) {
            EndpointReferenceType producerReference = messageHolder.getProducerReference();
            AbstractPublisher publisher = getPublisher(producerReference);
            if (publisher != null) {
            	publisher.notify(messageHolder);
            }
		}
	}

	protected AbstractPublisher getPublisher(EndpointReferenceType producerReference) {
		AbstractPublisher publisher = null;
		if (producerReference != null && 
			producerReference.getAddress() != null &&
			producerReference.getAddress().getValue() != null) {
			String address = producerReference.getAddress().getValue();
			publishers.get(address);
		}
		if (publisher == null) {
			publisher = anonymousPublisher;
		}
		return publisher;
	}
	
    /**
     * 
     * @param subscribeRequest
     * @return
     *     returns org.oasis_open.docs.wsn.b_1.SubscribeResponse
     * @throws SubscribeCreationFailedFault
     * @throws InvalidTopicExpressionFault
     * @throws TopicNotSupportedFault
     * @throws InvalidFilterFault
     * @throws InvalidProducerPropertiesExpressionFault
     * @throws ResourceUnknownFault
     * @throws InvalidUseRawValueFault
     * @throws InvalidMessageContentExpressionFault
     * @throws TopicExpressionDialectUnknownFault
     * @throws UnacceptableInitialTerminationTimeFault
     */
    @WebMethod(operationName = "Subscribe")
    @WebResult(name = "SubscribeResponse", targetNamespace = "http://docs.oasis-open.org/wsn/b-1", partName = "SubscribeResponse")
    public SubscribeResponse subscribe(
        @WebParam(name = "Subscribe", targetNamespace = "http://docs.oasis-open.org/wsn/b-1", partName = "SubscribeRequest")
        Subscribe subscribeRequest)
        throws InvalidFilterFault, InvalidMessageContentExpressionFault, InvalidProducerPropertiesExpressionFault, InvalidTopicExpressionFault, InvalidUseRawValueFault, ResourceUnknownFault, SubscribeCreationFailedFault, TopicExpressionDialectUnknownFault, TopicNotSupportedFault, UnacceptableInitialTerminationTimeFault {
    	
    	log.debug("Subscribe");
    	return handleSubscribe(subscribeRequest);
    }

	protected SubscribeResponse handleSubscribe(Subscribe subscribeRequest) throws InvalidFilterFault, InvalidMessageContentExpressionFault, InvalidProducerPropertiesExpressionFault, InvalidTopicExpressionFault, InvalidUseRawValueFault, SubscribeCreationFailedFault, TopicExpressionDialectUnknownFault, TopicNotSupportedFault, UnacceptableInitialTerminationTimeFault {
		AbstractSubscription subscription = null;
		boolean success = false;
		try {
			subscription = createSubcription(idGenerator.generateSanitizedId());
			subscriptions.put(subscription.getAddress(), subscription);
			subscription.create(subscribeRequest);
			subscription.register();
			SubscribeResponse response = new SubscribeResponse();
			response.setSubscriptionReference(createEndpointReference(subscription.getAddress()));
			success = true;
			return response;
		} catch (EndpointRegistrationException e) {
			SubscribeCreationFailedFaultType fault = new SubscribeCreationFailedFaultType();
			throw new SubscribeCreationFailedFault("Unable to register endpoint", fault, e);
		} finally {
			if (!success && subscription != null) {
				subscriptions.remove(subscription);
				try {
					subscription.unsubscribe();
				} catch (UnableToDestroySubscriptionFault e) {
					log.info("Error destroying subscription", e);
				}
			}
		}
	}
	
	/**
     * 
     * @param getCurrentMessageRequest
     * @return
     *     returns org.oasis_open.docs.wsn.b_1.GetCurrentMessageResponse
     * @throws MultipleTopicsSpecifiedFault
     * @throws TopicNotSupportedFault
     * @throws InvalidTopicExpressionFault
     * @throws ResourceUnknownFault
     * @throws TopicExpressionDialectUnknownFault
     * @throws NoCurrentMessageOnTopicFault
     */
    @WebMethod(operationName = "GetCurrentMessage")
    @WebResult(name = "GetCurrentMessageResponse", targetNamespace = "http://docs.oasis-open.org/wsn/b-1", partName = "GetCurrentMessageResponse")
    public GetCurrentMessageResponse getCurrentMessage(
        @WebParam(name = "GetCurrentMessage", targetNamespace = "http://docs.oasis-open.org/wsn/b-1", partName = "GetCurrentMessageRequest")
        GetCurrentMessage getCurrentMessageRequest)
        throws InvalidTopicExpressionFault, MultipleTopicsSpecifiedFault, NoCurrentMessageOnTopicFault, ResourceUnknownFault, TopicExpressionDialectUnknownFault, TopicNotSupportedFault {
    	
    	log.debug("GetCurrentMessage");
    	NoCurrentMessageOnTopicFaultType fault = new NoCurrentMessageOnTopicFaultType();
    	throw new NoCurrentMessageOnTopicFault("There is no current message on this topic.", fault);
    }

    /**
     * 
     * @param registerPublisherRequest
     * @return
     *     returns org.oasis_open.docs.wsn.br_1.RegisterPublisherResponse
     * @throws PublisherRegistrationRejectedFault
     * @throws InvalidTopicExpressionFault
     * @throws TopicNotSupportedFault
     * @throws ResourceUnknownFault
     * @throws PublisherRegistrationFailedFault
     */
    @WebMethod(operationName = "RegisterPublisher")
    @WebResult(name = "RegisterPublisherResponse", targetNamespace = "http://docs.oasis-open.org/wsn/br-1", partName = "RegisterPublisherResponse")
    public RegisterPublisherResponse registerPublisher(
        @WebParam(name = "RegisterPublisher", targetNamespace = "http://docs.oasis-open.org/wsn/br-1", partName = "RegisterPublisherRequest")
        RegisterPublisher registerPublisherRequest)
        throws InvalidTopicExpressionFault, PublisherRegistrationFailedFault, PublisherRegistrationRejectedFault, ResourceUnknownFault, TopicNotSupportedFault {
    	
    	log.debug("RegisterPublisher");
    	AbstractPublisher publisher = null;
    	boolean success = false;
    	try {
    		publisher = createPublisher(idGenerator.generateSanitizedId());
    		publishers.put(publisher.getAddress(), publisher);
    		publisher.register();
    		publisher.create(registerPublisherRequest);
    		RegisterPublisherResponse response = new RegisterPublisherResponse(); 
    		response.setPublisherRegistrationReference(createEndpointReference(publisher.getAddress()));
    		success = true;
    		return response;
    	} catch (EndpointRegistrationException e) {
    		PublisherRegistrationFailedFaultType fault = new PublisherRegistrationFailedFaultType();
    		throw new PublisherRegistrationFailedFault("Unable to register new endpoint", fault, e);
    	} finally {
			if (!success && publisher != null) {
				publishers.remove(publisher.getAddress());
				try {
					publisher.destroy();
				} catch (ResourceNotDestroyedFault e) {
					log.info("Error destroying publisher", e);
				}
			}
    	}
    }

    /**
     * 
     * @param createPullPointRequest
     * @return
     *     returns org.oasis_open.docs.wsn.b_1.CreatePullPointResponse
     * @throws UnableToCreatePullPoint
     * @throws PullNotificationNotSupportedFault
     */
    @WebMethod(operationName = "CreatePullPoint")
    @WebResult(name = "CreatePullPointResponse", targetNamespace = "http://docs.oasis-open.org/wsn/b-1", partName = "CreatePullPointResponse")
    public CreatePullPointResponse createPullPoint(
        @WebParam(name = "CreatePullPoint", targetNamespace = "http://docs.oasis-open.org/wsn/b-1", partName = "CreatePullPointRequest")
        CreatePullPoint createPullPointRequest)
        throws PullNotificationNotSupportedFault, UnableToCreatePullPoint {
    	
    	log.debug("CreatePullEndpoint");
    	AbstractPullPoint pullPoint = null;
    	boolean success = false;
    	try {
    		pullPoint = createPullPoint(idGenerator.generateSanitizedId());
    		pullPoints.put(pullPoint.getAddress(), pullPoint);
    		pullPoint.create(createPullPointRequest);
    		pullPoint.register();
    		CreatePullPointResponse response = new CreatePullPointResponse(); 
    		response.setPullPoint(createEndpointReference(pullPoint.getAddress()));
    		success = true;
    		return response;
    	} catch (EndpointRegistrationException e) {
    		UnableToCreatePullPointType fault = new UnableToCreatePullPointType();
    		throw new UnableToCreatePullPoint("Unable to register new endpoint", fault, e);
    	} finally {
			if (!success && pullPoint != null) {
				pullPoints.remove(pullPoint.getAddress());
				try {
					pullPoint.destroy();
				} catch (UnableToDestroyPullPoint e) {
					log.info("Error destroying pullPoint", e);
				}
			}
    	}
    }

	protected EndpointReferenceType createEndpointReference(String address) {
		EndpointReferenceType epr = new EndpointReferenceType();
		AttributedURIType addressUri = new AttributedURIType();
		addressUri.setValue(address);
		epr.setAddress(addressUri);
		return epr;
	}

	protected abstract AbstractPublisher createPublisher(String name);
	
	protected abstract AbstractPullPoint createPullPoint(String name);
	
	protected abstract AbstractSubscription createSubcription(String name);

}
