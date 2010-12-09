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
package org.apache.servicemix.cxfse;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CxfSeConfiguration {

    public static final String CONFIG_FILE = "component.properties"; 
    
    private String rootDir;
    private String componentName = "servicemix-cxf-se";
    private Properties properties = new Properties();
    private String busCfg;
    
    public boolean load() {
        File f = null;
        InputStream in = null;
        if (rootDir != null) {
            // try to find the property file in the workspace folder
            f = new File(rootDir, CONFIG_FILE);
            if (!f.exists()) {
                f = null;
            }
        }
        if (f == null) {
            // find property file in classpath if it is not available in workspace 
            in = this.getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);
            if (in == null) {
                return false;
            }
        }

        try {
            if (f != null) {
                properties.load(new FileInputStream(f));
            } else {
                properties.load(in);
            }
        } catch (IOException e) {
            throw new RuntimeException("Could not load component configuration", e);
        }
        if (properties.getProperty(componentName + ".busCfg") != null) {
            setBusCfg(properties.getProperty(componentName + ".busCfg"));
        }
        
        return true;
    }

    /**
     * @return Returns the rootDir.
     * @org.apache.xbean.Property hidden="true"
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

    /**
     * @return Returns the componentName.
     * @org.apache.xbean.Property hidden="true"
     */
    public String getComponentName() {
        return componentName;
    }

    /**
     * @param componentName The componentName to set.
     */
    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setBusCfg(String busCfg) {
        this.busCfg = busCfg;
    }

    public String getBusCfg() {
        return busCfg;
    }
    
}
