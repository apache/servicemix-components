package org.servicemix.wsn.client;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.oasis_open.docs.wsn.b_1.CreatePullPoint;
import org.oasis_open.docs.wsn.b_1.CreatePullPointResponse;
import org.oasis_open.docs.wsn.b_1.FilterType;
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
import org.servicemix.jbi.resolver.EndpointResolver;
import org.servicemix.jbi.resolver.ServiceNameEndpointResolver;
import org.servicemix.wsn.AbstractSubscription;
import org.w3._2005._03.addressing.EndpointReferenceType;

public class NotificationBroker {

	public static QName NOTIFICATION_BROKER = new QName("http://servicemix.org/wsnotification", "NotificationBroker"); 
	
	private ServiceMixClient client;
	private EndpointResolver resolver;

	
	public NotificationBroker(ComponentContext context) {
		this.client = new ServiceMixClientFacade(context);
		resolver = new ServiceNameEndpointResolver(NOTIFICATION_BROKER);
	}
	
	public NotificationBroker(JBIContainer container) throws JBIException, JAXBException {
		DefaultServiceMixClient client = new DefaultServiceMixClient(container);
		client.setMarshaler(new JAXBMarshaller(JAXBContext.newInstance(Subscribe.class)));
		this.client = client;
		resolver = new ServiceNameEndpointResolver(NOTIFICATION_BROKER);
	}
	
	public NotificationBroker(ServiceMixClient client) {
		this.client = client;
		resolver = new ServiceNameEndpointResolver(NOTIFICATION_BROKER);
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
		client.send(resolver, null, null, notify);
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
		SubscribeResponse response = (SubscribeResponse) client.request(resolver, null, null, subscribeRequest);
		return new Subscription(response.getSubscriptionReference(), client);
	}

	public GetCurrentMessageResponse getCurrentMessage(String topic) throws JBIException {
		return null;
	}

	public Publisher registerPublisher(EndpointReferenceType publisherReference,
									   String topic) throws JBIException {
		
		RegisterPublisher registerPublisherRequest = new RegisterPublisher();
		RegisterPublisherResponse response = (RegisterPublisherResponse) client.request(resolver, null, null, registerPublisherRequest);
		return new Publisher(response.getPublisherRegistrationReference(), client);
	}

	public PullPoint createPullPoint() throws JBIException {
		CreatePullPointResponse response = (CreatePullPointResponse) client.request(resolver, null, null, new CreatePullPoint());
		return new PullPoint(response.getPullPoint(), client);
	}

}
