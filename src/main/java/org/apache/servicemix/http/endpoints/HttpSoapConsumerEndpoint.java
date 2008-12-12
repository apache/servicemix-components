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

import java.io.InputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Iterator;
import java.util.List;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.management.DeploymentException;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.Service;
import javax.wsdl.WSDLException;
import javax.wsdl.Import;
import javax.wsdl.Types;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.extensions.ExtensibilityElement;
import javax.wsdl.extensions.schema.Schema;
import javax.wsdl.extensions.schema.SchemaImport;
import javax.wsdl.extensions.schema.SchemaReference;
import javax.xml.namespace.QName;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.tools.wsdl.PortTypeDecorator;
import org.apache.servicemix.http.HttpComponent;
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
    private String soapVersion = "1.1";
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

    /**
     * Specifies the SOAP version to use when generating a wsdl binding for the wsdl of the target endpoint.
     *
     * @param soapVersion the soapVersion to use
     * @org.apache.xbean.Property description="Specifies the SOAP version to use when generating a wsdl binding for
     *                                  the wsdl of the target endpoint.  The default value is '1.1'"
     */
    public void setSoapVersion(String soapVersion) {
    	this.soapVersion = soapVersion;
    }

    /**
     * Get the defined SOAP version to use
     *
     * @return the defined SOAP version to use
     */
    public String getSoapVersion() {
    	return this.soapVersion;
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

    /**
     * Populate the description with the provided WSDL
     *
     * @throws DeploymentException if the provided WSDL can't be read
     */
    protected void useProvidedWsdl() throws Exception {
        description = DomUtil.parse(wsdl.getInputStream());
        definition = javax.wsdl.factory.WSDLFactory.newInstance().newWSDLReader().readWSDL(null, description);
    }

    /**
     * Populate the description with the target endpoint WSDL
     * 
     * @throws Exception if the proxed WSDL is not found
     */
    protected void useProxiedWsdl() throws Exception {
    	// get the component context
    	ComponentContext componentContext = this.serviceUnit.getComponent().getComponentContext();
    	// get the targetService endpoint
    	ServiceEndpoint targetEndpoint = null;
    	// if the user has defined the targetService and targetEndpoint, use it
    	if (getTargetService() != null && getTargetEndpoint() != null) {
    		targetEndpoint = componentContext.getEndpoint(getTargetService(), getTargetEndpoint());
    	}
    	// if the user has defined the targetService, use it
    	if (targetEndpoint == null && getTargetService() != null) {
    		ServiceEndpoint[] endPoints = componentContext.getEndpointsForService(this.getTargetService());
    		if (endPoints != null && endPoints.length > 0) {
    			targetEndpoint = endPoints[0];
    		}
    	}
    	// if the user has defined the targetInterfaceName, use it
    	if (targetEndpoint == null && this.getTargetInterface() != null) {
    		ServiceEndpoint[] endPoints = componentContext.getEndpoints(this.getTargetInterface());
    		if (endPoints != null && endPoints.length > 0) {
    			targetEndpoint = endPoints[0];
    		}
    	}
    	// if the targetEndpoint has not be identified, raise an JBI exception
    	if (targetEndpoint == null) {
    		throw new JBIException("The target endpoint is not found.");
    	}
    	// get the target endpoint descriptor
    	Document targetEndpointDescriptor = componentContext.getEndpointDescriptor(targetEndpoint);
        // TODO: check for null
    	// get the target endpoint definition (based on the descriptor)
    	Definition targetEndpointDefinition = javax.wsdl.factory.WSDLFactory.newInstance().newWSDLReader().readWSDL(null, targetEndpointDescriptor);
    	// check if the WSDL is the target component as a WSDL definition
    	if (targetEndpointDefinition == null) {
    		throw new JBIException("The target component has no WSDL definition.");
    	}
    	// get the targetService in the endpoint definition
    	Service targetServiceInDefinition = targetEndpointDefinition.getService(this.getTargetService());
    	// get the targetEndpoint port
    	Port targetEndpointPort = (targetServiceInDefinition != null) ? targetServiceInDefinition.getPort(this.getTargetEndpoint()) : null;
    	// get the targetEndpoint port type
    	PortType targetEndpointPortType = (targetEndpointPort != null) ? targetEndpointPort.getBinding().getPortType() : null;
    	// try to get the endpoint port type using the first defined in the definition
    	if (targetEndpointPortType == null) {
    		QName[] portTypes = (QName[]) targetEndpointDefinition.getPortTypes().keySet().toArray(new QName[0]);
    		if(portTypes != null && portTypes.length > 0) {
    			targetEndpointPortType = targetEndpointDefinition.getPortType(portTypes[0]);
    		}
    	}
    	if(targetEndpointPortType == null) {
    		throw new JBIException("The target port type is not defined.");
    	}
    	// if the port type is found, make a cleanup on the definition
    	if (targetEndpointPortType != null) {
    		// cleanup port types
    		QName[] qnames = (QName[]) targetEndpointDefinition.getPortTypes().keySet().toArray(new QName[0]);
    		for(int i = 0; i < qnames.length; i++) {
    			if(!qnames[i].equals(targetEndpointPortType.getQName())) {
    				targetEndpointDefinition.removePortType(qnames[i]);
    			}
    		}
    		// cleanup services
    		qnames = (QName[]) targetEndpointDefinition.getServices().keySet().toArray(new QName[0]);
    		for(int i = 0; i < qnames.length; i++) {
    			targetEndpointDefinition.removeService(qnames[i]);
    		}
    		// cleanup bindings
    		qnames = (QName[]) targetEndpointDefinition.getBindings().keySet().toArray(new QName[0]);
    		for(int i = 0; i < qnames.length; i++) {
    			targetEndpointDefinition.removeBinding(qnames[i]);
    		}
    		// format the location URI
    		String location = this.getLocationURI();
    		if(!location.endsWith("/")) {
    			location += "/";
    		}
    		// construct the location URI using HTTP component configuration
    		HttpComponent httpComponent = (HttpComponent) this.serviceUnit.getComponent();
    		if (httpComponent.getConfiguration().isManaged()) {
    			// save the path
    			String path = new URI(location).getPath();
    			// format the new location
    			location = "http://localhost";
    			// construct the location using the HttpComponent configuration
    			if(httpComponent.getHost() != null) {
    				// prefix with the protocol
    				if(httpComponent.getProtocol() != null) {
    					// the protocol is defined in the component configuration
    					location = httpComponent.getProtocol() + "://";
    				}
    				else {
    					// define HTTP protocol as default
    					location = "http://";
    				}
    				// add the configured host
    				location += httpComponent.getHost();
    				// add the configured port number (if it's not the 80)
    				if(httpComponent.getPort() != 80) {
    					location += ":" + httpComponent.getPort();
    				}
    				// add the path (location URI)
    				if(httpComponent.getPath() != null) {
    					location += httpComponent.getPath();
    				}
    			}
    			// add the component configuration path mapping
    			location += httpComponent.getConfiguration().getMapping() + path; 
    		}
			if (targetEndpointPortType.getQName().getNamespaceURI().equals(this.getService().getNamespaceURI())) {
				PortTypeDecorator.decorate(targetEndpointDefinition, targetEndpointPortType, location, endpoint + "Binding", service.getLocalPart(), endpoint, soapVersion);
				definition = targetEndpointDefinition;
			}
			else {
				definition = PortTypeDecorator.createImportDef(targetEndpointDefinition, service.getNamespaceURI(), "porttypedef.wsdl");
				PortTypeDecorator.decorate(definition, targetEndpointPortType, location, endpoint + "Binding", service.getLocalPart(), endpoint, soapVersion);
			}
            description = javax.wsdl.factory.WSDLFactory.newInstance().newWSDLWriter().getDocument(definition);
    	}
    }

    @Override
    public void activate() throws Exception {
        if (wsdl != null) {
            // the user has provided a WSDL
            useProvidedWsdl();
        } else {
            // the user hasn't provided a WSDL, use the target endpoint one (if exist)
            useProxiedWsdl();
        }
    	// validate the WSDL in description
    	// get the document element
    	Element element = description.getDocumentElement();
    	// validate depending of the namespace
    	if (WSDLUtils.WSDL1_NAMESPACE.equals(element.getNamespaceURI())) {
    		validateWsdl1();
        } else if (WSDLUtils.WSDL2_NAMESPACE.equals(element.getNamespaceURI())) {
            validateWsdl2();
    	} else {
    		throw new DeploymentException("The WSDL namespace " + element.getNamespaceURI() + " is not supported");
    	}
    	super.activate();
    }
    
    @Override
    public void validate() throws DeploymentException {
        // the validate method is dedicated to the attributes value check

        // 1 - check the target attributes
    	// if the user doesn't define the targetService or targetInterface or targetUri,
    	// define the targetService/targetEndpoint as the current one
        if (getTargetService() == null && getTargetInterface() == null && getTargetUri() == null) {
            setTargetService(getService());
            setTargetEndpoint(getEndpoint());
        }
        
        // 2 - check the marshaler
        HttpSoapConsumerMarshaler marshaler;
        if (getMarshaler() instanceof HttpSoapConsumerMarshaler) {
            marshaler = (HttpSoapConsumerMarshaler) getMarshaler();
        } else if (getMarshaler() == null) {
            marshaler = new HttpSoapConsumerMarshaler();
            setMarshaler(marshaler);
        } else {
            throw new DeploymentException("The configured marshaler must inherit HttpSoapConsumerMarshaler");
        }
        // define the marshaler properties
        marshaler.setUseJbiWrapper(useJbiWrapper);
        marshaler.setPolicies(policies);
        super.validate();
    }

    protected void loadStaticResources() throws Exception {
        mapDefinition(definition);
    }

    protected void validateWsdl1() throws Exception {
        // get the WSDL definition from the description
        if (definition == null) {
            throw new DeploymentException("The WSDL definition is not found in the description");
        }
        if (validateWsdl) {
            WSIBPValidator validator = new WSIBPValidator(definition);
            if (!validator.isValid()) {
                throw new DeploymentException("WSDL is not WS-I BP compliant: " + validator.getErrors());
            }
        }
        Service svc;
        if (getService() != null) {
            svc = definition.getService(getService());
            if (svc == null) {
                throw new DeploymentException("Could not find service '" + getService() + "' in wsdl");
            }
        } else if (definition.getServices().size() == 1) {
            svc = (Service) definition.getServices().values().iterator().next();
            setService(svc.getQName());
        } else {
            throw new DeploymentException("If service is not set, the WSDL must contain a single service definition");
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
        description = WSDLUtils.getWSDL11Factory().newWSDLWriter().getDocument(definition);
        ((HttpSoapConsumerMarshaler) getMarshaler()).setBinding(BindingFactory.createBinding(port));
    }

    protected void validateWsdl2() throws Exception {
        new Wsdl2Validator().validate();
    }

    /**
     * Use an inner class to avoid having a strong dependency on Woden if not needed
     */
    protected class Wsdl2Validator {
        public void validate() throws Exception {
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
            ((HttpSoapConsumerMarshaler) getMarshaler()).setBinding(BindingFactory.createBinding(endpoint));
        }
    }

    protected void mapDefinition(Definition def) throws WSDLException {
        addResource("main.wsdl", javax.wsdl.factory.WSDLFactory.newInstance().newWSDLWriter().getDocument(def));
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
                    addResource(impLoc, javax.wsdl.factory.WSDLFactory.newInstance().newWSDLWriter().getDocument(impDef));
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
                    addResource(schemaLoc, schemaImp.getElement());
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
                addResource(schemaLoc, schemaImp.getElement());
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

}
