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
package org.apache.servicemix.cxfbc.ws.rm;

import java.net.URL;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.GreeterService;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.servicemix.cxfbc.ws.policy.ConnectionHelper;
import org.apache.servicemix.tck.SpringTestSupport;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.core.io.ClassPathResource;

public class CxfBCRMTest extends SpringTestSupport {
    private static final Logger LOG = Logger.getLogger(CxfBCRMTest.class.getName());
    private Bus bus;
    
    public void testDecoupled() throws Exception {
        SpringBusFactory bf = new SpringBusFactory();
        bus = bf.createBus("/org/apache/servicemix/cxfbc/ws/rm/decoupled.xml");
        BusFactory.setDefaultBus(bus);
        LoggingInInterceptor in = new LoggingInInterceptor();
        bus.getInInterceptors().add(in);
        bus.getInFaultInterceptors().add(in);
        LoggingOutInterceptor out = new LoggingOutInterceptor();
        bus.getOutInterceptors().add(out);
        bus.getOutFaultInterceptors().add(out);
        QName serviceName = new QName("http://cxf.apache.org/greeter_control", "GreeterService");
        URL wsdl = new ClassPathResource("/wsdl/greeter_control.wsdl").getURL();
        GreeterService gs = new GreeterService(wsdl, serviceName);
        final Greeter greeter = gs.getGreeterPort();
        LOG.fine("Created greeter client.");
       
        ConnectionHelper.setKeepAliveConnection(greeter, true);

                
        TwowayThread t = new TwowayThread(greeter);    
        t.start();
        
        // allow for partial response to twoway request to arrive
        
        long wait = 3000;
        while (wait > 0) {
            long start = System.currentTimeMillis();
            try {
                Thread.sleep(wait);
            } catch (InterruptedException ex) {
                // ignore
            }
            wait -= System.currentTimeMillis() - start;
        }

        greeter.greetMeOneWay("oneway");
        t.join();
        assertEquals("Unexpected response to twoway request", "oneway", t.getResponse());
    }
    
    
    
    @Override
    protected AbstractXmlApplicationContext createBeanFactory() {
        System.setProperty("derby.system.home", "/");
        //load cxf se and bc from spring config file
        return new ClassPathXmlApplicationContext(
            "org/apache/servicemix/cxfbc/ws/rm/xbean.xml");    
    }

}
