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
package org.apache.servicemix.cxfbc;

import java.io.File;
import java.net.URL;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.calculator.CalculatorPortType;
import org.apache.cxf.calculator.CalculatorService;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;

import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class CxfBcSUClassloaderTest extends SpringTestSupport {
    
    private static final Logger LOG = LogUtils.getL7dLogger(CxfBcSUClassloaderTest.class);
    private CxfBcComponent component;
   
    
    public void setUp() throws Exception {
        super.setUp();
        LOG.info("setUp is invoked");
        component = new CxfBcComponent();
        jbi.activateComponent(component, "CxfBcComponent");
        //Deploy second cxf bc consumer SU
        component.getServiceUnitManager().deploy("secondSU", getServiceUnitPath("secondSU"));
        component.getServiceUnitManager().init("secondSU", getServiceUnitPath("secondSU"));
        component.getServiceUnitManager().start("secondSU");
    }
    
        
    public void tearDown() throws Exception {
        component.getServiceUnitManager().stop("secondSU");
        component.getServiceUnitManager().shutDown("secondSU");
        component.getServiceUnitManager().undeploy("secondSU", getServiceUnitPath("secondSU"));
    }

    public void testSUClassLoader() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/calculator.wsdl");
        assertNotNull(wsdl);
        CalculatorService service = new CalculatorService(wsdl, new QName(
                "http://apache.org/cxf/calculator", "CalculatorService"));
        CalculatorPortType port = service.getCalculatorPort();
        ClientProxy.getClient(port).getInFaultInterceptors().add(new LoggingInInterceptor());
        ClientProxy.getClient(port).getInInterceptors().add(new LoggingInInterceptor());
        int ret = port.add(1, 2);
        assertEquals(ret, 3);
        
        wsdl = getClass().getClassLoader().getResource("org/apache/servicemix/cxfbc/secondSU/calculator.wsdl");
        assertNotNull(wsdl);
        service = new CalculatorService(wsdl, new QName(
                "http://apache.org/cxf/calculator", "CalculatorSecondService"));
        port = service.getCalculatorPort();
        ClientProxy.getClient(port).getInFaultInterceptors().add(new LoggingInInterceptor());
        ClientProxy.getClient(port).getInInterceptors().add(new LoggingInInterceptor());
        try {
            ret = port.add(1, 2);
            assertEquals(ret, 3);
        } catch (Exception e) {
            fail();
        }
    }
    
    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        // load cxf se and bc from spring config file
        return new ClassPathXmlApplicationContext(
                "org/apache/servicemix/cxfbc/xbean.xml");
    }
    
    protected AbstractXmlApplicationContext createBeanFactory(String beanFile) {
        //load cxf se and bc from specified spring config file
        return new ClassPathXmlApplicationContext(
            beanFile);
    }
    
    protected String getServiceUnitPath(String name) {
        URL url = getClass().getClassLoader().getResource("org/apache/servicemix/cxfbc/" + name + "/xbean.xml");
        File path = new File(url.getFile());
        path = path.getParentFile();
        System.out.println("the absolutepath is " + path.getAbsolutePath());
        return path.getAbsolutePath();
    }

}
