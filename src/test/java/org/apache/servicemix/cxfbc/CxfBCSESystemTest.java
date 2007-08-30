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
import javax.xml.namespace.QName;

import org.apache.cxf.calculator.AddNumbersFault;
import org.apache.cxf.calculator.CalculatorPortType;
import org.apache.cxf.calculator.CalculatorService;

import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class CxfBCSESystemTest extends SpringTestSupport {

    public void testCalculator() throws Exception {

        URL wsdl = getClass().getResource("/wsdl/calculator.wsdl");
        assertNotNull(wsdl);
        CalculatorService service = new CalculatorService(wsdl, new QName(
                "http://apache.org/cxf/calculator", "CalculatorService"));
        CalculatorPortType port = service.getCalculatorPort();
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
    
    public void testMultiClient() throws Exception {
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
                // TODO Auto-generated catch block
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

}
