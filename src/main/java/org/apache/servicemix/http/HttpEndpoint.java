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
package org.apache.servicemix.http;

import java.net.URI;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.http.HTTPAddress;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.ExternalEndpoint;
import org.apache.servicemix.common.ManagementSupport;
import org.apache.servicemix.common.tools.wsdl.PortTypeDecorator;
import org.apache.servicemix.common.security.AuthenticationService;
import org.apache.servicemix.common.security.KeystoreManager;
import org.apache.servicemix.http.processors.ConsumerProcessor;
import org.apache.servicemix.http.processors.ProviderProcessor;
import org.apache.servicemix.soap.SoapEndpoint;
import org.apache.servicemix.soap.SoapExchangeProcessor;

import com.ibm.wsdl.extensions.http.HTTPAddressImpl;

/**
 * an HTTP endpoint. This is the base for all HTTP endpoints.
 * 
 * @author gnodet
 * @version $Revision$
 * @org.apache.xbean.XBean element="endpoint" description="the base element for all HTTP endpoints"
 */
public class HttpEndpoint extends SoapEndpoint implements HttpEndpointType {

    protected ExtensibilityElement binding;
    protected String locationURI;
    protected SslParameters ssl;
    protected String authMethod;
    protected String soapAction;
    protected BasicAuthCredentials basicAuthentication;
    protected ProxyParameters proxy;
    protected boolean synchronous;
    protected boolean wantContentTypeHeaderFromExchangeIntoHttpRequest;
    protected int timeout;

    public HttpEndpoint() {
    }

    public HttpEndpoint(boolean dynamic) {
        super(dynamic);
    }

    /**
     * Determines if the HTTP provider processor copies the HTTP headers from the HTTP response into the JBI exchange.
     * 
     * @return <code>true</code> if the HTTP headers will be copied into the exchange
     */
    public boolean isWantContentTypeHeaderFromExchangeIntoHttpRequest() {
        return wantContentTypeHeaderFromExchangeIntoHttpRequest;
    }

    /**
     * Specifies if the HTTP provider processor copies the HTTP headers from the HTTP response into the JBI exchange. If the headers
     * will be used for a new HTTP reuquest, setting this to <code>true</code> leads to an error.
     * 
     * @param wantContentTypeHeaderFromExchangeIntoHttpRequest <code>true</code> if the HTTP headers will be copied into the exchange
     * @org.apache.xbean.Property description="Specifies if the HTTP provider will copy the HTTP request headers into the JBI
     *                            exchange. The default is <code>false</code>. This value overrides the value set for the component
     *                            using the <code>configuration</code> element."
     */
    public void setWantContentTypeHeaderFromExchangeIntoHttpRequest(boolean wantContentTypeHeaderFromExchangeIntoHttpRequest) {
        this.wantContentTypeHeaderFromExchangeIntoHttpRequest = wantContentTypeHeaderFromExchangeIntoHttpRequest;
    }

    /**
     * @return the synchronous
     */
    public boolean isSynchronous() {
        return synchronous;
    }

    /**
     * @param synchronous the synchronous to set
     */
    public void setSynchronous(boolean synchronous) {
        this.synchronous = synchronous;
    }

    /**
     * @return the soapAction
     */
    public String getSoapAction() {
        return soapAction;
    }

    /**
     * @param soapAction the soapAction to set
     */
    public void setSoapAction(String soapAction) {
        this.soapAction = soapAction;
    }

    /**
     * Returns a string describing the authentication scheme being used by an endpoint.
     * 
     * @return a string representing the authentication method used by an endpoint
     */
    public String getAuthMethod() {
        return authMethod;
    }

    /**
     * Specifies the authentication method used by a secure endpoint. The authentication method is a string naming the scheme used
     * for authenticating users.
     * 
     * @param authMethod a string naming the authentication scheme a secure endpoint should use
     * @org.apache.xbean.Property description="a string naming the scheme used for authenticating users"
     */
    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    /**
     * @return Returns the ssl.
     */
    public SslParameters getSsl() {
        return ssl;
    }

    /**
     * Sets the properties used to configure SSL for the endpoint.
     * 
     * @param ssl an <code>SslParameters</code> object containing the SSL properties
     * @org.apache.xbean.Property description="a bean containing the SSL configuration properties"
     */
    public void setSsl(SslParameters ssl) {
        this.ssl = ssl;
    }

    public ExtensibilityElement getBinding() {
        return binding;
    }

    public void setBinding(ExtensibilityElement binding) {
        this.binding = binding;
    }

    /**
     * Returns the URI to which the endpoint sends requests.
     * 
     * @return a string representing the URI to which requests are sent
     */
    public String getLocationURI() {
        return locationURI;
    }

