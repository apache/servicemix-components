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
package org.apache.servicemix.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Base class for component configuration.
 * Due to classloading mechanism in JBI, Shared Libraries are
 * not available at bootstrap time, so this class should be
 * copied in your own component and modified directly, instead
 * of inheriting it.  This only apply if the bootstrap uses the
 * configuration.
 * 
 * @author Guillaume Nodet
 * @since 3.0
 */
@Deprecated
public class PersistentConfiguration {
    
    public final static String CONFIG_FILE = "component.properties"; 
    
    protected String rootDir;
    protected Properties properties;
    
    public PersistentConfiguration() {
        properties = new Properties();
    }
    
    public boolean load() {
        if (rootDir == null) {
            return false;
        }
        File f = new File(rootDir, CONFIG_FILE);
        if (!f.exists()) {
            return false;
        }
        try {
            properties.load(new FileInputStream(f));
            return true;
        } catch (IOException e) {
            throw new RuntimeException("Could not load component configuration", e);
        }
    }
    
    public void save() {
        if (rootDir != null) {
            File f = new File(rootDir, CONFIG_FILE);
            try {
                this.properties.store(new FileOutputStream(f), null);
            } catch (Exception e) {
                throw new RuntimeException("Could not store component configuration", e);
            }
        }
    }

    /**
     * @return Returns the rootDir.
     */
    public String getRootDir() {
        return rootDir;
    }

    /**
     * @param rootDir The rootDir to set.
     */
    public void setRootDir(String rootDir) {
        this.rootDir = rootDir;
    }

}
