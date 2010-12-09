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
package org.apache.servicemix.soap.bindings.soap.interceptors;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.servicemix.soap.api.InterceptorChain;
import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.bindings.soap.Soap11;
import org.apache.servicemix.soap.bindings.soap.SoapFault;
import org.apache.servicemix.soap.bindings.soap.SoapInterceptor;
import org.apache.servicemix.soap.bindings.soap.SoapVersion;
import org.apache.servicemix.soap.core.AbstractInterceptor;
import org.apache.servicemix.soap.core.MessageImpl;
import org.apache.servicemix.soap.core.PhaseInterceptorChain;
import org.apache.servicemix.soap.util.DomUtil;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

/**
 * @author <a href="mailto:gnodet [at] gmail.com">Guillaume Nodet</a>
 */
public class MustUnderstandInterceptorTest extends TestCase {

    static QName HEADER_QNAME = new QName("urn:test", "header");
    
    public void test() {
        PhaseInterceptorChain chain = new PhaseInterceptorChain();
        MustUnderstandInterceptor interceptor = new MustUnderstandInterceptor();
        SoapVersion soapVersion = Soap11.getInstance();
        
        Message message = new MessageImpl();
        message.put(InterceptorChain.class, chain);
        message.put(SoapVersion.class, soapVersion);
        message.getSoapHeaders().put(HEADER_QNAME, createHeader(HEADER_QNAME, soapVersion));
        
        try {
            interceptor.handleMessage(message);
            fail("A SoapFault should have been thrown");
        } catch (SoapFault fault) {
        }
        
        chain.add(new DummyInterceptor(HEADER_QNAME));
        try {
            interceptor.handleMessage(message);
        } catch (SoapFault fault) {
            fail("A SoapFault should not have been thrown");
        }
    }
    
    public DocumentFragment createHeader(QName name, SoapVersion soapVersion) {
        Document doc = DomUtil.createDocument();
        DocumentFragment df = doc.createDocumentFragment();
        Element e = DomUtil.createElement(df, name);
        e.setAttributeNS(soapVersion.getNamespace(), 
                         soapVersion.getPrefix() + ":" + soapVersion.getAttrNameMustUnderstand(), 
                         "true");
        return df;
    }
    
    class DummyInterceptor extends AbstractInterceptor implements SoapInterceptor {

        private QName header;
        public DummyInterceptor(QName name) {
            header = name;
        }
        public Collection<URI> getRoles() {
            return Collections.emptyList();
        }
        public Collection<QName> getUnderstoodHeaders() {
            return Collections.singleton(header);
        }
        public void handleMessage(Message message) {
        }
    }
    
}
