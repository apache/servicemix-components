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

import java.net.URI;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.extensions.ExtensibilityElement;

import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.common.wsdl1.JbiExtension;
import org.apache.servicemix.http.processors.ConsumerProcessor;
import org.apache.servicemix.http.processors.ProviderProcessor;
import org.apache.servicemix.soap.SoapEndpoint;

/**
 * 
 * @author gnodet
 * @version $Revision$
 * @org.apache.xbean.XBean element="endpoint"
 *                  description="An http endpoint"
 * 
 */
public class HttpEndpoint extends SoapEndpoint {

    protected ExtensibilityElement binding;
    protected ExchangeProcessor processor;
    protected ServiceEndpoint activated;
    protected Role role;
    protected URI defaultMep;
    protected String locationURI;
    protected boolean soap;
    protected String soapVersion;
    
    public ExchangeProcessor getProcessor() {
        return this.processor;
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.Endpoint#activate()
     */
    public void activate() throws Exception {
        ComponentContext ctx = this.serviceUnit.getComponent().getComponentContext();
        if (getRole() == Role.PROVIDER) {
            activated = ctx.activateEndpoint(service, endpoint);
            processor = new ProviderProcessor(this);
        } else {
            activated = new HttpExternalEndpoint(this);
            ctx.registerExternalEndpoint(activated);
            processor = new ConsumerProcessor(this);
        }
        processor.start();
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.Endpoint#deactivate()
     */
    public void deactivate() throws Exception {
        ComponentContext ctx = this.serviceUnit.getComponent().getComponentContext();
        if (getRole() == Role.PROVIDER) {
            ServiceEndpoint ep = activated;
            activated = null;
            ctx.deactivateEndpoint(ep);
        } else {
            ServiceEndpoint ep = activated;
            activated = null;
            ctx.deregisterExternalEndpoint(ep);
        }
        processor.stop();
    }

    public ExtensibilityElement getBinding() {
        return binding;
    }

    public void setBinding(ExtensibilityElement binding) {
        this.binding = binding;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }
    
    /**
     * @org.apache.xbean.Property alias="role"
     * @param role
     */
    public void setRoleAsString(String role) {
        if (role == null) {
            throw new IllegalArgumentException("Role must be specified");
        } else if (JbiExtension.ROLE_CONSUMER.equals(role)) {
            setRole(Role.CONSUMER);
        } else if (JbiExtension.ROLE_PROVIDER.equals(role)) {
            setRole(Role.PROVIDER);
        } else {
            throw new IllegalArgumentException("Unrecognized role: " + role);
        }
    }

    public void setDefaultMep(URI defaultMep) {
        this.defaultMep = defaultMep;
    }

    public URI getDefaultMep() {
        return defaultMep;
    }

    public String getLocationURI() {
        return locationURI;
    }

    public void setLocationURI(String locationUri) {
        this.locationURI = locationUri;
    }

    public boolean isSoap() {
        return soap;
    }

    public void setSoap(boolean soap) {
        this.soap = soap;
    }

    public String getSoapVersion() {
        return soapVersion;
    }

    public void setSoapVersion(String soapVersion) {
        this.soapVersion = soapVersion;
    }

}
