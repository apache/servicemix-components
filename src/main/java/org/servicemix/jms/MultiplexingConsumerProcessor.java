/** 
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.servicemix.jms;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

import org.servicemix.common.BaseLifeCycle;
import org.servicemix.jbi.jaxp.StringSource;
import org.servicemix.jms.wsdl.JmsAddress;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import java.util.Map;

public class MultiplexingConsumerProcessor extends AbstractJmsProcessor implements MessageListener {

    protected Session session;
    protected Destination destination;
    protected MessageConsumer consumer;
    protected JmsMarshaler marshaler = new JmsMarshaler();
    protected Map pendingMessages = new ConcurrentHashMap();
    protected DeliveryChannel channel;
    
    public MultiplexingConsumerProcessor(JmsEndpoint endpoint) {
        super(endpoint);
    }

    protected void doStart(InitialContext ctx) throws Exception {
        JmsAddress address = endpoint.getAddress();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        if (address.getJndiDestinationName() != null) {
            destination = (Destination) ctx.lookup(address.getJndiDestinationName());
        } else if (address.getJmsProviderDestinationName() != null) {
            if (STYLE_QUEUE.equals(address.getDestinationStyle())) {
                destination = session.createQueue(address.getJmsProviderDestinationName());
            } else {
                destination = session.createTopic(address.getJmsProviderDestinationName());
            }
        } else {
            throw new IllegalStateException("No destination provided");
        }
        consumer = session.createConsumer(destination);
        consumer.setMessageListener(this);
        channel = endpoint.getServiceUnit().getComponent().getComponentContext().getDeliveryChannel();
    }


    protected void doStop() throws Exception {
        session = null;
        destination = null;
        consumer = null;
        pendingMessages.clear();
    }

    public void onMessage(final Message message) {
        try {
            endpoint.getServiceUnit().getComponent().getWorkManager().scheduleWork(new Work() {
                public void release() {
                }
                public void run() {
                    try {
                        MessageExchange exchange = channel.createExchangeFactory().createExchange(endpoint.getDefaultMep());
                        exchange.setService(endpoint.getService());
                        exchange.setInterfaceName(endpoint.getInterfaceName());
                        NormalizedMessage msg = exchange.createMessage();
                        msg.setContent(new StringSource(((TextMessage) message).getText()));
                        exchange.setMessage(msg, "in");
                        pendingMessages.put(exchange.getExchangeId(), message);
                        BaseLifeCycle lf = (BaseLifeCycle) endpoint.getServiceUnit().getComponent().getLifeCycle();
                        lf.sendConsumerExchange(exchange, MultiplexingConsumerProcessor.this);
                        
                    } catch (Exception e) {
                        // TODO: log exception
                        e.printStackTrace();
                    }
                }
            });
        } catch (WorkException e) {
            // TODO: log exception
            e.printStackTrace();
        }
    }

    public void process(MessageExchange exchange) throws Exception {
        Message message = (Message) pendingMessages.remove(exchange.getExchangeId());
        MessageProducer producer = null;
        try {
            producer = session.createProducer(message.getJMSReplyTo());
            Message response = marshaler.createMessage(((InOut) exchange).getOutMessage(), session);
            response.setJMSCorrelationID(message.getJMSCorrelationID());
            producer.send(response);
        } finally {
            if (producer != null) {
                producer.close();
            }
            exchange.setStatus(ExchangeStatus.DONE);
            channel.send(exchange);
        }
    }

}
