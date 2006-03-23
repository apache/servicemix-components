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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.http.HTTPAddress;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.common.wsdl1.JbiExtension;
import org.apache.servicemix.common.xbean.XBeanServiceUnit;
import org.apache.servicemix.http.processors.ConsumerProcessor;
import org.apache.servicemix.http.processors.ProviderProcessor;
import org.apache.servicemix.soap.SoapEndpoint;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

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
    protected ExchangeProcessor processor;
    protected ServiceEndpoint activated;
    protected Role role;
    protected URI defaultMep;
    protected String locationURI;
    protected boolean soap;
    protected String soapVersion;
    protected Resource wsdlResource;
    protected QName defaultOperation;
    
    /* (non-Javadoc)
     * @see org.apache.servicemix.common.Endpoint#getProcessor()
     */
    public ExchangeProcessor getProcessor() {
        return this.processor;
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.Endpoint#activate()
     */
    public void activate() throws Exception {
        ComponentContext ctx = this.serviceUnit.getComponent().getComponentContext();
        loadWsdl();
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
    
    /**
     * Load the wsdl for this endpoint.
     */
    protected void loadWsdl() {
        // Load WSDL from the resource
        if (description == null && wsdlResource != null) {
            InputStream is = null;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                if (serviceUnit instanceof XBeanServiceUnit) {
                    XBeanServiceUnit su = (XBeanServiceUnit) serviceUnit;
                    Thread.currentThread().setContextClassLoader(su.getKernel().getClassLoaderFor(su.getConfiguration()));
                }
                is = wsdlResource.getInputStream();
                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setNamespaceAware(true);
                Definition def = WSDLFactory.newInstance().newWSDLReader().readWSDL(null, new InputSource(is));
                overrideDefinition(def);
            } catch (Exception e) {
                logger.warn("Could not load description from resource", e);
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        // Ignore
                    }
                }
            }
        }
        // If the endpoint is a consumer, try to find
        // the proxied endpoint description
        if (description == null && definition == null && getRole() == Role.CONSUMER) {
            retrieveProxiedEndpointDefinition();
        }
        // If the wsdl definition is provided,
        // convert it to a DOM document
        if (description == null && definition != null) {
            try {
                description = WSDLFactory.newInstance().newWSDLWriter().getDocument(definition);
            } catch (Exception e) {
                logger.warn("Could not create document from wsdl description", e);
            }
        }
        // If the dom description is provided
        // convert it to a WSDL definition
        if (definition == null && description != null) {
            try {
                definition = WSDLFactory.newInstance().newWSDLReader().readWSDL(null, description);
            } catch (Exception e) {
                logger.warn("Could not create wsdl definition from dom document", e);
            }
        }
    }

    /**
     * Create a wsdl definition for a consumer endpoint.
     * Loads the target endpoint definition and add http binding
     * informations to it.
     */
    protected void retrieveProxiedEndpointDefinition() {
        try {
            if (service != null && endpoint != null) {
                ComponentContext ctx = this.serviceUnit.getComponent().getComponentContext();
                ServiceEndpoint se = ctx.getEndpoint(service, endpoint);
                if (se != null) {
                    Document doc = ctx.getEndpointDescriptor(se);
                    if (doc != null) {
                        WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
                        Definition def = reader.readWSDL(null, doc);
                        if (def != null) {
                            overrideDefinition(def);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Unable to retrieve target endpoint descriptor", e);
        }
    }
    
    protected void overrideDefinition(Definition def) {
        Service svc = def.getService(service);
        if (svc != null) {
            Port port = svc.getPort(endpoint);
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
    }

    public Resource getWsdlResource() {
        return wsdlResource;
    }

    public void setWsdlResource(Resource wsdlResource) {
        this.wsdlResource = wsdlResource;
    }

    /**
     * @return Returns the defaultOperation.
     */
    public QName getDefaultOperation() {
        return defaultOperation;
    }

    /**
     * @param defaultOperation The defaultOperation to set.
     */
    public void setDefaultOperation(QName defaultOperation) {
        this.defaultOperation = defaultOperation;
    }

}
