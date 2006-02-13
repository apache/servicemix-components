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
package org.apache.servicemix.wsn.jms;

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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.wsn.AbstractSubscription;
import org.apache.servicemix.wsn.jaxws.InvalidFilterFault;
import org.apache.servicemix.wsn.jaxws.InvalidMessageContentExpressionFault;
import org.apache.servicemix.wsn.jaxws.InvalidProducerPropertiesExpressionFault;
import org.apache.servicemix.wsn.jaxws.InvalidTopicExpressionFault;
import org.apache.servicemix.wsn.jaxws.PauseFailedFault;
import org.apache.servicemix.wsn.jaxws.ResumeFailedFault;
import org.apache.servicemix.wsn.jaxws.SubscribeCreationFailedFault;
import org.apache.servicemix.wsn.jaxws.TopicExpressionDialectUnknownFault;
import org.apache.servicemix.wsn.jaxws.TopicNotSupportedFault;
import org.apache.servicemix.wsn.jaxws.UnableToDestroySubscriptionFault;
import org.apache.servicemix.wsn.jaxws.UnacceptableInitialTerminationTimeFault;
import org.apache.servicemix.wsn.jaxws.UnacceptableTerminationTimeFault;
import org.oasis_open.docs.wsn.b_2.InvalidTopicExpressionFaultType;
import org.oasis_open.docs.wsn.b_2.PauseFailedFaultType;
import org.oasis_open.docs.wsn.b_2.ResumeFailedFaultType;
import org.oasis_open.docs.wsn.b_2.Subscribe;
import org.oasis_open.docs.wsn.b_2.SubscribeCreationFailedFaultType;
import org.oasis_open.docs.wsn.b_2.UnableToDestroySubscriptionFaultType;
import org.oasis_open.docs.wsn.b_2.UnacceptableTerminationTimeFaultType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

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
	protected void validateSubscription(Subscribe subscribeRequest) throws InvalidFilterFault, InvalidMessageContentExpressionFault, InvalidProducerPropertiesExpressionFault, InvalidTopicExpressionFault, SubscribeCreationFailedFault, TopicExpressionDialectUnknownFault, TopicNotSupportedFault, UnacceptableInitialTerminationTimeFault {
		super.validateSubscription(subscribeRequest);
		try {
			jmsTopic = topicConverter.toActiveMQTopic(topic);
		} catch (InvalidTopicException e) {
			InvalidTopicExpressionFaultType fault = new InvalidTopicExpressionFaultType();
			throw new InvalidTopicExpressionFault(e.getMessage(), fault);
		}
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

	public void onMessage(Message jmsMessage) {
		try {
			TextMessage text = (TextMessage) jmsMessage;
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(text.getText())));
			Element root = doc.getDocumentElement();
			Element holder = (Element) root.getElementsByTagNameNS(WSN_URI, "NotificationMessage").item(0);
			Element message = (Element) holder.getElementsByTagNameNS(WSN_URI, "Message").item(0);
			Element content = null;
			for (int i = 0; i < message.getChildNodes().getLength(); i++) {
				if (message.getChildNodes().item(i) instanceof Element) {
					content = (Element) message.getChildNodes().item(i);
					break;
				}
			}
	        boolean match = doFilter(content);
			if (match) {
				if (useRaw) {
					doNotify(content);
				} else {
					doNotify(root);
				}
			}
		} catch (Exception e) {
			log.warn("Error notifying consumer", e);
		}
	}
	
	protected boolean doFilter(Element content) {
		if (contentFilter != null) {
			if (!contentFilter.getDialect().equals(XPATH1_URI)) {
				throw new IllegalStateException("Unsupported dialect: " + contentFilter.getDialect());
			}
			try {
				XPathFactory xpfactory = XPathFactory.newInstance();
				XPath xpath = xpfactory.newXPath();
				XPathExpression exp = xpath.compile(contentFilter.getContent().get(0).toString());
				Boolean ret = (Boolean) exp.evaluate(content, XPathConstants.BOOLEAN);
				return ret.booleanValue();
			} catch (XPathExpressionException e) {
				log.warn("Could not filter notification", e);
			}
			return false;
		}
		return true;
	}
	
	protected abstract void doNotify(Element content);

}
