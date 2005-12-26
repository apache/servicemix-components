/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.http;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.TransformerException;

import java.util.Iterator;

/**
 * A class which marshalls a client HTTP request to a NMS message
 *
 * @version $Revision$
 */
public class HttpClientMarshaler {

    protected SourceTransformer sourceTransformer = new SourceTransformer();

    public void toNMS(NormalizedMessage normalizedMessage, HttpMethod method) throws Exception {
        addNmsProperties(normalizedMessage, method);

        normalizedMessage.setContent(new StringSource(method.getResponseBodyAsString()));
    }

    public void fromNMS(PostMethod method, MessageExchange exchange, NormalizedMessage normalizedMessage) throws Exception, TransformerException {
        addHttpHeaders(method, exchange);
        String text = sourceTransformer.toString(normalizedMessage.getContent());
        method.setRequestEntity(new StringRequestEntity(text));
    }

    protected void addHttpHeaders(HttpMethod method, MessageExchange exchange) {
        for (Iterator iter = exchange.getPropertyNames().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            Object value = exchange.getProperty(name);
            if (shouldIncludeHeader(exchange, name, value)) {
                method.addRequestHeader(name, value.toString());
            }
        }
    }

    protected void addNmsProperties(NormalizedMessage message, HttpMethod method) {
        Header[] headers = method.getResponseHeaders();
        for (int i = 0; i < headers.length; i++) {
            Header header = headers[i];
            String name = header.getName();
            String value = header.getValue();
            message.setProperty(name, value);
        }
    }


    /**
     * Decides whether or not the given header should be included in the JMS message.
     * By default this includes all suitable typed values
     */
    protected boolean shouldIncludeHeader(MessageExchange exchange, String name, Object value) {
        return value instanceof String && 
                !"Content-Length".equalsIgnoreCase(name) &&
                !"Content-Type".equalsIgnoreCase(name);
    }
}
