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
import org.apache.cxf.calculator.CalculatorImpl;
import org.apache.cxf.calculator.CalculatorPortType;
import org.apache.cxf.calculator.CalculatorService;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class CxfBcProviderConsumerTest extends SpringTestSupport {

    public void testBridge() throws Exception {
        
        URL wsdl = getClass().getResource("/wsdl/calculator.wsdl");
        // start external service
        EndpointImpl endpoint =
            (EndpointImpl)javax.xml.ws.Endpoint.publish("http://localhost:9001/bridgetest", 
                new CalculatorImpl());
                        
        endpoint.getInInterceptors().add(new LoggingInInterceptor());
        endpoint.getOutInterceptors().add(new LoggingOutInterceptor());
        endpoint.getInFaultInterceptors().add(new LoggingInInterceptor());
        endpoint.getOutFaultInterceptors().add(new LoggingOutInterceptor());

        // start external client
        
        assertNotNull(wsdl);
        CalculatorService service1 = new CalculatorService(wsdl, new QName(
                "http://apache.org/cxf/calculator", "CalculatorService"));
        CalculatorPortType port = service1.getCalculatorPort();
        ClientProxy.getClient(port).getInFaultInterceptors().add(new LoggingInInterceptor());
        ClientProxy.getClient(port).getInInterceptors().add(new LoggingInInterceptor());
        ClientProxy.getClient(port).getOutFaultInterceptors().add(new LoggingOutInterceptor());
        ClientProxy.getClient(port).getOutInterceptors().add(new LoggingInInterceptor());
        int ret = port.add(1, 2);
        assertEquals(ret, 3);
        try {
            port.add(1, -2);
            fail("should get exception");
        } catch (AddNumbersFault e) {
            assertEquals(e.getFaultInfo().getMessage(), "Negative number cant be added!");
        }

    }

    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext(
                "org/apache/servicemix/cxfbc/cxf_provider_consumer_bridge.xml");
    }

}
