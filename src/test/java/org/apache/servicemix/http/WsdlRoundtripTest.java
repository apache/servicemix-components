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

import java.io.StringWriter;

import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Definition;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.soap.SOAPOperation;
import javax.wsdl.factory.WSDLFactory;

import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class WsdlRoundtripTest extends SpringTestSupport {

    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("org/apache/servicemix/http/wsdlroundtrip.xml");
    }
    
    public void test() throws Exception {
        WSDLFactory wsdlFactory = WSDLFactory.newInstance();
        Definition def = wsdlFactory.newWSDLReader().readWSDL("http://localhost:8192/Service?wsdl");
        StringWriter writer = new StringWriter();
        wsdlFactory.newWSDLWriter().writeWSDL(def, writer);
        System.err.println(writer.toString());
        Binding b = (Binding) def.getBindings().values().iterator().next();
        BindingOperation bop = (BindingOperation) b.getBindingOperations().iterator().next();
        assertEquals(1, bop.getExtensibilityElements().size());
        ExtensibilityElement ee = (ExtensibilityElement) bop.getExtensibilityElements().iterator().next();
        assertTrue(ee instanceof SOAPOperation);
        assertEquals("", ((SOAPOperation) ee).getSoapActionURI());
    }

}
