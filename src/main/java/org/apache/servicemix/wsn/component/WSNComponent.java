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
package org.apache.servicemix.wsn.component;

import javax.jms.ConnectionFactory;

import org.apache.servicemix.common.BaseComponent;
import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.common.BaseServiceUnitManager;
import org.apache.servicemix.common.Deployer;

public class WSNComponent extends BaseComponent {

    @Override
	protected BaseLifeCycle createLifeCycle() {
		return new WSNLifeCycle(this);
	}

    @Override
    public BaseServiceUnitManager createServiceUnitManager() {
        Deployer[] deployers = new Deployer[] { new WSNDeployer(this) };
        return new BaseServiceUnitManager(this, deployers);
    }

	public ConnectionFactory getConnectionFactory() {
		return ((WSNLifeCycle) lifeCycle).getConnectionFactory();
	}

	public void setConnectionFactory(ConnectionFactory connectionFactory) {
		((WSNLifeCycle) lifeCycle).setConnectionFactory(connectionFactory);
	}
	
}
