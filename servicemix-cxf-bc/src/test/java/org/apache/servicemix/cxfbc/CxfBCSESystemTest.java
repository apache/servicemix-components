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

import java.net.URL;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.calculator.AddNumbersFault;
import org.apache.cxf.calculator.CalculatorPortType;
import org.apache.cxf.calculator.CalculatorService;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;

import org.apache.servicemix.jbi.container.SpringJBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class CxfBCSESystemTest extends SpringTestSupport {
    
    private static final Logger LOG = LogUtils.getL7dLogger(CxfBCSESystemTest.class);
    public void setUp() throws Exception {
        //override super setup
        LOG.info("setUp is invoked");
    }
    
    public void setUpJBI(String beanFile) throws Exception {
        if (context != null) {
            context.refresh();
        }
        transformer = new SourceTransformer();
        if (beanFile == null) {
            context = createBeanFactory();
        } else {
            context = createBeanFactory(beanFile);
        }
                
        jbi = (SpringJBIContainer) context.getBean("jbi");
        assertNotNull("JBI Container not found in spring!", jbi);
    }
    
    public void tearDown() throws Exception {
        if (context != null) {
            context.destroy();
            context = null;
        }
        if (jbi != null) {
            jbi.shutDown();
            jbi.destroy();
            jbi = null;
        }
    }

    public void testCalculatrorWithJBIWrapper() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/xbean.xml");
        calculatorTestBase();
    }
    
    public void testCalculatrorWithOutJBIWrapper() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/xbean_without_jbi_wrapper.xml");
        calculatorTestBase();
    }
    
    public void testMultipleClientWithJBIWrapper() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/xbean.xml");
        multiClientTestBase();
    }
    
    public void testMultipleClientWithoutJBIWrapper() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/xbean_without_jbi_wrapper.xml");
        multiClientTestBase();
    }
    
    private void calculatorTestBase() throws Exception {

        URL wsdl = getClass().getResource("/wsdl/calculator.wsdl");
        assertNotNull(wsdl);
        CalculatorService service = new CalculatorService(wsdl, new QName(
                "http://apache.org/cxf/calculator", "CalculatorService"));
        CalculatorPortType port = service.getCalculatorPort();
        ClientProxy.getClient(port).getInFaultInterceptors().add(new LoggingInInterceptor());
        ClientProxy.getClient(port).getInInterceptors().add(new LoggingInInterceptor());
        int ret = port.add(1, 2);
        assertEquals(ret, 3);
        try {
            port.add(1, -2);
            fail("should get exception since there is a negative arg");
        } catch (AddNumbersFault e) {
            assertEquals(e.getFaultInfo().getMessage(),
                    "Negative number cant be added!");
        }
    }
    
    private void multiClientTestBase() throws Exception {
        URL wsdl = getClass().getResource("/wsdl/calculator.wsdl");
        assertNotNull(wsdl);
        CalculatorService service = new CalculatorService(wsdl, new QName(
                "http://apache.org/cxf/calculator", "CalculatorService"));
        CalculatorPortType port = service.getCalculatorPort();
        MultiClientThread[] clients = new MultiClientThread[10];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new MultiClientThread(port);
        }
        
        for (int i = 0; i < clients.length; i++) {
            clients[i].start();
        }
        
        for (int i = 0; i < clients.length; i++) {
            clients[i].join();
        }
    }
    
    class MultiClientThread extends Thread {
        private CalculatorPortType port;
        
        public MultiClientThread(CalculatorPortType port) {
            this.port = port;
        }
        
        public void run() {
            try {
                assertEquals(port.add(1, 2), 3);
            } catch (AddNumbersFault e) {
                fail();
            }
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

}
