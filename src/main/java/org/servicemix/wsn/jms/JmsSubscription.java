package org.servicemix.wsn.jms;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.docs.wsn.b_1.PauseFailedFaultType;
import org.oasis_open.docs.wsn.b_1.ResumeFailedFaultType;
import org.oasis_open.docs.wsn.b_1.Subscribe;
import org.oasis_open.docs.wsn.b_1.SubscribeCreationFailedFaultType;
import org.oasis_open.docs.wsn.b_1.UnableToDestroySubscriptionFaultType;
import org.oasis_open.docs.wsn.b_1.UnacceptableTerminationTimeFaultType;
import org.servicemix.wsn.AbstractSubscription;
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
import org.servicemix.wsn.jaxws.UnableToDestroySubscriptionFault;
import org.servicemix.wsn.jaxws.UnacceptableInitialTerminationTimeFault;
import org.servicemix.wsn.jaxws.UnacceptableTerminationTimeFault;

public abstract class JmsSubscription extends AbstractSubscription implements MessageListener {

	private static Log log = LogFactory.getLog(JmsSubscription.class);
	
	private Connection connection;
	private Session session;
    private MessageConsumer consumer;
    private JmsTopicExpressionConverter topicConverter;
    private Topic jmsTopic;
	
	public JmsSubscription(String name) {
		super(name);
		topicConverter = new JmsTopicExpressionConverter();
	}

	protected void start() throws SubscribeCreationFailedFault {
		try {
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            consumer = session.createConsumer(jmsTopic);
            consumer.setMessageListener(this);
		} catch (JMSException e) {
			SubscribeCreationFailedFaultType fault = new SubscribeCreationFailedFaultType();
			throw new SubscribeCreationFailedFault("Error starting subscription", fault, e);
		}
	}
	
	@Override
	protected void validateSubscription(Subscribe subscribeRequest) throws InvalidFilterFault, InvalidMessageContentExpressionFault, InvalidProducerPropertiesExpressionFault, InvalidTopicExpressionFault, InvalidUseRawValueFault, SubscribeCreationFailedFault, TopicExpressionDialectUnknownFault, TopicNotSupportedFault, UnacceptableInitialTerminationTimeFault {
		super.validateSubscription(subscribeRequest);
		jmsTopic = topicConverter.toActiveMQTopic(topic);
	}
	
	@Override
	public void subscribe(Subscribe subscribeRequest) throws InvalidFilterFault, InvalidMessageContentExpressionFault, InvalidProducerPropertiesExpressionFault, InvalidTopicExpressionFault, InvalidUseRawValueFault, SubscribeCreationFailedFault, TopicExpressionDialectUnknownFault, TopicNotSupportedFault, UnacceptableInitialTerminationTimeFault {
		validateSubscription(subscribeRequest);
		start();
	}

	@Override
	protected void pause() throws PauseFailedFault {
		PauseFailedFaultType fault = new PauseFailedFaultType();
		throw new PauseFailedFault("Pause not supported", fault);
	}

	@Override
	protected void resume() throws ResumeFailedFault {
		ResumeFailedFaultType fault = new ResumeFailedFaultType();
		throw new ResumeFailedFault("Resume not supported", fault);
	}

	@Override
	protected void renew(XMLGregorianCalendar terminationTime) throws UnacceptableTerminationTimeFault {
		UnacceptableTerminationTimeFaultType fault = new UnacceptableTerminationTimeFaultType();
    	throw new UnacceptableTerminationTimeFault(
    			"TerminationTime is not supported",
    			fault);
	}

	@Override
	protected void unsubscribe() throws UnableToDestroySubscriptionFault {
		if (session != null) {
			try {
				session.close();
			} catch (JMSException e) {
				UnableToDestroySubscriptionFaultType fault = new UnableToDestroySubscriptionFaultType();
				throw new UnableToDestroySubscriptionFault("Unable to unsubscribe", fault, e);
			}
		}
	}

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public void onMessage(Message message) {
		try {
			TextMessage text = (TextMessage) message;
	        boolean match = true;
			if (contentFilter != null) {
				match = doFilter(text.getText());
			}
			if (match) {
				doNotify(text.getText());
			}
		} catch (Exception e) {
			log.warn("Error notifying consumer", e);
		}
	}
	
	protected boolean doFilter(String notify) {
		return true;
	}
	
	protected abstract void doNotify(String notify);

}
