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
package org.apache.servicemix.exec.tests.smx3;

import java.io.File;
import java.net.URI;
import java.net.URL;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.exec.ExecComponent;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test the Exec XBean descriptor.
 * 
 * @author jbonofre
 */
public class ExecEndpointTest {
    
    protected JBIContainer container;
    
    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    @Before
    public void setUp() throws Exception {
        // start SMX JBI container
        container = new JBIContainer();
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        container.setEmbedded(true);
        container.init();
        // deploy the exec component
        ExecComponent component = new ExecComponent();
        container.activateComponent(component, "ExecComponent");
        // start the JBI container
        container.start();
        // deploy an exec SU
        URL url = getClass().getClassLoader().getResource("xbean/xbean.xml");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("xbean", path.getAbsolutePath());
        component.getServiceUnitManager().init("xbean", path.getAbsolutePath());
        component.getServiceUnitManager().start("xbean");
    }
    
    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#tearDown()
     */
    @After
    public void tearDown() throws Exception {
       if (container != null) {
           container.shutDown();
       }
    }
    
    /**
     * <p>
     * Test if the exec endpoint described by the xbean.xml is deployed and expose a WSDL.
     * </p>
     * 
     * @throws Exception in case of lookup failure.
     */
    @Test(timeout=60000)
    public void testDeployment() throws Exception {
        // test if the endpoint is present
        assertNotNull("The endpoint http://test/service/exec is not found in the JBI container.", container.getRegistry().getEndpoint(new QName("http://test", "service"), "exec"));
        
        // test if the endpoint descriptor contains the WSDL
        assertNotNull("The endpoint http://test/service/exec descriptor is null", container.getRegistry().getEndpointDescriptor(container.getRegistry().getEndpoint(new QName("http://test", "service"), "exec")));
    }
    
    /**
     * <p>
     * InOnly test using a valid in message.
     * </p>
     * 
     * @throws Exception if an error occurs during the test.
     */
    @Test(timeout=60000)
    public void testInOnlyWithValidPayloadMessage() throws Exception {   
        // InOnly MEP test
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOnly inOnly = client.createInOnlyExchange();
        inOnly.setService(new QName("http://test", "service"));
        inOnly.getInMessage().setContent(new StringSource(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<exec:execRequest xmlns:exec=\"http://servicemix.apache.org/exec\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<command>touch</command>" +
                "<arguments>" +
                "<argument>/tmp/test</argument>" +
                "</arguments>" +
                "</exec:execRequest>"));
        client.sendSync(inOnly);
        
        if (inOnly.getStatus() == ExchangeStatus.ERROR) {
            fail("Received ERROR status.");
        } else if (inOnly.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(inOnly.getFault().getContent()));
        }
    }
    
    /**
     * <p>
     * InOnly test with an empty in message (using the static command).
     * </p>
     * 
     * @throws Exception
     */
    @Test(timeout=60000)
    public void testInOnlyWithEmptyMessage() throws Exception {
        // InOnly MEP test
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOnly inOnly = client.createInOnlyExchange();
        inOnly.setService(new QName("http://test", "service"));
        inOnly.getInMessage().setContent(new StringSource(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<exec:execRequest xmlns:exec=\"http://servicemix.apache.org/exec\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"/>"));
        client.sendSync(inOnly);
        
        if (inOnly.getStatus() == ExchangeStatus.ERROR) {
            fail("Received ERROR status.");
        } else if (inOnly.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(inOnly.getFault().getContent()));
        }
    }
    
    /**
     * <p>
     * InOut test using a valid in message.
     * </p>
     * 
     * @throws Exception if an error occurs during the test.
     */
    @Test(timeout=60000)
    public void testInOutWithValidMessage() throws Exception {
        // InOut MEP test
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOut inOut = client.createInOutExchange();
        inOut.setService(new QName("http://test", "service"));        
        inOut.getInMessage().setContent(new StringSource(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<exec:execRequest xmlns:exec=\"http://servicemix.apache.org/exec\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<command>ls</command>" +
                "</exec:execRequest>"));
        client.sendSync(inOut);
        
        if (inOut.getStatus() == ExchangeStatus.ERROR) {
            fail("Received ERROR status.");
        } else if (inOut.getFault() != null) {
            fail("Received fault: " + new SourceTransformer().toString(inOut.getFault().getContent()));
        } else {
            System.out.println(new SourceTransformer().toString(inOut.getMessage("out").getContent()));
        }
        
        client.done(inOut);
    }

}
