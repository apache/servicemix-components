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
package org.apache.servicemix.http.endpoints;

import java.io.IOException;

import javax.jbi.management.DeploymentException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.soap.api.Policy;
import org.apache.servicemix.soap.util.DomUtil;
import org.apache.servicemix.soap.wsdl.BindingFactory;
import org.apache.servicemix.soap.wsdl.WSDLUtils;
import org.apache.servicemix.soap.wsdl.validator.WSIBPValidator;
import org.apache.woden.WSDLFactory;
import org.apache.woden.WSDLReader;
import org.apache.woden.types.NCName;
import org.apache.woden.wsdl20.Description;
import org.apache.woden.wsdl20.Endpoint;
import org.apache.woden.wsdl20.xml.DescriptionElement;
import org.springframework.core.io.Resource;

/**
 * 
 * @author gnodet
 * @since 3.2
 * @org.apache.xbean.XBean element="soap-provider"
 */
public class HttpSoapProviderEndpoint extends HttpProviderEndpoint {

    private Resource wsdl;
    private boolean useJbiWrapper = true;
    private boolean validateWsdl = true;
    private Policy[] policies;

    public HttpSoapProviderEndpoint() {
        super();
    }

    public HttpSoapProviderEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component, endpoint);
    }

    public HttpSoapProviderEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    public Resource getWsdl() {
        return wsdl;
    }

    public void setWsdl(Resource wsdl) {
        this.wsdl = wsdl;
    }

    public boolean isValidateWsdl() {
        return validateWsdl;
    }

    public void setValidateWsdl(boolean validateWsdl) {
        this.validateWsdl = validateWsdl;
    }

    public boolean isUseJbiWrapper() {
        return useJbiWrapper;
    }

    public void setUseJbiWrapper(boolean useJbiWrapper) {
        this.useJbiWrapper = useJbiWrapper;
    }

    public Policy[] getPolicies() {
        return policies;
    }

    public void setPolicies(Policy[] policies) {
        this.policies = policies;
    }


    @Override
    public void validate() throws DeploymentException {
        if (wsdl == null) {
            throw new DeploymentException("wsdl property must be set");
        }
        HttpSoapProviderMarshaler marshaler;
        if (getMarshaler() instanceof HttpSoapProviderMarshaler) {
            marshaler = (HttpSoapProviderMarshaler) getMarshaler();
        } else if (getMarshaler() == null) {
            marshaler = new HttpSoapProviderMarshaler();
        } else {
            throw new DeploymentException("The configured marshaler must inherit HttpSoapProviderMarshaler");
        }
        try {
            description = DomUtil.parse(wsdl.getInputStream());
            Element elem = description.getDocumentElement();
            if (WSDLUtils.WSDL1_NAMESPACE.equals(elem.getNamespaceURI())) {
                validateWsdl1(marshaler);
            } else if (WSDLUtils.WSDL2_NAMESPACE.equals(elem.getNamespaceURI())) {
                validateWsdl2(marshaler);
            } else {
                throw new DeploymentException("Unrecognized wsdl namespace: " + elem.getNamespaceURI());
            }
            marshaler.setUseJbiWrapper(useJbiWrapper);
            marshaler.setPolicies(policies);
            setMarshaler(marshaler);
        } catch (DeploymentException e) {
            throw e;
        } catch (Exception e) {
            throw new DeploymentException("Unable to read WSDL from: " + wsdl, e);
        }
        super.validate();
    }

    protected void validateWsdl1(HttpSoapProviderMarshaler marshaler) throws WSDLException, IOException, DeploymentException {
        Definition def = WSDLUtils.createWSDL11Reader().readWSDL(wsdl.getURL().toString());
        if (validateWsdl) {
            WSIBPValidator validator = new WSIBPValidator(def);
            if (!validator.isValid()) {
                throw new DeploymentException("WSDL is not WS-I BP compliant: " + validator.getErrors());
            }
        }
        Service svc;
        if (getService() != null) {
            svc = def.getService(getService());
            if (svc == null) {
                throw new DeploymentException("Could not find service '" + getService() + "' in wsdl"); 
            }
        } else if (def.getServices().size() == 1) {
            svc = (Service) def.getServices().values().iterator().next();
            setService(svc.getQName());
        } else {
            throw new DeploymentException("If service is not set, the WSDL must contain a single service definition");
        }
        Port port;
        if (getEndpoint() != null) {
            port = svc.getPort(getEndpoint());
            if (port == null) {
                throw new DeploymentException("Cound not find port '" + getEndpoint() + "' "
                        + "in wsdl for service '" + getService() + "'");
            }
        } else if (svc.getPorts().size() == 1) {
            port = (Port) svc.getPorts().values().iterator().next();
            setEndpoint(port.getName());
        } else {
            throw new DeploymentException("If endpoint is not set, the WSDL service '" + getService() + "' "
                                             + "must contain a single port definition");
        }
        SOAPAddress sa11 = WSDLUtils.getExtension(port, SOAPAddress.class);
        SOAP12Address sa12 = WSDLUtils.getExtension(port, SOAP12Address.class);
        if (sa11 != null) {
            marshaler.setBaseUrl(sa11.getLocationURI());
        } else if (sa12 != null) {
            marshaler.setBaseUrl(sa12.getLocationURI());
        } else {
            throw new DeploymentException("No SOAP address defined on port '" + port.getName() + "'");
        }
        description = WSDLUtils.getWSDL11Factory().newWSDLWriter().getDocument(def);
        marshaler.setBinding(BindingFactory.createBinding(port));
    }
    
    protected void validateWsdl2(HttpSoapProviderMarshaler marshaler) throws
                                  org.apache.woden.WSDLException, IOException, DeploymentException {
        WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
        DescriptionElement descElement = reader.readWSDL(wsdl.getURL().toString());
        Description desc = descElement.toComponent();
        org.apache.woden.wsdl20.Service svc;
        if (getService() != null) {
            svc = desc.getService(getService());
            if (svc == null) {
                throw new DeploymentException("Could not find service '" + getService() + "' in wsdl"); 
            }
        } else if (desc.getServices().length == 1) {
            svc = desc.getServices()[0];
            setService(svc.getName());
        } else {
            throw new DeploymentException("If service is not set, the WSDL must contain a single service definition");
        }
        Endpoint endpoint;
        if (getEndpoint() != null) {
            endpoint = svc.getEndpoint(new NCName(getEndpoint()));
            if (endpoint == null) {
                throw new DeploymentException("Cound not find endpoint '" + getEndpoint() + "' in wsdl for service '" + getService() + "'");
            }
        } else if (svc.getEndpoints().length == 1) {
            endpoint = svc.getEndpoints()[0];
            setEndpoint(endpoint.getName().toString());
        } else {
            throw new DeploymentException("If endpoint is not set, the WSDL service '" + getService() + "' "
                                             + "must contain a single port definition");
        }
        marshaler.setBinding(BindingFactory.createBinding(endpoint));
    }

}
