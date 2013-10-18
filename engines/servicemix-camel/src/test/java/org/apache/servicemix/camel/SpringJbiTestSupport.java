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
package org.apache.servicemix.camel;

import java.io.File;
import java.net.URI;
import java.net.URL;

import org.apache.camel.builder.RouteBuilder;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhances {@link JbiTestSupport} to enable SU deployment as well
 */
public abstract class SpringJbiTestSupport extends JbiTestSupport {
    
    private final Logger logger = LoggerFactory.getLogger(SpringJbiTestSupport.class);
    
    private File tempRootDir;
    
    @Override
    protected void configureComponent(CamelJbiComponent component) throws Exception {
        super.configureComponent(component);

        // let's add a Camel SU from the camel-context.xml
        URL url = getClass().getResource(getServiceUnitName() + "-src/camel-context.xml");
        File path = new File(new URI(url.toString())).getParentFile();

        component.getServiceUnitManager().deploy(getServiceUnitName(), path.getAbsolutePath());
        component.getServiceUnitManager().init(getServiceUnitName(), path.getAbsolutePath());
        component.getServiceUnitManager().start(getServiceUnitName());

    }
    
    @Override
    protected void configureContainer(JBIContainer container) throws Exception {
        super.configureContainer(container);
        jbiContainer.setCreateMBeanServer(false);
        jbiContainer.setMonitorInstallationDirectory(false);
        tempRootDir = File.createTempFile("servicemix", "rootDir");
        tempRootDir.delete();
        File tempTemp = new File(tempRootDir.getAbsolutePath() + "/temp");
        if (!tempTemp.mkdirs()) {
            fail("Unable to create temporary working root directory [" + tempTemp.getAbsolutePath() + "]");
        }
        logger.info("Using temporary root directory [{}]", tempRootDir.getAbsolutePath());
        jbiContainer.setRootDir(tempRootDir.getAbsolutePath());

        jbiContainer.setEmbedded(true);
        jbiContainer.setCreateJmxConnector(false);
        jbiContainer.setFlowName("st");
    }
    
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        jbiContainer.stop();
        jbiContainer.shutDown();
        FileUtil.deleteFile(tempRootDir);
    }
    
    protected abstract String getServiceUnitName();
    
    @Override
    protected RouteBuilder createRoutes() {
        return null;
    }
}
