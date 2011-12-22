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
package org.apache.servicemix.camel.osgi;

import javax.jbi.messaging.MessageExchange;
import javax.xml.namespace.QName;

import junit.framework.TestCase;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.endpoints.SimpleEndpoint;

/**
 * Test cases for {@link org.apache.servicemix.camel.osgi.OsgiCamelJbiComponent}
 */
public class OsgiCamelJbiComponentTest extends TestCase {

    private static final Long TIMEOUT = 1000l;

    public void testShutdownTimeout() throws InterruptedException {
        final ThreadLocal<Long> shutdownTimeout = new ThreadLocal<Long>();
        shutdownTimeout.set(-1l);

        OsgiCamelJbiComponent component = new OsgiCamelJbiComponent() {
            @Override
            public void prepareShutdown(Endpoint endpoint, long timeout) throws InterruptedException {
                shutdownTimeout.set(timeout);
            }
        };

        final Endpoint endpoint = new MockEndpoint();

        // the default shutdown timeout should be 0
        component.prepareShutdown(endpoint);
        assertEquals("The default timeout is 0",
                     new Long(0), shutdownTimeout.get());

        // another value can be configured on the component
        component.setShutdownTimeout(TIMEOUT);
        component.prepareShutdown(endpoint);
        assertEquals("If a value is configured, that value should be used",
                     TIMEOUT, shutdownTimeout.get());

    }

    public static class MockEndpoint extends SimpleEndpoint {

        public MockEndpoint() {
            super();
            setService(new QName("urn:test", "service"));
            setEndpoint("endpoint");
        }

        @Override
        public MessageExchange.Role getRole() {
            return null;
        }

        @Override
        public void process(MessageExchange exchange) throws Exception {
            // graciously do nothing
        }
    }
}
