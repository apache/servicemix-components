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
package org.apache.servicemix.jms.jca;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.InitialContext;

import org.apache.servicemix.jms.AbstractJmsProcessor;
import org.apache.servicemix.jms.JmsEndpoint;

/**
 * 
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class JcaProviderProcessor extends AbstractJmsProcessor {

    protected Destination destination;
    protected Destination replyToDestination;
    protected DeliveryChannel channel;
    protected ConnectionFactory connectionFactory;
    
    public JcaProviderProcessor(JmsEndpoint endpoint) throws Exception {
        super(endpoint);
    }

    public void start() throws Exception {
        InitialContext ctx = getInitialContext();
        connectionFactory = getConnectionFactory(ctx);
        channel = endpoint.getServiceUnit().getComponent().getComponentContext().getDeliveryChannel();
        destination = endpoint.getDestination();
        if (destination == null) {
            if (endpoint.getJndiDestinationName() != null) {
                destination = (Destination) ctx.lookup(endpoint.getJndiDestinationName());
            } else if (endpoint.getJmsProviderDestinationName() == null) {
                throw new IllegalStateException("No destination provided");
            }
        }
    }


    public void stop() throws Exception {
        destination = null;
        replyToDestination = null;
    }

    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            return;
        }
        if (exchange instanceof InOnly == false && 
            exchange instanceof RobustInOnly == false) {
            exchange.setError(new UnsupportedOperationException("Use an InOnly or RobustInOnly MEP"));
            channel.send(exchange);
            return;
        }
        Connection connection = null;
        Session session = null;
        try {
            connection = connectionFactory.createConnection();
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            if (destination == null) {
                if (STYLE_QUEUE.equals(endpoint.getDestinationStyle())) {
                    destination = session.createQueue(endpoint.getJmsProviderDestinationName());
                } else {
                    destination = session.createTopic(endpoint.getJmsProviderDestinationName());
                }
            }
            MessageProducer producer = session.createProducer(destination);
            
            TextMessage msg = session.createTextMessage();
            NormalizedMessage nm = exchange.getMessage("in");
            fromNMS(nm, msg);
            producer.send(msg);
            exchange.setStatus(ExchangeStatus.DONE);
            channel.send(exchange);
        } finally {
            if (session != null) {
                session.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
    }

}
