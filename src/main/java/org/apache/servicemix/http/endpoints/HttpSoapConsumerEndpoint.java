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
import java.io.InputStream;

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
import org.xml.sax.InputSource;

/**
 * @author gnodet
 * @since 3.2
 * @org.apache.xbean.XBean element="soap-consumer" description=
 *                         "an HTTP consumer endpoint that is optimized to work with SOAP messages"
 */
public class HttpSoapConsumerEndpoint extends HttpConsumerEndpoint {

    private Resource wsdl;
    private boolean useJbiWrapper = true;
    private boolean validateWsdl = true;
    private Policy[] policies;

    public HttpSoapConsumerEndpoint() {
        super();
    }

    public HttpSoapConsumerEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component, endpoint);
    }

    public HttpSoapConsumerEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    public Resource getWsdl() {
        return wsdl;
    }

    /**
     * Specifies the WSDL document defining the messages sent by the endpoint.
     * 
     * @param wsdl a <code>Resource</code> object that points to the WSDL document
     * @org.apache.xbean.Property description="the URL of the WSDL document defining the endpoint's messages"
     */
    public void setWsdl(Resource wsdl) {
        this.wsdl = wsdl;
    }

    public boolean isValidateWsdl() {
        return validateWsdl;
    }

    /**
     * Specifies in the WSDL is validated against the WS-I basic profile.
     * 
     * @param validateWsdl <code>true</code> if WSDL is to be validated
     * @org.apache.xbean.Property description="Specifies if the WSDL is checked for WSI-BP compliance. Default is <code>true</code>."
     */
    public void setValidateWsdl(boolean validateWsdl) {
        this.validateWsdl = validateWsdl;
    }

    public boolean isUseJbiWrapper() {
        return useJbiWrapper;
    }

    /**
     * Specifies if the SOAP messages are wrapped in a JBI wrapper.
     * 
     * @param useJbiWrapper <code>true</code> if the SOAP messages are wrapped
     * @org.apache.xbean.Property description= "Specifies if the JBI wrapper is sent in the body of the message. Default is
     *                            <code>true</code>."
     */
    public void setUseJbiWrapper(boolean useJbiWrapper) {
        this.useJbiWrapper = useJbiWrapper;
    }

    public Policy[] getPolicies() {
        return policies;
    }

    /**
     * Specifies a list of interceptors that will process messages for the endpoint.
     * 
     * @param policies an array of <code>Policy</code> objects
     * @org.apache.xbean.Property description="a list of interceptors that will process messages"
     */
    public void setPolicies(Policy[] policies) {
        this.policies = policies;
    }

    @Override
    public void validate() throws DeploymentException {
        if (wsdl == null) {
            throw new DeploymentException("wsdl property must be set");
        }
        HttpSoapConsumerMarshaler marshaler;
        if (getMarshaler() instanceof HttpSoapConsumerMarshaler) {
            marshaler = (HttpSoapConsumerMarshaler)getMarshaler();
        } else if (getMarshaler() == null) {
            marshaler = new HttpSoapConsumerMarshaler();
        } else {
            throw new DeploymentException("The configured marshaler must inherit HttpSoapConsumerMarshaler");
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
        if (getTargetService() == null && getTargetInterface() == null && getTargetUri() == null) {
            setTargetService(getService());
            setTargetEndpoint(getEndpoint());
        }
        super.validate();
    }

    protected void loadStaticResources() {
        addResource(MAIN_WSDL, description);
    }

    protected void validateWsdl1(HttpSoapConsumerMarshaler marshaler) throws Exception {
        InputStream is = wsdl.getInputStream();
        try {
            InputSource inputSource = new InputSource(is);
            inputSource.setSystemId(wsdl.getURL().toString());
            Definition def = WSDLUtils.createWSDL11Reader().readWSDL(wsdl.getURL().toString(), inputSource);
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
                svc = (Service)def.getServices().values().iterator().next();
                setService(svc.getQName());
            } else {
                throw new DeploymentException(
                                              "If service is not set, the WSDL must contain a single service definition");
            }
            Port port;
            if (getEndpoint() != null) {
                port = svc.getPort(getEndpoint());
                if (port == null) {
                    throw new DeploymentException("Cound not find port '" + getEndpoint()
                                                  + "' in wsdl for service '" + getService() + "'");
                }
            } else if (svc.getPorts().size() == 1) {
                port = (Port)svc.getPorts().values().iterator().next();
                setEndpoint(port.getName());
            } else {
                throw new DeploymentException("If endpoint is not set, the WSDL service '" + getService()
                                              + "' must contain a single port definition");
            }
            SOAPAddress soapAddress = WSDLUtils.getExtension(port, SOAPAddress.class);
            if (soapAddress != null) {
                soapAddress.setLocationURI(getLocationURI());
            } else {
                SOAP12Address soap12Address = WSDLUtils.getExtension(port, SOAP12Address.class);
                if (soap12Address != null) {
                    soap12Address.setLocationURI(getLocationURI());
                }
            }
            description = WSDLUtils.getWSDL11Factory().newWSDLWriter().getDocument(def);
            marshaler.setBinding(BindingFactory.createBinding(port));
        } finally {
            is.close();
        }
    }

    protected void validateWsdl2(HttpSoapConsumerMarshaler marshaler) throws Exception {
        new Wsdl2Validator().validate(marshaler);
    }

    /**
     * Use an inner class to avoid having a strong dependency on Woden if not needed
     */
    protected class Wsdl2Validator {
        public void validate(HttpSoapConsumerMarshaler marshaler) throws Exception {
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
                throw new DeploymentException(
                                              "If service is not set, the WSDL must contain a single service definition");
            }
            Endpoint endpoint;
            if (getEndpoint() != null) {
                endpoint = svc.getEndpoint(new NCName(getEndpoint()));
                if (endpoint == null) {
                    throw new DeploymentException("Cound not find endpoint '" + getEndpoint()
                                                  + "' in wsdl for service '" + getService() + "'");
                }
            } else if (svc.getEndpoints().length == 1) {
                endpoint = svc.getEndpoints()[0];
                setEndpoint(endpoint.getName().toString());
            } else {
                throw new DeploymentException("If endpoint is not set, the WSDL service '" + getService()
                                              + "' must contain a single port definition");
            }
            marshaler.setBinding(BindingFactory.createBinding(endpoint));
        }
    }

}