    /**
     * Sets the URI to which an endpoint sends requests.
     * 
     * @param locationUri a string representing the URI
     * @org.apache.xbean.Property description="the URI to which a provider endpoint sends requests"
     */
    public void setLocationURI(String locationUri) {
        this.locationURI = locationUri;
    }

    /**
     * Sets the authentication data used for provider endpoints using basic authentication.
     * 
     * @param basicAuthCredentials the <code>BasicAuthCredentials</code> that will be used for authentication
     * @org.apache.xbean.Property description="authentication data for using basic HTTP authentication."
     */
    public void setBasicAuthentication(BasicAuthCredentials basicAuthCredentials) {
        this.basicAuthentication = basicAuthCredentials;
    }

    /**
     * Gets the authentication data used for provider endpoints using basic authentication.
     * 
     * @return the <code>BasicAuthCredentials</code> to use for authentication
     */
    public BasicAuthCredentials getBasicAuthentication() {
        return basicAuthentication;
    }

    /**
     * @return Returns the proxy.
     */
    public ProxyParameters getProxy() {
        return this.proxy;
    }

    /**
     * Sets the configuration information for the proxy used by a HTTP consumer endpoint. This configuration overrides the proxy
     * configuraiton specified at the component level.
     * 
     * @param proxy The proxy to set.
     * @org.apache.xbean.Property description="configuration used to establish a proxy for sending HTTP requests. This configuration overrides that which is set at the component level."
     */
    public void setProxy(ProxyParameters proxy) {
        this.proxy = proxy;
    }

    /**
     * Sets the endpoint's role. HTTP endpoints can be either consumers or providers. Specifying <code>consumer</code> for the role
     * defines the endpoint as a consumer which exposes an endpoint at a URL. Specifying <code>provider</code> for the role defines
     * the endpoint as a provider that sends HTTP requests to a URL.
     * 
     * @param role a string specifying the role of the endpoint
     * @org.apache.xbean.Property alias="role" description= "HTTP endpoints can be either consumers or providers. Specifying
     *                            <code>consumer</code> for the role defines the endpoint as a consumer which exposes an endpoint at
     *                            a URL. Specifying <code>provider</code> for the role defines the endpoint as a provider that sends
     *                            HTTP requests to a URL."
     */
    public void setRoleAsString(String role) {
        super.setRoleAsString(role);
    }

    /**
     * Specifies the timeout value for an HTTP endpoint. The timeout is specified in milliseconds. The default value is 0 which
     * means that the endpoint will never timeout.
     * 
     * @org.apache.xbean.Property description=
     *                            "the number of milliseconds before the endpoint times out. The default value is 0 which means that the endpoint will never timeout."
     * @param timeout the length time, in milliseconds, to wait before timing out
     */
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    /**
     * Returns the timeout value for an HTTP endpoint.
     * 
     * @return the timeout specified in milliseconds
     */
    public int getTimeout() {
        return timeout;
    }

    public void reloadWsdl() {
        description = null;
        definition = null;
        loadWsdl();
    }

    protected PortType getTargetPortType(Definition def) {
        PortType portType = null;
        // If the WSDL description only contain one PortType, use it
        if (def.getServices().size() == 0 && def.getPortTypes().size() == 1) {
            if (logger.isDebugEnabled()) {
                logger.debug("WSDL only defines a PortType, using this one");
            }
            portType = (PortType)def.getPortTypes().values().iterator().next();
        } else if (targetInterfaceName != null) {
            portType = def.getPortType(targetInterfaceName);
            if (portType == null && logger.isDebugEnabled()) {
                logger.debug("PortType for targetInterfaceName could not be found");
            }
        } else if (targetService != null && targetEndpoint != null) {
            Service svc = def.getService(targetService);
            Port port = (svc != null) ? svc.getPort(targetEndpoint) : null;
            portType = (port != null) ? port.getBinding().getPortType() : null;
            if (portType == null && logger.isDebugEnabled()) {
                logger.debug("PortType for targetService/targetEndpoint could not be found");
            }
        } else if (targetService != null) {
            Service svc = def.getService(targetService);
            if (svc != null && svc.getPorts().size() == 1) {
                Port port = (Port)svc.getPorts().values().iterator().next();
                portType = (port != null) ? port.getBinding().getPortType() : null;
            }
            if (portType == null && logger.isDebugEnabled()) {
                logger.debug("Service for targetService could not be found");
            }
        } else if (interfaceName != null) {
            portType = def.getPortType(interfaceName);
            if (portType == null && logger.isDebugEnabled()) {
                logger.debug("Service for targetInterfaceName could not be found");
            }
        } else {
            Service svc = def.getService(service);
            Port port = (svc != null) ? svc.getPort(endpoint) : null;
            portType = (port != null && port.getBinding() != null) ? port.getBinding().getPortType() : null;
            if (portType == null && logger.isDebugEnabled()) {
                logger.debug("Port for service/endpoint could not be found");
            }
        }
        return portType;
    }

