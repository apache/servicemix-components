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

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.jsmpp.bean.MessageRequest;
import org.jsmpp.bean.NumberingPlanIndicator;
import org.jsmpp.bean.SubmitSm;
import org.jsmpp.bean.TypeOfNumber;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Default SMPP Marshaler
 * 
 * @author jbonofre
 * @author lhein
 */
public class DefaultSmppMarshaler implements SmppMarshalerSupport {

    // logging facility
    private static final transient Log log = LogFactory.getLog(DefaultSmppMarshaler.class);

    // message tags
    private final static String TAG_MESSAGE = "message";
    private final static String TAG_SOURCE = "source";
    private final static String TAG_DESTINATION = "destination";
    private final static String TAG_TEXT = "text";
    private final static String TAG_TON = "ton";
    private final static String TAG_NPI = "npi";

    private final static String TAG_MESSAGE_OPEN = "<" + TAG_MESSAGE + ">";
    private final static String TAG_MESSAGE_CLOSE = "</" + TAG_MESSAGE + ">";
    private final static String TAG_SOURCE_OPEN = "<" + TAG_SOURCE + ">";
    private final static String TAG_SOURCE_CLOSE = "</" + TAG_SOURCE + ">";
    private final static String TAG_DESTINATION_OPEN = "<" + TAG_DESTINATION + ">";
    private final static String TAG_DESTINATION_CLOSE = "</" + TAG_DESTINATION + ">";
    private final static String TAG_TEXT_OPEN = "<" + TAG_TEXT + ">";
    private final static String TAG_TEXT_CLOSE = "</" + TAG_TEXT + ">";
    private final static String TAG_TON_OPEN = "<" + TAG_TON + ">";
    private final static String TAG_TON_CLOSE = "</" + TAG_TON + ">";
    private final static String TAG_NPI_OPEN = "<" + TAG_NPI + ">";
    private final static String TAG_NPI_CLOSE = "</" + TAG_NPI + ">";

    // source transformer
    private SourceTransformer transformer = new SourceTransformer();

