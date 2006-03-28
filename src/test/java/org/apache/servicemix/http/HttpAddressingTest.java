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

import java.io.InputStream;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
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
            if (me.getFault() != null) {
                fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
            } else if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else {
            Node node = new SourceTransformer().toDOMNode(me.getOutMessage());
            logger.info(new SourceTransformer().toString(node));
            assertEquals("myid", textValueOfXPath(node, "//*[local-name()='RelatesTo']"));
            assertNotNull(textValueOfXPath(node, "//*[local-name()='MessageID']"));
        }
    }
    
    public void testBad() throws Exception {
        DefaultServiceMixClient client = new DefaultServiceMixClient(jbi);
        InOut me = client.createInOutExchange();
        me.setService(new QName("http://test", "MyProviderService"));
        InputStream fis = getClass().getResourceAsStream("bad-addressing-request.xml");
        me.getInMessage().setContent(new StreamSource(fis));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ERROR, me.getStatus());
        assertNotNull(me.getFault());
        logger.info(new SourceTransformer().toString(me.getFault().getContent()));
    }
    
    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("org/apache/servicemix/http/addressing.xml");
    }
    
}
