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
package org.apache.servicemix.jms;

import java.util.Map;

import javax.jms.Message;
import javax.jms.Session;

import org.apache.servicemix.soap.marshalers.SoapMessage;

public interface JmsMarshaler {
    
    /**
     * Marshalls the JMS message into an XML/SOAP message
     *
     * @param src Message to marshall
     * @param soapHelper 
     * @throws Exception 
     */
    SoapMessage toSOAP(Message src) throws Exception;
    
    /**
     * Unmarshalls the SOAP message into an JMS message
     *
     * @param message Message to unmarshall
     * @param session Used to create the JMS message
     * @throws MessagingException
     * @throws JMSException
     */
    Message toJMS(SoapMessage message, Map headers, Session session) throws Exception;

    Message toJMS(Exception e, Session session) throws Exception;

}

