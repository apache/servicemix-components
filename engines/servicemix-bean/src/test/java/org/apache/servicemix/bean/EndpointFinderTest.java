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
package org.apache.servicemix.bean;

import javax.xml.namespace.QName;

/**
 * Test cases for {@link EndpointFinder}
 */
public class EndpointFinderTest extends AbstractBeanComponentTest {
    
    private EndpointFinder finder;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        finder = new EndpointFinder(component);
    }
    
    public void testNameOnly() throws Exception {
        BeanEndpoint endpoint = finder.createBeanEndpoint(MyNameEndpoint.class);
        assertNotNull(endpoint);
        assertEquals(component.getEPRServiceName(), endpoint.getService());
        assertEquals("test", endpoint.getEndpoint());
    }
    
    public void testLocalPartOnly() throws Exception {
        BeanEndpoint endpoint = finder.createBeanEndpoint(MyLocalPartEndpoint.class);
        assertNotNull(endpoint);
        assertEquals(new QName("test"), endpoint.getService());        
    }
    
    public void testCreateBeanEndpointWithNoAnnotation() throws Exception {
        try {
            finder.createBeanEndpoint(String.class);
            fail("Should have thrown an IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            //this is OK
        }
    }
    
    @Endpoint(name = "test")
    public static final class MyNameEndpoint {
        
    }
    
    @Endpoint(serviceName = "test")
    public static final class MyLocalPartEndpoint {
        
    }

}
