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
package org.apache.servicemix.camel;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.camel.processor.DeadLetterChannel;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.servicemix.MessageExchangeListener;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.components.util.ComponentSupport;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.helper.MessageUtil;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.tck.ReceiverComponent;
import org.springframework.util.Assert;

/**
 * Tests on handling fault messages with the Camel Exception handler
 */
public class JbiInOnlyWithErrorHandledTrueSpringDSLTest extends SpringJbiTestSupport {

    private static final QName TEST_SERVICE = new QName("urn:test", "error-handled-true");

    private ReceiverComponent receiver;
    private ReceiverComponent deadLetter;

    private static final String MESSAGE = "<just><a>test</a></just>";
    private static final Level LOG_LEVEL = Logger.getLogger("org.apache.servicemix").getEffectiveLevel();
    
    @Override
    protected void setUp() throws Exception {
        receiver = new ReceiverComponent() {
            public void onMessageExchange(MessageExchange exchange) throws MessagingException {
                NormalizedMessage inMessage = getInMessage(exchange);
                Object value = inMessage.getProperty(DeadLetterChannel.CAUGHT_EXCEPTION_HEADER);
                Assert.notNull(value, DeadLetterChannel.CAUGHT_EXCEPTION_HEADER + " property not set");
                try {
                    MessageUtil.enableContentRereadability(inMessage);
                    String message = new SourceTransformer().contentToString(inMessage);
                    Assert.isTrue(message.contains(MESSAGE));
                } catch (Exception e) {
                    throw new MessagingException(e);
                }
                
                super.onMessageExchange(exchange);
            }
        };
        deadLetter = new ReceiverComponent();

        super.setUp();

        // change the log level to avoid the conversion to DOMSource 
        Logger.getLogger("org.apache.servicemix").setLevel(Level.ERROR);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        // restore the original log level
        Logger.getLogger("org.apache.servicemix").setLevel(LOG_LEVEL);
    }
    
    public void testErrorHandledByExceptionClause() throws Exception {
        ServiceMixClient smxClient = getServicemixClient();
        MessageExchange[] exchanges = new MessageExchange[] {smxClient.createInOnlyExchange(), smxClient.createRobustInOnlyExchange()};
        for (MessageExchange exchange : exchanges) {
            exchange.setService(TEST_SERVICE);
            Source content = new StreamSource(new ByteArrayInputStream(MESSAGE.getBytes()));
            exchange.getMessage("in").setContent(content);

            smxClient.send(exchange);

            exchange = smxClient.receive();
            assertEquals(ExchangeStatus.DONE, exchange.getStatus());
            assertNotNull(exchange.getMessage("in").getProperty(DeadLetterChannel.CAUGHT_EXCEPTION_HEADER));
        }

        receiver.getMessageList().assertMessagesReceived(2);
        deadLetter.getMessageList().assertMessagesReceived(0);
    }

    @Override
    protected String getServiceUnitName() {
        return "su8";
    }

    @Override
    protected void appendJbiActivationSpecs(List<ActivationSpec> activationSpecList) {
        activationSpecList.add(createActivationSpec(new ReturnNullPointerExceptionErrorComponent(), new QName("urn:test",
                                                                                                              "npe-error-service")));

        activationSpecList.add(createActivationSpec(receiver, new QName("urn:test", "receiver-service")));
        activationSpecList.add(createActivationSpec(deadLetter, new QName("urn:test", "deadLetter-service")));
    }

    protected static class ReturnNullPointerExceptionErrorComponent extends ComponentSupport implements MessageExchangeListener {
        public void onMessageExchange(MessageExchange exchange) throws MessagingException {
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                // read the in message content before returning to ensure that the 
                // Camel DeadLetterChannel caches the stream correctly prior to re-delivery
                try {
                    new SourceTransformer().contentToString(exchange.getMessage("in"));
                } catch (Exception e) {
                    throw new MessagingException(e);
                }
                fail(exchange, new NullPointerException());
            }
        }
    }
}
