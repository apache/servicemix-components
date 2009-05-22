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
package org.apache.servicemix.camel;

import junit.framework.TestCase;

/**
 * Test cases for {@link JbiComponent}
 */
public class JbiComponentTest extends TestCase {
    
    private final static String IN_OUT = "http://www.w3.org/ns/wsdl/in-out";
    
    public void testGetUriWithMep() throws Exception {
        JbiComponent component = createJbiComponent();
        JbiEndpoint endpoint = (JbiEndpoint) component.createEndpoint("jbi:endpoint:urn:test:service?mep=in-out");
        assertEquals(IN_OUT, endpoint.getMep());        
    }
    
    public void testNullWhenNoJbiUri() throws Exception {
        assertNull(createJbiComponent().createEndpoint("somethingelse:service:urn:test"));
    }
    
    public void testExceptionWhenIllegalUri() {
        // expecting an exception when uri doesn't have name, endpoint or service after the jbi:
        assertIllegalArgumentExceptionOnInvalidUri("jbi:illegal:urn:test:service");
        // expecting an exception when using the wrong separators
        assertIllegalArgumentExceptionOnInvalidUri("jbi:endpoint:urn:test/service");
    }

    private void assertIllegalArgumentExceptionOnInvalidUri(String uri) {
        JbiComponent component = createJbiComponent();
        JbiEndpoint endpoint = (JbiEndpoint) component.createEndpoint(uri);
        try {
            component.createJbiEndpointFromCamel(endpoint);
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // ok, at least we got the exception we expected
        }
    }

    private JbiComponent createJbiComponent() {
        return new JbiComponent(new CamelJbiComponent());
    }
}
