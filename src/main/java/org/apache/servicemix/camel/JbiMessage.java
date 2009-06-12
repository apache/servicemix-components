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
package org.apache.servicemix.camel;

import java.util.HashMap;
import java.util.Map;

import javax.activation.DataHandler;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.Source;

import org.apache.camel.impl.DefaultMessage;

/**
 * A JBI {@link org.apache.camel.Message} which provides access to the underlying JBI features
 * such as {@link #getNormalizedMessage()}
 *
 * @version $Revision: 563665 $
 */
public class JbiMessage extends DefaultMessage {
    private NormalizedMessage normalizedMessage;

    public JbiMessage() {
    }

    public JbiMessage(NormalizedMessage normalizedMessage) {
        super();
        setNormalizedMessage(normalizedMessage);
    }

    @Override
    public String toString() {
        if (normalizedMessage != null) {
            return "JbiMessage: " + toString(normalizedMessage);
        } else {
            return "JbiMessage: " + getBody();
        }
    }

    @Override
    public JbiExchange getExchange() {
        return (JbiExchange) super.getExchange();
    }

    /**
     * Returns the underlying JBI message
     *
     * @return the underlying JBI message
     */
    public NormalizedMessage getNormalizedMessage() {
        return normalizedMessage;
    }

    protected final void setNormalizedMessage(NormalizedMessage normalizedMessage) {
        this.normalizedMessage = normalizedMessage;
        // copy all properties into the Camel Message
        if (normalizedMessage != null) {
            getHeaders().putAll(getHeaders(normalizedMessage));
            getAttachments().putAll(getAttachments(normalizedMessage));
        }
    }

    @Override
    public Object getHeader(String name) {
        Object answer = null;
        if (normalizedMessage != null) {
            answer = normalizedMessage.getProperty(name);
        }
        if (answer == null) {
            answer = super.getHeader(name);
        }
        return answer;
    }
    
    @Override
    protected Map<String, Object> createHeaders() {
        return new HashMap<String, Object>() {
            @Override
            public Object put(String key, Object value) {
                if (normalizedMessage != null) {
                    normalizedMessage.setProperty(key, value);
                }
                return super.put(key, value);
            }
        };
    }

    @Override
    public DataHandler getAttachment(String id) {
        DataHandler answer = null;
        if (normalizedMessage != null) {
            answer = normalizedMessage.getAttachment(id);
        }
        if (answer == null) {
            answer = super.getAttachment(id);
        }
        return answer;
    }
    
    @Override
    protected Map<String, DataHandler> createAttachments() {
        return new HashMap<String, DataHandler>() {
            @Override
            public DataHandler put(String key, DataHandler value) {
                if (normalizedMessage != null) {
                    try {
                        normalizedMessage.addAttachment(key, value);
                    } catch (MessagingException e) {
                        throw new JbiException(e);
                    }
                }
                return super.put(key, value);
            }
        };
    }

    @Override
    public JbiMessage newInstance() {
        return new JbiMessage();
    }

    @Override
    protected Object createBody() {
        if (normalizedMessage != null) {
            return getExchange().getBinding().extractBodyFromJbi(getExchange(), normalizedMessage);
        }
        return null;
    }

//    @Override
    public void setBody(Object body) {
        if (normalizedMessage != null) {
            Source source = getExchange().getBinding().convertBodyToJbi(getExchange(), body);
            if (source != null) {
                try {
                    normalizedMessage.setContent(source);
                } catch (MessagingException e) {
                    throw new JbiException(e);
                }
            }
        }
        super.setBody(body);
    }
    
    /*
     * Avoid use of normalizedMessage.toString() because it may iterate over the NormalizedMessage headers
     * after it has been sent through the NMR
     */
    private String toString(NormalizedMessage message) {
        return String.format("NormalizedMessage@%s(%s)", 
                             Integer.toHexString(message.hashCode()), message.getContent());
    }
    
    private Map<? extends String, ? extends Object> getHeaders(NormalizedMessage message) {
        Map<String, Object> headers = new HashMap<String, Object>();
        for (Object key : message.getPropertyNames()) {
            headers.put(key.toString(), message.getProperty(key.toString()));
        }
        return headers;
    }
    
    private Map<? extends String, ? extends DataHandler> getAttachments(NormalizedMessage message) {
        Map<String, DataHandler> attachments = new HashMap<String, DataHandler>();
        for (Object name : message.getAttachmentNames()) {
            attachments.put(name.toString(), message.getAttachment(name.toString()));
        }
        return attachments;
    }
}
