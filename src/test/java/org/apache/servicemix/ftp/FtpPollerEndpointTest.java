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
package org.apache.servicemix.ftp;

import java.net.URI;
import java.net.URISyntaxException;

import javax.jbi.management.DeploymentException;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

/**
 * Test cases for {@link FtpPollerEndpoint} 
 */
public class FtpPollerEndpointTest extends TestCase {

    public void testValidateNoCwdWhenRecursive() throws URISyntaxException {
        FtpPollerEndpoint endpoint = new FtpPollerEndpoint();
        endpoint.setUri(new URI("ftp://anonymous@just.a.server/test"));
        endpoint.setTargetService(new QName("test", "service"));
        endpoint.setChangeWorkingDirectory(true);
        try {
            endpoint.validate();
            fail("validate() should throw exception when changeWorkingDirectory='true' and recursive='true'");
        } catch (DeploymentException e) {
            //this is what we expect
            System.out.println(e);
        }
    }
}
