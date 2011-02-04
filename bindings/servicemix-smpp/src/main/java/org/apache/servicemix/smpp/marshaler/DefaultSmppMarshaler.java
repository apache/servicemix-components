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

import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.jsmpp.bean.*;
import org.jsmpp.bean.OptionalParameter.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.TransformerException;
import java.util.ArrayList;
import java.util.List;

/**
 * Default SMPP Marshaler
 *
 * @author jbonofre
 * @author lhein
 * @author mullerc
 */
public class DefaultSmppMarshaler implements SmppMarshalerSupport {

    private final Logger logger = LoggerFactory.getLogger(DefaultSmppMarshaler.class);

    private final static String TAG_MESSAGE = "message";
    private final static String TAG_SOURCE = "source";
    private final static String TAG_DESTINATION = "destination";
    private final static String TAG_TEXT = "text";
    private final static String TAG_TON = "ton";
    private final static String TAG_NPI = "npi";
    private final static String TAG_REGISTERED_DELIVERY = "registeredDelivery";
    private final static String TAG_SCHEDULE_DELIVERY_TIME = "scheduleDeliveryTime";
    private final static String TAG_VALIDITY_PERIOD = "validityPeriod";
    //Optional Parameter Flags
    private final static String DEST_ADDR_SUBUNIT = "DEST_ADDR_SUBUNIT";
    private final static String DEST_NETWORK_TYPE = "DEST_NETWORK_TYPE";
    private final static String DEST_BEARER_TYPE = "DEST_BEARER_TYPE";
    private final static String DEST_TELEMATICS_ID = "DEST_TELEMATICS_ID";
    private final static String SOURCE_ADDR_SUBUNIT = "SOURCE_ADDR_SUBUNIT";
    private final static String SOURCE_NETWORK_TYPE = "SOURCE_NETWORK_TYPE";
    private final static String SOURCE_BEARER_TYPE = "SOURCE_BEARER_TYPE";
    private final static String SOURCE_TELEMATICS_ID = "SOURCE_TELEMATICS_ID";
    private final static String QOS_TIME_TO_LIVE = "QOS_TIME_TO_LIVE";
    private final static String PAYLOAD_TYPE = "PAYLOAD_TYPE";
    private final static String ADDITIONAL_STATUS_INFO_TEXT = "ADDITIONAL_STATUS_INFO_TEXT";
    private final static String RECEIPTED_MESSAGE_ID = "RECEIPTED_MESSAGE_ID";
    private final static String MS_MSG_WAIT_FACILITIES = "MS_MSG_WAIT_FACILITIES";
    private final static String PRIVACY_INDICATOR = "PRIVACY_INDICATOR";
    private final static String SOURCE_SUBADDRESS = "SOURCE_SUBADDRESS";
    private final static String DEST_SUBADDRESS = "DEST_SUBADDRESS";
    private final static String USER_MESSAGE_REFERENCE = "USER_MESSAGE_REFERENCE";
    private final static String USER_RESPONSE_CODE = "USER_RESPONSE_CODE";
    private final static String SOURCE_PORT = "SOURCE_PORT";
    private final static String DESTINATION_PORT = "DESTINATION_PORT";
    private final static String SAR_MSG_REF_NUM = "SAR_MSG_REF_NUM";
    private final static String LANGUAGE_INDICATOR = "LANGUAGE_INDICATOR";
    private final static String SAR_TOTAL_SEGMENTS = "SAR_TOTAL_SEGMENTS";
    private final static String SAR_SEGMENT_SEQNUM = "SAR_SEGMENT_SEQNUM";
    private final static String SC_INTERFACE_VERSION = "SC_INTERFACE_VERSION";
    private final static String CALLBACK_NUM_PRES_IND = "CALLBACK_NUM_PRES_IND";
    private final static String CALLBACK_NUM_ATAG = "CALLBACK_NUM_ATAG";
    private final static String NUMBER_OF_MESSAGES = "NUMBER_OF_MESSAGES";
    private final static String CALLBACK_NUM = "CALLBACK_NUM";
    private final static String DPF_RESULT = "DPF_RESULT";
    private final static String SET_DPF = "SET_DPF";
    private final static String MS_AVAILABILITY_STATUS = "MS_AVAILABILITY_STATUS";
    private final static String NETWORK_ERROR_CODE = "NETWORK_ERROR_CODE";
    private final static String MESSAGE_PAYLOAD = "MESSAGE_PAYLOAD";
    private final static String DELIVERY_FAILURE_REASON = "DELIVERY_FAILURE_REASON";
    private final static String MORE_MESSAGES_TO_SEND = "MORE_MESSAGES_TO_SEND";
    private final static String MESSAGE_STATE = "MESSAGE_STATE";
    private final static String USSD_SERVICE_OP = "USSD_SERVICE_OP";
    private final static String DISPLAY_TIME = "DISPLAY_TIME";
    private final static String SMS_SIGNAL = "SMS_SIGNAL";
    private final static String MS_VALIDITY = "MS_VALIDITY";
    private final static String ALERT_ON_MESSAGE_DELIVERY = "ALERT_ON_MESSAGE_DELIVERY";
    private final static String ITS_REPLY_TYPE = "ITS_REPLY_TYPE";
    private final static String ITS_SESSION_INFO = "ITS_SESSION_INFO";
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
                logger.debug(TAG_SOURCE + ": " + sm.getSourceAddr());
            }

            if ((node = getNotEmptyNodeListOrNull(document, TAG_DESTINATION)) != null) {
                sm.setDestAddress(getFirstNodeValue(node));
                logger.debug(TAG_DESTINATION + ": " + sm.getDestAddress());
            }

            if ((node = getNotEmptyNodeListOrNull(document, TAG_TEXT)) != null) {
                sm.setShortMessage(getFirstNodeValue(node).getBytes());
                logger.debug(TAG_TEXT + ": " + new String(sm.getShortMessage()));
            }

            if ((node = getNotEmptyNodeListOrNull(document, TAG_TON)) != null) {
                ton = getFirstNodeValue(node);
                sm.setDestAddrTon(TypeOfNumber.valueOf(ton).value());
                sm.setSourceAddrTon(TypeOfNumber.valueOf(ton).value());
                logger.debug(TAG_TON + ": " + ton);
            }

            if ((node = getNotEmptyNodeListOrNull(document, TAG_NPI)) != null) {
                npi = getFirstNodeValue(node);
                sm.setDestAddrNpi(NumberingPlanIndicator.valueOf(npi).value());
                sm.setSourceAddrNpi(NumberingPlanIndicator.valueOf(npi).value());
                logger.debug(TAG_NPI + ": " + npi);
            }

            if ((node = getNotEmptyNodeListOrNull(document, TAG_REGISTERED_DELIVERY)) != null) {
                String registeredDelivery = getFirstNodeValue(node);
                sm.setRegisteredDelivery(SMSCDeliveryReceipt.valueOf(registeredDelivery).value());
                logger.debug(TAG_REGISTERED_DELIVERY + ": " + registeredDelivery);
            } else {
                sm.setRegisteredDelivery(SMSCDeliveryReceipt.DEFAULT.value());
                logger.debug(TAG_REGISTERED_DELIVERY + ": DEFAULT");
            }

            if ((node = getNotEmptyNodeListOrNull(document, TAG_SCHEDULE_DELIVERY_TIME)) != null) {
                sm.setScheduleDeliveryTime(getFirstNodeValue(node));
                logger.debug(TAG_SCHEDULE_DELIVERY_TIME + ": " + sm.getScheduleDeliveryTime());
            }

            if ((node = getNotEmptyNodeListOrNull(document, TAG_VALIDITY_PERIOD)) != null) {
                sm.setValidityPeriod(getFirstNodeValue(node));
                logger.debug(TAG_VALIDITY_PERIOD + ": " + sm.getValidityPeriod());
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
        applyOptionalParametersToRequest(sm, message);
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
            logger.error("The MessageRequest source address is not defined");
            throw new MessagingException("The MessageRequest source address is not defined");
        }

        if (mr.getDestAddress() == null || mr.getDestAddress().trim().length() < 1) {
            logger.error("The MessageRequest destination address is not defined");
            throw new MessagingException("The MessageRequest destination address is not defined");
        }

        try {
            NumberingPlanIndicator.valueOf(mr.getDestAddrNpi());
        } catch (IllegalArgumentException illegalArgumentException) {
            logger.error("The MessageRequest destination numbering plan indicator is not valid");
            throw new MessagingException("The MessageRequest destination numbering plan indicator is not valid");
        }

        try {
            TypeOfNumber.valueOf(mr.getDestAddrTon());
        } catch (IllegalArgumentException illegalArgumentException) {
            logger.error("The MessageRequest destination type of number is not valid");
            throw new MessagingException("The MessageRequest destination type of number is not valid");
        }

        try {
            determineSMSCDeliveryReceipt(mr.getRegisteredDelivery());
        } catch (IllegalArgumentException illegalArgumentException) {
            logger.error("The MessageRequest registered delivery is not valid");
            throw new MessagingException("The MessageRequest registered delivery is not valid");
        }

        if (mr.getShortMessage() == null || mr.getShortMessage().length == 0) {
            logger.warn("Received message without text content. Ignore the message");
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
        applyOptionalParametersToNormalizedMessage(mr, message);
    }

    private void applyOptionalParametersToNormalizedMessage(MessageRequest request, NormalizedMessage message) {
        OptionalParameter[] optionalParameter = request.getOptionalParametes();
        if (optionalParameter != null) {
            for (int i = 0; i < optionalParameter.length; i++) {
                String name = Tag.valueOf(optionalParameter[i].tag).name();
                if (optionalParameter[i] instanceof OptionalParameter.Byte) {
                    OptionalParameter.Byte parameter = (OptionalParameter.Byte) optionalParameter[i];
                    message.setProperty(name, parameter.getValue());
                } else if (optionalParameter[i] instanceof OptionalParameter.Short) {
                    OptionalParameter.Short parameter = (OptionalParameter.Short) optionalParameter[i];
                    message.setProperty(name, parameter.getValue());
                } else if (optionalParameter[i] instanceof OptionalParameter.Int) {
                    OptionalParameter.Int parameter = (OptionalParameter.Int) optionalParameter[i];
                    message.setProperty(name, parameter.getValue());
                } else if (optionalParameter[i] instanceof OptionalParameter.COctetString) {
                    OptionalParameter.COctetString parameter = (OptionalParameter.COctetString) optionalParameter[i];
                    message.setProperty(name, parameter.getValueAsString());
                } else if (optionalParameter[i] instanceof OptionalParameter.Null) {
                    OptionalParameter.Null paramter = (OptionalParameter.Null) optionalParameter[i];
                    message.setProperty(name, "");
                }
            }
        }
    }

    private void applyOptionalParametersToRequest(MessageRequest request, NormalizedMessage message) {
        List<OptionalParameter> optionalParameters = new ArrayList<OptionalParameter>();

        //DEST_ADDR_SUBUNIT
        Object destAddressSubunit = message.getProperty(DEST_ADDR_SUBUNIT);
        if (destAddressSubunit != null) {
            if (destAddressSubunit instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.DEST_ADDR_SUBUNIT, (Byte) destAddressSubunit));
            } else if (destAddressSubunit instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.DEST_ADDR_SUBUNIT, Byte.valueOf((String) destAddressSubunit)));
            }
        }

        //DEST_NETWORK_TYPE
        Object destNetworkType = message.getProperty(DEST_NETWORK_TYPE);
        if (destNetworkType != null) {
            if (destNetworkType instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.DEST_NETWORK_TYPE, (Byte) destNetworkType));
            } else if (destNetworkType instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.DEST_NETWORK_TYPE, Byte.valueOf((String) destNetworkType)));
            }
        }

        //DEST_BEARER_TYPE
        Object destBearerType = message.getProperty(DEST_BEARER_TYPE);
        if (destBearerType != null) {
            if (destBearerType instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.DEST_BEARER_TYPE, (Byte) destBearerType));
            } else if (destBearerType instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.DEST_BEARER_TYPE, Byte.valueOf((String) destBearerType)));
            }
        }

        //DEST_TELEMATICS_ID
        Object destTelematicsId = message.getProperty(DEST_TELEMATICS_ID);
        if (destTelematicsId != null) {
            if (destTelematicsId instanceof Short) {
                optionalParameters.add(new OptionalParameter.Short(Tag.DEST_TELEMATICS_ID, (Short) destTelematicsId));
            } else if (destTelematicsId instanceof String) {
                optionalParameters.add(new OptionalParameter.Short(Tag.DEST_TELEMATICS_ID, Short.valueOf((String) destTelematicsId)));
            }
        }

        //SOURCE_ADDR_SUBUNIT
        Object sourceAddrSubunit = message.getProperty(SOURCE_ADDR_SUBUNIT);
        if (sourceAddrSubunit != null) {
            if (sourceAddrSubunit instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SOURCE_ADDR_SUBUNIT, (Byte) sourceAddrSubunit));
            } else if (sourceAddrSubunit instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SOURCE_ADDR_SUBUNIT, Byte.valueOf((String) sourceAddrSubunit)));
            }
        }

        //SOURCE_NETWORK_TYPE
        Object sourceNetworkType = message.getProperty(SOURCE_NETWORK_TYPE);
        if (sourceNetworkType != null) {
            if (sourceNetworkType instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SOURCE_NETWORK_TYPE, (Byte) sourceNetworkType));
            } else if (sourceNetworkType instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SOURCE_NETWORK_TYPE, Byte.valueOf((String) sourceNetworkType)));
            }
        }

        //SOURCE_BEARER_TYPE
        Object sourceBearerType = message.getProperty(SOURCE_BEARER_TYPE);
        if (sourceBearerType != null) {
            if (sourceBearerType instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SOURCE_BEARER_TYPE, (Byte) sourceBearerType));
            } else if (sourceBearerType instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SOURCE_BEARER_TYPE, Byte.valueOf((String) sourceBearerType)));
            }
        }

        //SOURCE_TELEMATICS_ID
        Object sourceTelematicsId = message.getProperty(SOURCE_TELEMATICS_ID);
        if (sourceTelematicsId != null) {
            if (sourceTelematicsId instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SOURCE_TELEMATICS_ID, (Byte) sourceTelematicsId));
            } else if (sourceTelematicsId instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SOURCE_TELEMATICS_ID, Byte.valueOf((String) sourceTelematicsId)));
            }
        }

        //QOS_TIME_TO_LIVE
        Object qosTimeToLive = message.getProperty(QOS_TIME_TO_LIVE);
        if (qosTimeToLive != null) {
            if (qosTimeToLive instanceof Integer) {
                optionalParameters.add(new OptionalParameter.Int(Tag.QOS_TIME_TO_LIVE, (Integer) qosTimeToLive));
            } else if (qosTimeToLive instanceof String) {
                optionalParameters.add(new OptionalParameter.Int(Tag.QOS_TIME_TO_LIVE, Integer.valueOf((String) qosTimeToLive)));
            }
        }


        //PAYLOAD_TYPE
        Object payloadType = message.getProperty(PAYLOAD_TYPE);
        if (payloadType != null) {
            if (payloadType instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.PAYLOAD_TYPE, (Byte) payloadType));
            } else if (payloadType instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.PAYLOAD_TYPE, Byte.valueOf((String) payloadType)));
            }
        }

        //ADDITIONAL_STATUS_INFO_TEXT
        Object additionalStatusInfoText = message.getProperty(ADDITIONAL_STATUS_INFO_TEXT);
        if (additionalStatusInfoText != null) {
            if (additionalStatusInfoText instanceof String) {
                optionalParameters.add(new OptionalParameter.COctetString(Tag.ADDITIONAL_STATUS_INFO_TEXT.code(), (String) additionalStatusInfoText));
            }
        }

        //RECEIPTED_MESSAGE_ID
        Object recipientMessageId = message.getProperty(RECEIPTED_MESSAGE_ID);
        if (recipientMessageId != null) {
            if (recipientMessageId instanceof String) {
                optionalParameters.add(new OptionalParameter.COctetString(Tag.RECEIPTED_MESSAGE_ID.code(), (String) recipientMessageId));
            }
        }

        //MS_MSG_WAIT_FACILITIES
        Object msMsgWaitFacilities = message.getProperty(MS_MSG_WAIT_FACILITIES);
        if (msMsgWaitFacilities != null) {
            if (msMsgWaitFacilities instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.MS_MSG_WAIT_FACILITIES, (Byte) msMsgWaitFacilities));
            } else if (msMsgWaitFacilities instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.MS_MSG_WAIT_FACILITIES, Byte.valueOf((String) msMsgWaitFacilities)));
            }
        }

        //PRIVACY_INDICATOR
        Object privacyIndicator = message.getProperty(PRIVACY_INDICATOR);
        if (privacyIndicator != null) {
            if (privacyIndicator instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.PRIVACY_INDICATOR, (Byte) privacyIndicator));
            } else if (privacyIndicator instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.PRIVACY_INDICATOR, Byte.valueOf((String) privacyIndicator)));
            }
        }

        //SOURCE_SUBADDRESS
        Object sourceSubAddress = message.getProperty(SOURCE_SUBADDRESS);
        if (sourceSubAddress != null) {
            if (sourceSubAddress instanceof String) {
                optionalParameters.add(new OptionalParameter.COctetString(Tag.SOURCE_SUBADDRESS.code(), (String) sourceSubAddress));
            }
        }

        //DEST_SUBADDRESS
        Object destSubAddress = message.getProperty(DEST_SUBADDRESS);
        if (destSubAddress != null) {
            if (destSubAddress instanceof String) {
                optionalParameters.add(new OptionalParameter.COctetString(Tag.DEST_SUBADDRESS.code(), (String) destSubAddress));
            }
        }

        //USER_MESSAGE_REFERENCE
        Object userMessageReference = message.getProperty(USER_MESSAGE_REFERENCE);
        if (userMessageReference != null) {
            if (userMessageReference instanceof Short) {
                optionalParameters.add(new OptionalParameter.Short(Tag.USER_MESSAGE_REFERENCE, (Short) userMessageReference));
            } else if (userMessageReference instanceof String) {
                optionalParameters.add(new OptionalParameter.Short(Tag.USER_MESSAGE_REFERENCE, Short.valueOf((String) userMessageReference)));
            }
        }


        //USER_RESPONSE_CODE
        Object userResponseCode = message.getProperty(USER_RESPONSE_CODE);
        if (userResponseCode != null) {
            if (userResponseCode instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.USER_RESPONSE_CODE, (Byte) userResponseCode));
            } else if (userResponseCode instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.USER_RESPONSE_CODE, Byte.valueOf((String) userResponseCode)));
            }
        }


        //SOURCE_PORT
        Object sourcePort = message.getProperty(SOURCE_PORT);
        if (sourcePort != null) {
            if (sourcePort instanceof Short) {
                optionalParameters.add(new OptionalParameter.Short(Tag.SOURCE_PORT, (Short) sourcePort));
            } else if (sourcePort instanceof String) {
                optionalParameters.add(new OptionalParameter.Short(Tag.SOURCE_PORT, Short.valueOf((String) sourcePort)));
            }
        }


        //DESTINATION_PORT
        Object desitinationPort = message.getProperty(DESTINATION_PORT);
        if (desitinationPort != null) {
            if (desitinationPort instanceof Short) {
                optionalParameters.add(new OptionalParameter.Short(Tag.DESTINATION_PORT, (Short) desitinationPort));
            } else if (desitinationPort instanceof String) {
                optionalParameters.add(new OptionalParameter.Short(Tag.DESTINATION_PORT, Short.valueOf((String) desitinationPort)));
            }
        }

        //SAR_MSG_REF_NUM
        Object sarMsgRefNum = message.getProperty(SAR_MSG_REF_NUM);
        if (sarMsgRefNum != null) {
            if (sarMsgRefNum instanceof Short) {
                optionalParameters.add(new OptionalParameter.Short(Tag.SAR_MSG_REF_NUM, (Short) sarMsgRefNum));
            } else if (sarMsgRefNum instanceof String) {
                optionalParameters.add(new OptionalParameter.Short(Tag.SAR_MSG_REF_NUM, Short.valueOf((String) sarMsgRefNum)));
            }
        }

        //LANGUAGE_INDICATOR
        Object languageIndicator = message.getProperty(LANGUAGE_INDICATOR);
        if (languageIndicator != null) {
            if (languageIndicator instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.LANGUAGE_INDICATOR, (Byte) languageIndicator));
            } else if (languageIndicator instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.LANGUAGE_INDICATOR, Byte.valueOf((String) languageIndicator)));
            }
        }


        //SAR_TOTAL_SEGMENTS
        Object sarTotalSegments = message.getProperty(SAR_TOTAL_SEGMENTS);
        if (sarTotalSegments != null) {
            if (sarTotalSegments instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SAR_TOTAL_SEGMENTS, (Byte) sarTotalSegments));
            } else if (sarTotalSegments instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SAR_TOTAL_SEGMENTS, Byte.valueOf((String) sarTotalSegments)));
            }
        }

        //SAR_SEGMENT_SEQNUM
        Object sarSegmentSeqNum = message.getProperty(SAR_SEGMENT_SEQNUM);
        if (sarSegmentSeqNum != null) {
            if (sarSegmentSeqNum instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SAR_SEGMENT_SEQNUM, (Byte) sarSegmentSeqNum));
            } else if (sarSegmentSeqNum instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SAR_SEGMENT_SEQNUM, Byte.valueOf((String) sarSegmentSeqNum)));
            }
        }

        //SC_INTERFACE_VERSION
        Object scInterfaceVersion = message.getProperty(SC_INTERFACE_VERSION);
        if (scInterfaceVersion != null) {
            if (scInterfaceVersion instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SC_INTERFACE_VERSION, (Byte) scInterfaceVersion));
            } else if (scInterfaceVersion instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SC_INTERFACE_VERSION, Byte.valueOf((String) scInterfaceVersion)));
            }
        }


        //CALLBACK_NUM_PRES_IND
        Object callbackNumPresInd = message.getProperty(CALLBACK_NUM_PRES_IND);
        if (callbackNumPresInd != null) {
            if (callbackNumPresInd instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.CALLBACK_NUM_PRES_IND, (Byte) callbackNumPresInd));
            } else if (callbackNumPresInd instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.CALLBACK_NUM_PRES_IND, Byte.valueOf((String) callbackNumPresInd)));
            }
        }

        //CALLBACK_NUM_ATAG
        Object callbackNumAtag = message.getProperty(CALLBACK_NUM_ATAG);
        if (callbackNumAtag != null) {
            if (callbackNumAtag instanceof String) {
                optionalParameters.add(new OptionalParameter.COctetString(Tag.CALLBACK_NUM_ATAG.code(), (String) callbackNumAtag));
            }
        }

        //NUMBER_OF_MESSAGES
        Object numberOfMessages = message.getProperty(NUMBER_OF_MESSAGES);
        if (numberOfMessages != null) {
            if (numberOfMessages instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.NUMBER_OF_MESSAGES, (Byte) numberOfMessages));
            } else if (numberOfMessages instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.NUMBER_OF_MESSAGES, Byte.valueOf((String) numberOfMessages)));
            }
        }

        //CALLBACK_NUM
        Object callbackNum = message.getProperty(CALLBACK_NUM);
        if (callbackNum != null) {
            if (callbackNum instanceof String) {
                optionalParameters.add(new OptionalParameter.COctetString(Tag.CALLBACK_NUM.code(), (String) callbackNum));
            }
        }

        //DPF_RESULT
        Object dpfResult = message.getProperty(DPF_RESULT);
        if (dpfResult != null) {
            if (dpfResult instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.DPF_RESULT, (Byte) dpfResult));
            } else if (dpfResult instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.DPF_RESULT, Byte.valueOf((String) dpfResult)));
            }
        }

        //SET_DPF
        Object setDpf = message.getProperty(SET_DPF);
        if (setDpf != null) {
            if (setDpf instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SET_DPF, (Byte) setDpf));
            } else if (setDpf instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SET_DPF, Byte.valueOf((String) setDpf)));
            }
        }

        //MS_AVAILABILITY_STATUS
        Object msAvailabilityStatus = message.getProperty(MS_AVAILABILITY_STATUS);
        if (msAvailabilityStatus != null) {
            if (msAvailabilityStatus instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.MS_AVAILABILITY_STATUS, (Byte) msAvailabilityStatus));
            } else if (msAvailabilityStatus instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.MS_AVAILABILITY_STATUS, Byte.valueOf((String) msAvailabilityStatus)));
            }
        }

        //NETWORK_ERROR_CODE
        Object networkErrorCode = message.getProperty(NETWORK_ERROR_CODE);
        if (networkErrorCode != null) {
            if (networkErrorCode instanceof String) {
                optionalParameters.add(new OptionalParameter.COctetString(Tag.NETWORK_ERROR_CODE.code(), (String) networkErrorCode));
            }
        }

        //MESSAGE_PAYLOAD
        Object messagePayload = message.getProperty(MESSAGE_PAYLOAD);
        if (messagePayload != null) {
            if (messagePayload instanceof String) {
                optionalParameters.add(new OptionalParameter.COctetString(Tag.MESSAGE_PAYLOAD.code(), (String) messagePayload));
            }
        }

        //DELIVERY_FAILURE_REASON
        Object deliveryFailureReason = message.getProperty(DELIVERY_FAILURE_REASON);
        if (deliveryFailureReason != null) {
            if (deliveryFailureReason instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.DELIVERY_FAILURE_REASON, (Byte) deliveryFailureReason));
            } else if (deliveryFailureReason instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.DELIVERY_FAILURE_REASON, Byte.valueOf((String) deliveryFailureReason)));
            }
        }

        //MORE_MESSAGES_TO_SEND
        Object moreMessagesToSent = message.getProperty(MORE_MESSAGES_TO_SEND);
        if (moreMessagesToSent != null) {
            if (moreMessagesToSent instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.MORE_MESSAGES_TO_SEND, (Byte) moreMessagesToSent));
            } else if (moreMessagesToSent instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.MORE_MESSAGES_TO_SEND, Byte.valueOf((String) moreMessagesToSent)));
            }
        }

        //MESSAGE_STATE
        Object messageState = message.getProperty(MESSAGE_STATE);
        if (messageState != null) {
            if (messageState instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.MESSAGE_STATE, (Byte) messageState));
            } else if (messageState instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.MESSAGE_STATE, Byte.valueOf((String) messageState)));
            }
        }

        //USSD_SERVICE_OP
        Object ussdServiceOP = message.getProperty(USSD_SERVICE_OP);
        if (ussdServiceOP != null) {
            if (ussdServiceOP instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.USSD_SERVICE_OP, (Byte) ussdServiceOP));
            } else if (ussdServiceOP instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.USSD_SERVICE_OP, Byte.valueOf((String) ussdServiceOP)));
            }
        }

        //DISPLAY_TIME
        Object displayTime = message.getProperty(DISPLAY_TIME);
        if (displayTime != null) {
            if (displayTime instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.DISPLAY_TIME, (Byte) displayTime));
            } else if (displayTime instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.DISPLAY_TIME, Byte.valueOf((String) displayTime)));
            }
        }

        //SMS_SIGNAL
        Object smsSignal = message.getProperty(SMS_SIGNAL);
        if (smsSignal != null) {
            if (smsSignal instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SMS_SIGNAL, (Byte) smsSignal));
            } else if (smsSignal instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.SMS_SIGNAL, Byte.valueOf((String) smsSignal)));
            }
        }

        //ALERT_ON_MESSAGE_DELIVERY
        Object alertOnMessageDelivery = message.getProperty(ALERT_ON_MESSAGE_DELIVERY);
        if (alertOnMessageDelivery != null) {
            optionalParameters.add(new OptionalParameter.Null(Tag.ALERT_ON_MESSAGE_DELIVERY));
        }

        //MS_VALIDITY
        Object msValidity = message.getProperty(MS_VALIDITY);
        if (msValidity != null) {
            if (msValidity instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.MS_VALIDITY, (Byte) msValidity));
            } else if (msValidity instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.MS_VALIDITY, Byte.valueOf((String) msValidity)));
            }
        }


        //ITS_REPLY_TYPE
        Object itsReplyType = message.getProperty(ITS_REPLY_TYPE);
        if (itsReplyType != null) {
            if (itsReplyType instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.ITS_REPLY_TYPE, (Byte) itsReplyType));
            } else if (itsReplyType instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.ITS_REPLY_TYPE, Byte.valueOf((String) itsReplyType)));
            }
        }


        //ITS_SESSION_INFO
        Object itsSessionInfo = message.getProperty(ITS_SESSION_INFO);
        if (itsSessionInfo != null) {
            if (itsSessionInfo instanceof Byte) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.ITS_SESSION_INFO, (Byte) itsSessionInfo));
            } else if (itsSessionInfo instanceof String) {
                optionalParameters.add(new OptionalParameter.Byte(Tag.ITS_SESSION_INFO, Byte.valueOf((String) itsSessionInfo)));
            }
        }

        if (!optionalParameters.isEmpty()) {
            request.setOptionalParametes(optionalParameters.toArray(new OptionalParameter[optionalParameters.size()]));
        }
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
     *                                  with specified byte value.
     */
    private SMSCDeliveryReceipt determineSMSCDeliveryReceipt(byte value) {
        for (SMSCDeliveryReceipt val : SMSCDeliveryReceipt.values()) {
            if (val.value() == value) {
                return val;
            }
        }

        throw new IllegalArgumentException("No enum const SMSCDeliveryReceipt with value " + value);
	}
}
