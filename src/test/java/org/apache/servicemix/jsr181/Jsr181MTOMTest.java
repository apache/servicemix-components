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
package org.apache.servicemix.jsr181;

import javax.activation.DataHandler;
import javax.jbi.messaging.InOut;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.Destination;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.util.ByteArrayDataSource;
import org.w3c.dom.Document;

public class Jsr181MTOMTest extends TestCase {

    protected JBIContainer container;
    
    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        container.setMonitorInstallationDirectory(false);
        container.setNamingContext(new InitialContext());
        container.setEmbedded(true);
        container.init();
    }
    
    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
    }

    public void testMtom() throws Exception {
        Jsr181SpringComponent jsr181 = new Jsr181SpringComponent();
        Jsr181Endpoint ep = new Jsr181Endpoint();
        ep.setPojo(new EchoWithAttachment());
        ep.setMtomEnabled(true);
        jsr181.setEndpoints(new Jsr181Endpoint[] { ep });
        
        container.activateComponent(jsr181, "jsr181");
        container.start();
        
        QName service = new QName("http://jsr181.servicemix.apache.org", "EchoWithAttachment");
        ServiceEndpoint se = container.getRegistry().getEndpointsForService(service)[0];
        Document doc = container.getRegistry().getEndpointDescriptor(se);
        String wsdl = new SourceTransformer().toString(doc);
        System.err.println(wsdl);
        
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        Destination dest = client.createDestination("service:http://jsr181.servicemix.apache.org/EchoWithAttachment");
        InOut me = dest.createInOutExchange();
        me.getInMessage().setContent(new StringSource("<echo xmlns:xop='http://www.w3.org/2004/08/xop/include'><msg>hello world</msg><binary><xop:Include href='binary'/></binary></echo>"));
        me.getInMessage().addAttachment("binary", new DataHandler(new ByteArrayDataSource(new byte[] { 0, 1 , 2}, "image/jpg")));
        
        client.sendSync(me);
        
    }
    
    public static class EchoWithAttachment {
        
        /*
         * Do not use byte[] until xfire-1.2
        public String echo(String msg, byte[] binary) {
            if (binary == null || binary.length == 0) {
                throw new NullPointerException("binary is null");
            }
            return "Echo: " + msg;
        }
        */
        
        public String echo(String msg, DataHandler binary) {
            if (binary == null || binary.getDataSource() == null) {
                throw new NullPointerException("binary is null");
            }
            return "Echo: " + msg;
        }
    }
    
}
