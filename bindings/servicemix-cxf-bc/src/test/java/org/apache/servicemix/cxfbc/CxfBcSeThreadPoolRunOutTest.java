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
import javax.xml.ws.soap.SOAPBinding;
import org.apache.cxf.calculator.AddNumbersFault;
import org.apache.cxf.calculator.CalculatorPortType;
import org.apache.cxf.calculator.CalculatorService;
import org.apache.cxf.common.logging.LogUtils;


import org.apache.servicemix.jbi.container.SpringJBIContainer;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class CxfBcSeThreadPoolRunOutTest extends CxfBcSpringTestSupport {
    
    private static final Logger LOG = LogUtils.getL7dLogger(CxfBcSeThreadPoolRunOutTest.class);
    
      
    static {
        System.setProperty("java.util.concurrent.RejectedExecutionHandler", "java.util.concurrent.ThreadPoolExecutor$AbortPolicy");
    }
    
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

    
    
    public void testMultipleClient() throws Exception {
        setUpJBI("org/apache/servicemix/cxfbc/xbean_threadpool_runout.xml");
        multiClientTestBase();
    }
    
    
    
    private void multiClientTestBase() throws Exception {
        
        URL wsdl = getClass().getResource("/wsdl/calculator.wsdl");
        assertNotNull(wsdl);
        CalculatorService service = new CalculatorService(wsdl, new QName(
                "http://apache.org/cxf/calculator", "CalculatorService"));
        QName endpoint = new QName("http://apache.org/cxf/calculator", "CalculatorPort");
        service.addPort(endpoint, 
                SOAPBinding.SOAP12HTTP_BINDING, "http://localhost:19000/CalculatorService/SoapPort");
        CalculatorPortType port = service.getPort(endpoint, CalculatorPortType.class);
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
        
        for (int i = 0; i < clients.length; i++) {
            assertTrue(clients[i].getResult().toString().equals("3") 
                       || clients[i].getResult().toString().contains("rejected from java.util.concurrent.ThreadPoolExecutor"));
        }
        
    }
    
    class MultiClientThread extends Thread {
        private CalculatorPortType port;
        private Object ret;
        
        public MultiClientThread(CalculatorPortType port) {
            this.port = port;
        }
        
        public void run() {
            try {
                ret = port.add(1, 2);
            } catch (Exception e) {
                ret = e.getMessage();                
            }
        }
        
        public Object getResult() {
            return ret;
        }
    }

    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        // load cxf se and bc from spring config file
        return new ClassPathXmlApplicationContext(
                "org/apache/servicemix/cxfbc/xbean_threadpool_runout.xml");
    }
    
    protected AbstractXmlApplicationContext createBeanFactory(String beanFile) {
        //load cxf se and bc from specified spring config file
        return new ClassPathXmlApplicationContext(
            beanFile);
    }

}
