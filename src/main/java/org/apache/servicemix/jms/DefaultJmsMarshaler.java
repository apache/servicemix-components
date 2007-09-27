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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import javax.jms.BytesMessage;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.servicemix.soap.SoapHelper;
import org.apache.servicemix.soap.marshalers.SoapMessage;
import org.apache.servicemix.soap.marshalers.SoapWriter;

/**
 * Encapsulates the conversion to and from JMS messages
 */
public class DefaultJmsMarshaler implements JmsMarshaler {
    public static final String CONTENT_TYPE = "MimeContentType";

    private JmsEndpoint endpoint;
    
    public DefaultJmsMarshaler(JmsEndpoint endpoint) {
        this.endpoint = endpoint;
    }
    
    /**
     * Converts an {@link Exception} into an JMS message. This method will be
     * invoked when the {@link MessageExchange} contains an error.
     * 
     * @param e
     *            Exception to convert
     * @param session
     *            JMS session used to create JMS messages
     * @return JMS message
     * @see MessageExchange#getError()
     */
    public Message toJMS(Exception e, Session session) throws Exception {
        return session.createObjectMessage(e);
    }
    
    /**
     * Template method to allow custom functionality. Custom JmsMarshallers
     * should override this method.
     * 
     * @param message Source message
     * @param session JMS session used to create JMS messages
     * @return JMS version of the specified source SOAP message
     * @throws Exception if an IO error occurs
     * @throws JMSException if a JMS error occurs
     */
    protected Message toJMS(SoapMessage message, Session session) throws Exception {
        SoapHelper soapHelper = new SoapHelper(endpoint);
        
        // turn SOAP message into byte array/string
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SoapWriter writer = soapHelper.getSoapMarshaler().createWriter(message);
        writer.write(baos);
        
        // create text message
        TextMessage msg = session.createTextMessage();
        msg.setText(baos.toString());
        
        // overwrite whatever content-type was passed on to us with the one
        // the SoapWriter constructed
        msg.setStringProperty(CONTENT_TYPE, writer.getContentType());
        
        return msg;
    }
    
    /**
     * Converts a SOAP message to a JMS message, including any message headers.
     * 
     * @param message
     *            message to convert
     * @param headers
     *            protocol headers present in the NormalizedMessage
     * @param session
     *            JMS session used to create JMS messages
     * @throws Exception if something bad happens
     * @return JMS message
     */
    public Message toJMS(SoapMessage message, Map headers, Session session) throws Exception {
        // create message
        Message msg = toJMS(message, session);
        
        // add protocol headers to message
        if (headers != null) {
            for (Iterator it = headers.keySet().iterator(); it.hasNext();) {
                String name = (String) it.next();
                Object value = headers.get(name);
                if (shouldIncludeHeader(name, value)) {
                    msg.setObjectProperty(name, value);
                }
            }
        }
        
        return msg;
    }

    /**
     * Template method to allow custom functionality. Custom JmsMarshalers
     * should override this method.
     * 
     * @param message
     *            Message to be turned into XML/SOAP
     * @return Stream containing either the whole SOAP envelope or just the
     *         payload of the body.
     * @throws Exception
     *             if JMS message is an ObjectMessage containing an Exception
     *             (the containing exception is thrown.)
     * @throws JMSException
     *             if a JMS problem occurs
     * @throws UnsupportedOperationException
     *             if the JMS message is an ObjectMessage which contains
     *             something other than an Exception
     * @throws IllegalArgumentException
     *             if the message is anything other than a TextMessage or
     *             BytesMessage
     */
    protected InputStream toXmlInputStream(Message message) throws Exception {
        InputStream is = null;
        if (message instanceof ObjectMessage) {
            Object o = ((ObjectMessage) message).getObject();
            if (o instanceof Exception) {
                throw (Exception) o;
            } else {
                throw new UnsupportedOperationException("Can not handle objects of type " + o.getClass().getName());
            }
        } else if (message instanceof TextMessage) {
            is = new ByteArrayInputStream(((TextMessage) message).getText().getBytes());
        } else if (message instanceof BytesMessage) {
            int length = (int) ((BytesMessage) message).getBodyLength();
            byte[] bytes = new byte[length];
            ((BytesMessage) message).readBytes(bytes);
            is = new ByteArrayInputStream(bytes);
        } else {
            throw new IllegalArgumentException("JMS message should be a text or bytes message");
        }
        
        return is;
    }
    
    /**
     * Converts a JMS message into a SOAP message
     * 
     * @param message
     *            JMS message to convert
     * @return SOAP representation of the specified JMS message
     * @throws Exception
     *             if an IO exception occurs
     */
    public SoapMessage toSOAP(Message message) throws Exception {
        SoapHelper soapHelper = new SoapHelper(endpoint);
       
        InputStream is = toXmlInputStream(message);
        String contentType = message.getStringProperty(CONTENT_TYPE);
        SoapMessage soap = soapHelper.getSoapMarshaler().createReader().read(is, contentType);

        return soap;
    }

    private boolean shouldIncludeHeader(String name, Object value) {
        return (value instanceof String || value instanceof Number || value instanceof Date)
                        && (!endpoint.isNeedJavaIdentifiers() || isJavaIdentifier(name));
    }
    
    private static boolean isJavaIdentifier(String s) {
        int n = s.length();
        if (n == 0) {
            return false;
        }
        if (!Character.isJavaIdentifierStart(s.charAt(0))) {
            return false;
        }
        for (int i = 1; i < n; i++) {
            if (!Character.isJavaIdentifierPart(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}

