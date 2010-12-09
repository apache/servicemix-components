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
import org.springframework.context.support.AbstractXmlApplicationContext;

public class CxfSeMultipleOperationsTest extends CxfSeSpringTestSupport {

    private DefaultServiceMixClient client;
    private InOut io;

    protected void setUp() throws Exception {
        super.setUp();
        client = new DefaultServiceMixClient(jbi);

    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testMultipleOperations() throws Exception {
        io = client.createInOutExchange();
        io.setService(new QName("http://org.apache.servicemix.cxfse", "Greeter"));

        io.getMessage("in").setContent(new StringSource(
                "<jbi:message xmlns:jbi='http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper'>"
              + "<jbi:part> "
              + "<gr:GreetMe xmlns:gr='http://org.apache.servicemix.cxfse'>"
              + "<name>"
              + "Eugene"
              + "</name>"
              +	"</gr:GreetMe>"
              + "</jbi:part> "
              + "</jbi:message>"));

        client.sendSync(io);
        assertTrue(new SourceTransformer().contentToString(
                io.getOutMessage()).indexOf("<msg>Hello Eugene</msg>") >= 0);

        client.done(io);


        io = client.createInOutExchange();
        io.setService(new QName("http://org.apache.servicemix.cxfse", "Greeter"));

        io.getMessage("in").setContent(new StringSource(
                "<jbi:message xmlns:jbi='http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper'>"
              + "<jbi:part> "
              + "<gr:SayHi xmlns:gr='http://org.apache.servicemix.cxfse'/>"
              + "</jbi:part> "
              + "</jbi:message>"));

        client.sendSync(io);
        assertTrue(new SourceTransformer().contentToString(
                io.getOutMessage()).indexOf("<msg>Hi</msg>") >= 0);

        client.done(io);
    }


    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("org/apache/servicemix/cxfse/xbean_multiple-operations.xml");
    }

}
