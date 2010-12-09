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
package org.apache.servicemix.http;

import java.io.File;

import junit.framework.TestCase;

import org.apache.servicemix.util.FileUtil;

public class HttpConfigurationTest extends TestCase {

    private HttpConfiguration httpConfig;

    protected void setUp() throws Exception {
        super.setUp();
        httpConfig = new HttpConfiguration();
    }

    protected void tearDown() throws Exception {
        httpConfig = null;
    }

    // Test load() when rootDir is not set.
    public void testLoadNoRootDir() throws Exception {
        boolean isLoaded = httpConfig.load();
        assertTrue("HTTP Config should NOT be loaded with no root dir", !isLoaded);
    }

    // Test load() when config file does not exist.
    public void testLoadNoConfigFile() throws Exception {
        httpConfig.setRootDir("src/test/resources");
        assertTrue("HTTP Config should NOT be loaded with no config file", !httpConfig.load());
    }

    // Test save() (to config file) and load() (from config file).
    public void testSaveAndLoad() throws Exception {
        File rootDir = new File("target/httpConfig-test");
        if (!rootDir.exists()) {
            rootDir.mkdirs(); 
        }
        httpConfig.setRootDir(rootDir.getAbsolutePath());

        // Save the HTTP Config (mostly default values)
        httpConfig.save();

        File configFile = new File(rootDir, HttpConfiguration.CONFIG_FILE);
        assertTrue("HTTP Config file should exist", configFile.exists());
        boolean isLoaded = httpConfig.load();
        assertTrue("HTTP Config should be loaded", isLoaded);

        // clean up
        FileUtil.deleteFile(new File(httpConfig.getRootDir()));
    }

    // Test setMapping when input string does not begin with "/"
    public void testSetMappingBeginsNoSlash() throws Exception {
        String strMap = "pathHasNoSlash";
        httpConfig.setMapping(strMap);

        String actMap = httpConfig.getMapping();

        assertTrue("HTTP Config Mapping should begin with /", actMap.equals("/" + strMap));
    }

    // Test setMapping when input string ends with "/"
    public void testSetMappingEndsWithSlash() throws Exception {
        String strMap1 = "/pathEndsWithSlash";
        String strMap2 = "/";
        httpConfig.setMapping(strMap1 + strMap2);
 
        String actMap = httpConfig.getMapping();

        assertTrue("HTTP Config Mapping should not end with /", actMap.equals(strMap1));
    }

}
