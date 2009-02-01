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
import org.jsmpp.bean.SMSCDeliveryReceipt;
import org.jsmpp.bean.SubmitSm;
import org.jsmpp.bean.TypeOfNumber;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Default SMPP Marshaler
 * 
 * @author jbonofre
 * @author lhein
 * @author mullerc
 */
public class DefaultSmppMarshaler implements SmppMarshalerSupport {

    private static final transient Log log = LogFactory.getLog(DefaultSmppMarshaler.class);

    private final static String TAG_MESSAGE = "message";
    private final static String TAG_SOURCE = "source";
    private final static String TAG_DESTINATION = "destination";
    private final static String TAG_TEXT = "text";
    private final static String TAG_TON = "ton";
    private final static String TAG_NPI = "npi";
    private final static String TAG_REGISTERED_DELIVERY = "registeredDelivery";
    private final static String TAG_SCHEDULE_DELIVERY_TIME = "scheduleDeliveryTime";
    private final static String TAG_VALIDITY_PERIOD = "validityPeriod";

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
    private final static String TAG_REGISTERED_DELIVERY_OPEN = "<" + TAG_REGISTERED_DELIVERY + ">";
    private final static String TAG_REGISTERED_DELIVERY_CLOSE = "</" + TAG_REGISTERED_DELIVERY + ">";
    private final static String TAG_SCHEDULE_DELIVERY_TIME_OPEN = "<" + TAG_SCHEDULE_DELIVERY_TIME + ">";
    private final static String TAG_SCHEDULE_DELIVERY_TIME_CLOSE = "</" + TAG_SCHEDULE_DELIVERY_TIME + ">";
    private final static String TAG_VALIDITY_PERIOD_OPEN = "<" + TAG_VALIDITY_PERIOD + ">";
    private final static String TAG_VALIDITY_PERIOD_CLOSE = "</" + TAG_VALIDITY_PERIOD + ">";
    
    private SourceTransformer transformer = new SourceTransformer();

