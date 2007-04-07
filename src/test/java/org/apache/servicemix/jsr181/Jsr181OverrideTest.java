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

import javax.naming.InitialContext;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.servicemix.jbi.container.JBIContainer;

import test.EchoService;
import test.EchoService2;

public class Jsr181OverrideTest extends TestCase {

    protected JBIContainer container;
    
    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        container.setMonitorInstallationDirectory(false);
        container.setNamingContext(new InitialContext());
        container.setEmbedded(true);
        container.init();
        container.start();
    }
    
    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
    }
    
    public void testWithSpecifiedNamesWithAnnotations() throws Exception {
        QName svcName = new QName("svcUri", "service");
        QName itfName = new QName("itfUri", "interface");
        String epName = "endpoint";
        Jsr181Component component = new Jsr181Component();
        Jsr181Endpoint endpoint = new Jsr181Endpoint();
        endpoint.setService(svcName);
        endpoint.setInterfaceName(itfName);
        endpoint.setEndpoint(epName);
        endpoint.setPojo(new EchoService());
        component.setEndpoints(new Jsr181Endpoint[] {endpoint });
        container.activateComponent(component, "JSR181Component");
    }
    
    public void testWithSpecifiedNamesWithoutAnnotations() throws Exception {
        QName svcName = new QName("svcUri", "service");
        QName itfName = new QName("itfUri", "interface");
        String epName = "endpoint";
        Jsr181Component component = new Jsr181Component();
        Jsr181Endpoint endpoint = new Jsr181Endpoint();
        endpoint.setService(svcName);
        endpoint.setInterfaceName(itfName);
        endpoint.setEndpoint(epName);
        endpoint.setPojo(new EchoService2());
        component.setEndpoints(new Jsr181Endpoint[] {endpoint });
        container.activateComponent(component, "JSR181Component");
    }
    
}
