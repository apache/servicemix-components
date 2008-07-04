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
package org.apache.servicemix.cxfbc.ws.security;



import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.hello_world_soap_http.Greeter;
import org.apache.servicemix.cxfbc.CxfBcComponent;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;

public class CxfBcHttpsConsumerTest extends SpringTestSupport {
    
    private static final java.net.URL WSDL_LOC;
    static {
        java.net.URL tmp = null;
        try {
            tmp = CxfBCSecurityTest.class.getClassLoader().getResource(
                "org/apache/servicemix/cxfbc/ws/security/hello_world.wsdl"
            );
        } catch (final Exception e) {
            e.printStackTrace();
        }
        WSDL_LOC = tmp;
    }
    
    public void testHttps() throws Exception {
        
        Bus bus = new SpringBusFactory().createBus(
                "org/apache/servicemix/cxfbc/ws/security/provider/WibbleClient.xml"); 
        BusFactory.setDefaultBus(bus);
        LoggingInInterceptor in = new LoggingInInterceptor();
        bus.getInInterceptors().add(in);
        bus.getInFaultInterceptors().add(in);
        LoggingOutInterceptor out = new LoggingOutInterceptor();
        bus.getOutInterceptors().add(out);
        bus.getOutFaultInterceptors().add(out);
        final javax.xml.ws.Service svc = javax.xml.ws.Service.create(WSDL_LOC,
                new javax.xml.namespace.QName(
                        "http://apache.org/hello_world_soap_http",
                        "SOAPServiceWSSecurity"));
        svc.addPort(new javax.xml.namespace.QName(
                "http://apache.org/hello_world_soap_http",
                "TimestampSignEncrypt"), SOAPBinding.SOAP11HTTP_BINDING,
                "https://localhost:9001/SoapContext/SoapPort");
        final Greeter greeter = svc.getPort(new javax.xml.namespace.QName(
                "http://apache.org/hello_world_soap_http",
                "TimestampSignEncrypt"), Greeter.class);
        String ret = greeter.sayHi();
        assertEquals(ret, "Bonjour");
        ret = greeter.greetMe("ffang");
        assertEquals(ret, "Hello ffang");
        
        //test redeploy
        CxfBcComponent component = (CxfBcComponent) jbi.getComponent("servicemix-cxfbc").getComponent();
        component.getServiceUnitManager().stop("#default#");
        component.getServiceUnitManager().shutDown("#default#");
        Thread.sleep(10000);
        component.getServiceUnitManager().init("#default#", null);
        component.getServiceUnitManager().start("#default#");
        ret = greeter.sayHi();
        assertEquals(ret, "Bonjour");
        ret = greeter.greetMe("ffang");
        assertEquals(ret, "Hello ffang");
    }

    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        return new ClassPathXmlApplicationContext(
            "org/apache/servicemix/cxfbc/ws/security/xbean-https.xml");
    }

}
