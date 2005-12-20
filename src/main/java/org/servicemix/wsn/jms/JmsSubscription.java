package org.servicemix.wsn.jms;

import java.io.IOException;
import java.io.StringReader;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public abstract class JmsSubscription extends AbstractSubscription implements MessageListener {

	private static Log log = LogFactory.getLog(JmsSubscription.class);
	
	private Connection connection;
	private Session session;
    private JmsTopicExpressionConverter topicConverter;
    private Topic jmsTopic;
	
	public JmsSubscription(String name) {
		super(name);
		topicConverter = new JmsTopicExpressionConverter();
	}

	protected void start() throws SubscribeCreationFailedFault {
		try {
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			MessageConsumer consumer = session.createConsumer(jmsTopic);
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
		if (session == null) {
			PauseFailedFaultType fault = new PauseFailedFaultType();
			throw new PauseFailedFault("Subscription is already paused", fault);
		} else {
			try {
				session.close();
			} catch (JMSException e) {
				PauseFailedFaultType fault = new PauseFailedFaultType();
				throw new PauseFailedFault("Error pausing subscription", fault, e);
			} finally {
				session = null;
			}
		}
	}

	@Override
	protected void resume() throws ResumeFailedFault {
		if (session != null) {
			ResumeFailedFaultType fault = new ResumeFailedFaultType();
			throw new ResumeFailedFault("Subscription is already running", fault);
		} else {
			try {
				session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
				MessageConsumer consumer = session.createConsumer(jmsTopic);
	            consumer.setMessageListener(this);
			} catch (JMSException e) {
				ResumeFailedFaultType fault = new ResumeFailedFaultType();
				throw new ResumeFailedFault("Error resuming subscription", fault, e);
			}
		}
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
		super.unsubscribe();
		if (session != null) {
			try {
				session.close();
			} catch (JMSException e) {
				UnableToDestroySubscriptionFaultType fault = new UnableToDestroySubscriptionFaultType();
				throw new UnableToDestroySubscriptionFault("Unable to unsubscribe", fault, e);
			} finally {
				session = null;
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
	        boolean match = doFilter(text.getText());
			if (match) {
				doNotify(text.getText());
			}
		} catch (Exception e) {
			log.warn("Error notifying consumer", e);
		}
	}
	
	protected boolean doFilter(String notify) {
		if (contentFilter != null) {
			if (!contentFilter.getDialect().equals(XPATH1_URI)) {
				throw new IllegalStateException("Unsupported dialect: " + contentFilter.getDialect());
			}
			try {
				DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
				factory.setNamespaceAware(true);
				Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(notify)));
				Element root = doc.getDocumentElement();
				Element holder = (Element) root.getElementsByTagNameNS("http://docs.oasis-open.org/wsn/b-1", "NotificationMessage").item(0);
				Element message = (Element) holder.getElementsByTagNameNS("http://docs.oasis-open.org/wsn/b-1", "Message").item(0);
				Element content = null;
				for (int i = 0; i < message.getChildNodes().getLength(); i++) {
					if (message.getChildNodes().item(i) instanceof Element) {
						content = (Element) message.getChildNodes().item(i);
						break;
					}
				}
				XPathFactory xpfactory = XPathFactory.newInstance();
				XPath xpath = xpfactory.newXPath();
				XPathExpression exp = xpath.compile(contentFilter.getContent().get(0).toString());
				Boolean ret = (Boolean) exp.evaluate(content, XPathConstants.BOOLEAN);
				return ret.booleanValue();
			} catch (SAXException e) {
				log.warn("Could not filter notification", e);
			} catch (IOException e) {
				log.warn("Could not filter notification", e);
			} catch (ParserConfigurationException e) {
				log.warn("Could not filter notification", e);
			} catch (XPathExpressionException e) {
				log.warn("Could not filter notification", e);
			}
			return false;
		}
		return true;
	}
	
	protected abstract void doNotify(String notify);

}
