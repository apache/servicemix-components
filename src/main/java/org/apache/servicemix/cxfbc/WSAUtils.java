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
package org.apache.servicemix.cxfbc;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AddressingPropertiesImpl;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.addressing.RelatesToType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

public final class WSAUtils {

    public static final String WSA_HEADERS_INBOUND = "javax.xml.ws.addressing.context.inbound";

    public static final String WSA_HEADERS_OUTBOUND = "javax.xml.ws.addressing.context.outbound";

    private static final org.apache.cxf.ws.addressing.ObjectFactory WSA_OBJECT_FACTORY = new org.apache.cxf.ws.addressing.ObjectFactory();

    private WSAUtils() {
        
    }
    
    public static AddressingProperties getCXFAddressingPropertiesFromMap(
            Map<String, String> wsAddressingAsMap) {

        
        AddressingProperties maps = new AddressingPropertiesImpl();
        if (wsAddressingAsMap == null) {
            return maps;
        }
        for (String wsaHeaderKey : wsAddressingAsMap.keySet()) {

            String wsaHeaderValue = wsAddressingAsMap.get(wsaHeaderKey);
            System.out.println(" WSA HEADER KEY -> " + wsaHeaderKey);
            System.out.println(" WSA HEADER VALUE -> " + wsaHeaderKey);
            if (Names.WSA_MESSAGEID_NAME.equals(wsaHeaderKey)) {
                AttributedURIType aAttributedURIType = WSA_OBJECT_FACTORY
                        .createAttributedURIType();
                aAttributedURIType.setValue(wsaHeaderValue);
                maps.setMessageID(aAttributedURIType);
            } else if (Names.WSA_TO_NAME.equals(wsaHeaderKey)) {
                maps.setTo(EndpointReferenceUtils
                        .getEndpointReference(wsaHeaderValue));
            } else if (Names.WSA_FROM_NAME.equals(wsaHeaderKey)) {
                maps.setTo(EndpointReferenceUtils
                        .getEndpointReference(wsaHeaderValue));
            } else if (Names.WSA_REPLYTO_NAME.equals(wsaHeaderKey)) {
                maps.setReplyTo(EndpointReferenceUtils
                        .getEndpointReference(wsaHeaderValue));
            } else if (Names.WSA_FAULTTO_NAME.equals(wsaHeaderKey)) {
                // System.out.println( " **WSA_FAULTTO_NAME**");
                maps.setFaultTo(EndpointReferenceUtils
                        .getEndpointReference(wsaHeaderValue));
            } else if (Names.WSA_RELATESTO_NAME.equals(wsaHeaderKey)) {
                RelatesToType aRelatesToType = WSA_OBJECT_FACTORY
                        .createRelatesToType();
                aRelatesToType.setValue(wsaHeaderValue);
                maps.setRelatesTo(aRelatesToType);
            } else if (Names.WSA_ACTION_NAME.equals(wsaHeaderKey)) {
                AttributedURIType aAttributedURIType = WSA_OBJECT_FACTORY
                        .createAttributedURIType();
                aAttributedURIType.setValue(wsaHeaderValue);
                maps.setAction(aAttributedURIType);
            }
        }
        return maps;
    }

    public static Map<String, String> getAsMap(AddressingProperties maps) {

        Map<String, String> returnMap = new HashMap<String, String>();

        if (maps.getMessageID() != null) {
            returnMap.put(Names.WSA_MESSAGEID_NAME, maps.getMessageID()
                    .getValue());
        }
        if (maps.getTo() != null) {
            returnMap.put(Names.WSA_TO_NAME, maps.getTo().getValue());
        }
        if (maps.getFrom() != null) {
            returnMap.put(Names.WSA_FROM_NAME, maps.getFrom().getAddress()
                    .getValue());
        }
        if (maps.getReplyTo() != null) {
            returnMap.put(Names.WSA_REPLYTO_NAME, maps.getReplyTo()
                    .getAddress().getValue());
        }
        if (maps.getFaultTo() != null) {
            returnMap.put(Names.WSA_FAULTTO_NAME, maps.getFaultTo()
                    .getAddress().getValue());
        }
        if (maps.getRelatesTo() != null) {
            returnMap.put(Names.WSA_RELATESTO_NAME, maps.getRelatesTo()
                    .getValue());
        }
        if (maps.getAction() != null) {
            returnMap.put(Names.WSA_ACTION_NAME, maps.getAction().getValue());
        }
        return returnMap;
    }
}
