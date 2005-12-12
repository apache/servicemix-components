/** 
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
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
package org.servicemix.jsr181;

import org.servicemix.common.BaseComponent;
import org.servicemix.common.Endpoint;
import org.servicemix.common.xbean.AbstractXBeanDeployer;

import javax.jbi.management.DeploymentException;

public class Jsr181XBeanDeployer extends AbstractXBeanDeployer {

    public Jsr181XBeanDeployer(BaseComponent component) {
        super(component);
    }

    protected boolean validate(Endpoint endpoint) throws DeploymentException {
        if (endpoint instanceof Jsr181Endpoint == false) {
            throw failure("deploy", "Endpoint should be a Jsr181 endpoint", null);
        }
        Jsr181Endpoint ep = (Jsr181Endpoint) endpoint;
        if (ep.getPojo() == null && ep.getPojoClass() == null) {
            throw failure("deploy", "Endpoint must have a non-null pojo or a pojoClass", null);
        }
        try {
            ep.registerService();
        } catch (Exception e) {
            throw failure("deploy", "Could not register endpoint", e);
        }
        return true;
    }


}
