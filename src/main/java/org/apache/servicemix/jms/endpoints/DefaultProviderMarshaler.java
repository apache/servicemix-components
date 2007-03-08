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

import java.util.Map;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.servicemix.jbi.jaxp.SourceTransformer;

public class DefaultProviderMarshaler implements JmsProviderMarshaler {

    private Map<String, Object> jmsProperties;
    private SourceTransformer transformer = new SourceTransformer();
    
    /**
     * @return the jmsProperties
     */
    public Map<String, Object> getJmsProperties() {
        return jmsProperties;
    }

    /**
     * @param jmsProperties the jmsProperties to set
     */
    public void setJmsProperties(Map<String, Object> jmsProperties) {
        this.jmsProperties = jmsProperties;
    }

    public Message createMessage(MessageExchange exchange, NormalizedMessage in, Session session) throws Exception {
        TextMessage text = session.createTextMessage();
        text.setText(transformer.contentToString(in));
        if (jmsProperties != null) {
            for (Map.Entry<String, Object> e : jmsProperties.entrySet()) {
                text.setObjectProperty(e.getKey(), e.getValue());
            }
        }
        return text;
    }

    public Object getDestination(MessageExchange exchange) {
        // TODO Auto-generated method stub
        return null;
    }

}
