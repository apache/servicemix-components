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
package org.apache.servicemix.soap.interceptors.xml;

import java.io.InputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.servicemix.soap.api.Fault;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.core.AbstractInterceptor;
import org.apache.servicemix.soap.util.stax.DOMStreamReader;
import org.apache.servicemix.soap.util.stax.ExtendedXMLStreamReader;
import org.apache.servicemix.soap.util.stax.StaxUtil;
import org.w3c.dom.Document;

/**
 * Creates an XMLStreamReader from the InputStream on the Message.
 * 
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class StaxInInterceptor extends AbstractInterceptor {

    public static final String ENCODING = "Encoding";

    public StaxInInterceptor() {
    }

    public void handleMessage(Message message) {
        try {
            XMLStreamReader reader;
            Document doc = message.getContent(Document.class);
            if (doc != null) {
                reader = new DOMStreamReader(doc.getDocumentElement());
            } else {
                InputStream is = message.getContent(InputStream.class);
                if (is != null) {
                    // TODO: where does encoding constant go?
                    String encoding = (String) message.get(ENCODING);
                    reader = StaxUtil.createReader(is, encoding);
                    reader = new ExtendedXMLStreamReader(reader);
                } else {
                    return;
                }
            }
            reader.nextTag();
            message.setContent(XMLStreamReader.class, reader);
        } catch (XMLStreamException e) {
            throw new Fault(e);
        }
    }

}
