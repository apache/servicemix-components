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
package org.apache.servicemix.cxfbc.ws.addressing;

import java.io.IOException;

import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import org.apache.cxf.testutil.common.ServerLauncher;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.cxfbc.provider.MyServer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class CxfBcProviderAddressingTest extends SpringTestSupport {

    private DefaultServiceMixClient client;

    private InOut io;

    private ServerLauncher sl;

    protected void setUp() throws Exception {
        super.setUp();
        assertTrue("Server failed to launch",
        // run the server in another process
                // set this to false to fork
                launchServer(MyServer.class, false));
    }

    public void testMAPs() throws Exception {
        client = new DefaultServiceMixClient(jbi);
        io = client.createInOutExchange();
        io.setService(new QName(
                "http://apache.org/hello_world_soap_http_provider",
                "SOAPService"));
        io.setInterfaceName(new QName(
                "http://apache.org/hello_world_soap_http_provider", "Greeter"));
        io.setOperation(new QName(
                "http://apache.org/hello_world_soap_http_provider", "sayHi"));
        // send message to proxy
        io.getInMessage().setContent(
                new StringSource(
                        "<message xmlns='http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper'>"
                                + "<part> " + "<sayHi/>" + "</part> "
                                + "</message>"));
        client.sendSync(io);
        if (io.getFault() != null) {
            fail("Failed to process request: "
                    + new SourceTransformer().contentToString(io.getFault()));
        }
        assertTrue(new SourceTransformer().contentToString(io.getOutMessage())
                .indexOf("Hello") >= 0);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        try {
            sl.stopServer();
        } catch (IOException ex) {
            ex.printStackTrace();
            fail("failed to stop server " + sl.getClass());
        }
    }

    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext(
                "org/apache/servicemix/cxfbc/ws/addressing/xbean_provider_with_bus_http_conduit.xml");
    }

    public boolean launchServer(Class<?> clz, boolean inProcess) {
        boolean ok = false;
        try {
            sl = new ServerLauncher(clz.getName(), inProcess);
            ok = sl.launchServer();
            assertTrue("server failed to launch", ok);

        } catch (IOException ex) {
            ex.printStackTrace();
            fail("failed to launch server " + clz);
        }

        return ok;
    }

}
