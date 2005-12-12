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
package org.servicemix.http;

import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.http.HTTPAddress;
import javax.wsdl.extensions.http.HTTPBinding;

import org.servicemix.common.BaseComponent;
import org.servicemix.common.Endpoint;
import org.servicemix.common.wsdl1.AbstractWsdl1Deployer;
import org.servicemix.common.wsdl1.JbiEndpoint;

/**
 * @author gnodet
 *
 */
public class HttpWsdl1Deployer extends AbstractWsdl1Deployer {

    public HttpWsdl1Deployer(BaseComponent component) {
        super(component);
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.wsdl1.AbstractWsdl1Deployer#createEndpoint(javax.wsdl.extensions.ExtensibilityElement, javax.wsdl.extensions.ExtensibilityElement)
     */
    protected Endpoint createEndpoint(ExtensibilityElement portElement, 
                                      ExtensibilityElement bindingElement,
                                      JbiEndpoint jbiEndpoint) {
        if (jbiEndpoint == null) {
            return null;
        }
        HttpEndpoint endpoint = new HttpEndpoint();
        endpoint.setRole(jbiEndpoint.getRole());
        endpoint.setDefaultMep(jbiEndpoint.getDefaultMep());
        endpoint.setLocationURI(((HTTPAddress) portElement).getLocationURI());
        endpoint.setBinding((HTTPBinding) bindingElement);
        return endpoint;
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.wsdl1.AbstractWsdl1Deployer#filterPortElement(javax.wsdl.extensions.ExtensibilityElement)
     */
    protected boolean filterPortElement(ExtensibilityElement element) {
        return element instanceof HTTPAddress;
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.wsdl1.AbstractWsdl1Deployer#filterBindingElement(javax.wsdl.extensions.ExtensibilityElement)
     */
    protected boolean filterBindingElement(ExtensibilityElement element) {
        return element instanceof HTTPBinding;
    }

}
