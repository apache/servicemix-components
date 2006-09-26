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

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.EventListener;

import junit.framework.TestCase;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.components.util.EchoComponent;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.util.DOMUtil;
import org.apache.servicemix.jbi.view.DotViewEndpointListener;
import org.apache.servicemix.tck.ReceiverComponent;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;

import com.sun.org.apache.xpath.internal.CachedXPathAPI;

public class Jsr181ComplexPojoTest extends TestCase {

    //private static Log logger =  LogFactory.getLog(Jsr181ComponentTest.class);
    
    protected JBIContainer container;
    protected SourceTransformer transformer = new SourceTransformer();
    private boolean useJmx;
    protected EventListener[] listeners;
    

    public static void main(String[] args) {
        Jsr181ComplexPojoTest test = new Jsr181ComplexPojoTest();
        test.useJmx = true;
        test.listeners = new EventListener[] { new DotViewEndpointListener() };
        
        try {
            test.setUp();
            test.testComplexOneWay();
            
            // now lets wait for the user to terminate things
            System.out.println("Press enter to terminate the program.");
            System.out.println("In the meantime you can use your JMX console to view the current MBeans");
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            reader.readLine();

            
            test.tearDown();
        }
        catch (Exception e) {
            System.err.println("Caught: " + e);
            e.printStackTrace();
        }
    }
    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setUseMBeanServer(useJmx);
        container.setCreateMBeanServer(useJmx);
        container.setMonitorInstallationDirectory(false);
        container.setNamingContext(new InitialContext());
        container.setEmbedded(true);
        container.setFlowName("st");
        if (listeners != null) {
            container.setListeners(listeners);
        }
        container.init();
    }
    
    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
    }
    
    public void testComplexOneWay() throws Exception {
        Jsr181SpringComponent component = new Jsr181SpringComponent();
        Jsr181Endpoint endpoint = new Jsr181Endpoint();
        endpoint.setPojo(new ComplexPojoImpl());
        endpoint.setServiceInterface(ComplexPojo.class.getName());
        component.setEndpoints(new Jsr181Endpoint[] { endpoint });
        container.activateComponent(component, "JSR181Component");
        
        ReceiverComponent receiver = new ReceiverComponent();
        container.activateComponent(receiver, "Receiver");
        
        container.start();

        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setInterfaceName(new QName("http://jsr181.servicemix.apache.org", "ComplexPojoPortType"));
        me.getInMessage().setContent(new StringSource("<oneWay xmlns='http://jsr181.servicemix.apache.org'><in0>world</in0></oneWay>"));
        client.sendSync(me);
        
        // Wait all acks being processed
        Thread.sleep(100);
    }
    
    public void testComplexTwoWay() throws Exception {
        Jsr181SpringComponent component = new Jsr181SpringComponent();
        Jsr181Endpoint endpoint = new Jsr181Endpoint();
        endpoint.setPojo(new ComplexPojoImpl());
        endpoint.setServiceInterface(ComplexPojo.class.getName());
        component.setEndpoints(new Jsr181Endpoint[] { endpoint });
        container.activateComponent(component, "JSR181Component");
        
        EchoComponent echo = new EchoComponent();
        ActivationSpec as = new ActivationSpec();
        as.setComponent(echo);
        as.setService(ReceiverComponent.SERVICE);
        as.setComponentName("Echo");
        container.activateComponent(as);
        
        container.start();

        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setInterfaceName(new QName("http://jsr181.servicemix.apache.org", "ComplexPojoPortType"));
        me.getInMessage().setContent(new StringSource("<twoWay xmlns='http://jsr181.servicemix.apache.org'><in0>world</in0></twoWay>"));
        client.sendSync(me);
        client.done(me);
        
        // Wait all acks being processed
        Thread.sleep(100);
    }
    
    public void testFault() throws Exception {
        Jsr181SpringComponent component = new Jsr181SpringComponent();
        Jsr181Endpoint endpoint = new Jsr181Endpoint();
        endpoint.setPojo(new ComplexPojoImpl());
        endpoint.setServiceInterface(ComplexPojo.class.getName());
        component.setEndpoints(new Jsr181Endpoint[] { endpoint });
        container.activateComponent(component, "JSR181Component");
        
        EchoComponent echo = new EchoComponent();
        ActivationSpec as = new ActivationSpec();
        as.setComponent(echo);
        as.setService(ReceiverComponent.SERVICE);
        as.setComponentName("Echo");
        container.activateComponent(as);
        
        container.start();

        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setInterfaceName(new QName("http://jsr181.servicemix.apache.org", "ComplexPojoPortType"));
        me.getInMessage().setContent(new StringSource("<hel lo>world</hello"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getFault());
        Node n = transformer.toDOMNode(me.getFault());
        System.err.println(transformer.toString(n));
        assertNotNull(textValueOfXPath(n, "/stack"));
        client.done(me);
        
        // Wait all acks being processed
        Thread.sleep(100);
    }
    
    protected String textValueOfXPath(Node node, String xpath) throws TransformerException {
        CachedXPathAPI cachedXPathAPI = new CachedXPathAPI();
        NodeIterator iterator = cachedXPathAPI.selectNodeIterator(node, xpath);
        Node root = iterator.nextNode();
        if (root instanceof Element) {
            Element element = (Element) root;
            if (element == null) {
                return "";
            }
            String text = DOMUtil.getElementText(element);
            return text;
        }
        else if (root != null) {
            return root.getNodeValue();
        } else {
            return null;
        }
    }
    
    public interface ComplexPojo {
        public void oneWay(Source src) throws Exception;
        public Source twoWay(Source src) throws Exception;
    }
    
    public static class ComplexPojoImpl implements ComplexPojo {
        private ComponentContext context;

        public ComponentContext getContext() {
            return context;
        }

        public void setContext(ComponentContext context) {
            this.context = context;
        }
        
        public void oneWay(Source src) throws Exception {
            DeliveryChannel channel = context.getDeliveryChannel();
            MessageExchangeFactory factory = channel.createExchangeFactory();
            InOnly inonly = factory.createInOnlyExchange();
            inonly.setService(ReceiverComponent.SERVICE);
            NormalizedMessage msg = inonly.createMessage();
            msg.setContent(src);
            inonly.setInMessage(msg);
            channel.send(inonly);
        }
        
        public Source twoWay(Source src) throws Exception {
            DeliveryChannel channel = context.getDeliveryChannel();
            MessageExchangeFactory factory = channel.createExchangeFactory();
            InOut inout = factory.createInOutExchange();
            inout.setService(ReceiverComponent.SERVICE);
            NormalizedMessage msg = inout.createMessage();
            msg.setContent(src);
            inout.setInMessage(msg);
            channel.sendSync(inout);
            Source outSrc = inout.getOutMessage().getContent();
            inout.setStatus(ExchangeStatus.DONE);
            channel.send(inout);
            return outSrc;
        }
    }
    
}
