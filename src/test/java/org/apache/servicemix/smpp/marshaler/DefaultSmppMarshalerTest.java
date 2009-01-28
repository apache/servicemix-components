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
package org.apache.servicemix.smpp.marshaler;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import junit.framework.TestCase;

import org.apache.servicemix.id.IdGenerator;
import org.apache.servicemix.jbi.helper.MessageExchangePattern;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.messaging.MessageExchangeFactoryImpl;
import org.apache.servicemix.smpp.marshaler.DefaultSmppMarshaler;
import org.apache.servicemix.smpp.marshaler.SmppMarshalerSupport;
import org.jsmpp.bean.MessageRequest;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.SubmitSm;
import org.jsmpp.bean.TypeOfNumber;
import org.xml.sax.SAXException;

/**
 * Unit tests on the SMPP marshaler
 * 
 * @author jbonofre
 */
public class DefaultSmppMarshalerTest extends TestCase {

    private static final String SOURCE = "0123456789";
    private static final String DESTINATION = "9876543210";
    private static final String TEXT = "This is a SMPP test ...";
    private static final String NPI = "NATIONAL";
    private static final String TON = "INTERNATIONAL";

    private static final String MSG_VALID = "<message><source>" + SOURCE + "</source><destination>"
                                            + DESTINATION + "</destination><text>" + TEXT + "</text><npi>"
                                            + NPI + "</npi><ton>" + TON + "</ton></message>";
    private static final String MSG_INVALID = "Test breaker ...";
    private static final String MSG_INVALID_DEST = "<message><source>" + SOURCE + "</source><text>" + TEXT
                                                   + "</text><npi>" + NPI + "</npi><ton>" + TON
                                                   + "</ton></message>";
    private static final String MSG_INVALID_TON = "<message><source>" + SOURCE + "</source><destination>"
                                                  + DESTINATION + "</destination><text>" + TEXT
                                                  + "</text><npi>" + NPI + "</npi></message>";
    private static final String MSG_INVALID_NPI = "<message><source>" + SOURCE + "</source><destination>"
                                                  + DESTINATION + "</destination><text>" + TEXT
                                                  + "</text><ton>" + TON + "</ton></message>";

    private SmppMarshalerSupport marshaler;
    private MessageExchangeFactory factory;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    public void setUp() throws Exception {
        this.marshaler = new DefaultSmppMarshaler();
        this.factory = new MessageExchangeFactoryImpl(new IdGenerator(), new AtomicBoolean(false));
    }

    /**
     * @see junit.framework.TestCase#tearDown()
     */
    public void tearDown() {
        this.marshaler = null;
        this.factory = null;
    }

    // UNIT TESTS

    public void testFromNMSValid() {
        try {
            // construct the MessageExchange and NormalizedMessage
            MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
            NormalizedMessage message = exchange.createMessage();
            message.setContent(new StringSource(MSG_VALID));
            exchange.setMessage(message, "in");
            // use the marshaler to converts the NormalizedMessage to a
            // MessageRequest
            MessageRequest mr = marshaler.fromNMS(exchange, message);
            assertEquals("The message text is not the same: ", TEXT, new String(mr.getShortMessage()));
            assertEquals("The destination address is not the same: ", mr.getDestAddress(), DESTINATION);
            assertEquals("The source address is not the same: ", SOURCE, mr.getSourceAddr());
            assertEquals("The destination type of number is not the same: ", TON, TypeOfNumber
                .valueOf(mr.getDestAddrTon()).toString());
            assertEquals("The source type of number is not the same: ", TON, TypeOfNumber
                .valueOf(mr.getSourceAddrTon()).toString());
            assertEquals("The destination numbering plan indicator is not the same: ", NPI,
                         NumberingPlanIndicator.valueOf(mr.getDestAddrNpi()).toString());
            assertEquals("The source numbering plan indicator is not the same: ", NPI, NumberingPlanIndicator
                .valueOf(mr.getSourceAddrNpi()).toString());
        } catch (MessagingException messagingException) {
            fail("Messaging exception occurs when constructing the exchange and the normalized message : "
                 + messagingException.getMessage());
        } catch (TransformerException transformerException) {
            fail("Transformer exception occurs while using the marshaler to converts the normalized message to the message request : "
                 + transformerException.getMessage());
        }
    }

    public void testFromNMSNullExchange() {
        try {
            marshaler.fromNMS(null, null);
            fail("Seems we processed a message with null exchange...");
        } catch (TransformerException transformerException) {
            // fine
        }
    }

    public void testFromNMSInvalid() {
        try {
            MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
            NormalizedMessage message = exchange.createMessage();
            message.setContent(new StringSource(MSG_INVALID));
            exchange.setMessage(message, "in");
            // use the marshaler to converts the NormalizedMessage to a
            // MessageRequest
            MessageRequest mr = marshaler.fromNMS(exchange, message);
            fail("Seems we processed a invalid message...");
        } catch (MessagingException messagingException) {
            fail("Messaging exception occurs : " + messagingException.getMessage());
        } catch (TransformerException transformerException) {
            // fine
        }
    }

