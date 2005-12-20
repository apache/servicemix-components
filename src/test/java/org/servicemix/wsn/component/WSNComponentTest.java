package org.servicemix.wsn.component;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.jbi.JBIException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.Source;

import junit.framework.TestCase;

import org.activemq.ActiveMQConnectionFactory;
import org.activemq.broker.BrokerService;
import org.oasis_open.docs.wsn.b_1.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_1.Notify;
import org.oasis_open.docs.wsn.b_1.Subscribe;
import org.oasis_open.docs.wsn.b_1.SubscribeResponse;
import org.oasis_open.docs.wsn.b_1.Unsubscribe;
import org.oasis_open.docs.wsn.b_1.UnsubscribeResponse;
import org.servicemix.MessageExchangeListener;
import org.servicemix.components.util.ComponentSupport;
import org.servicemix.jbi.container.ActivationSpec;
import org.servicemix.jbi.container.JBIContainer;
import org.servicemix.jbi.jaxp.SourceTransformer;
import org.servicemix.jbi.jaxp.StringSource;
import org.servicemix.tck.ReceiverComponent;
import org.servicemix.wsn.client.AbstractWSAClient;
import org.servicemix.wsn.client.NotificationBroker;
import org.servicemix.wsn.client.Publisher;
import org.servicemix.wsn.client.PullPoint;
import org.servicemix.wsn.client.Subscription;
import org.w3._2005._03.addressing.AttributedURIType;
import org.w3._2005._03.addressing.EndpointReferenceType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

public class WSNComponentTest extends TestCase {
	
	public static QName NOTIFICATION_BROKER = new QName("http://servicemix.org/wsnotification", "NotificationBroker"); 
	
	private JBIContainer jbi;
	private BrokerService jmsBroker;
	private NotificationBroker wsnBroker;
	
	protected void setUp() throws Exception {
		jmsBroker = new BrokerService();
		jmsBroker.setPersistent(false);
		jmsBroker.addConnector("vm://localhost");
		jmsBroker.start();

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
		
		wsnBroker = new NotificationBroker(jbi);
	}
	
	protected void tearDown() throws Exception {
		if (jbi != null) {
			jbi.shutDown();
		}
		if (jmsBroker != null) {
			jmsBroker.stop();
		}
	}
	
	public void testInvalidSubscribription() throws Exception {
		try {
			wsnBroker.subscribe(null, null, null);
			fail("Expected an exception");
		} catch (JBIException e) {
			// ok
		}
	}
	
	public void testNotify() throws Exception {
		ReceiverComponent receiver = new ReceiverComponent();
		jbi.activateComponent(receiver, "receiver");
		
		EndpointReferenceType consumer = createEPR(ReceiverComponent.SERVICE, ReceiverComponent.ENDPOINT);
		wsnBroker.subscribe(consumer, "myTopic", null);
		
		wsnBroker.notify("myTopic", parse("<hello>world</hello>"));
		// Wait for notification
		Thread.sleep(50);
		
		receiver.getMessageList().assertMessagesReceived(1);
		
		// Wait for acks to be processed
		Thread.sleep(50);
	}

	public void testUnsubscribe() throws Exception {
		PullPoint pullPoint = wsnBroker.createPullPoint();
		Subscription subscription = wsnBroker.subscribe(pullPoint.getEndpoint(), "myTopic", null);
		
		wsnBroker.notify("myTopic", new Notify());
		// Wait for notification
		Thread.sleep(50);
		
		assertEquals(1, pullPoint.getMessages(0).size());

		subscription.unsubscribe();
		
		wsnBroker.notify("myTopic", new Notify());
		// Wait for notification
		Thread.sleep(50);
		
		assertEquals(0, pullPoint.getMessages(0).size());

		// Wait for acks to be processed
		Thread.sleep(50);
	}

	public void testPauseResume() throws Exception {
		PullPoint pullPoint = wsnBroker.createPullPoint();
		Subscription subscription = wsnBroker.subscribe(pullPoint.getEndpoint(), "myTopic", null);
		
		wsnBroker.notify("myTopic", new Notify());
		// Wait for notification
		Thread.sleep(50);
		
		assertEquals(1, pullPoint.getMessages(0).size());

		subscription.pause();
		
		wsnBroker.notify("myTopic", new Notify());
		// Wait for notification
		Thread.sleep(50);
		
		assertEquals(0, pullPoint.getMessages(0).size());

		subscription.resume();
		
		wsnBroker.notify("myTopic", new Notify());
		// Wait for notification
		Thread.sleep(50);
		
		assertEquals(1, pullPoint.getMessages(0).size());

		// Wait for acks to be processed
		Thread.sleep(50);
	}