    protected void overrideDefinition(Definition def) throws Exception {
        PortType portType = getTargetPortType(def);
        if (portType != null) {
            QName[] names = (QName[])def.getPortTypes().keySet().toArray(new QName[0]);
            for (int i = 0; i < names.length; i++) {
                if (!names[i].equals(portType.getQName())) {
                    def.removePortType(names[i]);
                }
            }
            names = (QName[])def.getServices().keySet().toArray(new QName[0]);
            for (int i = 0; i < names.length; i++) {
                def.removeService(names[i]);
            }
            names = (QName[])def.getBindings().keySet().toArray(new QName[0]);
            for (int i = 0; i < names.length; i++) {
                def.removeBinding(names[i]);
            }
            String location = getLocationURI();
            if (!location.endsWith("/")) {
                location += "/";
            }
            HttpComponent comp = (HttpComponent)getServiceUnit().getComponent();
            if (comp.getConfiguration().isManaged()) {
                // Save the path
                String path = new URI(location).getPath();
                location = "http://localhost";
                if (comp.getHost() != null) {
                    if (comp.getProtocol() != null) {
                        location = comp.getProtocol() + "://";
                    } else {
                        location = "http://";
                    }
                    location += comp.getHost();
                    if (comp.getPort() != 80) {
                        location += ":" + comp.getPort();
                    }
                    if (comp.getPath() != null) {
                        location += comp.getPath();
                    }
                }
                location += comp.getConfiguration().getMapping() + path;
            }
            if (portType.getQName().getNamespaceURI().equals(service.getNamespaceURI())) {
                if (isSoap()) {
                    PortTypeDecorator.decorate(def, portType, location, endpoint + "Binding", service
                        .getLocalPart(), endpoint, soapVersion);
                    definition = def;
                } else {
                    Binding bnd = def.createBinding();
                    bnd.setPortType(portType);
                    bnd.setQName(new QName(service.getNamespaceURI(), endpoint + "Binding"));
                    bnd.setUndefined(false);
                    def.addBinding(bnd);
                    Port port = def.createPort();
                    port.setName(endpoint);
                    port.setBinding(bnd);
                    HTTPAddress address = new HTTPAddressImpl();
                    address.setLocationURI(location);
                    port.addExtensibilityElement(address);
                    def.addNamespace("http", "http://schemas.xmlsoap.org/wsdl/http/");
                    Service svc = def.createService();
                    svc.setQName(service);
                    svc.addPort(port);
                    def.addService(svc);
                    definition = def;
                }
            } else {
                definition = PortTypeDecorator.createImportDef(def, service.getNamespaceURI(),
                                                               "porttypedef.wsdl");
                PortTypeDecorator.decorate(definition, portType, location, endpoint + "Binding", service
                    .getLocalPart(), endpoint, soapVersion);
            }
        }
    }

    protected SoapExchangeProcessor createProviderProcessor() {
        return new ProviderProcessor(this);
    }

    protected SoapExchangeProcessor createConsumerProcessor() {
        return new ConsumerProcessor(this);
    }

    protected ServiceEndpoint createExternalEndpoint() {
        return new ExternalEndpoint(getServiceUnit().getComponent().getEPRElementName(), getLocationURI(),
                                    getService(), getEndpoint(), getInterfaceName());
    }

    public AuthenticationService getAuthenticationService() {
        HttpComponent comp = (HttpComponent)getServiceUnit().getComponent();
        return AuthenticationService.Proxy.create(comp.getAuthenticationService());
    }

    public KeystoreManager getKeystoreManager() {
        HttpComponent comp = (HttpComponent)getServiceUnit().getComponent();
        return KeystoreManager.Proxy.create(comp.getKeystoreManager());
    }

    public void validate() throws DeploymentException {
        if (getRole() == null) {
            throw failure("deploy", "Endpoint must have a defined role", null);
        }
        if (getLocationURI() == null) {
            throw failure("deploy", "Endpoint must have a defined locationURI", null);
        }
        if (!isSoap() && getRole() == Role.CONSUMER && getDefaultMep() == null) {
            throw failure("deploy", "Non soap endpoints must have a defined defaultMep", null);
        }
    }

    protected DeploymentException failure(String task, String info, Throwable e) {
        ManagementSupport.Message msg = new ManagementSupport.Message();
        msg.setComponent(serviceUnit.getComponent().getComponentName());
        msg.setTask(task);
        msg.setResult("FAILED");
        msg.setType("ERROR");
        msg.setMessage(info);
        msg.setException(e);
        return new DeploymentException(ManagementSupport.createComponentMessage(msg));
    }

}
