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

import junit.framework.TestCase;
import org.apache.servicemix.id.IdGenerator;
import org.apache.servicemix.jbi.helper.MessageExchangePattern;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.messaging.MessageExchangeFactoryImpl;
import org.jsmpp.bean.*;
import org.jsmpp.bean.OptionalParameter.Tag;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.TransformerException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Unit tests on the SMPP marshaler
 *
 * @author jbonofre
 * @author mullerc
 */
public class DefaultSmppMarshalerTest extends TestCase {

    private static final String SOURCE = "0123456789";
    private static final String DESTINATION = "9876543210";
    private static final String TEXT = "This is a SMPP test ...";
    private static final String NPI = "NATIONAL";
    private static final String TON = "INTERNATIONAL";
    private static final String REGISTERED_DELIVERY = "SUCCESS_FAILURE";
    private static final String SCHEDULE_DELIVERY_TIME = "091231143301300+";
    private static final String VALIDITY_PERIOD = "091231153301300+";
    private static final String MSG_VALID_MIN_ATTR =
            "<message>"
                    + "<source>" + SOURCE + "</source>"
                    + "<destination>" + DESTINATION + "</destination>"
                    + "<text>" + TEXT + "</text>"
                    + "<npi>" + NPI + "</npi>"
                    + "<ton>" + TON + "</ton>"
                    + "</message>";
    private static final String MSG_VALID_MAX_ATTR =
            "<message>"
                    + "<source>" + SOURCE + "</source>"
                    + "<destination>" + DESTINATION + "</destination>"
                    + "<text>" + TEXT + "</text>"
                    + "<npi>" + NPI + "</npi>"
                    + "<ton>" + TON + "</ton>"
                    + "<registeredDelivery>" + REGISTERED_DELIVERY + "</registeredDelivery>"
                    + "<scheduleDeliveryTime>" + SCHEDULE_DELIVERY_TIME + "</scheduleDeliveryTime>"
                    + "<validityPeriod>" + VALIDITY_PERIOD + "</validityPeriod>"
                    + "</message>";
    private static final String MSG_VALID_MAX_DEF_ATTR =
            "<message>"
                    + "<source>" + SOURCE + "</source>"
                    + "<destination>" + DESTINATION + "</destination>"
                    + "<text>" + TEXT + "</text>"
                    + "<npi>" + NPI + "</npi>"
                    + "<ton>" + TON + "</ton>"
                    + "<registeredDelivery>DEFAULT</registeredDelivery>"
                    + "</message>";
    private static final String MSG_INVALID = "Test breaker ...";
    private static final String MSG_INVALID_DEST =
            "<message>"
                    + "<source>" + SOURCE + "</source>"
                    + "<text>" + TEXT + "</text>"
                    + "<npi>" + NPI + "</npi>"
                    + "<ton>" + TON + "</ton>"
                    + "</message>";
    private static final String MSG_INVALID_TON =
            "<message>"
                    + "<source>" + SOURCE + "</source>"
                    + "<destination>" + DESTINATION + "</destination>"
                    + "<text>" + TEXT + "</text>"
                    + "<npi>" + NPI + "</npi>"
                    + "</message>";
    private static final String MSG_INVALID_NPI =
            "<message>"
                    + "<source>" + SOURCE + "</source>"
                    + "<destination>" + DESTINATION + "</destination>"
                    + "<text>" + TEXT + "</text>"
                    + "<ton>" + TON + "</ton>"
                    + "</message>";
    private static final String MSG_INVALID_REGISTERED_DELIVERY =
            "<message>"
                    + "<source>" + SOURCE + "</source>"
                    + "<destination>" + DESTINATION + "</destination>"
                    + "<text>" + TEXT + "</text>"
                    + "<npi>" + NPI + "</npi>"
                    + "<ton>" + TON + "</ton>"
                    + "<registeredDelivery>xxx</registeredDelivery>"
                    + "</message>";
    private SmppMarshalerSupport marshaler;
    private MessageExchangeFactory factory;

    public void setUp() throws Exception {
        this.marshaler = new DefaultSmppMarshaler();
        this.factory = new MessageExchangeFactoryImpl(new IdGenerator(), new AtomicBoolean(false));
    }

    // UNIT TESTS

