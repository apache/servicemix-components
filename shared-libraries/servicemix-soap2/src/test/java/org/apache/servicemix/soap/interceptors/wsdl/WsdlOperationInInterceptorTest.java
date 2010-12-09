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
package org.apache.servicemix.soap.interceptors.wsdl;

import java.io.ByteArrayInputStream;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;

import junit.framework.TestCase;

import org.apache.servicemix.soap.api.Message;
import org.apache.servicemix.soap.api.model.Binding;
import org.apache.servicemix.soap.api.model.Operation;
import org.apache.servicemix.soap.bindings.soap.impl.Wsdl1SoapBindingImpl;
import org.apache.servicemix.soap.bindings.soap.impl.Wsdl1SoapMessageImpl;
import org.apache.servicemix.soap.bindings.soap.impl.Wsdl1SoapOperationImpl;
import org.apache.servicemix.soap.core.MessageImpl;
import org.apache.servicemix.soap.util.stax.StaxUtil;

public class WsdlOperationInInterceptorTest extends TestCase {

    private Binding<?> binding;
    private WsdlOperationInInterceptor interceptor;
    
    protected void setUp() throws Exception {
        Wsdl1SoapBindingImpl b = new Wsdl1SoapBindingImpl();
        Wsdl1SoapOperationImpl o1 = new Wsdl1SoapOperationImpl();
        Wsdl1SoapMessageImpl input = new Wsdl1SoapMessageImpl();
        input.setElementName(new QName("hello"));
        o1.setInput(input);
        b.addOperation(o1);
        
        binding = b;
        interceptor = new WsdlOperationInInterceptor();
    }
    
    public void test() throws Exception {
        String str = "<hello />";
        Message message = createDefaultMessage(str);
        interceptor.handleMessage(message);
        
        assertNotNull(message.get(Operation.class));
    }

    private Message createDefaultMessage(String str) throws Exception {
        Message message = new MessageImpl();
        message.put(Binding.class, binding);
        XMLStreamReader reader = StaxUtil.createReader(new ByteArrayInputStream(str.getBytes()));
        message.setContent(XMLStreamReader.class, reader);
        reader.nextTag();
        return message;
    }
    
}
