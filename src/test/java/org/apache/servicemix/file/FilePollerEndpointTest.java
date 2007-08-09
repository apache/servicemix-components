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
package org.apache.servicemix.file;

import java.io.File;

import javax.jbi.management.DeploymentException;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

public class FilePollerEndpointTest extends TestCase {
    
    private static final File DATA = new File("target/test/data");
    private static final File ARCHIVE = new File("target/test/archive");
    private FilePollerEndpoint endpoint;

    @Override
    protected void setUp() throws Exception {
        endpoint = new FilePollerEndpoint();
        endpoint.setTargetService(new QName("urn:test", "service"));
    }
    
    public void testValidateNoFile() throws Exception {
        try {
            endpoint.validate();
            fail("validate() should throw an exception when file has not been set");
        } catch (DeploymentException e) {
            //test succeeds
        }
    }

    public void testValidateArchiveNoDirectory() throws Exception {
        endpoint.setFile(DATA);
        File archive = null;
        try {
            archive = File.createTempFile("servicemix", "test");
            endpoint.setArchive(archive);
            endpoint.validate();
            fail("validate() should throw an exception when archive doesn't refer to a directory");
        } catch (DeploymentException e) {
            //test succeeds
        } finally {
            if (archive != null) {
                archive.delete();
            }
        }
    }
    
    public void testValidateArchiveWithoutDelete() throws Exception {
        endpoint.setFile(DATA);
        endpoint.setArchive(ARCHIVE);
        endpoint.setDeleteFile(false);
        try {
            endpoint.validate();
            fail("validate() should throw an exception when archive was set without delete");
        } catch (DeploymentException e) {
            //test succeeds
        }
    }
}
