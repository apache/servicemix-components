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
package org.apache.servicemix.cxfse;

import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class CxfSeXmlBeansTest extends CxfSeSpringTestSupport {

    private DefaultServiceMixClient client;
    private InOut io;
    
    protected void setUp() throws Exception {
        super.setUp();
        client = new DefaultServiceMixClient(jbi);
        
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testXmlBeans() throws Exception {
        client = new DefaultServiceMixClient(jbi);
        io = client.createInOutExchange();
        io.setService(new QName("http://apache.org/hello_world_soap_http/xmlbeans", "SOAPService"));
        io.setInterfaceName(new QName("http://apache.org/hello_world_soap_http/xmlbeans", "Greeter"));
        io.setOperation(new QName("http://apache.org/hello_world_soap_http/xmlbeans", "greetMe"));
        io.getMessage("in").setContent(new StringSource(
                "<message xmlns='http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper'>"
              + "<part> "
              + "<greetMe xmlns='http://apache.org/hello_world_soap_http/xmlbeans/types'><requestType>"
              + "xmlbeans"
              + "</requestType></greetMe>"
              + "</part> "
              + "</message>"));
        client.sendSync(io);
       
        assertTrue(new SourceTransformer().contentToString(
              io.getMessage("out")).indexOf("Hello xmlbeans") > 0);
        client.done(io);
    }
    
    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("org/apache/servicemix/cxfse/xmlbeans.xml");
    }

}
