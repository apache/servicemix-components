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

import java.util.ArrayList;
import java.util.EventListener;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.naming.InitialContext;
import javax.wsdl.Definition;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

import org.w3c.dom.Document;

import junit.framework.TestCase;

import com.ibm.wsdl.Constants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.view.DotViewEndpointListener;
import org.apache.servicemix.jbi.view.DotViewFlowListener;
import org.apache.servicemix.jsr181.xfire.JbiProxyFactoryBean;
import test.complex.OrderService;
import test.complex.OrderServiceImpl;
import test.complex.model.Cart;
import test.complex.model.OrderConfirmation;
import test.complex.model.OrderItem;

/**
 * This test shows creating and calling a service from a pojo. The Service is
 * using the common example of an order business process with a shopping cart.
 * As the business method uses complex in and out parameters it can show the
 * handling of complex types.
 * 
 * This test also checks for jira bug
 * http://issues.apache.org/activemq/browse/SM-739 The WSDLFlattener skips
 * complex types that live in another namespace than the service
 * 
 * @author Christian Schneider
 * 
 */
public class Jsr181ComplexTypeTest extends TestCase {

    static final String BROKER_SERVER = "wschris";
    static final int BROKER_PORT = 61216;

    private static transient Log log = LogFactory.getLog(Jsr181ComplexTypeTest.class);

    protected JBIContainer container;

    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setName("ComplexTypeContainer");
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        container.setMonitorInstallationDirectory(false);
        container.setNamingContext(new InitialContext());
        container.setEmbedded(true);
        container.setListeners(new EventListener[] {new DotViewFlowListener(), new DotViewEndpointListener() });
        container.init();
    }

    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
    }

    public void testOrder() throws Exception {
        BasicConfigurator.configure(new ConsoleAppender());
        Logger.getRootLogger().setLevel(Level.DEBUG);
        container.start();

        Jsr181Component component = new Jsr181Component();

        // Create an xfire endpoint for our pojo order service
        Jsr181Endpoint orderEndpoint = new Jsr181Endpoint();
        orderEndpoint.setPojo(new OrderServiceImpl());
        orderEndpoint.setServiceInterface(OrderService.class.getCanonicalName());

        component.setEndpoints(new Jsr181Endpoint[] {orderEndpoint });
        container.activateComponent(component, "JSR181Component");

        log.info(orderEndpoint.getServiceInterface());
        log.info(orderEndpoint.getInterfaceName());
        log.info(orderEndpoint.getEndpoint());

        // Create interface based proxy
        JbiProxyFactoryBean pf = new JbiProxyFactoryBean();
        pf.setContainer(container);
        pf.setInterfaceName(orderEndpoint.getInterfaceName());
        pf.setEndpoint(orderEndpoint.getEndpoint());
        pf.setType(OrderService.class);
        OrderService orderService = (OrderService) pf.getObject();

        // Prepare cart for order request
        Cart cart = new Cart();
        OrderItem orderItem = new OrderItem();
        orderItem.setCount(2);
        orderItem.setItem("Book");
        cart.getItems().add(orderItem);

        // Call the service
        OrderConfirmation orderConfirmation = orderService.order(cart);
        orderConfirmation = orderService.order(cart);
        orderConfirmation = orderService.order(cart);

        // Check that we get the expected order confirmation
        assertNotNull(orderConfirmation);
        cart = orderConfirmation.getCart();
        assertNotNull(cart);
        assertEquals(1, cart.getItems().size());
        orderItem = cart.getItems().get(0);
        assertEquals(2, orderItem.getCount());
        assertEquals("Book", orderItem.getItem());

        // Analyse the WSDL that is generated from the pojo service
        Document description = orderEndpoint.getDescription();
        SourceTransformer transformer = new SourceTransformer();
        log.info(transformer.toString(description));
        WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
        reader.setFeature(Constants.FEATURE_VERBOSE, false);
        Definition definition = reader.readWSDL(null, description);
        Map namespaces = definition.getNamespaces();

        String serviceNameSpace = orderEndpoint.getInterfaceName().getNamespaceURI();
        String modelNameSpace = "http://model.complex.test";

        // The WSDL should import the namespaces for the service and the model
        assertTrue("Namespace " + serviceNameSpace + " present", namespaces.containsValue(serviceNameSpace));
        assertTrue("Namespace " + modelNameSpace + " present", namespaces.containsValue(modelNameSpace));

        Iterator<Schema> schemaIt = definition.getTypes().getExtensibilityElements().iterator();

        List<String> schemaTargetNameSpaceList = new ArrayList<String>();
        while (schemaIt.hasNext()) {
            Schema schema = schemaIt.next();
            String targetNameSpace = schema.getElement().getAttribute("targetNamespace");
            schemaTargetNameSpaceList.add(targetNameSpace);
        }

        // There should be type definitions for the service and the model
        // namespace
        assertTrue("Type definitions present for namespace " + serviceNameSpace, schemaTargetNameSpaceList
                .contains(serviceNameSpace));
        assertTrue("Type definitions present for namespace " + modelNameSpace, schemaTargetNameSpaceList
                .contains(modelNameSpace));

        // The WSDL should be abstract so we should have no bindings
        assertEquals("Bindings count ", 0, definition.getBindings().size());
    }
}
