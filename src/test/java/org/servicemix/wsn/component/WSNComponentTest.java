package org.servicemix.wsn.component;

import java.io.StringReader;
import java.util.List;

import javax.jbi.JBIException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;

import junit.framework.TestCase;

import org.activemq.ActiveMQConnectionFactory;
import org.activemq.broker.BrokerService;
import org.oasis_open.docs.wsn.b_1.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_1.Notify;
import org.servicemix.jbi.container.ActivationSpec;
import org.servicemix.jbi.container.JBIContainer;
import org.servicemix.jbi.jaxp.SourceTransformer;
import org.servicemix.tck.ReceiverComponent;
import org.servicemix.wsn.client.NotificationBroker;
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
}