   /*
    * (non-Javadoc)
    * @see org.apache.servicemix.smpp.marshaler.SmppMarshalerSupport#fromNMS(javax.jbi.messaging.MessageExchange, javax.jbi.messaging.NormalizedMessage)
    */
    public MessageRequest fromNMS(MessageExchange exchange, NormalizedMessage message)
        throws TransformerException {
        SubmitSm sm = new SubmitSm();
        String ton = null;
        String npi = null;
        try {
            log.debug("Convert normalized message content to DOM document");
            Document document = transformer.toDOMDocument(message);
            log.debug("Normalize test representation");
            document.getDocumentElement().normalize();
            log.debug("Get the normalized message source");
            NodeList node = document.getElementsByTagName(TAG_SOURCE);
            if (node != null && node.getLength() > 0) {
                log.debug("The source exists in the normalized message");
                sm.setSourceAddr(node.item(0).getChildNodes().item(0).getNodeValue());
                log.debug(TAG_SOURCE + ": " + sm.getSourceAddr());
            }
            log.debug("Get the normalized message destination");
            node = document.getElementsByTagName(TAG_DESTINATION);
            if (node != null && node.getLength() > 0) {
                log.debug("The destination exists in the normalized message");
                sm.setDestAddress(node.item(0).getChildNodes().item(0).getNodeValue());
                log.debug(TAG_DESTINATION + ": " + sm.getDestAddress());
            }
            log.debug("Get the normalized message text");
            node = document.getElementsByTagName(TAG_TEXT);
            if (node != null && node.getLength() > 0) {
                log.debug("The text exists in the normalized message");
                sm.setShortMessage(node.item(0).getChildNodes().item(0).getNodeValue().getBytes());
                log.debug(TAG_TEXT + ": " + new String(sm.getShortMessage()));
            }
            log.debug("Get the normalized message TON");
            node = document.getElementsByTagName(TAG_TON);
            if (node != null && node.getLength() > 0) {
                log.debug("The TON exists in the normalized message");
                ton = node.item(0).getChildNodes().item(0).getNodeValue();
                sm.setDestAddrTon(TypeOfNumber.valueOf(ton).value());
                sm.setSourceAddrTon(TypeOfNumber.valueOf(ton).value());
                log.debug(TAG_TON + ": " + ton);
            }
            log.debug("Get the normalized message NPI");
            node = document.getElementsByTagName(TAG_NPI);
            if (node != null && node.getLength() > 0) {
                log.debug("The NPI exists in the normalized message");
                npi = node.item(0).getChildNodes().item(0).getNodeValue();
                sm.setDestAddrNpi(NumberingPlanIndicator.valueOf(npi).value());
                sm.setSourceAddrNpi(NumberingPlanIndicator.valueOf(npi).value());
                log.debug(TAG_NPI + ": " + npi);
            }

            log.debug("Check the mandatory attribute 'source'");
            if (sm.getSourceAddr() == null) {
                throw new TransformerException("Invalid message content. Missing tag: " + TAG_SOURCE);
            }
            log.debug("Check the mandatory attribute 'destination'");
            if (sm.getDestAddress() == null) {
                throw new TransformerException("Invalid message content. Missing tag: " + TAG_DESTINATION);
            }
            log.debug("Check the mandatory attribute 'ton'");
            if (ton == null) {
                throw new TransformerException("Invalid message content. Missing tag: " + TAG_TON);
            }
            if (npi == null) {
                throw new TransformerException("Invalid message content. Missing tag: " + TAG_NPI);
            }
        } catch (Exception exception) {
            throw new TransformerException(exception);
        }
        return sm;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.servicemix.smpp.marshaler.SmppMarshalerSupport#toNMS(javax.jbi.messaging.NormalizedMessage, org.jsmpp.bean.MessageRequest)
     */
    public void toNMS(NormalizedMessage message, MessageRequest mr) throws MessagingException {
        if (message == null) {
            throw new MessagingException("The NormalizedMessage is null");
        }
        if (mr == null) {
            throw new MessagingException("The MessageRequest is null");
        }

        log.debug("Check if the MessageRequest is valid");
        log.debug("Check the MessageRequest source address");
        if (mr.getSourceAddr() == null || mr.getSourceAddr().trim().length() < 1) {
            log.error("The MessageRequest source address is not defined");
            throw new MessagingException("The MessageRequest source address is not defined");
        }
        log.debug("Check the MessageRequest destination address");
        if (mr.getDestAddress() == null || mr.getDestAddress().trim().length() < 1) {
            log.error("The MessageRequest destination address is not defined");
            throw new MessagingException("The MessageRequest destination address is not defined");
        }
        log.debug("Check the MessageRequest destination numbering plan indicator");
        try {
            NumberingPlanIndicator.valueOf(mr.getDestAddrNpi());
        } catch (IllegalArgumentException illegalArgumentException) {
            log.error("The MessageRequest destination numbering plan indicator is not valid");
            throw new MessagingException(
                                         "The MessageRequest destination numbering plan indicator is not valid");
        }
        log.debug("Check the MessageRequest destination type of numbner");
        try {
            TypeOfNumber.valueOf(mr.getDestAddrTon());
        } catch (IllegalArgumentException illegalArgumentException) {
            log.error("The MessageRequest destination type of number is not valid");
            throw new MessagingException("The MessageRequest destination type of number is not valid");
        }

        String text = null;
        try {
            text = new String(mr.getShortMessage());
        } catch (NullPointerException exception) {
            log.warn("The MessageRequest Short Message is null");
        }

        if (text != null && text.trim().length() > 0) {
            StringBuffer data = new StringBuffer();

            // build the message content
            data.append(TAG_MESSAGE_OPEN);

            data.append(TAG_SOURCE_OPEN);
            data.append(mr.getSourceAddr());
            data.append(TAG_SOURCE_CLOSE);

            data.append(TAG_DESTINATION_OPEN);
            data.append(mr.getDestAddress());
            data.append(TAG_DESTINATION_CLOSE);

            data.append(TAG_TEXT_OPEN);
            data.append(text);
            data.append(TAG_TEXT_CLOSE);

            data.append(TAG_NPI_OPEN);
            data.append(NumberingPlanIndicator.valueOf(mr.getDestAddrNpi()).toString());
            data.append(TAG_NPI_CLOSE);

            data.append(TAG_TON_OPEN);
            data.append(TypeOfNumber.valueOf(mr.getDestAddrTon()).toString());
            data.append(TAG_TON_CLOSE);

            data.append(TAG_MESSAGE_CLOSE);

            // put the content to message body
            message.setContent(new StringSource(data.toString()));
        } else {
            log.debug("Received message without text content. Ignore the message");
        }
    }
}