    public void testFromNMSValidMinAttr() throws Exception {
        MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
        NormalizedMessage message = exchange.createMessage();
        message.setContent(new StringSource(MSG_VALID_MIN_ATTR));
        exchange.setMessage(message, "in");

        MessageRequest mr = this.marshaler.fromNMS(exchange, message);

        assertEquals(TEXT, new String(mr.getShortMessage()));
        assertEquals(mr.getDestAddress(), DESTINATION);
        assertEquals(SOURCE, mr.getSourceAddr());
        assertEquals(TON, TypeOfNumber.valueOf(mr.getDestAddrTon()).toString());
        assertEquals(TON, TypeOfNumber.valueOf(mr.getSourceAddrTon()).toString());
        assertEquals(NPI, NumberingPlanIndicator.valueOf(mr.getDestAddrNpi()).toString());
        assertEquals(NPI, NumberingPlanIndicator.valueOf(mr.getSourceAddrNpi()).toString());
        assertEquals((byte) 0x00, mr.getRegisteredDelivery());
        assertNull(mr.getScheduleDeliveryTime());
        assertNull(mr.getValidityPeriod());
    }

    public void testFromNMSValidMaxAttr() throws Exception {
        MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
        NormalizedMessage message = exchange.createMessage();
        message.setContent(new StringSource(MSG_VALID_MAX_ATTR));
        exchange.setMessage(message, "in");

        MessageRequest mr = this.marshaler.fromNMS(exchange, message);

        assertEquals(TEXT, new String(mr.getShortMessage()));
        assertEquals(mr.getDestAddress(), DESTINATION);
        assertEquals(SOURCE, mr.getSourceAddr());
        assertEquals(TON, TypeOfNumber.valueOf(mr.getDestAddrTon()).toString());
        assertEquals(TON, TypeOfNumber.valueOf(mr.getSourceAddrTon()).toString());
        assertEquals(NPI, NumberingPlanIndicator.valueOf(mr.getDestAddrNpi()).toString());
        assertEquals(NPI, NumberingPlanIndicator.valueOf(mr.getSourceAddrNpi()).toString());
        assertEquals((byte) 0x01, mr.getRegisteredDelivery());
        assertEquals(SCHEDULE_DELIVERY_TIME, mr.getScheduleDeliveryTime());
        assertEquals(VALIDITY_PERIOD, mr.getValidityPeriod());
    }

    public void testFromNMSNullExchange() {
        try {
            this.marshaler.fromNMS(null, null);
            fail("Seems we processed a message with null exchange...");
        } catch (TransformerException transformerException) {
            // expected
        }
    }

    public void testFromNMSInvalid() throws Exception {
        try {
            MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
            NormalizedMessage message = exchange.createMessage();
            message.setContent(new StringSource(MSG_INVALID));
            exchange.setMessage(message, "in");

            this.marshaler.fromNMS(exchange, message);

            fail("Seems we processed a invalid message...");
        } catch (TransformerException transformerException) {
            // expected
        }
    }

    public void testFromNMSInvalidDest() throws Exception {
        try {
            MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
            NormalizedMessage message = exchange.createMessage();
            message.setContent(new StringSource(MSG_INVALID_DEST));
            exchange.setMessage(message, "in");

            this.marshaler.fromNMS(exchange, message);

            fail("Seems we processed a message with a invalid destination...");
        } catch (TransformerException transformerException) {
            // expected
        }
    }

    public void testFromNMSInvalidTon() throws Exception {
        try {
            MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
            NormalizedMessage message = exchange.createMessage();
            message.setContent(new StringSource(MSG_INVALID_TON));
            exchange.setMessage(message, "in");

            this.marshaler.fromNMS(exchange, message);

            fail("Seems we processed a message with a invlid type of number...");
        } catch (TransformerException transformerException) {
            // expected
        }
    }

    public void testFromNMSInvalidNpi() throws Exception {
        try {
            MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
            NormalizedMessage message = exchange.createMessage();
            message.setContent(new StringSource(MSG_INVALID_NPI));
            exchange.setMessage(message, "in");

            this.marshaler.fromNMS(exchange, message);

            fail("Seems we processed a message with a invlid numbering plan indicator...");
        } catch (TransformerException transformerException) {
            // expected
        }
    }

    public void testFromNMSInvalidRegisteredDelivery() throws MessagingException {
        try {
            MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
            NormalizedMessage message = exchange.createMessage();
            message.setContent(new StringSource(MSG_INVALID_REGISTERED_DELIVERY));
            exchange.setMessage(message, "in");

            this.marshaler.fromNMS(exchange, message);

            fail("Seems we processed a message with a invlid registered delivery value...");
        } catch (TransformerException transformerException) {
            // expected
        }
    }

