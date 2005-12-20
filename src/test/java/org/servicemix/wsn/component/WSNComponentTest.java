package org.servicemix.wsn.component;

import java.io.StringReader;
import java.math.BigInteger;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;

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
import org.oasis_open.docs.wsn.b_1.QueryExpressionType;
import org.oasis_open.docs.wsn.b_1.Subscribe;
import org.oasis_open.docs.wsn.b_1.SubscribeResponse;
import org.oasis_open.docs.wsn.b_1.TopicExpressionType;
import org.servicemix.client.DefaultServiceMixClient;
import org.servicemix.jbi.container.ActivationSpec;
import org.servicemix.jbi.container.JBIContainer;
import org.servicemix.jbi.jaxp.SourceTransformer;
import org.servicemix.jbi.resolver.EndpointResolver;
import org.servicemix.jbi.resolver.ServiceAndEndpointNameResolver;
import org.servicemix.jbi.resolver.ServiceNameEndpointResolver;
import org.servicemix.tck.ReceiverComponent;
import org.servicemix.wsn.AbstractSubscription;
import org.servicemix.wsn.JAXBMarshaller;
import org.w3._2005._03.addressing.AttributedURIType;
import org.w3._2005._03.addressing.EndpointReferenceType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class WSNComponentTest extends TestCase {
	
	public static QName NOTIFICATION_BROKER = new QName("http://servicemix.org/wsnotification", "NotificationBroker"); 
	
	private JBIContainer jbi;
	private BrokerService broker;
	private JAXBContext context;
	private DefaultServiceMixClient client;
	
	protected void setUp() throws Exception {
		broker = new BrokerService();
		broker.setPersistent(false);
		broker.addConnector("vm://localhost");
		broker.start();

		jbi = new JBIContainer();
		jbi.setEmbedded(true);
		jbi.init();
		jbi.start();
		
		WSNComponent component = new WSNComponent();
		component.setConnectionFactory(new ActiveMQConnectionFactory("vm://localhost"));
		ActivationSpec as = new ActivationSpec();
		as.setComponentName("servicemix-wsn2005");
		as.setComponent(component);
		jbi.activateComponent(as);
		
		context = JAXBContext.newInstance(Subscribe.class, SubscribeResponse.class);

		client = new DefaultServiceMixClient(jbi);
		client.setMarshaler(new JAXBMarshaller(context));
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
		ReceiverComponent receiver = new ReceiverComponent();
		jbi.activateComponent(receiver, "receiver");
		
		EndpointReferenceType consumer = createEPR(ReceiverComponent.SERVICE, ReceiverComponent.ENDPOINT);
		EndpointReferenceType subscription = subscribe(consumer, "myTopic", null);

		notify("myTopic", parse("<hello>world</hello>"));
		// Wait for notification
		Thread.sleep(50);
		
		receiver.getMessageList().assertMessagesReceived(1);
		
		// Wait for acks to be processed
		Thread.sleep(50);
	}

	public void testPull() throws Exception {
		EndpointReferenceType pullPoint = createPullPoint();
		subscribe(pullPoint, "myTopic", null);
		
		notify("myTopic", new Notify());
		// Wait for notification
		Thread.sleep(50);
		
		List<NotificationMessageHolderType> msgs = getMessages(pullPoint, 0);
		assertNotNull(msgs);
		assertEquals(1, msgs.size());

		// Wait for acks to be processed
		Thread.sleep(50);
	}
	
	public void testPullWithFilter() throws Exception {
		EndpointReferenceType pullPoint1 = createPullPoint();
		EndpointReferenceType pullPoint2 = createPullPoint();
		EndpointReferenceType subscription1 = subscribe(pullPoint1, "myTopic", "@type = 'a'");
		EndpointReferenceType subscription2 = subscribe(pullPoint2, "myTopic", "@type = 'b'");
		
		notify("myTopic", parse("<msg type='a'/>"));
		// Wait for notification
		Thread.sleep(50);

		assertEquals(1, getMessages(pullPoint1, 0).size());
		assertEquals(0, getMessages(pullPoint2, 0).size());
		
		notify("myTopic", parse("<msg type='b'/>"));
		// Wait for notification
		Thread.sleep(50);

		assertEquals(0, getMessages(pullPoint1, 0).size());
		assertEquals(1, getMessages(pullPoint2, 0).size());
		
		notify("myTopic", parse("<msg type='c'/>"));
		// Wait for notification
		Thread.sleep(50);

		assertEquals(0, getMessages(pullPoint1, 0).size());
		assertEquals(0, getMessages(pullPoint2, 0).size());
	}
	
	protected Element parse(String txt) throws Exception {
		DocumentBuilder builder = new SourceTransformer().createDocumentBuilder();
		InputSource is = new InputSource(new StringReader(txt));
		Document doc = builder.parse(is);
		return doc.getDocumentElement();
	}
	
	protected EndpointReferenceType createEPR(QName service, String endpoint) {
		EndpointReferenceType epr = new EndpointReferenceType();
		epr.setAddress(new AttributedURIType());
		epr.getAddress().setValue(service.getNamespaceURI() + "/" + service.getLocalPart() + "/" + endpoint);
		return epr;
	}
	
	protected EndpointReferenceType createPullPoint() throws Exception {
		EndpointResolver resolver = new ServiceNameEndpointResolver(NOTIFICATION_BROKER);
		CreatePullPointResponse response = (CreatePullPointResponse) client.request(resolver, null, null, new CreatePullPoint());
		return response.getPullPoint();
	}
	
	protected EndpointReferenceType subscribe(EndpointReferenceType consumer, 
											  String topic,
											  String xpath) throws Exception {
		EndpointResolver resolver = new ServiceNameEndpointResolver(NOTIFICATION_BROKER);
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
		return response.getSubscriptionReference();
	}
	
	protected void notify(String topic, Object msg) throws Exception {
		EndpointResolver resolver = new ServiceNameEndpointResolver(NOTIFICATION_BROKER);
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
	
	protected List<NotificationMessageHolderType> getMessages(EndpointReferenceType pullPoint, int max) throws Exception {
		EndpointResolver resolver = resolveWSA(pullPoint);
		GetMessages getMessages = new GetMessages();
		getMessages.setMaximumNumber(BigInteger.valueOf(max));
		GetMessagesResponse response = (GetMessagesResponse) client.request(resolver, null, null, getMessages);
		return response.getNotificationMessage();
	}
	
	protected EndpointResolver resolveWSA(EndpointReferenceType ref) {
		String[] parts = split(ref.getAddress().getValue());
		return new ServiceAndEndpointNameResolver(new QName(parts[0], parts[1]), parts[2]);
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
