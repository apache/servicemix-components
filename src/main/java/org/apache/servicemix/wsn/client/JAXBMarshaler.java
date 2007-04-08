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
package org.apache.servicemix.wsn.client;

import java.io.StringWriter;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.transform.Source;

import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.messaging.DefaultMarshaler;

public class JAXBMarshaler extends DefaultMarshaler {

    private JAXBContext context;

    public JAXBMarshaler(JAXBContext context) {
        this.context = context;
    }

    public JAXBContext getContext() {
        return context;
    }

    public void setContext(JAXBContext context) {
        this.context = context;
    }

    protected Object defaultUnmarshal(MessageExchange exchange, NormalizedMessage message) {
        try {
            Source content = message.getContent();
            return context.createUnmarshaller().unmarshal(content);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected Source asContent(NormalizedMessage message, Object body) {
        try {
            StringWriter writer = new StringWriter();
            context.createMarshaller().marshal(body, writer);
            return new StringSource(writer.toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void marshal(MessageExchange exchange, NormalizedMessage message, Object body) throws MessagingException {
        if (body instanceof Source) {
            message.setContent((Source) body);
        } else {
            Source content = asContent(message, body);
            message.setContent(content);
        }
    }
}
