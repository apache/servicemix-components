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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jbi.component.ComponentLifeCycle;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.Types;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.http.HTTPAddress;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.http.processors.ConsumerProcessor;
import org.apache.servicemix.http.processors.ProviderProcessor;
import org.apache.servicemix.http.tools.PortTypeDecorator;
import org.apache.servicemix.jbi.security.auth.AuthenticationService;
import org.apache.servicemix.jbi.security.keystore.KeystoreManager;
import org.apache.servicemix.soap.SoapEndpoint;

import com.ibm.wsdl.extensions.http.HTTPAddressImpl;

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
    protected Map wsdls = new HashMap();
    protected SslParameters ssl;
    protected String authMethod;
    protected String soapAction;
    
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
     * @return the authMethod
     */
    public String getAuthMethod() {
        return authMethod;
    }

    /**
     * @param authMethod the authMethod to set
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
     * @param ssl The ssl to set.
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

    protected PortType getTargetPortType(Definition def) {
        PortType portType = null;
        // If the WSDL description only contain one PortType, use it
        if (def.getServices().size() == 0 && def.getPortTypes().size() == 1) {
            if (logger.isDebugEnabled()) {
                logger.debug("WSDL only defines a PortType, using this one");
            }
            portType = (PortType) def.getPortTypes().values().iterator().next();
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
                Port port = (Port) svc.getPorts().values().iterator().next();
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
            QName[] names = (QName[]) def.getPortTypes().keySet().toArray(new QName[0]);
            for (int i = 0; i < names.length; i++) {
                if (!names[i].equals(portType.getQName())) {
                    def.removePortType(names[i]);
                }
            }
            names = (QName[]) def.getServices().keySet().toArray(new QName[0]);
            for (int i = 0; i < names.length; i++) {
                def.removeService(names[i]);
            }
            names = (QName[]) def.getBindings().keySet().toArray(new QName[0]);
            for (int i = 0; i < names.length; i++) {
                def.removeBinding(names[i]);
            }
            String location = getLocationURI();
            HttpLifeCycle lf = (HttpLifeCycle) getServiceUnit().getComponent().getLifeCycle();
            if (lf.getConfiguration().isManaged()) {
                // TODO: need to find the port of the web server
                location = "http://localhost" + lf.getConfiguration().getMapping() + new URI(location).getPath();
            }
            if (portType.getQName().getNamespaceURI().equals(service.getNamespaceURI())) {
                if (isSoap()) {
                    PortTypeDecorator.decorate(
                            def, 
                            portType, 
                            location, 
                            endpoint + "Binding",
                            service.getLocalPart(),
                            endpoint);       
                    definition = def;
                    wsdls.put("main.wsdl", def);
                } else {
                    Binding binding = def.createBinding();
                    binding.setPortType(portType);
                    binding.setQName(new QName(service.getNamespaceURI(), endpoint + "Binding"));
                    binding.setUndefined(false);
                    def.addBinding(binding);
                    Port port = def.createPort();
                    port.setName(endpoint);
                    port.setBinding(binding);
                    HTTPAddress address = new HTTPAddressImpl();
                    address.setLocationURI(location);
                    port.addExtensibilityElement(address);
                    def.addNamespace("http", "http://schemas.xmlsoap.org/wsdl/http/");
                    Service svc = def.createService();
                    svc.setQName(service);
                    svc.addPort(port);
                    def.addService(svc);
                    definition = def;
                    wsdls.put("main.wsdl", def);
                }
            } else {
                definition = PortTypeDecorator.createImportDef(def, service.getNamespaceURI(), "porttypedef.wsdl");
                PortTypeDecorator.decorate(
                        definition, 
                        portType, 
                        location, 
                        endpoint + "Binding",
                        service.getLocalPart(),
                        endpoint);       
                wsdls.put("main.wsdl", definition);
                wsdls.put("porttypedef.wsdl", def);
            }
            mapImports(def);
        }
    }
    
    protected void mapImports(Definition def) {
        // Add other imports to mapping
        Map imports = def.getImports();
        for (Iterator iter = imports.values().iterator(); iter.hasNext();) {
            List imps = (List) iter.next();
            for (Iterator iterator = imps.iterator(); iterator.hasNext();) {
                Import imp = (Import) iterator.next();
                Definition impDef = imp.getDefinition();
                String impLoc = imp.getLocationURI();
                if (impDef != null && impLoc != null && !URI.create(impLoc).isAbsolute()) {
                    wsdls.put(impLoc, impDef);
                    mapImports(impDef);
                }
            }
        }
        // Add schemas to mapping
        Types types = def.getTypes();
        if (types != null) {
            for (Iterator it = types.getExtensibilityElements().iterator(); it.hasNext();) {
                ExtensibilityElement ee = (ExtensibilityElement) it.next();
                if (ee instanceof Schema) {
                    Schema schema = (Schema) ee;
                    Map schemaImports = schema.getImports();
                    for (Iterator iter = schemaImports.values().iterator(); iter.hasNext();) {
                        List imps = (List) iter.next();
                        for (Iterator iterator = imps.iterator(); iterator.hasNext();) {
                            SchemaImport schemaImport = (SchemaImport) iterator.next();
                            Schema schemaImp = schemaImport.getReferencedSchema();
                            String schemaLoc = schemaImport.getSchemaLocationURI();
                            if (schemaLoc != null && schemaImp != null && schemaImp.getElement() != null && !URI.create(schemaLoc).isAbsolute()) {
                                wsdls.put(schemaLoc, schemaImp.getElement());
                            }
                        }
                    }
                }
            }
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

    /**
     * @return Returns the wsdls.
     */
    public Map getWsdls() {
        return wsdls;
    }

    public AuthenticationService getAuthenticationService() {
        ComponentLifeCycle lf = getServiceUnit().getComponent().getLifeCycle();
        return ((HttpLifeCycle) lf).getAuthenticationService();
    }

    public KeystoreManager getKeystoreManager() {
        ComponentLifeCycle lf = getServiceUnit().getComponent().getLifeCycle();
        return ((HttpLifeCycle) lf).getKeystoreManager();
    }

}
