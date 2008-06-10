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
package org.apache.servicemix.wsn.client;

import javax.xml.transform.Source;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import junit.framework.TestCase;
import org.apache.servicemix.jbi.jaxp.StringSource;

public class AbstractWSAClientTest extends TestCase {

    public void test() {
        Source src = new StringSource("<EndpointReference xmlns='http://www.w3.org/2005/08/addressing'><Address>"
                                      + "endpoint:http://www.consumer.org/service/endpoint</Address></EndpointReference>");
        W3CEndpointReference ref = new W3CEndpointReference(src);
        String address = AbstractWSAClient.getWSAAddress(ref);
        assertEquals("endpoint:http://www.consumer.org/service/endpoint", address);
    }

}
