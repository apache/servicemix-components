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

import java.util.logging.Logger;

import javax.xml.ws.Endpoint;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class HttpsServer extends AbstractBusTestServerBase {
    
    private static final Logger LOG = LogUtils.getL7dLogger(HttpsServer.class);
    
    protected void run()  {
        SpringBusFactory factory = new SpringBusFactory();
        Bus bus = factory.createBus(
            "org/apache/servicemix/cxfbc/ws/security/provider/CherryServer.xml"
        );
        BusFactory.setDefaultBus(bus);
        Object implementor = new GreeterImpl();
        String address = "https://localhost:9001/SoapContext/SoapPort";
        Endpoint.publish(address, implementor);
    }
        
    public static void main(String[] args) {
        try { 
            HttpsServer s = new HttpsServer(); 
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally { 
            LOG.info("done!");
        }
    }
}