   /*
    * (non-Javadoc)
    * @see org.apache.servicemix.smpp.marshaler.SmppMarshalerSupport#fromNMS(javax.jbi.messaging.MessageExchange, javax.jbi.messaging.NormalizedMessage)
    */
    public MessageRequest fromNMS(MessageExchange exchange, NormalizedMessage message) throws TransformerException {
        SubmitSm sm = new SubmitSm();
        String ton = null;
        String npi = null;
        
        try {
            Document document = transformer.toDOMDocument(message);
            document.getDocumentElement().normalize();
            NodeList node = null;
            
            if ((node = getNotEmptyNodeListOrNull(document, TAG_SOURCE)) != null) {
                sm.setSourceAddr(getFirstNodeValue(node));
                log.debug(TAG_SOURCE + ": " + sm.getSourceAddr());
            }

            if ((node = getNotEmptyNodeListOrNull(document, TAG_DESTINATION)) != null) {
            	sm.setDestAddress(getFirstNodeValue(node));
            	log.debug(TAG_DESTINATION + ": " + sm.getDestAddress());
            }

            if ((node = getNotEmptyNodeListOrNull(document, TAG_TEXT)) != null) {
            	sm.setShortMessage(getFirstNodeValue(node).getBytes());
            	log.debug(TAG_TEXT + ": " + new String(sm.getShortMessage()));
            }
            
            if ((node = getNotEmptyNodeListOrNull(document, TAG_TON)) != null) {
            	ton = getFirstNodeValue(node);
                sm.setDestAddrTon(TypeOfNumber.valueOf(ton).value());
                sm.setSourceAddrTon(TypeOfNumber.valueOf(ton).value());
                log.debug(TAG_TON + ": " + ton);
            }

            if ((node = getNotEmptyNodeListOrNull(document, TAG_NPI)) != null) {
            	npi = getFirstNodeValue(node);
                sm.setDestAddrNpi(NumberingPlanIndicator.valueOf(npi).value());
                sm.setSourceAddrNpi(NumberingPlanIndicator.valueOf(npi).value());
                log.debug(TAG_NPI + ": " + npi);
            }
            
            if ((node = getNotEmptyNodeListOrNull(document, TAG_REGISTERED_DELIVERY)) != null) {
                String registeredDelivery = getFirstNodeValue(node);
                sm.setRegisteredDelivery(SMSCDeliveryReceipt.valueOf(registeredDelivery).value());
                log.debug(TAG_REGISTERED_DELIVERY + ": " + registeredDelivery);
            } else {
            	sm.setRegisteredDelivery(SMSCDeliveryReceipt.DEFAULT.value());
            	log.debug(TAG_REGISTERED_DELIVERY + ": DEFAULT");
            }

            if ((node = getNotEmptyNodeListOrNull(document, TAG_SCHEDULE_DELIVERY_TIME)) != null) {
                sm.setScheduleDeliveryTime(getFirstNodeValue(node));
                log.debug(TAG_SCHEDULE_DELIVERY_TIME + ": " + sm.getScheduleDeliveryTime());
            }
            
            if ((node = getNotEmptyNodeListOrNull(document, TAG_VALIDITY_PERIOD)) != null) {
                sm.setValidityPeriod(getFirstNodeValue(node));
                log.debug(TAG_VALIDITY_PERIOD + ": " + sm.getValidityPeriod());
            }
        } catch (Exception exception) {
            throw new TransformerException(exception);
        }
        
        if (sm.getSourceAddr() == null) {
            throw new TransformerException("Invalid message content. Missing tag: " + TAG_SOURCE);
        }

        if (sm.getDestAddress() == null) {
            throw new TransformerException("Invalid message content. Missing tag: " + TAG_DESTINATION);
        }

        if (ton == null) {
            throw new TransformerException("Invalid message content. Missing tag: " + TAG_TON);
        }
        
        if (npi == null) {
            throw new TransformerException("Invalid message content. Missing tag: " + TAG_NPI);
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

        if (mr.getSourceAddr() == null || mr.getSourceAddr().trim().length() < 1) {
            log.error("The MessageRequest source address is not defined");
            throw new MessagingException("The MessageRequest source address is not defined");
        }
        
        if (mr.getDestAddress() == null || mr.getDestAddress().trim().length() < 1) {
            log.error("The MessageRequest destination address is not defined");
            throw new MessagingException("The MessageRequest destination address is not defined");
        }
        
        try {
            NumberingPlanIndicator.valueOf(mr.getDestAddrNpi());
        } catch (IllegalArgumentException illegalArgumentException) {
            log.error("The MessageRequest destination numbering plan indicator is not valid");
            throw new MessagingException("The MessageRequest destination numbering plan indicator is not valid");
        }
        
        try {
            TypeOfNumber.valueOf(mr.getDestAddrTon());
        } catch (IllegalArgumentException illegalArgumentException) {
            log.error("The MessageRequest destination type of number is not valid");
            throw new MessagingException("The MessageRequest destination type of number is not valid");
        }
        
        try {
        	determineSMSCDeliveryReceipt(mr.getRegisteredDelivery());
        } catch (IllegalArgumentException illegalArgumentException) {
            log.error("The MessageRequest registered delivery is not valid");
            throw new MessagingException("The MessageRequest registered delivery is not valid");
        }
        
        if (mr.getShortMessage() == null || mr.getShortMessage().length == 0) {
        	log.warn("Received message without text content. Ignore the message");
        	return;
        }

        StringBuffer data = new StringBuffer();
        data.append(TAG_MESSAGE_OPEN);

        data.append(TAG_SOURCE_OPEN);
        data.append(mr.getSourceAddr());
        data.append(TAG_SOURCE_CLOSE);

        data.append(TAG_DESTINATION_OPEN);
        data.append(mr.getDestAddress());
        data.append(TAG_DESTINATION_CLOSE);

        data.append(TAG_TEXT_OPEN);
        data.append(new String(mr.getShortMessage()));
        data.append(TAG_TEXT_CLOSE);

        data.append(TAG_NPI_OPEN);
        data.append(NumberingPlanIndicator.valueOf(mr.getDestAddrNpi()).toString());
        data.append(TAG_NPI_CLOSE);

        data.append(TAG_TON_OPEN);
        data.append(TypeOfNumber.valueOf(mr.getDestAddrTon()).toString());
        data.append(TAG_TON_CLOSE);
        
        data.append(TAG_REGISTERED_DELIVERY_OPEN);
        data.append(determineSMSCDeliveryReceipt(mr.getRegisteredDelivery()).toString());
        data.append(TAG_REGISTERED_DELIVERY_CLOSE);
        
        if (mr.getScheduleDeliveryTime() != null && mr.getScheduleDeliveryTime().trim().length() > 0) {
            data.append(TAG_SCHEDULE_DELIVERY_TIME_OPEN);
            data.append(mr.getScheduleDeliveryTime());
            data.append(TAG_SCHEDULE_DELIVERY_TIME_CLOSE);            	
        }

        if (mr.getValidityPeriod() != null && mr.getValidityPeriod().trim().length() > 0) {
            data.append(TAG_VALIDITY_PERIOD_OPEN);
            data.append(mr.getValidityPeriod());
            data.append(TAG_VALIDITY_PERIOD_CLOSE);            	
        }
        
        data.append(TAG_MESSAGE_CLOSE);

        message.setContent(new StringSource(data.toString()));
    }
    
    private String getFirstNodeValue(NodeList node) {
    	return node.item(0).getChildNodes().item(0).getNodeValue();
    }
    
    private NodeList getNotEmptyNodeListOrNull(Document document, String nodeName) {
    	NodeList node = document.getElementsByTagName(nodeName);
    	return (node != null && node.getLength() > 0) ? node : null;
    }
    
	/**
     * Get the <tt>SMSCDeliveryReceipt</tt> based on the specified byte value
     * representation.
     * 
     * @param value is the byte value representation.
     * @return is the enum const related to the specified byte value.
     * @throws IllegalArgumentException if there is no enum const associated
     *         with specified byte value.
     */
	private SMSCDeliveryReceipt determineSMSCDeliveryReceipt(byte value) {
		for (SMSCDeliveryReceipt val : SMSCDeliveryReceipt.values()) {
			if (val.value() == value)
				return val;
		}
		
		throw new IllegalArgumentException("No enum const SMSCDeliveryReceipt with value " + value);
	}
}