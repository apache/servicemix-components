package org.servicemix.wsn.client;

import java.util.List;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.oasis_open.docs.wsn.b_1.CreatePullPoint;
import org.oasis_open.docs.wsn.b_1.CreatePullPointResponse;
import org.oasis_open.docs.wsn.b_1.FilterType;
import org.oasis_open.docs.wsn.b_1.GetCurrentMessage;
import org.oasis_open.docs.wsn.b_1.GetCurrentMessageResponse;
import org.oasis_open.docs.wsn.b_1.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_1.Notify;
import org.oasis_open.docs.wsn.b_1.QueryExpressionType;
import org.oasis_open.docs.wsn.b_1.Subscribe;
import org.oasis_open.docs.wsn.b_1.SubscribeResponse;
import org.oasis_open.docs.wsn.b_1.TopicExpressionType;
import org.oasis_open.docs.wsn.br_1.RegisterPublisher;
import org.oasis_open.docs.wsn.br_1.RegisterPublisherResponse;
import org.servicemix.client.DefaultServiceMixClient;
import org.servicemix.client.ServiceMixClient;
import org.servicemix.client.ServiceMixClientFacade;
import org.servicemix.jbi.container.JBIContainer;
import org.servicemix.jbi.resolver.ServiceNameEndpointResolver;
import org.servicemix.wsn.AbstractSubscription;
import org.w3._2005._03.addressing.EndpointReferenceType;

public class NotificationBroker extends AbstractWSAClient {

	public static String WSN_URI = "http://servicemix.org/wsnotification";
	public static String WSN_SERVICE = "NotificationBroker";
	
	public static QName NOTIFICATION_BROKER = new QName(WSN_URI, WSN_SERVICE); 
	
	public NotificationBroker(ComponentContext context) throws JAXBException {
		ServiceMixClientFacade client = new ServiceMixClientFacade(context); 
		client.setMarshaler(new JAXBMarshaller(JAXBContext.newInstance(Subscribe.class, RegisterPublisher.class)));
		setClient(client);
		setResolver(new ServiceNameEndpointResolver(NOTIFICATION_BROKER));
	}
	
	public NotificationBroker(ComponentContext context, String brokerName) throws JAXBException {
		ServiceMixClientFacade client = new ServiceMixClientFacade(context); 
		client.setMarshaler(new JAXBMarshaller(JAXBContext.newInstance(Subscribe.class, RegisterPublisher.class)));
		setClient(client);
		setEndpoint(createWSA(WSN_URI + "/" + WSN_SERVICE + "/" + brokerName));
		setResolver(resolveWSA(getEndpoint()));
	}
	
	public NotificationBroker(JBIContainer container) throws JBIException, JAXBException {
		DefaultServiceMixClient client = new DefaultServiceMixClient(container);
		client.setMarshaler(new JAXBMarshaller(JAXBContext.newInstance(Subscribe.class, RegisterPublisher.class)));
		setClient(client);
		setResolver(new ServiceNameEndpointResolver(NOTIFICATION_BROKER));
	}
	
	public NotificationBroker(JBIContainer container, String brokerName) throws JBIException, JAXBException {
		DefaultServiceMixClient client = new DefaultServiceMixClient(container);
		client.setMarshaler(new JAXBMarshaller(JAXBContext.newInstance(Subscribe.class)));
		setClient(client);
		setEndpoint(createWSA(WSN_URI + "/" + WSN_SERVICE + "/" + brokerName));
		setResolver(resolveWSA(getEndpoint()));
	}
	
	public NotificationBroker(ServiceMixClient client) {
		setClient(client);
		setResolver(new ServiceNameEndpointResolver(NOTIFICATION_BROKER));
	}

	public NotificationBroker(ServiceMixClient client, String brokerName) {
		setClient(client);
		setEndpoint(createWSA(WSN_URI + "/" + WSN_SERVICE + "/" + brokerName));
		setResolver(resolveWSA(getEndpoint()));
	}

	public void notify(String topic, Object msg) throws JBIException {
		Notify notify = new Notify();
		NotificationMessageHolderType holder = new NotificationMessageHolderType();
		if (topic != null) {
			TopicExpressionType topicExp = new TopicExpressionType();
			topicExp.getContent().add(topic);
			holder.setTopic(topicExp);
		}
		holder.setMessage(new NotificationMessageHolderType.Message());
		holder.getMessage().setAny(msg);
		notify.getNotificationMessage().add(holder);
		send(notify);
	}

	public Subscription subscribe(EndpointReferenceType consumer, 
			  					  String topic,
			  					  String xpath) throws JBIException {
		
		Subscribe subscribeRequest = new Subscribe();
		subscribeRequest.setConsumerReference(consumer);
		subscribeRequest.setFilter(new FilterType());
		if (topic != null) {
			TopicExpressionType topicExp = new TopicExpressionType();
			topicExp.getContent().add(topic);
			subscribeRequest.getFilter().getAny().add(new JAXBElement<TopicExpressionType>(AbstractSubscription.QNAME_TOPIC_EXPRESSION, TopicExpressionType.class, topicExp));
		}
		if (xpath != null) {
			QueryExpressionType xpathExp = new QueryExpressionType();
			xpathExp.setDialect(AbstractSubscription.XPATH1_URI);
			xpathExp.getContent().add(xpath);
			subscribeRequest.getFilter().getAny().add(new JAXBElement<QueryExpressionType>(AbstractSubscription.QNAME_MESSAGE_CONTENT, QueryExpressionType.class, xpathExp));
		}
		SubscribeResponse response = (SubscribeResponse) request(subscribeRequest);
		return new Subscription(response.getSubscriptionReference(), getClient());
	}

	public List<Object> getCurrentMessage(String topic) throws JBIException {
		GetCurrentMessage getCurrentMessageRequest = new GetCurrentMessage();
		if (topic != null) {
			TopicExpressionType topicExp = new TopicExpressionType();
			topicExp.getContent().add(topic);
			getCurrentMessageRequest.setTopic(topicExp);
		}
		GetCurrentMessageResponse response = (GetCurrentMessageResponse) request(getCurrentMessageRequest);
		return response.getAny();
	}

	public Publisher registerPublisher(EndpointReferenceType publisherReference,
									   String topic,
									   boolean demand) throws JBIException {
		
		RegisterPublisher registerPublisherRequest = new RegisterPublisher();
		registerPublisherRequest.setPublisherReference(publisherReference);
		if (topic != null) {
			TopicExpressionType topicExp = new TopicExpressionType();
			topicExp.getContent().add(topic);
			registerPublisherRequest.getTopic().add(topicExp);
		}
		registerPublisherRequest.setDemand(Boolean.valueOf(demand));
		RegisterPublisherResponse response = (RegisterPublisherResponse) request(registerPublisherRequest);
		return new Publisher(response.getPublisherRegistrationReference(), getClient());
	}

	public PullPoint createPullPoint() throws JBIException {
		CreatePullPointResponse response = (CreatePullPointResponse) request(new CreatePullPoint());
		return new PullPoint(response.getPullPoint(), getClient());
	}

}
