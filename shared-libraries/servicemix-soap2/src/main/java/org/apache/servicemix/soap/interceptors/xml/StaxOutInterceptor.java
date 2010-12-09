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

import java.io.OutputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.servicemix.soap.api.Fault;
import org.apache.servicemix.soap.api.InterceptorChain;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.core.AbstractInterceptor;
import org.apache.servicemix.soap.util.stax.StaxUtil;

/**
 * Creates an XMLStreamReader from the InputStream on the Message.
 * 
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class StaxOutInterceptor extends AbstractInterceptor {

    public StaxOutInterceptor() {
    }

    public void handleMessage(Message message) {
        OutputStream os = message.getContent(OutputStream.class);
        if (os == null) {
            throw new NullPointerException("OutputStream content not found");
        }
        try {
            // TODO: handle encoding
            XMLStreamWriter writer = StaxUtil.createWriter(os);
            message.setContent(XMLStreamWriter.class, writer);
            InterceptorChain chain = message.get(InterceptorChain.class);
            chain.doIntercept(message);
            writer.flush();
        } catch (XMLStreamException e) {
            throw new Fault(e);
        }
    }

}
