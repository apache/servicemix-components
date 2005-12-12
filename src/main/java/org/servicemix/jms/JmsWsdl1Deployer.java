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

package org.servicemix.jms;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;

import org.servicemix.common.BaseComponent;
import org.servicemix.common.Endpoint;
import org.servicemix.common.wsdl1.AbstractWsdl1Deployer;
import org.servicemix.common.wsdl1.JbiEndpoint;
import org.servicemix.jms.wsdl.JmsAddress;
import org.servicemix.jms.wsdl.JmsBinding;
import org.servicemix.jms.wsdl.JmsExtension;

/**
 * @author gnodet
 *
 */
public class JmsWsdl1Deployer extends AbstractWsdl1Deployer {

    public JmsWsdl1Deployer(BaseComponent component) {
        super(component);
    }

    protected Endpoint createEndpoint(ExtensibilityElement portElement, 
                                      ExtensibilityElement bindingElement,
                                      JbiEndpoint jbiEndpoint) {
        if (jbiEndpoint == null) {
            return null;
        }
        JmsEndpoint endpoint = new JmsEndpoint();
        endpoint.setRole(jbiEndpoint.getRole());
        endpoint.setDefaultMep(jbiEndpoint.getDefaultMep());
        endpoint.setAddress((JmsAddress) portElement);
        endpoint.setBinding((JmsBinding) bindingElement);
        return endpoint;
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.wsdl.AbstractWsdlDeployer#filterPortElement(javax.wsdl.extensions.ExtensibilityElement)
     */
    protected boolean filterPortElement(ExtensibilityElement element) {
        return element instanceof JmsAddress;
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.wsdl.AbstractWsdlDeployer#filterBindingElement(javax.wsdl.extensions.ExtensibilityElement)
     */
    protected boolean filterBindingElement(ExtensibilityElement element) {
        return element instanceof JmsBinding;
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.wsdl.AbstractWsdlDeployer#registerExtensions(javax.wsdl.extensions.ExtensionRegistry)
     */
    protected void registerExtensions(ExtensionRegistry registry) {
        super.registerExtensions(registry);
        JmsExtension.register(registry);
    }

}
