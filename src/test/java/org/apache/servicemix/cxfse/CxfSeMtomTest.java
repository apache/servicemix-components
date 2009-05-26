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

import java.io.InputStream;

import javax.activation.DataHandler;
import javax.jbi.messaging.InOut;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.namespace.QName;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.messaging.NormalizedMessageImpl;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class CxfSeMtomTest extends SpringTestSupport {

    private DefaultServiceMixClient client;
    private InOut io;
    
    protected void setUp() throws Exception {
        super.setUp();
        client = new DefaultServiceMixClient(jbi);
        
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testMtom() throws Exception {
        io = client.createInOutExchange();
        io.setService(new QName("http://cxf.apache.org/mime", "TestMtomService"));
        io.setOperation(new QName("http://cxf.apache.org/mime", "testXop"));
        DataHandler dh = new DataHandler(new ByteArrayDataSource("foobar".getBytes(), 
            "application/octet-stream"));
        io.getInMessage().setContent(new StringSource(
                  "<message xmlns=\"http://java.sun.com/xml/ns/jbi/wsdl-11-wrapper\">"
                + "  <part>"
                + "<testXop xmlns=\"http://cxf.apache.org/mime/types\">" 
                        + "<name>call detail</name>" 
                        + "<attachinfo><xop:Include xmlns:xop=\"http://www.w3.org/2004/08/xop/include\" " 
                        + "href=\"cid:3f348f6c-aa77-406d-991c-4854786afb7b-1@cxf.apache.org\"/></attachinfo>" 
                        + "</testXop>"
                + "  </part>"
                + "</message>"));
        io.getInMessage().addAttachment("3f348f6c-aa77-406d-991c-4854786afb7b-1@cxf.apache.org", dh);
        client.sendSync(io);
             
        assertTrue(new SourceTransformer().contentToString(
                io.getOutMessage()).indexOf("call detailfoobar") >= 0);
        assertEquals(io.getOutMessage().getAttachmentNames().size(), 1);
        NormalizedMessageImpl out = (NormalizedMessageImpl) io.getOutMessage();
        dh = out.getAttachment((String) out.getAttachmentNames().iterator().next());
        InputStream bis = dh.getDataSource().getInputStream();
        byte b[] = new byte[10];
        bis.read(b, 0, 10);
        String attachContent = new String(b);
        assertEquals(attachContent, "testfoobar");
        client.done(io);
    }
    
    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("org/apache/servicemix/cxfse/mtom.xml");
    }

}
