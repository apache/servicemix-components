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
package org.apache.servicemix.jms.endpoints;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.xml.transform.Source;

import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.messaging.MessageExchangeSupport;

public class DefaultConsumerMarshaler implements JmsConsumerMarshaler {
    
    private URI mep;

    public DefaultConsumerMarshaler() {
        this.mep = MessageExchangeSupport.IN_ONLY;
    }
    
    public DefaultConsumerMarshaler(URI mep) {
        this.mep = mep;
    }
    
    /**
     * @return the mep
     */
    public URI getMep() {
        return mep;
    }

    /**
     * @param mep the mep to set
     */
    public void setMep(URI mep) {
        this.mep = mep;
    }

    public JmsContext createContext(Message message) throws Exception {
        return new Context(message);
    }

    public MessageExchange createExchange(JmsContext jmsContext, ComponentContext jbiContext) throws Exception {
        Context ctx = (Context) jmsContext;
        MessageExchange exchange = jbiContext.getDeliveryChannel().createExchangeFactory().createExchange(mep);
        NormalizedMessage inMessage = exchange.createMessage();
        populateMessage(ctx.message, inMessage);
        exchange.setMessage(inMessage, "in");
        return exchange;
    }

    public Message createOut(MessageExchange exchange, NormalizedMessage outMsg, Session session, JmsContext context) throws Exception {
        String text = new SourceTransformer().contentToString(outMsg);
        return session.createTextMessage(text);
    }

    public Message createFault(MessageExchange exchange, Fault fault, Session session, JmsContext context) throws Exception {
        String text = new SourceTransformer().contentToString(fault);
        return session.createTextMessage(text);
    }

    public Message createError(MessageExchange exchange, Exception error, Session session, JmsContext context) throws Exception {
        throw error;
    }

    protected void populateMessage(Message message, NormalizedMessage normalizedMessage) throws Exception {
        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            Source source = new StringSource(textMessage.getText());
            normalizedMessage.setContent(source);
        } else {
            throw new UnsupportedOperationException("JMS message is not a TextMessage");
        }
    }

    protected static class Context implements JmsContext, Serializable {
        Message message;
        Context(Message message) {
            this.message = message;
        }
        public Message getMessage() {
            return this.message;
        }
        private void writeObject(ObjectOutputStream out) throws IOException {
            out.writeObject(message);
        }
        private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
            message = (Message) in.readObject();
        }
    }

}
