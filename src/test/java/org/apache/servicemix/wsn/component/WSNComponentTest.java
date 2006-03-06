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
package org.apache.servicemix.wsn.component;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerService;
import org.apache.servicemix.MessageExchangeListener;
import org.apache.servicemix.components.util.ComponentSupport;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.Receiver;
import org.apache.servicemix.tck.ReceiverComponent;
import org.apache.servicemix.wsn.client.AbstractWSAClient;
import org.apache.servicemix.wsn.client.CreatePullPoint;
import org.apache.servicemix.wsn.client.NotificationBroker;
import org.apache.servicemix.wsn.client.Publisher;
import org.apache.servicemix.wsn.client.PullPoint;
import org.apache.servicemix.wsn.client.Subscription;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.b_2.SubscribeResponse;
import org.oasis_open.docs.wsn.b_2.Unsubscribe;
import org.oasis_open.docs.wsn.b_2.UnsubscribeResponse;
import org.w3._2005._08.addressing.AttributedURIType;
import org.w3._2005._08.addressing.EndpointReferenceType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class WSNComponentTest extends TestCase {
	
	public static QName NOTIFICATION_BROKER = new QName("http://servicemix.org/wsnotification", "NotificationBroker"); 
	
	private JBIContainer jbi;
	private BrokerService jmsBroker;
	private NotificationBroker wsnBroker;
    private CreatePullPoint wsnCreatePullPoint;
    private WSNComponent wsnComponent;
	
	protected void setUp() throws Exception {
		jmsBroker = new BrokerService();
		jmsBroker.setPersistent(false);
		jmsBroker.addConnector("vm://localhost");
		jmsBroker.start();

		jbi = new JBIContainer();
		jbi.setEmbedded(true);
		jbi.init();
		jbi.start();
		
		wsnComponent = new WSNComponent();
        wsnComponent.setConnectionFactory(new ActiveMQConnectionFactory("vm://localhost"));
		ActivationSpec as = new ActivationSpec();
		as.setComponentName("servicemix-wsn2005");
		as.setComponent(wsnComponent);
		jbi.activateComponent(as);
		
		wsnBroker = new NotificationBroker(jbi);
        wsnCreatePullPoint = new CreatePullPoint(jbi);
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
		NormalizedMessage msg = (NormalizedMessage) receiver.getMessageList().getMessages().get(0);
		Node node = new SourceTransformer().toDOMNode(msg);
		assertEquals("Notify", node.getLocalName());
		
		// Wait for acks to be processed
		Thread.sleep(50);
	}

	public void testRawNotify() throws Exception {
		ReceiverComponent receiver = new ReceiverComponent();
		jbi.activateComponent(receiver, "receiver");


		// START SNIPPET: notify
		EndpointReferenceType consumer = createEPR(ReceiverComponent.SERVICE, ReceiverComponent.ENDPOINT);
		wsnBroker.subscribe(consumer, "myTopic", null, true);
		
		Element body = parse("<hello>world</hello>");
        wsnBroker.notify("myTopic", body);
        // END SNIPPET: notify
        
		// Wait for notification
		Thread.sleep(50);
		
		receiver.getMessageList().assertMessagesReceived(1);
		NormalizedMessage msg = (NormalizedMessage) receiver.getMessageList().getMessages().get(0);
		Node node = new SourceTransformer().toDOMNode(msg);
		assertEquals("hello", node.getLocalName());
		
		// Wait for acks to be processed
		Thread.sleep(50);
	}

	public void testUnsubscribe() throws Exception {
        // START SNIPPET: sub
		PullPoint pullPoint = wsnCreatePullPoint.createPullPoint();
		Subscription subscription = wsnBroker.subscribe(pullPoint.getEndpoint(), "myTopic", null);
        // END SNIPPET: sub
		
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
		PullPoint pullPoint = wsnCreatePullPoint.createPullPoint();
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
		PullPoint pullPoint = wsnCreatePullPoint.createPullPoint();
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
		PullPoint pullPoint1 = wsnCreatePullPoint.createPullPoint();
		PullPoint pullPoint2 = wsnCreatePullPoint.createPullPoint();
		wsnBroker.subscribe(pullPoint1.getEndpoint(), "myTopic", "@type = 'a'");
		wsnBroker.subscribe(pullPoint2.getEndpoint(), "myTopic", "@type = 'b'");
		
		wsnBroker.notify("myTopic", parse("<msg type='a'/>"));
		// Wait for notification
		Thread.sleep(500);

		assertEquals(1, pullPoint1.getMessages(0).size());
		assertEquals(0, pullPoint2.getMessages(0).size());
		
		wsnBroker.notify("myTopic", parse("<msg type='b'/>"));
		// Wait for notification
		Thread.sleep(500);

		assertEquals(0, pullPoint1.getMessages(0).size());
		assertEquals(1, pullPoint2.getMessages(0).size());

		wsnBroker.notify("myTopic", parse("<msg type='c'/>"));
		// Wait for notification
		Thread.sleep(500);

		assertEquals(0, pullPoint1.getMessages(0).size());
		assertEquals(0, pullPoint2.getMessages(0).size());
	}
	
	public void testDemandBasedPublisher() throws Exception {
		PublisherComponent publisherComponent = new PublisherComponent();
		jbi.activateComponent(publisherComponent, "publisher");
		
		Publisher publisher = wsnBroker.registerPublisher(
									AbstractWSAClient.createWSA(PublisherComponent.SERVICE.getNamespaceURI() + "/" + PublisherComponent.SERVICE.getLocalPart() + "/" + PublisherComponent.ENDPOINT), 
									"myTopic", true);
		
		Thread.sleep(50);
		assertNull(publisherComponent.getSubscription());

		PullPoint pullPoint = wsnCreatePullPoint.createPullPoint();
		Subscription subscription = wsnBroker.subscribe(pullPoint.getEndpoint(), "myTopic", null);

		Thread.sleep(500);
		assertNotNull(publisherComponent.getSubscription());
		
		subscription.unsubscribe();
		
		Thread.sleep(500);
		assertNull(publisherComponent.getSubscription());
		
		publisher.destroy();
		
		Thread.sleep(50);
	}
    
    public void testDeployPullPoint() throws Exception {
        URL url = getClass().getClassLoader().getResource("pullpoint/pullpoint.xml");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        wsnComponent.getServiceUnitManager().deploy("pullpoint", path.getAbsolutePath());

        wsnComponent.getServiceUnitManager().start("pullpoint");
        
        wsnBroker.notify("myTopic", parse("<hello>world</hello>"));
        PullPoint pullPoint = new PullPoint(AbstractWSAClient.createWSA("http://www.consumer.org/service/endpoint"), 
                                            jbi);
        assertEquals(1, pullPoint.getMessages(0).size());
    }
        
    public void testDeploySubscription() throws Exception {
        URL url = getClass().getClassLoader().getResource("subscription/subscribe.xml");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        wsnComponent.getServiceUnitManager().deploy("subscription", path.getAbsolutePath());
        
        ActivationSpec consumer = new ActivationSpec();
        consumer.setService(new QName("http://www.consumer.org", "service"));
        consumer.setEndpoint("endpoint");
        Receiver receiver = new ReceiverComponent();
        consumer.setComponent(receiver);
        jbi.activateComponent(consumer);
        
        wsnComponent.getServiceUnitManager().start("subscription");

        wsnBroker.notify("myTopic", parse("<hello>world</hello>"));
        // Wait for notification
        Thread.sleep(50);
        receiver.getMessageList().assertMessagesReceived(1);
        receiver.getMessageList().flushMessages();
        
        wsnComponent.getServiceUnitManager().stop("subscription");

        wsnBroker.notify("myTopic", parse("<hello>world</hello>"));
        // Wait for notification
        Thread.sleep(50);
        assertEquals(0, receiver.getMessageList().flushMessages().size());
        
        wsnComponent.getServiceUnitManager().start("subscription");

        wsnBroker.notify("myTopic", parse("<hello>world</hello>"));
        // Wait for notification
        Thread.sleep(50);
        receiver.getMessageList().assertMessagesReceived(1);
        receiver.getMessageList().flushMessages();
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
