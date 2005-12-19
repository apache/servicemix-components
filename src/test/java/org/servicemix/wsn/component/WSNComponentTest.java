package org.servicemix.wsn.component;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.activemq.ActiveMQConnectionFactory;
import org.activemq.broker.BrokerService;
import org.oasis_open.docs.wsn.b_1.CreatePullPoint;
import org.oasis_open.docs.wsn.b_1.CreatePullPointResponse;
import org.oasis_open.docs.wsn.b_1.FilterType;
import org.oasis_open.docs.wsn.b_1.GetMessages;
import org.oasis_open.docs.wsn.b_1.GetMessagesResponse;
import org.oasis_open.docs.wsn.b_1.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_1.Notify;
import org.oasis_open.docs.wsn.b_1.Subscribe;
import org.oasis_open.docs.wsn.b_1.SubscribeResponse;
import org.oasis_open.docs.wsn.b_1.TopicExpressionType;
import org.servicemix.client.DefaultServiceMixClient;
import org.servicemix.jbi.container.ActivationSpec;
import org.servicemix.jbi.container.JBIContainer;
import org.servicemix.jbi.resolver.EndpointResolver;
import org.servicemix.jbi.resolver.ServiceAndEndpointNameResolver;
import org.servicemix.jbi.resolver.ServiceNameEndpointResolver;
import org.servicemix.tck.ReceiverComponent;
import org.servicemix.wsn.JAXBMarshaller;
import org.w3._2005._03.addressing.AttributedURIType;
import org.w3._2005._03.addressing.EndpointReferenceType;

public class WSNComponentTest extends TestCase {
	
	public static QName NOTIFICATION_BROKER = QName.valueOf("{http://servicemix.org/wsnotification}NotificationBroker"); 
	
	private JBIContainer jbi;
	private BrokerService broker;
	private JAXBContext context;
	
	protected void setUp() throws Exception {
		jbi = new JBIContainer();
		jbi.setEmbedded(true);
		jbi.init();
		
		broker = new BrokerService();
		broker.setPersistent(false);
		broker.addConnector("vm://localhost");
		broker.start();

		context = JAXBContext.newInstance(Subscribe.class, SubscribeResponse.class);
	}
	
	protected void tearDown() throws Exception {
		if (jbi != null) {
			jbi.shutDown();
		}
		if (broker != null) {
			broker.stop();
		}
	}
	
	public void testNB() throws Exception {
		jbi.start();
		
		WSNComponent component = new WSNComponent();
		component.setConnectionFactory(new ActiveMQConnectionFactory("vm://localhost"));
		ActivationSpec as = new ActivationSpec();
		as.setComponentName("broker");
		as.setComponent(component);
		jbi.activateComponent(as);
		
		ReceiverComponent receiver = new ReceiverComponent();
		jbi.activateComponent(receiver, "receiver");
		
		DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
		EndpointResolver resolver = new ServiceNameEndpointResolver(NOTIFICATION_BROKER);
		client.setMarshaler(new JAXBMarshaller(context));
		
		Subscribe subscribeRequest = new Subscribe();
		EndpointReferenceType consumerReference = new EndpointReferenceType();
		consumerReference.setAddress(new AttributedURIType());
		consumerReference.getAddress().setValue(ReceiverComponent.SERVICE.getNamespaceURI() + "/" + ReceiverComponent.SERVICE.getLocalPart() + "/" + ReceiverComponent.ENDPOINT);
		subscribeRequest.setConsumerReference(consumerReference);
		subscribeRequest.setFilter(new FilterType());
		TopicExpressionType topic = new TopicExpressionType();
		topic.getContent().add("myTopic");
		subscribeRequest.getFilter().getAny().add(new JAXBElement<TopicExpressionType>(new QName("http://docs.oasis-open.org/wsn/b-1", "TopicExpression"), TopicExpressionType.class, topic));
		SubscribeResponse subscribeResponse = (SubscribeResponse) client.request(resolver, null, null, subscribeRequest);
		
		Thread.sleep(500);
		
		Notify notify = new Notify();
		NotificationMessageHolderType holder = new NotificationMessageHolderType();
		holder.setTopic(topic);
		holder.setMessage(new NotificationMessageHolderType.Message());
		holder.getMessage().setAny(new Notify());
		notify.getNotificationMessage().add(holder);
		client.send(resolver, null, null, notify);
		
		receiver.getMessageList().assertMessagesReceived(1);
		
		// Wait for acks to be processed
		Thread.sleep(50);
	}

	public void testPull() throws Exception {
		jbi.start();
		
		WSNComponent component = new WSNComponent();
		component.setConnectionFactory(new ActiveMQConnectionFactory("vm://localhost"));
		ActivationSpec as = new ActivationSpec();
		as.setComponentName("broker");
		as.setComponent(component);
		jbi.activateComponent(as);
		
		DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
		EndpointResolver resolver = new ServiceNameEndpointResolver(NOTIFICATION_BROKER);
		client.setMarshaler(new JAXBMarshaller(context));
		
		CreatePullPoint createPullPoint = new CreatePullPoint();
		CreatePullPointResponse createPullPointResponse = (CreatePullPointResponse) client.request(resolver, null, null, createPullPoint);
		
		Subscribe subscribeRequest = new Subscribe();
		subscribeRequest.setConsumerReference(createPullPointResponse.getPullPoint());
		subscribeRequest.setFilter(new FilterType());
		TopicExpressionType topic = new TopicExpressionType();
		topic.getContent().add("myTopic");
		subscribeRequest.getFilter().getAny().add(new JAXBElement<TopicExpressionType>(new QName("http://docs.oasis-open.org/wsn/b-1", "TopicExpression"), TopicExpressionType.class, topic));
		SubscribeResponse subscribeResponse = (SubscribeResponse) client.request(resolver, null, null, subscribeRequest);
		
		// Wait for notification
		Thread.sleep(500);
		
		Notify notify = new Notify();
		NotificationMessageHolderType holder = new NotificationMessageHolderType();
		holder.setTopic(topic);
		holder.setMessage(new NotificationMessageHolderType.Message());
		holder.getMessage().setAny(new Notify());
		notify.getNotificationMessage().add(holder);
		client.send(resolver, null, null, notify);
		
		// Wait for notification
		Thread.sleep(500);
		
		String[] parts = split(createPullPointResponse.getPullPoint().getAddress().getValue());
		resolver = new ServiceAndEndpointNameResolver(new QName(parts[0], parts[1]), parts[2]);
		
		GetMessages getMessages = new GetMessages();
		GetMessagesResponse getMessagesResponse = (GetMessagesResponse) client.request(resolver, null, null, getMessages);

		assertNotNull(getMessagesResponse);
		assertNotNull(getMessagesResponse.getNotificationMessage());
		assertEquals(1, getMessagesResponse.getNotificationMessage().size());
		
		// Wait for acks to be processed
		Thread.sleep(50);
	}

    protected String[] split(String uri) {
		char sep;
		if (uri.indexOf('/') > 0) {
			sep = '/';
		} else {
			sep = ':';
		}
		int idx1 = uri.lastIndexOf(sep);
		int idx2 = uri.lastIndexOf(sep, idx1 - 1);
		String epName = uri.substring(idx1 + 1);
		String svcName = uri.substring(idx2 + 1, idx1);
		String nsUri   = uri.substring(0, idx2);
    	return new String[] { nsUri, svcName, epName };
    }
}