    public void testToNMSValidMinAttr() throws Exception {
        MessageRequest mr = new SubmitSm();
        mr.setDestAddress(DESTINATION);
        mr.setDestAddrNpi(NumberingPlanIndicator.valueOf(NPI).value());
        mr.setDestAddrTon(TypeOfNumber.valueOf(TON).value());
        mr.setSourceAddr(SOURCE);
        mr.setSourceAddrNpi(NumberingPlanIndicator.valueOf(NPI).value());
        mr.setSourceAddrTon(TypeOfNumber.valueOf(TON).value());
        mr.setShortMessage(TEXT.getBytes());

        MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
        NormalizedMessage message = exchange.createMessage();
        exchange.setMessage(message, "in");

        this.marshaler.toNMS(message, mr);

        assertEquals(MSG_VALID_MAX_DEF_ATTR, new SourceTransformer().contentToString(message));
    }

    public void testToNMSValidMaxAttr() throws Exception {
        MessageRequest mr = new SubmitSm();
        mr.setDestAddress(DESTINATION);
        mr.setDestAddrNpi(NumberingPlanIndicator.valueOf(NPI).value());
        mr.setDestAddrTon(TypeOfNumber.valueOf(TON).value());
        mr.setSourceAddr(SOURCE);
        mr.setSourceAddrNpi(NumberingPlanIndicator.valueOf(NPI).value());
        mr.setSourceAddrTon(TypeOfNumber.valueOf(TON).value());
        mr.setShortMessage(TEXT.getBytes());
        mr.setRegisteredDelivery((byte) 0x01);
        mr.setScheduleDeliveryTime(SCHEDULE_DELIVERY_TIME);
        mr.setValidityPeriod(VALIDITY_PERIOD);

        MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
        NormalizedMessage message = exchange.createMessage();
        exchange.setMessage(message, "in");

        this.marshaler.toNMS(message, mr);

        assertEquals(MSG_VALID_MAX_ATTR, new SourceTransformer().contentToString(message));
    }

    public void testToNMSInvalid() throws Exception {
        MessageRequest mr = new SubmitSm();
        mr.setShortMessage(TEXT.getBytes());

        MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
        NormalizedMessage message = exchange.createMessage();
        exchange.setMessage(message, "in");

        try {
            this.marshaler.toNMS(message, mr);

            fail("Seems we processed an invalid MessageRequest...");
        } catch (MessagingException messagingException) {
            // expected
        }
    }

    public void testToNMSNullMessageRequest() throws Exception {
        MessageRequest mr = null;
        MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
        NormalizedMessage message = exchange.createMessage();
        exchange.setMessage(message, "in");

        try {
            this.marshaler.toNMS(message, mr);

            fail("Seems we processed a Null MessageRequest...");
        } catch (MessagingException messagingException) {
            // expected
        }
    }

    public void testToNMSNullMessage() {
        MessageRequest mr = new SubmitSm();
        mr.setShortMessage(TEXT.getBytes());

        try {
            marshaler.toNMS(null, mr);

            fail("Seems we processed a MessageRequest with a Null NormalizedMessage...");
        } catch (MessagingException messagingException) {
            // expected
        }
    }

    public void testOptinalParametersOnRequest() throws Exception {
        MessageRequest mr = new SubmitSm();
        mr.setDestAddress(DESTINATION);
        mr.setDestAddrNpi(NumberingPlanIndicator.valueOf(NPI).value());
        mr.setDestAddrTon(TypeOfNumber.valueOf(TON).value());
        mr.setSourceAddr(SOURCE);
        mr.setSourceAddrNpi(NumberingPlanIndicator.valueOf(NPI).value());
        mr.setSourceAddrTon(TypeOfNumber.valueOf(TON).value());
        mr.setShortMessage(TEXT.getBytes());


        mr.setOptionalParametes(new OptionalParameter.Byte(Tag.DEST_ADDR_SUBUNIT, (byte) 1), new OptionalParameter.Null(Tag.ALERT_ON_MESSAGE_DELIVERY));

        MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
        NormalizedMessage message = exchange.createMessage();
        exchange.setMessage(message, "in");

        this.marshaler.toNMS(message, mr);

        assertEquals(message.getProperty("DEST_ADDR_SUBUNIT"), (byte) 1);
        assertNotNull(message.getProperty("ALERT_ON_MESSAGE_DELIVERY"));
    }

    public void testOptinalParametersOnExchange() throws Exception {
        MessageExchange exchange = this.factory.createExchange(MessageExchangePattern.IN_ONLY);
        NormalizedMessage message = exchange.createMessage();
        message.setProperty("ALERT_ON_MESSAGE_DELIVERY", "");
        message.setProperty("PAYLOAD_TYPE", "0");
        message.setContent(new StringSource(MSG_VALID_MIN_ATTR));
        exchange.setMessage(message, "in");

        MessageRequest mr = this.marshaler.fromNMS(exchange, message);

        OptionalParameter[] optionalParameters = mr.getOptionalParametes();
        assertEquals(optionalParameters.length, 2);
    }
}
