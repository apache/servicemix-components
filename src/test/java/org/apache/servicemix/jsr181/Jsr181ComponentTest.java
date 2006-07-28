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

import java.io.File;
import java.net.URL;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jsr181.Jsr181Component;

import test.EchoService;

public class Jsr181ComponentTest extends TestCase {

    private static Log logger =  LogFactory.getLog(Jsr181ComponentTest.class);
    
    protected JBIContainer container;
    
    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        container.setMonitorInstallationDirectory(false);
        container.setNamingContext(new InitialContext());
        container.setEmbedded(true);
        container.init();
    }
    
    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
    }
    
    public void testCommonsAnnotations() throws Exception {
        Jsr181Component component = new Jsr181Component();
        container.activateComponent(component, "JSR181Component");

        // Start container
        container.start();
        
        // Deploy SU
        component.getServiceUnitManager().deploy("su", getServiceUnitPath("good1"));
        component.getServiceUnitManager().init("su", getServiceUnitPath("good1"));
        component.getServiceUnitManager().start("su");
        
        assertNotNull(EchoService.instance);
        assertNotNull(EchoService.instance.getContext());
        
        // Call it
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setInterfaceName(new QName("http://test", "EchoServicePortType"));
        me.getInMessage().setContent(new StringSource("<echo xmlns='http://test'><in0>world</in0></echo>"));
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
    
    
    public void testWithoutAnnotations() throws Exception {
        Jsr181Component component = new Jsr181Component();
        container.activateComponent(component, "JSR181Component");

        // Start container
        container.start();
        
        // Deploy SU
        component.getServiceUnitManager().deploy("good2", getServiceUnitPath("good2"));
        component.getServiceUnitManager().init("good2", getServiceUnitPath("good2"));
        component.getServiceUnitManager().start("good2");
        
        assertNotNull(EchoService.instance);
        assertNotNull(EchoService.instance.getContext());
        
        // Call it
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setInterfaceName(new QName("http://test", "EchoService2PortType"));
        me.getInMessage().setContent(new StringSource("<echo xmlns='http://test'><in0>world</in0></echo>"));
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
    
    public void testDeployUndeploy() throws Exception {
        Jsr181Component component = new Jsr181Component();
        container.activateComponent(component, "JSR181Component");
        component.getServiceUnitManager().deploy("d/u", getServiceUnitPath("good1"));
        component.getServiceUnitManager().shutDown("d/u");
        component.getServiceUnitManager().undeploy("d/u", getServiceUnitPath("good1"));
        component.getServiceUnitManager().deploy("d/u", getServiceUnitPath("good1"));
        component.getServiceUnitManager().start("d/u");
        component.getServiceUnitManager().stop("d/u");
        component.getServiceUnitManager().shutDown("d/u");
    }
    
    public void testNoPojoNoClass() throws Exception {
        Jsr181Component component = new Jsr181Component();
        container.activateComponent(component, "JSR181Component");
        try {
            component.getServiceUnitManager().deploy("bad1", getServiceUnitPath("bad1"));
            fail("Expected an exception");
        } catch (Exception e) {
            // ok
            logger.info(e.getMessage());
        }
    }
    
    public void testNoEndpoints() throws Exception {
        Jsr181Component component = new Jsr181Component();
        container.activateComponent(component, "JSR181Component");
        try {
            component.getServiceUnitManager().deploy("bad2", getServiceUnitPath("bad2"));
            fail("Expected an exception");
        } catch (Exception e) {
            // ok
            logger.info(e.getMessage());
        }
    }
    
    public void testDuplicates() throws Exception {
        Jsr181Component component = new Jsr181Component();
        container.activateComponent(component, "JSR181Component");
        try {
            component.getServiceUnitManager().deploy("bad3", getServiceUnitPath("bad3"));
            fail("Expected an exception");
        } catch (Exception e) {
            // ok
            logger.info(e.getMessage());
        }
    }
    
    public void testNotJsr181Endpoint() throws Exception {
        Jsr181Component component = new Jsr181Component();
        container.activateComponent(component, "JSR181Component");
        try {
            component.getServiceUnitManager().deploy("bad4", getServiceUnitPath("bad4"));
            fail("Expected an exception");
        } catch (Exception e) {
            // ok
            logger.info(e.getMessage());
        }
    }
    
    public void testUnrecognizedTypeMapping() throws Exception {
        Jsr181Component component = new Jsr181Component();
        container.activateComponent(component, "JSR181Component");
        try {
            component.getServiceUnitManager().deploy("bad5", getServiceUnitPath("bad5"));
            fail("Expected an exception");
        } catch (Exception e) {
            // ok
            logger.info(e.getMessage());
        }
    }
    
    public void testUnrecognizedAnnotations() throws Exception {
        Jsr181Component component = new Jsr181Component();
        container.activateComponent(component, "JSR181Component");
        try {
            component.getServiceUnitManager().deploy("bad6", getServiceUnitPath("bad6"));
            fail("Expected an exception");
        } catch (Exception e) {
            // ok
            logger.info(e.getMessage());
        }
    }
    
    protected String getServiceUnitPath(String name) {
        URL url = getClass().getClassLoader().getResource(name + "/xbean.xml");
        File path = new File(url.getFile());
        path = path.getParentFile();
        return path.getAbsolutePath();
    }
    
}
