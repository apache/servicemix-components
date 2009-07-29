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
package org.apache.servicemix.soap;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.Definition;
import javax.wsdl.Import;
import javax.wsdl.Types;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.wsdl.extensions.schema.SchemaReference;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.JbiConstants;
import org.apache.servicemix.common.endpoints.AbstractEndpoint;
import org.apache.servicemix.common.security.AuthenticationService;
import org.apache.servicemix.common.security.KeystoreManager;
import org.apache.servicemix.common.wsdl1.JbiExtension;
import org.apache.servicemix.soap.handlers.addressing.AddressingHandler;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.w3c.dom.Document;

import com.ibm.wsdl.Constants;

public abstract class SoapEndpoint extends AbstractEndpoint {

    protected ServiceEndpoint activated;
    protected SoapExchangeProcessor processor;
    protected Role role;
    protected URI defaultMep = JbiConstants.IN_OUT;
    protected boolean soap;
    protected String soapVersion;
    protected Resource wsdlResource;
    protected QName defaultOperation;
    protected QName targetInterfaceName;
    protected QName targetService;
    protected String targetEndpoint;
    protected List policies;
    protected Map wsdls = new HashMap();
    protected boolean dynamic;
    
    public SoapEndpoint() {
        policies = Collections.singletonList(new AddressingHandler());
    }

    public SoapEndpoint(boolean dynamic) {
        this();
        this.dynamic = dynamic;
    }

    public AuthenticationService getAuthenticationService() {
        return null;
    }
    
    public KeystoreManager getKeystoreManager() {
        return null;
    }
    
