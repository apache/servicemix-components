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
package org.apache.servicemix.jms;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;

import org.apache.servicemix.common.BaseComponent;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.wsdl1.AbstractWsdl1Deployer;
import org.apache.servicemix.common.wsdl1.JbiEndpoint;
import org.apache.servicemix.jms.wsdl.JmsAddress;
import org.apache.servicemix.jms.wsdl.JmsBinding;
import org.apache.servicemix.jms.wsdl.JmsExtension;

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
        endpoint.setDefaultOperation(jbiEndpoint.getDefaultOperation());
        endpoint.setDestinationStyle(((JmsAddress) portElement).getDestinationStyle());
        endpoint.setInitialContextFactory(((JmsAddress) portElement).getInitialContextFactory());
        endpoint.setJmsProviderDestinationName(((JmsAddress) portElement).getJmsProviderDestinationName());
        endpoint.setJndiConnectionFactoryName(((JmsAddress) portElement).getJndiConnectionFactoryName());
        endpoint.setJndiDestinationName(((JmsAddress) portElement).getJndiDestinationName());
        endpoint.setJndiProviderURL(((JmsAddress) portElement).getJndiProviderURL());
        endpoint.setBinding((JmsBinding) bindingElement);
        return endpoint;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.wsdl.AbstractWsdlDeployer#filterPortElement(javax.wsdl.extensions.ExtensibilityElement)
     */
    protected boolean filterPortElement(ExtensibilityElement element) {
        return element instanceof JmsAddress;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.wsdl.AbstractWsdlDeployer#filterBindingElement(javax.wsdl.extensions.ExtensibilityElement)
     */
    protected boolean filterBindingElement(ExtensibilityElement element) {
        return element instanceof JmsBinding;
    }

    /* (non-Javadoc)
     * @see org.apache.servicemix.common.wsdl.AbstractWsdlDeployer#registerExtensions(javax.wsdl.extensions.ExtensionRegistry)
     */
    protected void registerExtensions(ExtensionRegistry registry) {
        super.registerExtensions(registry);
        JmsExtension.register(registry);
    }

}
