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

import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.core.AbstractInterceptor;
import org.apache.servicemix.soap.util.DomUtil;
import org.w3c.dom.Document;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class DomInInterceptor extends AbstractInterceptor {

    public DomInInterceptor() {
        addBefore(StaxInInterceptor.class.getName());
    }
    
    /**
     * Create a DOM document from an InputStream content.
     * 
     * @see org.apache.servicemix.soap.api.Interceptor#handleMessage(org.apache.servicemix.soap.api.Message)
     */
    public void handleMessage(Message message) {
        InputStream is = message.getContent(InputStream.class);
        if (is == null) {
            return;
        }
        Document document = DomUtil.parse(is);
        message.setContent(Document.class, document);
    }
    
}
