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
package org.apache.servicemix.cxfbc.interceptors;

import java.io.InputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.DepthXMLStreamReader;
import org.apache.cxf.staxutils.StaxUtils;

public class RetrievePayLoadInterceptor extends AbstractPhaseInterceptor<Message> {

    public RetrievePayLoadInterceptor() {
        super(Phase.POST_STREAM);
    }
    
    public void handleMessage(Message message) throws Fault {
        InputStream is = message.getContent(InputStream.class);
        XMLStreamReader xmlReader = null;
        if (is != null) {
            StreamSource bodySource = new StreamSource(message.getContent(InputStream.class));
            xmlReader = StaxUtils.createXMLStreamReader(bodySource);
            findBody(message, xmlReader);
            message.setContent(XMLStreamReader.class, xmlReader);
        }
    }

    private void findBody(Message message, XMLStreamReader xmlReader) {
        DepthXMLStreamReader reader = new DepthXMLStreamReader(xmlReader);
        try {
            int depth = reader.getDepth();
            int event = reader.getEventType();
            while (reader.getDepth() >= depth && reader.hasNext()) {
                QName name = null;
                if (event == XMLStreamReader.START_ELEMENT) {
                    name = reader.getName();
                }
                if (event == XMLStreamReader.START_ELEMENT && name.equals(((SoapMessage)message).getVersion().getBody())) {
                    reader.nextTag();
                    return;
                }
                event = reader.next();
            }
            return;
        } catch (XMLStreamException e) {
            throw new RuntimeException("Couldn't parse stream.", e);
        }
    }
}
