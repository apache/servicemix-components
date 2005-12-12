/** 
 * 
 * Copyright 2005 Protique Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.servicemix.http;

import org.servicemix.common.BaseComponent;
import org.servicemix.common.BaseLifeCycle;
import org.servicemix.common.BaseServiceUnitManager;
import org.servicemix.common.Deployer;

public class HttpComponent extends BaseComponent {

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
        Deployer[] deployers = new Deployer[] { new HttpWsdl1Deployer(this), new HttpXBeanDeployer(this) };
        return new BaseServiceUnitManager(this, deployers);
    }

}
