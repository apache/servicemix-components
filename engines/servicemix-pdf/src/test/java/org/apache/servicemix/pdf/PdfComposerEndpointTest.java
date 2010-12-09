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
package org.apache.servicemix.pdf;

import java.io.File;
import java.net.URI;
import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.pdf.PdfComponent;

import junit.framework.TestCase;

/**
 * <p>
 * Test the PdfComposer XBean descriptor.
 * </p>
 * 
 * @author jbonofre
 */
public class PdfComposerEndpointTest extends TestCase {
    
    private JBIContainer container;
    
    /*
     * (non-Javadoc)
     * @see junit.framework.TestCase#setUp()
     */
    protected void setUp() throws Exception {
        // start ServiceMix JBI container
        container = new JBIContainer();
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        container.setEmbedded(true);
        container.init();
        // deploy the pdfcomposer component
        PdfComponent component = new PdfComponent();
        container.activateComponent(component, "PdfComponent");
        // start the JBI container
        container.start();
        // deploy a PdfComposer SU
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
    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
    }
    
    /**
     * <p>
     * Test if the endpoints in the xbean have been correctly deployed.
     * </p>
     * @throws Exception
     */
    public void testDeployment() throws Exception {
        // test if the {http://test/service}pdfcomposer endpoint is deployed
        assertNotNull("The endpoint {http://test/service}pdfcomposer is not found in the JBI container.", container.getRegistry().getEndpoint(new QName("http://test", "service"), "pdfcomposer"));
        
        // test if the {http://test/service}pdfcomposer endpoint descriptor contains the abstract WSDL
        assertNotNull("The endpoint {http://test/service}pdfcomposer descriptor is null.", container.getRegistry().getEndpointDescriptor(container.getRegistry().getEndpoint(new QName("http://test", "service"), "pdfcomposer")));
    }

}
