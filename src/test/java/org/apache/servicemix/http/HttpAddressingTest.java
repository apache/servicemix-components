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

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.util.FileUtil;
import org.apache.servicemix.tck.SpringTestSupport;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.w3c.dom.Node;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;

public class HttpAddressingTest extends SpringTestSupport {

    private static Log logger =  LogFactory.getLog(HttpAddressingTest.class);

    public void testOk() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("http://test", "MyProviderService"));
        InputStream fis = getClass().getResourceAsStream("addressing-request.xml");
        me.getInMessage().setContent(new StreamSource(fis));
        client.sendSync(me);
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else if (me.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
        } else {
            Node node = new SourceTransformer().toDOMNode(me.getOutMessage());
            logger.info(new SourceTransformer().toString(node));
            assertEquals("myid", textValueOfXPath(node, "//*[local-name()='RelatesTo']"));
            assertNotNull(textValueOfXPath(node, "//*[local-name()='MessageID']"));
        }
    }
    
    public void testOkFromUrl() throws Exception {
        URLConnection connection = new URL("http://localhost:8192/Service/").openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        OutputStream os = connection.getOutputStream();
        // Post the request file.
        InputStream fis = getClass().getResourceAsStream("addressing-request.xml");
        FileUtil.copyInputStream(fis, os);
        // Read the response.
        InputStream is = connection.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtil.copyInputStream(is, baos);
        System.err.println(baos.toString());
    }
    
    public void testBad() throws Exception {
        // This test is bit weird, because the http consumer is not soap
        // so it will just forward the HTTP error
        //
        // TODO: note that WSA based faults are not created
        //
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("http://test", "MyProviderService"));
        InputStream fis = getClass().getResourceAsStream("bad-addressing-request.xml");
        me.getInMessage().setContent(new StreamSource(fis));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        logger.info(new SourceTransformer().toString(me.getFault().getContent()));
    }
    
    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("org/apache/servicemix/http/addressing.xml");
    }
    
}
