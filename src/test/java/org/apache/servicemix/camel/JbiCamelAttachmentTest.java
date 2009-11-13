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
package org.apache.servicemix.camel;

import java.io.File;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultExchange;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.jaxp.StringSource;

/**
 * Tests for attachment handling by servicemix-camel JBI component 
 */
public class JbiCamelAttachmentTest extends JbiTestSupport {
    
    private static final String ATTACHMENT_ID = "attach1";
    private static final File TEST_FILE = new File("src/test/resources/attachment.png");

    public void testPreserveAttachments() throws Exception {
        Exchange exchange = new DefaultExchange(camelContext);
        DataHandler attachment = new DataHandler(new FileDataSource(TEST_FILE));
        exchange.getIn().addAttachment(ATTACHMENT_ID, attachment);
        
        MockEndpoint mockA = getMockEndpoint("mock:a");
        mockA.expectedMessageCount(1);
        
        client.send("direct:a", exchange);
        
        mockA.assertIsSatisfied();
        Exchange received = mockA.getReceivedExchanges().get(0);
        assertNotNull(received.getIn().getAttachment(ATTACHMENT_ID));
        if (received.getIn().getAttachment(ATTACHMENT_ID).getDataSource() instanceof FileDataSource) {
            FileDataSource fds = (FileDataSource)received.getIn().getAttachment(ATTACHMENT_ID).getDataSource();
            assertEquals(TEST_FILE, fds.getFile());
        } else {
            fail("Expected a FileDataSource, but received a " + received.getIn().getAttachment(ATTACHMENT_ID).getDataSource().getClass());
        }
    }
    
    public void testGetAttachmentsFromCamelProcessor() throws Exception {
        InOut inout = getServicemixClient().createInOutExchange();
        inout.setService(new QName("urn:test", "inout-service"));
        inout.getInMessage().setContent(new StringSource("<request>Could I get an attachment, Mr. Camel?</request>"));
        getServicemixClient().sendSync(inout);
        assertEquals(ExchangeStatus.ACTIVE, inout.getStatus());
        inout.setStatus(ExchangeStatus.DONE);
        getServicemixClient().done(inout);
        assertNotNull(inout.getOutMessage().getAttachment(ATTACHMENT_ID));
        Thread.sleep(500);
    }

    @Override
    protected void appendJbiActivationSpecs(List<ActivationSpec> activationSpecList) {
        // no additional activation specs required
    }

    @Override
    protected RouteBuilder createRoutes() {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // from Camel to JBI...
                from("direct:a").to("jbi:service:urn:test:service");
                // ...and the other way around
                from("jbi:service:urn:test:service").to("mock:a");
                
                // and now with a Camel processor to add the attachment
                from("jbi:service:urn:test:inout-service").process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        exchange.getOut().setBody(exchange.getIn().getBody());
                        exchange.getOut().addAttachment(ATTACHMENT_ID, new DataHandler(new FileDataSource(TEST_FILE)));
                    }                    
                });
            }
        };
    }
}
