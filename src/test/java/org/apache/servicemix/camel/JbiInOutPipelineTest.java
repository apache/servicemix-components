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

import java.util.List;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.namespace.QName;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.jaxp.StringSource;

/**
 * 
 */
public class JbiInOutPipelineTest extends JbiTestSupport {
    
    private static final String MESSAGE = "<just><a>test</a></just>";
    
    private static final String HEADER_ORIGINAL = "original";
    private static final String HEADER_TRANSFORMER = "transformer";    

    public void testPipelineWithMessageHeadersAndAttachements() throws Exception {
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        InOut exchange = client.createInOutExchange();
        exchange.setService(new QName("urn:test", "input"));
        exchange.getInMessage().setContent(new StringSource(MESSAGE));
        exchange.getInMessage().setProperty(HEADER_ORIGINAL, "my-original-header-value");
        client.send(exchange);
        assertNotNull("Expecting to receive a DONE/ERROR MessageExchange", client.receive(10000));
        client.done(exchange);
        assertEquals(ExchangeStatus.DONE, exchange.getStatus());
        
        // check the exchange message
        NormalizedMessage normalizedMessage = exchange.getOutMessage();
        assertNotNull(normalizedMessage);
        assertNull(normalizedMessage.getAttachment("test1.xml"));
        assertNull(normalizedMessage.getAttachment("test2.xml"));
        assertEquals(normalizedMessage.getAttachmentNames().size(), 0);
        assertNull(normalizedMessage.getProperty(HEADER_ORIGINAL));
        assertNotNull(normalizedMessage.getProperty(HEADER_TRANSFORMER));
        Thread.sleep(1000);
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
                from("jbi:service:urn:test:input")
                    .process(new Processor() {
                        public void process(Exchange exchange) throws Exception {
                            // do nothing here , just walk around the issue of CAMEL-1955
                        }
                    }) 
                    .to("jbi:service:urn:test:addAttachments?mep=in-out")
                    .to("jbi:service:urn:test:transformer?mep=in-out");
                    
                
                from("jbi:service:urn:test:transformer").process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        // let's copy everything
                        exchange.getOut().copyFrom(exchange.getIn());
                        // check the headers and add another one
                        assertNotNull(exchange.getOut().getHeader(HEADER_ORIGINAL));
                        assertNotNull(exchange.getOut().getAttachment("test1.xml"));
                        exchange.getOut().removeAttachment("test1.xml");
                        exchange.getOut().removeAttachment("test2.xml");
                        
                        exchange.getOut().setHeader(HEADER_TRANSFORMER, "my-transformer-header-value");
                        exchange.getOut().removeHeader(HEADER_ORIGINAL);
                    }                    
                });
                
                from("jbi:service:urn:test:addAttachments").process(new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        // let's copy everything
                        exchange.getOut().copyFrom(exchange.getIn());
                        // check the headers and add another one
                        exchange.getOut().addAttachment("test1.xml", new DataHandler(new FileDataSource("pom.xml")));
                        exchange.getOut().addAttachment("test2.xml", new DataHandler(new FileDataSource("pom.xml")));
                    }
                    
                });
            }
        };
    }
}