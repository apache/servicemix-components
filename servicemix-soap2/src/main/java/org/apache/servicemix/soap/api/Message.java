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
package org.apache.servicemix.soap.api;

import java.util.Map;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;

import org.w3c.dom.DocumentFragment;


public interface Message extends Map<String, Object> {

    public static final String CONTENT_TYPE = "Content-Type";
    
    /**
     * Acces to attachments
     * @return
     */
    Map<String, DataHandler> getAttachments();

    /**
     * Retreive the encapsulated content as a particular type (a result type
     * if message is outbound, a source type if message is inbound)
     * 
     * @param format the expected content format 
     * @return the encapsulated content
     */    
    <T> T getContent(Class<T> format);

    /**
     * Provide the encapsulated content as a particular type (a result type
     * if message is outbound, a source type if message is inbound)
     * 
     * @param format the provided content format 
     * @param content the content to be encapsulated
     */    
    <T> void setContent(Class<T> format, Object content);
    
    /**
     * Convenience method for storing/retrieving typed objects from the map.
     * equivilent to:  (T)get(key.getName());
     * @param <T> key
     * @return
     */
    <T> T get(Class<T> key);
    
    /**
     * Convenience method for storing/retrieving typed objects from the map.
     * equivilent to:  put(key.getName(), value);
     * @param <T> key
     * @return
     */
    <T> void put(Class<T> key, T value);
    
    /**
     * Access to the transport level headers
     * @return
     */
    Map<String, String> getTransportHeaders();
    
    /**
     * Access to soap headers
     * @return
     */
    Map<QName, DocumentFragment> getSoapHeaders();

}
