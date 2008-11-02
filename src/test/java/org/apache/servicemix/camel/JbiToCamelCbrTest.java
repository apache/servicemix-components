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

import java.io.StringReader;
import java.util.List;

import javax.jbi.messaging.InOnly;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.converter.jaxp.StringSource;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.client.ServiceMixClient;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;

/**
 * Tests on correct handling of several XML Source implementations being sent by ServiceMix to Camel  
 */
public class JbiToCamelCbrTest extends JbiTestSupport {
    
    private static final String MESSAGE_IN_FRENCH = "<message>bonjour</message>";
    private static final String MESSAGE_IN_ENGLISH = "<message>hello</message>";
    private final SourceTransformer transformer = new SourceTransformer();
    private Level level = null;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        // let's disable DEBUG logging for ServiceMix or all message content will be DOMSource anyway
        if (level == null) {
            level = Logger.getLogger("org.apache.servicemix").getLevel();
            Logger.getLogger("org.apache.servicemix").setLevel(Level.INFO);
        }
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        // restore the original log level
        if (level != null) {
            Logger.getLogger("org.apache.servicemix").setLevel(level);
            level = null;
        }
    }

    public void testCbrWithStringSource() throws Exception {
        doTestCbr(new StringSource(MESSAGE_IN_FRENCH),
                  new StringSource(MESSAGE_IN_ENGLISH));
    }
    
    public void testCbrWithStreamSource() throws Exception {
        doTestCbr(new StreamSource(new StringReader(MESSAGE_IN_FRENCH)),
                  new StreamSource(new StringReader(MESSAGE_IN_ENGLISH)));
    }
    
    public void testCbrWithDomSource() throws Exception {
        doTestCbr(transformer.toDOMSource(new StringSource(MESSAGE_IN_FRENCH)),
                  transformer.toDOMSource(new StringSource(MESSAGE_IN_ENGLISH)));
    }
    
    public void doTestCbr(Source... bodies) throws Exception {
        MockEndpoint french = getMockEndpoint("mock:french");
        french.expectedMessageCount(1);
        MockEndpoint english = getMockEndpoint("mock:english");
        english.expectedMessageCount(1);
        
        ServiceMixClient client = new DefaultServiceMixClient(jbiContainer);
        for (Source body : bodies) {
            InOnly exchange = client.createInOnlyExchange();
            exchange.setService(new QName("urn:test", "polyglot"));
            
            exchange.getInMessage().setContent(body);
            client.sendSync(exchange);
        }
        
        french.assertIsSatisfied();
        english.assertIsSatisfied();
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
                streamCaching(); // remove streamCaching and the conversion to String once we use Camel 1.5
                from("jbi:service:urn:test:polyglot").streamCaching().convertBodyTo(String.class)
                    .choice()
                        .when().xpath("/message/text() = 'bonjour'").to("mock:french")
                        .when().xpath("/message/text() = 'hello'").to("mock:english");
            }
        };
    }    

}
