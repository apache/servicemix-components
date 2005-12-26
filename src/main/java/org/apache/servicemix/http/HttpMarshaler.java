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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Iterator;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.servicemix.components.util.MarshalerSupport;
import org.apache.servicemix.jbi.jaxp.StringSource;

/**
 * A class which marshalls a HTTP request to a NMS message
 * 
 * @version $Revision$
 */
public class HttpMarshaler extends MarshalerSupport {

    protected static final Source EMPTY_CONTENT = new StringSource("<payload/>");

    private String contentType = "text/xml";

    public void toNMS(MessageExchange exchange, NormalizedMessage inMessage, HttpServletRequest request) throws IOException, MessagingException {
        addNmsProperties(exchange, request);
        String method = request.getMethod();
        if (method != null && method.equalsIgnoreCase("POST")) {
            inMessage.setContent(new StreamSource(request.getInputStream()));
        }
        else {
            Enumeration enumeration = request.getParameterNames();
            while (enumeration.hasMoreElements()) {
                String name = (String) enumeration.nextElement();
                String value = request.getParameter(name);
                inMessage.setProperty(name, value);
            }
            inMessage.setContent(EMPTY_CONTENT);
        }
    }

    public void toResponse(MessageExchange exchange, NormalizedMessage message, HttpServletResponse response) throws IOException, TransformerException {
        if (message != null) {
            addHttpHeaders(response, message);
        }

        response.setContentType(contentType);
        getTransformer().toResult(message.getContent(), new StreamResult(response.getOutputStream()));
    }

    // Properties
    // -------------------------------------------------------------------------
    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    // Implementation methods
    // -------------------------------------------------------------------------
    protected void addNmsProperties(MessageExchange exchange, HttpServletRequest request) {
        Enumeration enumeration = request.getHeaderNames();
        while (enumeration.hasMoreElements()) {
            String name = (String) enumeration.nextElement();
            String value = request.getHeader(name);
            exchange.setProperty(name, value);
        }
    }
    
    protected void addHttpHeaders(HttpServletResponse response, NormalizedMessage normalizedMessage) {
        for (Iterator iter = normalizedMessage.getPropertyNames().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            Object value = normalizedMessage.getProperty(name);
            if (shouldIncludeHeader(normalizedMessage, name, value)) {
                response.setHeader(name, value.toString());
            }
        }
    }

    /**
     * Decides whether or not the given header should be included in the JMS
     * message. By default this includes all suitable typed values
     */
    protected boolean shouldIncludeHeader(NormalizedMessage normalizedMessage, String name, Object value) {
        return value instanceof String && 
        		!"Content-Length".equalsIgnoreCase(name) &&
        		!"Content-Type".equalsIgnoreCase(name);
    }

}