    public void testFromNMSInvalidDest() {
        try {
            MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
            NormalizedMessage message = exchange.createMessage();
            message.setContent(new StringSource(MSG_INVALID_DEST));
            exchange.setMessage(message, "in");
            // use the marshaler to converts the NormalizedMessage to a
            // MessageRequest
            MessageRequest mr = marshaler.fromNMS(exchange, message);
            fail("Seems we processed a message with a invalid destination...");
        } catch (MessagingException messagingException) {
            fail("Messaging exception occurs : " + messagingException.getMessage());
        } catch (TransformerException transformerException) {
            // fine
        }
    }

    public void testFromNMSInvalidTon() {
        try {
            MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
            NormalizedMessage message = exchange.createMessage();
            message.setContent(new StringSource(MSG_INVALID_TON));
            exchange.setMessage(message, "in");
            // use the marshaler to converts the NormalizedMessage to a
            // MessageRequest
            MessageRequest mr = marshaler.fromNMS(exchange, message);
            fail("Seems we processed a message with a invlid type of number...");
        } catch (MessagingException messagingException) {
            fail("Messaging exception occurs : " + messagingException.getMessage());
        } catch (TransformerException transformerException) {
            // fine
        }
    }

    public void testFromNMSInvalidNpi() {
        try {
            MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
            NormalizedMessage message = exchange.createMessage();
            message.setContent(new StringSource(MSG_INVALID_NPI));
            exchange.setMessage(message, "in");
            // use the marshaler to converts the NormalizedMessage to a
            // MessageRequest
            MessageRequest mr = marshaler.fromNMS(exchange, message);
            fail("Seems we processed a message with a invlid numbering plan indicator...");
        } catch (MessagingException messagingException) {
            fail("Messaging exception occurs : " + messagingException.getMessage());
        } catch (TransformerException transformerException) {
            // fine
        }
    }

    public void testToNMSValid() {
        // constructs the MessageRequest
        MessageRequest mr = new SubmitSm();
        mr.setDestAddress(DESTINATION);
        mr.setDestAddrNpi(NumberingPlanIndicator.valueOf(NPI).value());
        mr.setDestAddrTon(TypeOfNumber.valueOf(TON).value());
        mr.setSourceAddr(SOURCE);
        mr.setSourceAddrNpi(NumberingPlanIndicator.valueOf(NPI).value());
        mr.setSourceAddrTon(TypeOfNumber.valueOf(TON).value());
        mr.setShortMessage(TEXT.getBytes());
        try {
            MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
            NormalizedMessage message = exchange.createMessage();
            exchange.setMessage(message, "in");
            marshaler.toNMS(message, mr);
            SourceTransformer sourceTransformer = new SourceTransformer();
            assertEquals("Message not correct: ", MSG_VALID, sourceTransformer.contentToString(message));
        } catch (MessagingException messagingException) {
            fail("Messaging exception occurs during the construction of the MessageExchange and NormalizedMessage: "
                 + messagingException.getMessage());
        } catch (TransformerException transformerException) {
            fail("Transformer exception occurs using the marshaler: " + transformerException.getMessage());
        } catch (ParserConfigurationException parserConfigurationException) {
            fail("Parser configuration exception occurs using the SourceTransformer: "
                 + parserConfigurationException.getMessage());
        } catch (SAXException saxException) {
            fail("SAX exception occurs using the SourceTransformer: " + saxException.getMessage());
        } catch (IOException ioException) {
            fail("IO exception occurs using the SourceTransformer: " + ioException.getMessage());
        }
    }

    public void testToNMSInvalid() {
        // constructs a invalid MessageRequest (without destination, source,
        // NPI, TON)
        MessageRequest mr = new SubmitSm();
        mr.setShortMessage(TEXT.getBytes());
        try {
            MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
            NormalizedMessage message = exchange.createMessage();
            exchange.setMessage(message, "in");
            marshaler.toNMS(message, mr);
            fail("Seems we processed an invalid MessageRequest...");
        } catch (MessagingException messagingException) {
            // fine
        }
    }

    public void testToNMSNullMessageRequest() {
        MessageRequest mr = null;
        try {
            MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
            NormalizedMessage message = exchange.createMessage();
            exchange.setMessage(message, "in");
            marshaler.toNMS(message, mr);
            fail("Seems we processed a Null MessageRequest...");
        } catch (MessagingException messagingException) {
            // fine
        }
    }

    public void testToNMSNullMessage() {
        // constructs a invalid MessageRequest (without destination, source,
        // NPI, TON)
        MessageRequest mr = new SubmitSm();
        mr.setShortMessage(TEXT.getBytes());
        try {
            marshaler.toNMS(null, mr);
            fail("Seems we processed a MessageRequest with a Null NormalizedMessage...");
        } catch (MessagingException messagingException) {
            // fine
        }
    }

}
