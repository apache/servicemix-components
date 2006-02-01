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
package org.apache.servicemix.jsr181;

import org.apache.servicemix.common.BaseComponent;
import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.jsr181.xfire.JbiTransport;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.XFireFactory;
import org.codehaus.xfire.transport.Transport;

public class Jsr181LifeCycle extends BaseLifeCycle {

    protected XFire xfire;
    protected Jsr181Configuration configuration;
    
    public Jsr181LifeCycle(BaseComponent component) {
        super(component);
        configuration = new Jsr181Configuration();
    }

    /**
     * @return Returns the configuration.
     */
    public Jsr181Configuration getConfiguration() {
        return configuration;
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.BaseComponentLifeCycle#getExtensionMBean()
     */
    protected Object getExtensionMBean() throws Exception {
        return configuration;
    }

    /**
     * @return Returns the xfire.
     */
    public XFire getXFire() {
        return xfire;
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.BaseLifeCycle#doInit()
     */
    protected void doInit() throws Exception {
        super.doInit();
        configuration.setRootDir(context.getWorkspaceRoot());
        configuration.load();
        xfire = XFireFactory.newInstance().getXFire();
        Object[] transports = xfire.getTransportManager().getTransports().toArray();
        for (int i = 0; i < transports.length; i++) {
            xfire.getTransportManager().unregister((Transport) transports[i]);
        }
        xfire.getTransportManager().register(new JbiTransport(this.context));
    }
    
}
