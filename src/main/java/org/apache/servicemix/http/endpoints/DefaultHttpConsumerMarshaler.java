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
package org.apache.servicemix.http.endpoints;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;

import org.apache.servicemix.jbi.jaxp.StAXSourceTransformer;
import org.apache.servicemix.jbi.jaxp.XMLStreamHelper;
import org.apache.servicemix.jbi.messaging.MessageExchangeSupport;

/**
 * The default consumer marshaler used for non-soap consumer endpoints.
 * 
 * @author gnodet
 * @since 3.2
 */
public class DefaultHttpConsumerMarshaler implements HttpConsumerMarshaler {
    
    private StAXSourceTransformer transformer = new StAXSourceTransformer();
    private URI defaultMep;

    public DefaultHttpConsumerMarshaler() {
        this(MessageExchangeSupport.IN_OUT);
    }
    
    public DefaultHttpConsumerMarshaler(URI defaultMep) {
        this.defaultMep = defaultMep;
    }
    
    public URI getDefaultMep() {
        return defaultMep;
    }

    public void setDefaultMep(URI defaultMep) {
        this.defaultMep = defaultMep;
    }

    public MessageExchange createExchange(HttpServletRequest request, ComponentContext context) throws Exception {
        MessageExchange me;
        me = context.getDeliveryChannel().createExchangeFactory().createExchange(getDefaultMep());
        NormalizedMessage in = me.createMessage();
        in.setContent(new StreamSource(request.getInputStream()));
        me.setMessage(in, "in");
        return me;
    }

    public void sendOut(MessageExchange exchange, NormalizedMessage outMsg, HttpServletRequest request, HttpServletResponse response) throws Exception {
        XMLStreamReader reader = transformer.toXMLStreamReader(outMsg.getContent());
        XMLStreamWriter writer = transformer.getOutputFactory().createXMLStreamWriter(response.getWriter());
        writer.writeStartDocument();
        XMLStreamHelper.copy(reader, writer);
        writer.writeEndDocument();
        writer.flush();
        response.setStatus(HttpServletResponse.SC_OK);
    }
    
    public void sendFault(MessageExchange exchange, Fault fault, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        XMLStreamReader reader = transformer.toXMLStreamReader(fault.getContent());
        XMLStreamWriter writer = transformer.getOutputFactory().createXMLStreamWriter(response.getWriter());
        XMLStreamHelper.copy(reader, writer);
    }

    public void sendError(MessageExchange exchange, Exception error, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        XMLStreamWriter writer = transformer.getOutputFactory().createXMLStreamWriter(response.getWriter());
        writer.writeStartDocument();
        writer.writeStartElement("error");
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        error.printStackTrace(pw);
        pw.close();
        writer.writeCData(sw.toString());
        writer.writeEndElement();
        writer.writeEndDocument();
    }
    
    public void sendAccepted(MessageExchange exchange, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_ACCEPTED);
    }

}
