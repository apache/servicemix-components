package org.servicemix.wsn;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.XMLGregorianCalendar;

import junit.framework.TestCase;

import org.oasis_open.docs.wsn.b_1.Subscribe;
import org.servicemix.wsn.jaxws.InvalidFilterFault;
import org.servicemix.wsn.jaxws.InvalidMessageContentExpressionFault;
import org.servicemix.wsn.jaxws.InvalidProducerPropertiesExpressionFault;
import org.servicemix.wsn.jaxws.InvalidTopicExpressionFault;
import org.servicemix.wsn.jaxws.InvalidUseRawValueFault;
import org.servicemix.wsn.jaxws.PauseFailedFault;
import org.servicemix.wsn.jaxws.ResumeFailedFault;
import org.servicemix.wsn.jaxws.SubscribeCreationFailedFault;
import org.servicemix.wsn.jaxws.TopicExpressionDialectUnknownFault;
import org.servicemix.wsn.jaxws.TopicNotSupportedFault;
import org.servicemix.wsn.jaxws.UnacceptableInitialTerminationTimeFault;
import org.servicemix.wsn.jaxws.UnacceptableTerminationTimeFault;

public class SubscriptionTest extends TestCase {

	private JAXBContext context;
	private Unmarshaller unmarshaller;
	private AbstractSubscription subscription;
	
	protected void setUp() throws Exception {
		context = JAXBContext.newInstance(Subscribe.class);
		unmarshaller = context.createUnmarshaller();
		subscription = new DummySubscription("mySubscription");
	}
	
	protected Subscribe getSubscription(String file) throws JAXBException, IOException {
		InputStream is = getClass().getResourceAsStream(file);
		Subscribe subscribe = (Subscribe) unmarshaller.unmarshal(is);
		is.close();
		return subscribe;
	}
	
	public void testWithNilITT() throws Exception {
		Subscribe subscribe = getSubscription("subscribe-nil-itt.xml");
		subscription.validateSubscription(subscribe);
	}
	
	public void testWithAbsoluteITT() throws Exception {
		Subscribe subscribe = getSubscription("subscribe-abs-itt.xml");
		try {
			subscription.validateSubscription(subscribe);
			fail("Invalid initial termination time used. Fault was expected.");
		} catch (UnacceptableInitialTerminationTimeFault e) {
			// OK
		}
	}
	
	public void testWithEmptyITT() throws Exception {
		Subscribe subscribe = getSubscription("subscribe-empty-itt.xml");
		try {
			subscription.validateSubscription(subscribe);
			fail("Invalid initial termination time used. Fault was expected.");
		} catch (UnacceptableInitialTerminationTimeFault e) {
			// OK
		}
	}
	
	public void testWithNoITT() throws Exception {
		Subscribe subscribe = getSubscription("subscribe-no-itt.xml");
		subscription.validateSubscription(subscribe);
	}
	
	public void testWithUseRaw() throws Exception {
		Subscribe subscribe = getSubscription("subscribe-raw.xml");
		try {
			subscription.validateSubscription(subscribe);
			fail("UseRaw used. Fault was expected.");
		} catch (InvalidUseRawValueFault e) {
			// OK
		}
	}
	
	public void testWithProducerProperties() throws Exception {
		Subscribe subscribe = getSubscription("subscribe-pp.xml");
		try {
			subscription.validateSubscription(subscribe);
			fail("ProducerProperties used. Fault was expected.");
		} catch (InvalidProducerPropertiesExpressionFault e) {
			// OK
		}
	}
	
	public void testWithNoTopic() throws Exception {
		Subscribe subscribe = getSubscription("subscribe-no-topic.xml");
		try {
			subscription.validateSubscription(subscribe);
			fail("ProducerProperties used. Fault was expected.");
		} catch (InvalidFilterFault e) {
			// OK
		}
	}
	
	public void testWithEPR() throws Exception {
		Subscribe subscribe = getSubscription("subscribe-epr.xml");
		subscription.validateSubscription(subscribe);
	}
	
	public static class DummySubscription extends AbstractSubscription {

		public DummySubscription(String name) {
			super(name);
		}

		@Override
		public void subscribe(Subscribe subscribeRequest) throws InvalidFilterFault, InvalidMessageContentExpressionFault, InvalidProducerPropertiesExpressionFault, InvalidTopicExpressionFault, InvalidUseRawValueFault, SubscribeCreationFailedFault, TopicExpressionDialectUnknownFault, TopicNotSupportedFault, UnacceptableInitialTerminationTimeFault {
		}

		@Override
		protected void pause() throws PauseFailedFault {
		}

		@Override
		protected void resume() throws ResumeFailedFault {
		}

		@Override
		protected void renew(XMLGregorianCalendar terminationTime) throws UnacceptableTerminationTimeFault {
		}
		
	}
	
}
