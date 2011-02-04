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
package org.apache.servicemix.http;

import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.servicemix.common.DefaultServiceUnit;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

public class HttpEndpointLoadWsdlTest extends TestCase {

    /*
     * Test method for 'org.apache.servicemix.soap.SoapEndpoint.loadWsdl()'
     */
    public void testLoadWsdl() throws Exception {
        MyHttpEndpoint endpoint = new MyHttpEndpoint();
        endpoint.setWsdlResource(new ClassPathResource("org/apache/servicemix/http/wsdl/relative.wsdl"));
        endpoint.setTargetEndpoint("TestPort");
        endpoint.setTargetService(new QName("http://test.namespace/wsdl", "TestService"));
        endpoint.setLocationURI("http://0.0.0.0:1234/services/service1/");
        endpoint.setSoap(true);
        endpoint.setService(new QName("http://test.namespace/wsdl", "Service"));

        endpoint.loadWsdl();

        assertFalse(endpoint.getWsdls().isEmpty());

        assertEquals(7, endpoint.getWsdls().size());
        assertTrue(endpoint.getWsdls().containsKey("Service1/Requests.xsd"));
        assertTrue(endpoint.getWsdls().containsKey("Service1/subschema/SubSchema.xsd"));
        assertTrue(endpoint.getWsdls().containsKey("common/DataType.xsd"));
        assertTrue(endpoint.getWsdls().containsKey("common/TestEnum.xsd"));
        assertTrue(endpoint.getWsdls().containsKey("Service1/Responses.xsd"));
        assertTrue(endpoint.getWsdls().containsKey("common/Fault.xsd"));
        assertTrue(endpoint.getWsdls().containsKey("main.wsdl"));
    }

    public class MyHttpEndpoint extends HttpEndpoint {
        public MyHttpEndpoint() {
            super();
            setServiceUnit(new MyServiceUnit());
        }
        // make the protected method public so I can call it.
        public void loadWsdl() {
            logger = LoggerFactory.getLogger(getClass());
            super.loadWsdl();
        }
        // this needs to work:
        // getServiceUnit().getComponent().getLifeCycle().getConfiguration().isManaged()
    }

    public class MyServiceUnit extends DefaultServiceUnit {
        public MyServiceUnit() {
            super(new HttpComponent());
        }
    }
}
