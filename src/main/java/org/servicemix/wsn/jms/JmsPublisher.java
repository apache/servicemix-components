package org.servicemix.wsn.jms;

import java.io.StringWriter;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.docs.wsn.b_1.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_1.Notify;
import org.servicemix.wsn.AbstractPublisher;
import org.servicemix.wsn.jaxws.TopicExpressionDialectUnknownFault;

public class JmsPublisher extends AbstractPublisher {

	private static Log log = LogFactory.getLog(JmsPublisher.class);
	
	private Connection connection;
	private JmsTopicExpressionConverter topicConverter;
	private JAXBContext jaxbContext;

	public JmsPublisher(String name) {
		super(name);
		topicConverter = new JmsTopicExpressionConverter();
		try {
			jaxbContext = JAXBContext.newInstance(Notify.class);
		} catch (JAXBException e) {
			throw new RuntimeException("Unable to create JAXB context", e);
		}
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
		} catch (TopicExpressionDialectUnknownFault e) {
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

	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

}
