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

import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.http.HTTPAddress;
import javax.wsdl.extensions.http.HTTPBinding;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap.SOAPBinding;

import org.apache.servicemix.common.BaseComponent;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.wsdl1.AbstractWsdl1Deployer;
import org.apache.servicemix.common.wsdl1.JbiEndpoint;

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
        if (portElement instanceof HTTPAddress && bindingElement instanceof HTTPBinding) {
            if (!"POST".equals(((HTTPBinding) bindingElement).getVerb())) {
                throw new UnsupportedOperationException(((HTTPBinding) bindingElement).getVerb() + " not supported");
            }
            HttpEndpoint endpoint = new HttpEndpoint();
            endpoint.setSoap(false);
            endpoint.setRole(jbiEndpoint.getRole());
            endpoint.setDefaultMep(jbiEndpoint.getDefaultMep());
            endpoint.setLocationURI(((HTTPAddress) portElement).getLocationURI());
            endpoint.setBinding(bindingElement);
            return endpoint;
        } else if (portElement instanceof SOAPAddress && bindingElement instanceof SOAPBinding) {
            HttpEndpoint endpoint = new HttpEndpoint();
            endpoint.setSoap(true);
            endpoint.setRole(jbiEndpoint.getRole());
            endpoint.setDefaultMep(jbiEndpoint.getDefaultMep());
            endpoint.setLocationURI(((SOAPAddress) portElement).getLocationURI());
            endpoint.setBinding(bindingElement);
            return endpoint;
        } else {
            return null;
        }
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.wsdl1.AbstractWsdl1Deployer#filterPortElement(javax.wsdl.extensions.ExtensibilityElement)
     */
    protected boolean filterPortElement(ExtensibilityElement element) {
        if (element instanceof HTTPAddress) {
            return true;
        }
        if (element instanceof SOAPAddress) {
            String uri = ((SOAPAddress) element).getLocationURI();
            if (uri.startsWith("http://") || uri.startsWith("https://")) {
                return true;
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.wsdl1.AbstractWsdl1Deployer#filterBindingElement(javax.wsdl.extensions.ExtensibilityElement)
     */
    protected boolean filterBindingElement(ExtensibilityElement element) {
        return element instanceof HTTPBinding || element instanceof SOAPBinding;
    }

}
