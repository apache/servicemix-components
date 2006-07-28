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
package org.apache.servicemix.http;

import java.io.StringWriter;

import javax.servlet.http.HttpServletResponse;
import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.w3c.dom.Document;

public class WsdlRoundtripTest extends SpringTestSupport {

    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("org/apache/servicemix/http/wsdlroundtrip.xml");
    }
    
    public void test() throws Exception {
        GetMethod get = new GetMethod("http://localhost:8192/Service/?wsdl");
        int state = new HttpClient().executeMethod(get);
        assertEquals(HttpServletResponse.SC_OK, state);
        Document doc = (Document) new SourceTransformer().toDOMNode(new StringSource(get.getResponseBodyAsString()));
        
        // Test WSDL
        WSDLFactory factory = WSDLFactory.newInstance();
        WSDLReader reader = factory.newWSDLReader();
        Definition def;
        def = reader.readWSDL("http://localhost:8192/Service/?wsdl", doc);

        StringWriter writer = new StringWriter();
        factory.newWSDLWriter().writeWSDL(def, writer);
        System.err.println(writer.toString());
        Binding b = (Binding) def.getBindings().values().iterator().next();
        BindingOperation bop = (BindingOperation) b.getBindingOperations().iterator().next();
        assertEquals(1, bop.getExtensibilityElements().size());
        ExtensibilityElement ee = (ExtensibilityElement) bop.getExtensibilityElements().iterator().next();
        assertTrue(ee instanceof SOAPOperation);
        assertEquals("", ((SOAPOperation) ee).getSoapActionURI());
    }

}