	public void testPull() throws Exception {
		PullPoint pullPoint = wsnBroker.createPullPoint();
		wsnBroker.subscribe(pullPoint.getEndpoint(), "myTopic", null);
		
		wsnBroker.notify("myTopic", new Notify());
		// Wait for notification
		Thread.sleep(50);
		
		List<NotificationMessageHolderType> msgs = pullPoint.getMessages(0);
		assertNotNull(msgs);
		assertEquals(1, msgs.size());

		// Wait for acks to be processed
		Thread.sleep(50);
	}
	
	public void testPullWithFilter() throws Exception {
		PullPoint pullPoint1 = wsnBroker.createPullPoint();
		PullPoint pullPoint2 = wsnBroker.createPullPoint();
		wsnBroker.subscribe(pullPoint1.getEndpoint(), "myTopic", "@type = 'a'");
		wsnBroker.subscribe(pullPoint2.getEndpoint(), "myTopic", "@type = 'b'");
		
		wsnBroker.notify("myTopic", parse("<msg type='a'/>"));
		// Wait for notification
		Thread.sleep(50);

		assertEquals(1, pullPoint1.getMessages(0).size());
		assertEquals(0, pullPoint2.getMessages(0).size());
		
		wsnBroker.notify("myTopic", parse("<msg type='b'/>"));
		// Wait for notification
		Thread.sleep(50);

		assertEquals(0, pullPoint1.getMessages(0).size());
		assertEquals(1, pullPoint2.getMessages(0).size());

		wsnBroker.notify("myTopic", parse("<msg type='c'/>"));
		// Wait for notification
		Thread.sleep(50);

		assertEquals(0, pullPoint1.getMessages(0).size());
		assertEquals(0, pullPoint2.getMessages(0).size());
	}
	
	public void testDemandeBasedPublisher() throws Exception {
		PublisherComponent publisherComponent = new PublisherComponent();
		jbi.activateComponent(publisherComponent, "publisher");
		
		Publisher publisher = wsnBroker.registerPublisher(
									AbstractWSAClient.createWSA(PublisherComponent.SERVICE.getNamespaceURI() + "/" + PublisherComponent.SERVICE.getLocalPart() + "/" + PublisherComponent.ENDPOINT), 
									"myTopic", true);
		
		Thread.sleep(50);
		assertNull(publisherComponent.getSubscription());

		PullPoint pullPoint = wsnBroker.createPullPoint();
		Subscription subscription = wsnBroker.subscribe(pullPoint.getEndpoint(), "myTopic", null);

		Thread.sleep(50);
		assertNotNull(publisherComponent.getSubscription());
		
		subscription.unsubscribe();
		
		Thread.sleep(50);
		assertNull(publisherComponent.getSubscription());
		
		publisher.destroy();
		
		Thread.sleep(50);
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
	
	public static class PublisherComponent extends ComponentSupport implements MessageExchangeListener {
	    public static final QName SERVICE = new QName("http://servicemix.org/example", "publisher");
	    public static final String ENDPOINT = "publisher";
	    private Object subscription;
	    public PublisherComponent() {
	    	super(SERVICE, ENDPOINT);
	    }
		public Object getSubscription() {
			return subscription;
		}
		public void onMessageExchange(MessageExchange exchange) throws MessagingException {
			if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
				try {
					JAXBContext jaxbContext = JAXBContext.newInstance(Subscribe.class);
					Source src = exchange.getMessage("in").getContent();
					Object input = jaxbContext.createUnmarshaller().unmarshal(src);
					if (input instanceof Subscribe) {
						subscription = input;
						SubscribeResponse response = new SubscribeResponse();
						response.setSubscriptionReference(AbstractWSAClient.createWSA(PublisherComponent.SERVICE.getNamespaceURI() + "/" + PublisherComponent.SERVICE.getLocalPart() + "/" + PublisherComponent.ENDPOINT));
						StringWriter writer = new StringWriter();
						jaxbContext.createMarshaller().marshal(response, writer);
						NormalizedMessage out = exchange.createMessage();
						out.setContent(new StringSource(writer.toString()));
						exchange.setMessage(out, "out");
						send(exchange);
					} else if (input instanceof Unsubscribe) {
						subscription = null;
						UnsubscribeResponse response = new UnsubscribeResponse();
						StringWriter writer = new StringWriter();
						jaxbContext.createMarshaller().marshal(response, writer);
						NormalizedMessage out = exchange.createMessage();
						out.setContent(new StringSource(writer.toString()));
						exchange.setMessage(out, "out");
						send(exchange);
					} else {
						throw new Exception("Unkown request");
					}
				} catch (Exception e) {
					exchange.setError(e);
					send(exchange);
				}
			} else if (exchange.getStatus() == ExchangeStatus.ERROR) {
				exchange.setStatus(ExchangeStatus.DONE);
				send(exchange);
			}
		}
	}
}
