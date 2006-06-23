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
package org.apache.servicemix.http;

import javax.jbi.servicedesc.ServiceEndpoint;

import org.apache.servicemix.common.BaseComponent;
import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.common.BaseServiceUnitManager;
import org.apache.servicemix.common.Deployer;
import org.apache.servicemix.http.jetty.JCLLogger;
import org.w3c.dom.DocumentFragment;

public class HttpComponent extends BaseComponent {

    static {
        JCLLogger.init();
    }
    
    /* (non-Javadoc)
     * @see org.servicemix.common.BaseComponent#createLifeCycle()
     */
    protected BaseLifeCycle createLifeCycle() {
        return new HttpLifeCycle(this);
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.BaseComponent#createServiceUnitManager()
     */
    public BaseServiceUnitManager createServiceUnitManager() {
        Deployer[] deployers = new Deployer[] { new HttpXBeanDeployer(this), new HttpWsdl1Deployer(this) };
        return new BaseServiceUnitManager(this, deployers);
    }

    /* (non-Javadoc)
     * @see javax.jbi.component.Component#resolveEndpointReference(org.w3c.dom.DocumentFragment)
     */
    public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
        return HttpResolvedEndpoint.resolveEndpoint(epr);
    }
    
}
