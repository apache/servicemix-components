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

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.servicemix.common.ResolvedEndpoint;

/**
 * @version $Revision: $
 */
public class UriConfigurationTest extends TestCase {


    public void testUriConfiguration() throws Exception {
        ServiceEndpoint serviceEndpoint = new ResolvedEndpoint("xmpp://servicemix-user@localhost/test-user@localhost", new QName("test"));

        XMPPComponent component = new XMPPComponent();
        PrivateChatEndpoint endpoint = (PrivateChatEndpoint) component.createEndpoint(serviceEndpoint);
        assertNotNull("Should have created an endpoint", endpoint);

        assertEquals("localhost", endpoint.getHost());
        assertEquals("servicemix-user", endpoint.getUser());
        assertEquals("test-user@localhost", endpoint.getParticipant());
    }
}
