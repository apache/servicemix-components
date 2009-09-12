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
package org.apache.servicemix.xmpp;

import junit.framework.TestCase;

import java.net.URI;

/**
 * @version $Revision: $
 */
public class UriConfigurationTest extends TestCase {


    public void testUriConfiguration() throws Exception {
        PrivateChatEndpoint endpoint = new PrivateChatEndpoint();
        endpoint.setUri(new URI("xmpp://servicemix-user@localhost/test-user@localhost"));
        assertNotNull("Should have created an endpoint", endpoint);

        assertEquals("localhost", endpoint.getHost());
        assertEquals("servicemix-user", endpoint.getUser());
        assertEquals("test-user@localhost", endpoint.getParticipant());
    }
}
