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

import java.io.File;
import java.net.URI;
import java.net.URL;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.w3c.dom.Document;

public class HttpXBeanDeployerTest extends TestCase {

    private static Log logger =  LogFactory.getLog(HttpSpringTest.class);

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

    public void test() throws Exception {
        // HTTP Component
        HttpComponent component = new HttpComponent();
        container.activateComponent(component, "HTTPComponent");
        
        // Add a receiver component
        ActivationSpec asEcho = new ActivationSpec("echo", new EchoComponent() {
            public Document getServiceDescription(ServiceEndpoint endpoint) {
                try {
                    Definition def = WSDLFactory.newInstance().newDefinition();
                    PortType type = def.createPortType();
                    type.setUndefined(false);
                    type.setQName(new QName("http://test", "MyConsumerInterface"));
                    Binding binding = def.createBinding();
                    binding.setQName(new QName("http://test", "MyConsumerBinding"));
                    binding.setUndefined(false);
                    binding.setPortType(type);
                    Service svc = def.createService();
                    svc.setQName(new QName("http://test", "MyConsumerService"));
                    Port port = def.createPort();
                    port.setBinding(binding);
                    port.setName("myConsumer");
                    svc.addPort(port);
                    def.setTargetNamespace("http://test");
                    def.addNamespace("tns", "http://test");
                    def.addPortType(type);
                    def.addBinding(binding);
                    def.addService(svc);
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
        
        // Start container
        container.start();

        // Deploy SU
        URL url = getClass().getClassLoader().getResource("xbean/xbean.xml");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("xbean", path.getAbsolutePath());
        component.getServiceUnitManager().start("xbean");
        
        // Test wsdls
        assertNotNull(container.getRegistry().getEndpointDescriptor(
                container.getRegistry().getEndpoint(
                        new QName("http://test", "MyProviderService"), "myProvider")));
        assertNotNull(container.getRegistry().getEndpointDescriptor(
                container.getRegistry().getExternalEndpointsForService(
                        new QName("http://test", "MyConsumerService"))[0]));
        assertNotNull(container.getRegistry().getEndpointDescriptor(
                container.getRegistry().getExternalEndpointsForService(
                        new QName("http://test", "MySoapService"))[0]));
        
        // Test 
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setService(new QName("http://test", "MyProviderService"));
        me.getInMessage().setContent(new StringSource("<echo xmlns='http://test'><echoin0>world</echoin0></echo>"));
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
            logger.info(new SourceTransformer().toString(me.getOutMessage().getContent()));
        }
    }
    
}
