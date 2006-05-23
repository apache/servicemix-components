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

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.servlet.http.HttpServletResponse;
import javax.wsdl.Definition;
import javax.wsdl.PortType;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.w3c.dom.Document;

public class HttpWsdlTest extends TestCase {

    protected JBIContainer container;
    
    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        container.setEmbedded(true);
        container.init();
    }
    
    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
    }

    public void testWithNonStandaloneWsdl() throws Exception {
        // Add a receiver component
        ActivationSpec asEcho = new ActivationSpec("echo", new EchoComponent() {
            public Document getServiceDescription(ServiceEndpoint endpoint) {
                try {
                    Definition def = WSDLFactory.newInstance().newDefinition();
                    PortType type = def.createPortType();
                    type.setUndefined(false);
                    type.setQName(new QName("http://porttype.test", "MyConsumerInterface"));
                    def.setTargetNamespace("http://porttype.test");
                    def.addNamespace("tns", "http://porttype.test");
                    def.addPortType(type);
                    Document doc = WSDLFactory.newInstance().newWSDLWriter().getDocument(def);
                    return doc;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
        asEcho.setEndpoint("myConsumer");
        asEcho.setService(new QName("http://test", "MyConsumerService"));
        container.activateComponent(asEcho);
        
        // HTTP Component
        HttpEndpoint ep = new HttpEndpoint();
        ep.setService(new QName("http://test", "MyConsumerService"));
        ep.setEndpoint("myConsumer");
        ep.setRoleAsString("consumer");
        ep.setLocationURI("http://localhost:8195/Service/");
        HttpSpringComponent http = new HttpSpringComponent();
        http.setEndpoints(new HttpEndpoint[] { ep });
        container.activateComponent(http, "HttpWsdlTest");
        
        // Start container
        container.start();

        GetMethod get = new GetMethod("http://localhost:8195/Service/?wsdl");
        int state = new HttpClient().executeMethod(get);
        assertEquals(HttpServletResponse.SC_OK, state);
        Document doc = (Document) new SourceTransformer().toDOMNode(new StringSource(get.getResponseBodyAsString()));
        
        // Test WSDL
        WSDLFactory factory = WSDLFactory.newInstance();
        WSDLReader reader = factory.newWSDLReader();
        Definition def;
        def = reader.readWSDL("http://localhost:8195/Service/?wsdl", doc);
        assertNotNull(def);
        assertNotNull(def.getImports());
        assertEquals(1, def.getImports().size());
    }
    
}