    /**
     * @return the policies
     */
    public List getPolicies() {
        return policies;
    }
    /**
     * @param policies the policies to set
     */
    public void setPolicies(List policies) {
        this.policies = policies;
    }
    /**
     * @return Returns the defaultMep.
     */
    public URI getDefaultMep() {
        return defaultMep;
    }
    /**
     * @param defaultMep The defaultMep to set.
     */
    public void setDefaultMep(URI defaultMep) {
        this.defaultMep = defaultMep;
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
    /**
     * @return Returns the role.
     */
    public Role getRole() {
        return role;
    }
    /**
     * @param role The role to set.
     */
    public void setRole(Role role) {
        this.role = role;
    }
    /**
     * @return Returns the soap.
     */
    public boolean isSoap() {
        return soap;
    }
    /**
     * @param soap The soap to set.
     */
    public void setSoap(boolean soap) {
        this.soap = soap;
    }
    /**
     * @return Returns the soapVersion.
     */
    public String getSoapVersion() {
        return soapVersion;
    }
    /**
     * @param soapVersion The soapVersion to set.
     */
    public void setSoapVersion(String soapVersion) {
        this.soapVersion = soapVersion;
    }
    /**
     * @return Returns the targetEndpoint.
     */
    public String getTargetEndpoint() {
        return targetEndpoint;
    }
    /**
     * @param targetEndpoint The targetEndpoint to set.
     */
    public void setTargetEndpoint(String targetEndpoint) {
        this.targetEndpoint = targetEndpoint;
    }
    /**
     * @return Returns the targetInterfaceName.
     */
    public QName getTargetInterfaceName() {
        return targetInterfaceName;
    }
    /**
     * @param targetInterfaceName The targetInterfaceName to set.
     */
    public void setTargetInterfaceName(QName targetInterfaceName) {
        this.targetInterfaceName = targetInterfaceName;
    }
    /**
     * @return Returns the targetServiceName.
     */
    public QName getTargetService() {
        return targetService;
    }
    /**
     * @param targetServiceName The targetServiceName to set.
     */
    public void setTargetService(QName targetServiceName) {
        this.targetService = targetServiceName;
    }
    /**
     * @return Returns the wsdlResource.
     */
    public Resource getWsdlResource() {
        return wsdlResource;
    }
    /**
     * @param wsdlResource The wsdlResource to set.
     */
    public void setWsdlResource(Resource wsdlResource) {
        this.wsdlResource = wsdlResource;
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
    
    /**
     * In addition to setting the description, attempts to set
     * the wsdlResource to fix a reloading issue.
     */
    @Override
    public void setDescription(Document description) {
        super.setDescription(description);
        String uri = description.getBaseURI();
        if (uri != null) {
            try {
                URL url = new URL(uri);
                logger.debug("Setting wsdlResource: " + url.toExternalForm());
                this.setWsdlResource(new UrlResource(url));
            } catch (MalformedURLException e) {
                logger.warn("Could not parse URL", e);
            }
        }
    }

    /**
     * Load the wsdl for this endpoint.
     */
    protected void loadWsdl() {
        // Load WSDL from the resource
        if (description == null && wsdlResource != null) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            try {
                Thread.currentThread().setContextClassLoader(serviceUnit.getConfigurationClassLoader());
                WSDLReader reader = WSDLFactory.newInstance().newWSDLReader(); 
                reader.setFeature(Constants.FEATURE_VERBOSE, false);
                Definition def = reader.readWSDL(wsdlResource.getURL().toString());
                overrideDefinition(def);
            } catch (Exception e) {
                logger.warn("Could not load description from resource", e);
            } finally {
                Thread.currentThread().setContextClassLoader(cl);
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
        if (definition != null) {
            try {
                mapDefinition(definition);
            } catch (Exception e) {
                logger.warn("Could not map wsdl definition to documents", e);
            }
        }
    }

    /**
     * Create a wsdl definition for a consumer endpoint.
     * Loads the target endpoint definition and add http binding
     * informations to it.
     */
    protected void retrieveProxiedEndpointDefinition() {
        if (logger.isDebugEnabled()) {
            logger.debug("Retrieving proxied endpoint definition");
        }
        try {
            ComponentContext ctx = this.serviceUnit.getComponent().getComponentContext();
            ServiceEndpoint ep = null;
            if (targetService != null && targetEndpoint != null) {
                ep = ctx.getEndpoint(targetService, targetEndpoint);
                if (ep == null && logger.isDebugEnabled()) {
                    logger.debug("Could not retrieve endpoint targetService/targetEndpoint");
                }
            }
            if (ep == null && targetService != null) {
                ServiceEndpoint[] eps = ctx.getEndpointsForService(targetService);
                if (eps != null && eps.length > 0) {
                    ep = eps[0];
                }
                if (ep == null && logger.isDebugEnabled()) {
                    logger.debug("Could not retrieve endpoint for targetService");
                }
            }
            if (ep == null && targetInterfaceName != null) {
                ServiceEndpoint[] eps = ctx.getEndpoints(targetInterfaceName);
                if (eps != null && eps.length > 0) {
                    ep = eps[0];
                }
                if (ep == null && logger.isDebugEnabled()) {
                    logger.debug("Could not retrieve endpoint for targetInterfaceName");
                }
            }
            if (ep == null && service != null && endpoint != null) {
                ep = ctx.getEndpoint(service, endpoint);
                if (ep == null && logger.isDebugEnabled()) {
                    logger.debug("Could not retrieve endpoint for service/endpoint");
                }
            }
            if (ep != null) {
                Document doc = ctx.getEndpointDescriptor(ep);
                if (doc != null) {
                    WSDLReader reader = WSDLFactory.newInstance().newWSDLReader(); 
                    reader.setFeature(Constants.FEATURE_VERBOSE, false);
                    Definition def = reader.readWSDL(null, doc);
                    if (def != null) {
                        overrideDefinition(def);
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Unable to retrieve target endpoint descriptor", e);
        }
    }
    
    /* (non-Javadoc)
     * @see org.servicemix.common.Endpoint#activate()
     */
    public void activate() throws Exception {
        if (dynamic) {
            if (getRole() == Role.PROVIDER) {
                processor = createProviderProcessor();
            } else {
                processor = createConsumerProcessor();
            }
        } else {
            ComponentContext ctx = this.serviceUnit.getComponent().getComponentContext();
            loadWsdl();
            if (getRole() == Role.PROVIDER) {
                activated = ctx.activateEndpoint(service, endpoint);
                processor = createProviderProcessor();
            } else {
                activated = createExternalEndpoint();
                ctx.registerExternalEndpoint(activated);
                processor = createConsumerProcessor();
            }
        }
        processor.init();
    }
    
    public void start() throws Exception {
        processor.start();
    }

    public void stop() throws Exception {
        processor.stop();
    }

    public void process(MessageExchange exchange) throws Exception {
        processor.process(exchange);
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.Endpoint#deactivate()
     */
    public void deactivate() throws Exception {
        processor.shutdown();
        if (activated != null) {
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
        }
    }

    protected abstract void overrideDefinition(Definition def) throws Exception;
    
    protected abstract SoapExchangeProcessor createProviderProcessor();
    
    protected abstract SoapExchangeProcessor createConsumerProcessor();
    
    protected abstract ServiceEndpoint createExternalEndpoint();

    protected WSDLReader createWsdlReader() throws WSDLException {
        WSDLFactory factory = WSDLFactory.newInstance();
        ExtensionRegistry registry = factory.newPopulatedExtensionRegistry();
        registerExtensions(registry);
        WSDLReader reader = factory.newWSDLReader();
        reader.setFeature(Constants.FEATURE_VERBOSE, false);
        reader.setExtensionRegistry(registry);
        return reader;
    }
    
    protected WSDLWriter createWsdlWriter() throws WSDLException {
        WSDLFactory factory = WSDLFactory.newInstance();
        ExtensionRegistry registry = factory.newPopulatedExtensionRegistry();
        registerExtensions(registry);
        WSDLWriter writer = factory.newWSDLWriter();
        //writer.setExtensionRegistry(registry);
        return writer;
    }
    
    protected void registerExtensions(ExtensionRegistry registry) {
        JbiExtension.register(registry);
    }

    
    protected void mapDefinition(Definition def) throws WSDLException {
        wsdls.put("main.wsdl", createWsdlWriter().getDocument(def));
        mapImports(def, "");
    }

    protected void mapImports(Definition def, String contextPath) throws WSDLException {
        // Add other imports to mapping
        Map imports = def.getImports();
        for (Iterator iter = imports.values().iterator(); iter.hasNext();) {
            List imps = (List) iter.next();
            for (Iterator iterator = imps.iterator(); iterator.hasNext();) {
                Import imp = (Import) iterator.next();
                Definition impDef = imp.getDefinition();
                String impLoc = imp.getLocationURI();
                if (impDef != null && impLoc != null && !URI.create(impLoc).isAbsolute()) {
                    impLoc = resolveRelativeURI(contextPath, impLoc);
                    wsdls.put(impLoc, createWsdlWriter().getDocument(impDef));
                    mapImports(impDef, getURIParent(impLoc));
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
                    mapSchema(schema, "");
                }
            }
        }
    }

    private void mapSchema(Schema schema, String contextPath) {
        Map schemaImports = schema.getImports();
        for (Iterator iter = schemaImports.values().iterator(); iter.hasNext();) {
            List imps = (List) iter.next();
            for (Iterator iterator = imps.iterator(); iterator.hasNext();) {
                SchemaImport schemaImport = (SchemaImport) iterator.next();
                Schema schemaImp = schemaImport.getReferencedSchema();
                String schemaLoc = schemaImport.getSchemaLocationURI();
                if (schemaLoc != null && schemaImp != null && schemaImp.getElement() != null && !URI.create(schemaLoc).isAbsolute()) {
                    schemaLoc = resolveRelativeURI(contextPath, schemaLoc);
                    wsdls.put(schemaLoc, schemaImp.getElement());
                    // recursively map imported schemas
                    mapSchema(schemaImp, getURIParent(schemaLoc));
                }
            }
        }
        List schemaIncludes = schema.getIncludes();
        for (Iterator iter = schemaIncludes.iterator(); iter.hasNext();) {
            SchemaReference schemaInclude = (SchemaReference) iter.next();
            Schema schemaImp = schemaInclude.getReferencedSchema();
            String schemaLoc = schemaInclude.getSchemaLocationURI();
            if (schemaLoc != null && schemaImp != null && schemaImp.getElement() != null && !URI.create(schemaLoc).isAbsolute()) {
                schemaLoc = resolveRelativeURI(contextPath, schemaLoc);
                wsdls.put(schemaLoc, schemaImp.getElement());
                // recursively map included schemas
                mapSchema(schemaImp, getURIParent(schemaLoc));
            }
        }
    }
    
    /**
     * Combines a relative path with a current directory, normalising any
     * relative pathnames like "." and "..".
     * <p>
     * Example:
     * <table>
     * <tr><th>context</th><th>path</th><th>resolveRelativeURI(context, path)</th></tr>
     * <tr><td>addressModification</td><td>../common/DataType.xsd</td><td>common/DataType.xsd</td></tr>
     * </table>
     *
     * @param context The current directory.
     * @param path The relative path to resolve against the current directory.
     * @return the normalised path.
     */
    private static String resolveRelativeURI(String context, String path) {
        if (context.length() > 0) {
            return URI.create(context + "/" + path).normalize().getPath();
        } else {
            return path;
        }
    }

    /**
     * Removes the filename part of a URI path.
     *
     * @param path A URI path part.
     * @return The URI path part with the filename part removed.
     */
    private static String getURIParent(String path) {
        return URI.create(path + "/..").normalize().getPath();
    }

    /**
     * @return Returns the wsdls.
     */
    public Map getWsdls() {
        return wsdls;
    }
    
}
