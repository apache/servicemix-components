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
package org.apache.servicemix.soap.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;

import org.apache.servicemix.soap.api.Message;
import org.w3c.dom.DocumentFragment;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class MessageImpl extends HashMap<String, Object> implements Message {
    
    /**
     * Generated serial version UID 
     */
    private static final long serialVersionUID = 6062413098383260636L;
    
    private Map<String, DataHandler> attachments = new HashMap<String, DataHandler>();
    private Map<Class<?>, Object> contents = new HashMap<Class<?>, Object>();
    private Map<QName, DocumentFragment> soapHeaders = new HashMap<QName, DocumentFragment>();
    private Map<String, String> transportHeaders = new HashMap<String, String>();
    
    public Map<String, DataHandler> getAttachments() {
        return attachments;
    }

    public String getAttachmentMimeType() {
        //for sub class overriding
        return null;
    }
    
    public <T> T getContent(Class<T> format) {
        return format.cast(contents.get(format));
    }

    public <T> void setContent(Class<T> format, Object content) {
        contents.put(format, content);
    }

    public Set<Class<?>> getContentFormats() {
        return contents.keySet();
    }

    public <T> T get(Class<T> key) {
        return key.cast(get(key.getName()));
    }

    public <T> void put(Class<T> key, T value) {
        put(key.getName(), value);
    }

    public Map<QName, DocumentFragment> getSoapHeaders() {
        return soapHeaders;
    }

    public Map<String, String> getTransportHeaders() {
        return transportHeaders;
    }
}
