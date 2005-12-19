package org.servicemix.wsn.jms;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.DeliveryMode;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.oasis_open.docs.wsn.b_1.NotificationMessageHolderType;
import org.oasis_open.docs.wsn.b_1.Notify;
import org.oasis_open.docs.wsrf.r_1.ResourceUnknownFaultType;
import org.servicemix.wsn.AbstractPullPoint;
import org.servicemix.wsn.jaxws.ResourceUnknownFault;

public class JmsPullPoint extends AbstractPullPoint {

	private static Log log = LogFactory.getLog(JmsPullPoint.class);
	
	private JAXBContext jaxbContext;
	private Connection connection;
	private Session session;
	private Queue queue;
	private MessageProducer producer;
	private MessageConsumer consumer;

	public JmsPullPoint(String name)  {
		super(name);
		try {
			jaxbContext = JAXBContext.newInstance(Notify.class);
		} catch (JAXBException e) {
			throw new RuntimeException("Could not create PullEndpoint", e);
		}
	}
	
	protected void initSession() throws JMSException {
		if (session == null) {
			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
			queue = session.createQueue(getName());
			producer = session.createProducer(queue);
			producer.setDeliveryMode(DeliveryMode.PERSISTENT);
			consumer = session.createConsumer(queue);
		}
	}

	@Override
	protected synchronized void store(NotificationMessageHolderType messageHolder) {
		try {
			initSession();
            Notify notify = new Notify();
            notify.getNotificationMessage().add(messageHolder);
            StringWriter writer = new StringWriter();
            jaxbContext.createMarshaller().marshal(notify, writer);
            Message message = session.createTextMessage(writer.toString());
            producer.send(message);
		} catch (JMSException e) {
			log.warn("Error storing message", e);
			if (session != null) {
				try {
					session.close();
				} catch (JMSException inner) {
					log.debug("Error closing session", inner);
				} finally {
					session = null;
				}
			}
		} catch (JAXBException e) {
			log.warn("Error storing message", e);
		}
	}

	@Override
	protected synchronized List<NotificationMessageHolderType> getMessages(int max) throws ResourceUnknownFault {
		Session session = null;
		try {
			if (max == 0) {
				max = 256;
			}
			initSession();
			List<NotificationMessageHolderType> messages = new ArrayList<NotificationMessageHolderType>();
			for (int i = 0; i < max; i++) {
				Message msg = consumer.receiveNoWait();
				if (msg == null) {
					break;
				}
				TextMessage txtMsg = (TextMessage) msg;
				StringReader reader = new StringReader(txtMsg.getText());
				Notify notify = (Notify) jaxbContext.createUnmarshaller().unmarshal(reader);
				messages.addAll(notify.getNotificationMessage());
			}
			return messages;
		} catch (JMSException e) {
			log.info("Error retrieving messages", e);
			if (session != null) {
				try {
					session.close();
				} catch (JMSException inner) {
					log.debug("Error closing session", inner);
				} finally {
					session = null;
				}
			}
			ResourceUnknownFaultType fault = new ResourceUnknownFaultType();
			throw new ResourceUnknownFault("Unable to retrieve messages", fault, e);
		} catch (JAXBException e) {
			log.info("Error retrieving messages", e);
			ResourceUnknownFaultType fault = new ResourceUnknownFaultType();
			throw new ResourceUnknownFault("Unable to retrieve messages", fault, e);
		}
	}
	
	public Connection getConnection() {
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

}
