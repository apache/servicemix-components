/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import java.io.StringWriter;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.activemq.advisory.ConsumerEvent;
import org.apache.activemq.advisory.ConsumerEventSource;
import org.apache.activemq.advisory.ConsumerListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.wsn.AbstractPublisher;
import org.apache.servicemix.wsn.jaxws.InvalidTopicExpressionFault;
import org.apache.servicemix.wsn.jaxws.PublisherRegistrationFailedFault;
import org.apache.servicemix.wsn.jaxws.PublisherRegistrationRejectedFault;
import org.apache.servicemix.wsn.jaxws.ResourceNotDestroyedFault;
import org.apache.servicemix.wsn.jaxws.ResourceUnknownFault;
import org.apache.servicemix.wsn.jaxws.TopicNotSupportedFault;
import org.oasis_open.docs.wsn.b_2.InvalidTopicExpressionFaultType;
import org.oasis_open.docs.wsn.b_2.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_2.Notify;
import org.oasis_open.docs.wsn.br_2.PublisherRegistrationFailedFaultType;
import org.oasis_open.docs.wsn.br_2.RegisterPublisher;
import org.oasis_open.docs.wsn.br_2.ResourceNotDestroyedFaultType;

public abstract class JmsPublisher extends AbstractPublisher implements ConsumerListener {

	private static Log log = LogFactory.getLog(JmsPublisher.class);
	
	private Connection connection;
	private JmsTopicExpressionConverter topicConverter;
	private JAXBContext jaxbContext;
    private Topic jmsTopic;
    private ConsumerEventSource advisory;
    private Object subscription;

	public JmsPublisher(String name) {
		super(name);
		topicConverter = new JmsTopicExpressionConverter();
		try {
			jaxbContext = JAXBContext.newInstance(Notify.class);
		} catch (JAXBException e) {
			throw new RuntimeException("Unable to create JAXB context", e);
		}
	}

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	@Override
	public void notify(NotificationMessageHolderType messageHolder) {
		Session session = null;
		try {
            Topic topic = topicConverter.toActiveMQTopic(messageHolder.getTopic());
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            MessageProducer producer = session.createProducer(topic);
            Notify notify = new Notify();
            notify.getNotificationMessage().add(messageHolder);
            StringWriter writer = new StringWriter();
            jaxbContext.createMarshaller().marshal(notify, writer);
            Message message = session.createTextMessage(writer.toString());
            producer.send(message);
		} catch (JMSException e) {
			log.warn("Error dispatching message", e);
		} catch (JAXBException e) {
			log.warn("Error dispatching message", e);
		} catch (InvalidTopicException e) {
			log.warn("Error dispatching message", e);
		} finally {
			if (session != null) {
				try {
					session.close();
				} catch (JMSException e) {
					log.debug("Error closing session", e);
				}
			}
		}
	}

	@Override
	protected void validatePublisher(RegisterPublisher registerPublisherRequest) throws InvalidTopicExpressionFault, PublisherRegistrationFailedFault, PublisherRegistrationRejectedFault, ResourceUnknownFault, TopicNotSupportedFault {
		super.validatePublisher(registerPublisherRequest);
		try {
			jmsTopic = topicConverter.toActiveMQTopic(topic);
		} catch (InvalidTopicException e) {
			InvalidTopicExpressionFaultType fault = new InvalidTopicExpressionFaultType();
			throw new InvalidTopicExpressionFault(e.getMessage(), fault);
		}
	}
	
	@Override
	protected void start() throws PublisherRegistrationFailedFault {
		if (demand) {
			try {
				advisory = new ConsumerEventSource(connection, jmsTopic);
				advisory.setConsumerListener(this);
				advisory.start();
			} catch (Exception e) {
				PublisherRegistrationFailedFaultType fault = new PublisherRegistrationFailedFaultType();
				throw new PublisherRegistrationFailedFault("Error starting demand-based publisher", fault, e);
			}
		}
	}

    protected void destroy() throws ResourceNotDestroyedFault {
		try {
			if (advisory != null) {
				advisory.stop();
	    	}
		} catch (Exception e) {
			ResourceNotDestroyedFaultType fault = new ResourceNotDestroyedFaultType();
			throw new ResourceNotDestroyedFault("Error destroying publisher", fault, e);
		} finally {
			super.destroy();
		}
    }
	
	public void onConsumerEvent(ConsumerEvent event) {
		if (event.getConsumerCount() > 0) {
			if (subscription == null) {
				// start subscription
				subscription = startSubscription();
			}
		} else {
			if (subscription != null) {
				// destroy subscription
				Object sub = subscription;
				subscription = null;
				destroySubscription(sub);
			}
		}
	}

	protected abstract void destroySubscription(Object subscription);

	protected abstract Object startSubscription();
	

}
