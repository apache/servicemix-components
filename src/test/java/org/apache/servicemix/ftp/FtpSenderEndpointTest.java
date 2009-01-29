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

import junit.framework.TestCase;

/**
 * Test cases for {@link FtpSenderEndpoint} 
 */
public class FtpSenderEndpointTest extends TestCase {

    public void testGetUploadNameWithPrefix() {
        FtpSenderEndpoint endpoint = new FtpSenderEndpoint();
        endpoint.setUploadPrefix("work/");
        assertEquals("work/myfile.xml", endpoint.getUploadName("myfile.xml"));
    }
    
    public void testGetUploadNameWithSuffix() {
        FtpSenderEndpoint endpoint = new FtpSenderEndpoint();
        endpoint.setUploadSuffix(".tmp");
        assertEquals("myfile.xml.tmp", endpoint.getUploadName("myfile.xml"));
    }
    
    public void testGetUploadNameWithPrefixAndSuffix() {
        FtpSenderEndpoint endpoint = new FtpSenderEndpoint();
        endpoint.setUploadPrefix("work/");
        endpoint.setUploadSuffix(".tmp");
        assertEquals("work/myfile.xml.tmp", endpoint.getUploadName("myfile.xml"));
    } 
}
