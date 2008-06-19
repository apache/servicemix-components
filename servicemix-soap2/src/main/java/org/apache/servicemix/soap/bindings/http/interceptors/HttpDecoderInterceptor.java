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
package org.apache.servicemix.soap.bindings.http.interceptors;

import java.io.InputStream;
import java.util.List;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;

import org.apache.servicemix.soap.api.Fault;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.api.model.Binding;
import org.apache.servicemix.soap.api.model.Operation;
import org.apache.servicemix.soap.api.model.wsdl2.Wsdl2Message.ContentModel;
import org.apache.servicemix.soap.bindings.http.HttpConstants;
import org.apache.servicemix.soap.bindings.http.interceptors.IriDecoderHelper.Param;
import org.apache.servicemix.soap.bindings.http.model.Wsdl2HttpBinding;
import org.apache.servicemix.soap.bindings.http.model.Wsdl2HttpMessage;
import org.apache.servicemix.soap.bindings.http.model.Wsdl2HttpOperation;
import org.apache.servicemix.soap.core.AbstractInterceptor;
import org.w3c.dom.Document;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class HttpDecoderInterceptor extends AbstractInterceptor {

    private final boolean server;
    
    public HttpDecoderInterceptor(boolean server) {
        this.server = server;
    }
    
    public void handleMessage(Message message) {
        Operation<?> operation = message.get(Operation.class);
        if (operation instanceof Wsdl2HttpOperation == false) {
            return;
        }
        Wsdl2HttpBinding httpBinding = (Wsdl2HttpBinding) message.get(Binding.class);
        Wsdl2HttpOperation httpOperation = (Wsdl2HttpOperation) operation;
        Wsdl2HttpMessage httpMessage;
        String serialization;
        if (server) {
            httpMessage = httpOperation.getInput();
            serialization = httpOperation.getHttpInputSerialization();
        } else {
            httpMessage = httpOperation.getOutput();
            serialization = httpOperation.getHttpOutputSerialization();
        }
        if (httpMessage.getContentModel() == ContentModel.ELEMENT ||
            httpMessage.getContentModel() == ContentModel.ANY) {
            // Serialization rules for XML
            if (HttpConstants.SERIAL_APP_URLENCODED.equals(serialization)) {
                normalizeUrlEncoded(message, httpBinding, httpOperation, httpMessage);
            } else if (HttpConstants.SERIAL_MULTIPART_FORMDATA.equals(serialization)) {
                normalizeFormData(message);
            } else {
                normalizeXml(message);
            }
        } else if (httpMessage.getContentModel() == ContentModel.NONE) {
            if (message.getContent(Source.class) != null) {
                throw new Fault("No content allowed");
            }
        } else {
            throw new Fault("Unsupported content model: " + httpMessage.getContentModel());
        }
    }

    private void normalizeXml(Message message) {
        // TODO Auto-generated method stub
        
    }

    private void normalizeFormData(Message message) {
        // TODO Auto-generated method stub
        
    }

    private void normalizeUrlEncoded(Message message, Wsdl2HttpBinding binding, Wsdl2HttpOperation httpOperation, Wsdl2HttpMessage httpMessage) {
        InputStream is = message.getContent(InputStream.class);
        String uri = message.getTransportHeaders().get(HttpConstants.REQUEST_URI);
        String loc = IriDecoderHelper.combine(binding.getLocation(), httpOperation.getHttpLocation());
        List<Param> params = IriDecoderHelper.decode(uri, loc, is);
        Document doc = IriDecoderHelper.buildDocument(httpMessage.getElementDeclaration(), params);
        message.setContent(Source.class, new DOMSource(doc));
    }

}
