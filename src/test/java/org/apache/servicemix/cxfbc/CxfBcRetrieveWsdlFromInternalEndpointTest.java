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

import javax.xml.namespace.QName;
import javax.xml.ws.soap.SOAPBinding;


import org.apache.cxf.calculator.AddNumbersFault;
import org.apache.cxf.calculator.CalculatorPortType;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;


public class CxfBcRetrieveWsdlFromInternalEndpointTest extends SpringTestSupport {

    private CxfBcComponent component;
    
    
    
    protected void setUp() throws Exception {
        super.setUp();
        
        component = new CxfBcComponent();
        jbi.activateComponent(component, "CxfBcComponent");
        CxfBcConsumer cxfBcConsumer = new CxfBcConsumer();
        cxfBcConsumer.setTargetEndpoint("CalculatorPort");
        cxfBcConsumer.setTargetService(new QName("http://apache.org/cxf/calculator", "CalculatorService"));
        cxfBcConsumer.setLocationURI("http://localhost:9000/CalculatorService/SoapPort");
        component.addEndpoint(cxfBcConsumer);
        component.start();
    }
    
    protected void tearDown() throws Exception {
 
    }
    
    public void testRetrieveWsdlFromIntermalEndpoint() throws Exception {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.getInInterceptors().add(new LoggingInInterceptor());
        factory.getOutInterceptors().add(new LoggingOutInterceptor());
        factory.setServiceClass(CalculatorPortType.class);
        factory.setBindingId(SOAPBinding.SOAP11HTTP_BINDING);
        factory.setAddress("http://localhost:9000/CalculatorService/SoapPort");
        CalculatorPortType port = (CalculatorPortType) factory.create();
        
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
    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext("org/apache/servicemix/cxfbc/xbean_retrieve_wsdl_from_internal_wsdl.xml");
    }
 
}
