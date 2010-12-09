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
package org.apache.servicemix.jms.endpoints;

import javax.jbi.management.DeploymentException;
import javax.jms.ConnectionFactory;
import javax.xml.namespace.QName;

import junit.framework.TestCase;
import org.easymock.EasyMock;
import org.springframework.core.io.ClassPathResource;

/**
 * Test cases for {@link org.apache.servicemix.jms.endpoints.JmsSoapProviderEndpoint}
 */
public class JmsSoapProviderEndpointTest extends TestCase {

    private static final QName SERVICE = new QName("http://servicemix.org/test/", "Hello");
    private static final String ENDPOINT = "testService";

    public void testValidateInstallsDefaultMarshaler() throws Exception {
        JmsSoapProviderEndpoint endpoint = new JmsSoapProviderEndpoint();
        endpoint.setService(SERVICE);
        endpoint.setEndpoint(ENDPOINT);
        endpoint.setConnectionFactory(EasyMock.createMock(ConnectionFactory.class));
        endpoint.setWsdl(new ClassPathResource("provider/soap_endpoint.wsdl"));
        endpoint.validate();

        assertNotNull("Default marshaler should have been configured",
                      endpoint.getMarshaler());
        assertTrue("Default marshaler should be JmsSoapProviderMarshaler",
                   endpoint.getMarshaler() instanceof JmsSoapProviderMarshaler);
    }

    public void testValidateChecksMarshalerType() throws Exception {
        JmsSoapProviderEndpoint endpoint = new JmsSoapProviderEndpoint();
        endpoint.setService(SERVICE);
        endpoint.setEndpoint(ENDPOINT);
        endpoint.setConnectionFactory(EasyMock.createMock(ConnectionFactory.class));
        endpoint.setWsdl(new ClassPathResource("provider/soap_endpoint.wsdl"));
        endpoint.setMarshaler(new DefaultProviderMarshaler());
        try {
            endpoint.validate();
            fail("Validate should have thrown a DeploymentException");
        } catch (DeploymentException e) {
            //this is OK
        }
    }

    public void testValidateKeepsSetMarshaler() throws Exception {
        JmsSoapProviderMarshaler marshaler = new CustomSoapProviderMarshaler();

        JmsSoapProviderEndpoint endpoint = new JmsSoapProviderEndpoint();
        endpoint.setService(SERVICE);
        endpoint.setEndpoint(ENDPOINT);
        endpoint.setConnectionFactory(EasyMock.createMock(ConnectionFactory.class));
        endpoint.setWsdl(new ClassPathResource("provider/soap_endpoint.wsdl"));
        endpoint.setMarshaler(marshaler);
        endpoint.validate();

        assertSame("Custom marshaler should be used on the JMS endpoint",
                   marshaler, endpoint.getMarshaler());
    }

    /*
     * Customer JMS Soap marshaler implementation
     */
    private class CustomSoapProviderMarshaler extends JmsSoapProviderMarshaler {
    }
}
