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
package org.apache.servicemix.drools.model;

import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.servicemix.expression.JAXPBooleanXPathExpression;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;

public class Message {

    private final static SourceTransformer TRANFORMER = new SourceTransformer();
    
    private final NormalizedMessage message;
    
    public Message(NormalizedMessage message) {
        this.message = message;
        // Make sure message is re-readable
        Source content = message.getContent();
        if (content != null) {
            try {
                content = new DOMSource(TRANFORMER.toDOMElement(content));
                message.setContent(content);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        
    }
    
    public NormalizedMessage getInternalMessage() {
        return this.message;
    }
    
    public boolean xpath(String xpath) throws Exception {
        JAXPBooleanXPathExpression expression = new JAXPBooleanXPathExpression(xpath);
        Boolean b = (Boolean) expression.evaluate(null, message);
        return b.booleanValue();
    }
    
    public Object getProperty(String name) {
        return message.getProperty(name);
    }
    
    public void setProperty(String name, Object value) {
        message.setProperty(name, value);
    }
    
}
