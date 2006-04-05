/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.jms;

import org.apache.servicemix.common.BaseComponent;
import org.apache.servicemix.common.BaseLifeCycle;

public class JmsLifeCycle extends BaseLifeCycle {

    protected JmsConfiguration configuration;
    
    public JmsLifeCycle(BaseComponent component) {
        super(component);
        configuration = new JmsConfiguration();
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.BaseComponentLifeCycle#getExtensionMBean()
     */
    protected Object getExtensionMBean() throws Exception {
        return configuration;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.BaseLifeCycle#doInit()
     */
    protected void doInit() throws Exception {
        super.doInit();
        configuration.setRootDir(context.getWorkspaceRoot());
        configuration.load();
    }
    
    /**
     * @return Returns the configuration.
     */
    public JmsConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * @param configuration The configuration to set.
     */
    public void setConfiguration(JmsConfiguration configuration) {
        this.configuration = configuration;
    }

}
