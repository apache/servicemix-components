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

import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.StringSource;

public class Jsr181ProxySUTest extends TestCase {

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
    
    public void test() throws Exception {
        Jsr181Component component = new Jsr181Component();
        container.activateComponent(component, "JSR181Component");

        // Start container
        container.start();
        
        // Deploy SU
        component.getServiceUnitManager().deploy("target", getServiceUnitPath("target"));
        component.getServiceUnitManager().init("target", getServiceUnitPath("target"));
        component.getServiceUnitManager().start("target");
        
        component.getServiceUnitManager().deploy("proxy", getServiceUnitPath("proxy"));
        component.getServiceUnitManager().init("proxy", getServiceUnitPath("proxy"));
        component.getServiceUnitManager().start("proxy");
        
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange(new QName("http://test", "Echo"), null, null);
        me.setInMessage(me.createMessage());
        me.getInMessage().setContent(new StringSource("<echo xmlns='http://test'><msg>world</msg></echo>"));
        client.sendSync(me);
        assertEquals(ExchangeStatus.ACTIVE, me.getStatus());
        assertNotNull(me.getOutMessage());
        client.done(me);
    }
    
    protected String getServiceUnitPath(String name) {
        URL url = getClass().getClassLoader().getResource(name + "/xbean.xml");
        File path = new File(url.getFile());
        path = path.getParentFile();
        return path.getAbsolutePath();
    }
    
}
