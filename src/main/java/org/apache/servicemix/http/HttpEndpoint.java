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

import java.util.Iterator;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.http.HTTPAddress;
import javax.wsdl.extensions.soap.SOAPAddress;

import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.http.processors.ConsumerProcessor;
import org.apache.servicemix.http.processors.ProviderProcessor;
import org.apache.servicemix.soap.SoapEndpoint;

import com.ibm.wsdl.extensions.http.HTTPAddressImpl;
import com.ibm.wsdl.extensions.soap.SOAPAddressImpl;

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
    protected String locationURI;
    
    public ExtensibilityElement getBinding() {
        return binding;
    }

    public void setBinding(ExtensibilityElement binding) {
        this.binding = binding;
    }

    public String getLocationURI() {
        return locationURI;
    }

    public void setLocationURI(String locationUri) {
        this.locationURI = locationUri;
    }

    /**
     * @org.apache.xbean.Property alias="role"
     * @param role
     */
    public void setRoleAsString(String role) {
        super.setRoleAsString(role);
    }

    protected void overrideDefinition(Definition def) {
        Service svc = null;
        Port port = null;
        if (targetService != null && targetEndpoint != null) {
            svc = def.getService(targetService);
            port = (svc != null) ? svc.getPort(targetEndpoint) : null;
        } else if (targetInterfaceName != null) {
            Iterator it = def.getServices().values().iterator();
            svc = it .hasNext() ? (Service) it.next() : null;
            if (svc != null) {
                it = svc.getPorts().values().iterator();
                port = (it.hasNext()) ? (Port) it.next() : null;
            }
        } else {
            svc = def.getService(service);
            port = (svc != null) ? svc.getPort(endpoint) : null;
        }
        if (port != null) {
            port.getExtensibilityElements().clear();
            if (isSoap()) {
                SOAPAddress address = new SOAPAddressImpl();
                address.setLocationURI(getLocationURI());
                port.addExtensibilityElement(address);
                def.addNamespace("soap", "http://schemas.xmlsoap.org/wsdl/soap/");
            } else {
                HTTPAddress address = new HTTPAddressImpl();
                address.setLocationURI(getLocationURI());
                port.addExtensibilityElement(address);
                def.addNamespace("http", "http://schemas.xmlsoap.org/wsdl/http/");
            }
            svc.getPorts().clear();
            svc.addPort(port);
            definition = def;
        }
    }

    protected ExchangeProcessor createProviderProcessor() {
        return new ProviderProcessor(this);
    }

    protected ExchangeProcessor createConsumerProcessor() {
        return new ConsumerProcessor(this);
    }

    protected ServiceEndpoint createExternalEndpoint() {
        return new HttpExternalEndpoint(this);
    }

}
