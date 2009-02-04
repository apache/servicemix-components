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

import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import javax.jbi.messaging.NormalizedMessage;
import javax.jms.JMSException;
import javax.jms.Message;

/**
 * A usefull base class for marshalers.
 * 
 * @author Łukasz Dywicki <a href="ldywicki@pocztowy.pl">email</a> $Id$
 * @author lhein
 */
public abstract class AbstractJmsMarshaler {

    public static final String DONE_JMS_PROPERTY = "JBIDone";

    public static final String FAULT_JMS_PROPERTY = "JBIFault";

    public static final String ERROR_JMS_PROPERTY = "JBIError";

    public static final String CONTENT_TYPE_PROPERTY = "ContentType";

    /**
     * Should marshaler copy properties set in messages?
     */
    private boolean copyProperties = true;

    private boolean needJavaIdentifiers;

    /**
     * a blacklist for properties which shouldn't be copied
     */
    private List<String> propertyBlackList = null;

    /**
     * Get value from field copyProperties.
     * 
     * @return The copyProperties field value.
     */
    public boolean isCopyProperties() {
        return copyProperties;
    }

    /**
     * Set value from copyProperties field.
     * 
     * @param copyProperties New value for copyProperties field.
     */
    public void setCopyProperties(boolean copyProperties) {
        this.copyProperties = copyProperties;
    }

    /**
     * Copy properties from JMS message to JBI message.
     * 
     * @param message Received JMS message.
     * @param inMessage Created JBI message.
     * @throws JMSException If there is any problems with accessing to message
     *             properties.
     */
    @SuppressWarnings("unchecked")
    protected void copyPropertiesFromJMS(Message message, NormalizedMessage inMessage) throws JMSException {
        Enumeration<String> names = message.getPropertyNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            Object value = message.getObjectProperty(name);
            if (!isBlackListed(name)) {
                inMessage.setProperty(name, value);
            }
        }
    }

    /**
     * Copy properties from JBI message to JMS message.
     * 
     * @param outMessage Received JBI message.
     * @param message Created JMS message.
     * @throws JMSException If there is any problems with saving JMS message
     *             properties.
     */
    @SuppressWarnings("unchecked")
    protected void copyPropertiesFromNM(NormalizedMessage outMessage, Message message) throws JMSException {
        Set<String> names = outMessage.getPropertyNames();
        for (String name : names) {
            Object value = outMessage.getProperty(name);
            if (!isBlackListed(name)) {
                if (shouldIncludeHeader(name, value)) {
                    message.setObjectProperty(name, value);
                }
            }
        }
    }

    /**
     * checks whether the header property should be included or not
     * 
     * @param name the property name
     * @param value the property value
     * @return true if it should be copied
     */
    protected boolean shouldIncludeHeader(String name, Object value) {
        return (value instanceof Boolean || value instanceof Byte || value instanceof Short
                || value instanceof Integer || value instanceof Long || value instanceof Float
                || value instanceof Double || value instanceof String)
               && (!isNeedJavaIdentifiers() || isJavaIdentifier(name));
    }

    /**
     * checks if a property is on black list
     * 
     * @param name the property
     * @return true if on black list
     */
    public boolean isBlackListed(String name) {
        return (this.propertyBlackList != null && this.propertyBlackList.contains(name));
    }

    /**
     * checks if a property is a java identifier
     * 
     * @param s the property name
     * @return true if java identifier
     */
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

    /**
     * @return Returns the propertyBlackList.
     */
    public List<String> getPropertyBlackList() {
        return this.propertyBlackList;
    }

    /**
     * @param propertyBlackList The propertyBlackList to set.
     */
    public void setPropertyBlackList(List<String> propertyBlackList) {
        this.propertyBlackList = propertyBlackList;
    }

    /**
     * @return Returns the needJavaIdentifiers.
     */
    public boolean isNeedJavaIdentifiers() {
        return this.needJavaIdentifiers;
    }

    /**
     * @param needJavaIdentifiers The needJavaIdentifiers to set.
     */
    public void setNeedJavaIdentifiers(boolean needJavaIdentifiers) {
        this.needJavaIdentifiers = needJavaIdentifiers;
    }
}
