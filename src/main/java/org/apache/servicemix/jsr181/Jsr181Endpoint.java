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
package org.apache.servicemix.jsr181;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.InvalidParameterException;
import java.util.HashMap;
import java.util.Map;

import javax.jbi.component.ComponentContext;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.Binding;
import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.PortType;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.EndpointComponentContext;
import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.common.ManagementSupport;
import org.apache.servicemix.common.tools.wsdl.WSDLFlattener;
import org.apache.servicemix.jsr181.xfire.JbiFaultSerializer;
import org.apache.servicemix.jsr181.xfire.ServiceFactoryHelper;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.annotations.AnnotationServiceFactory;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.service.binding.ObjectServiceFactory;
import org.codehaus.xfire.service.invoker.BeanInvoker;
import org.codehaus.xfire.soap.SoapConstants;
import org.springframework.core.io.Resource;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.ibm.wsdl.Constants;

/**
 * 
 * @author gnodet
 * @version $Revision$
 * @org.apache.xbean.XBean element="endpoint"
 *                  description="A jsr181 endpoint"
 * 
 */
public class Jsr181Endpoint extends Endpoint {

    protected Object pojo;
    protected String pojoClass;
    protected String annotations;
    protected String typeMapping;
    protected String serviceInterface;
    protected String style = "wrapped";
    
    protected ServiceEndpoint activated;
    protected Service xfireService;
    protected ExchangeProcessor processor;
    protected Resource wsdlResource;
    protected boolean mtomEnabled = false;
    
    public Jsr181Endpoint() {
        processor = new Jsr181ExchangeProcessor(this);
    }
    
    /**
     * @return the style
     */
    public String getStyle() {
        return style;
    }

    /**
     * Service style: can be <code>rpc</code>, <code>document</code>,
     * <code>wrapped</code> or <code>message</code>.
     * Default to <code>wrapped</code>
     * @param style the style to set
     */
    public void setStyle(String style) {
        this.style = style;
    }

    /**
     * @return the wsdlResource
     */
    public Resource getWsdlResource() {
        return wsdlResource;
    }

    /**
     * @param wsdlResource the wsdlResource to set
     */
    public void setWsdlResource(Resource wsdlResource) {
        this.wsdlResource = wsdlResource;
    }

    /**
     * @return Returns the pojo.
     */
    public Object getPojo() {
        return pojo;
    }

    /**
     * @param pojo The pojo to set.
     */
    public void setPojo(Object pojo) {
        this.pojo = pojo;
    }

    /**
     * @return the mtomEnabled
     */
    public boolean isMtomEnabled() {
        return mtomEnabled;
    }

    /**
     * @param mtomEnabled the mtomEnabled to set
     */
    public void setMtomEnabled(boolean mtomEnabled) {
        this.mtomEnabled = mtomEnabled;
    }

    /**
     * @return Returns the xfireService.
     */
    public Service getXFireService() {
        return xfireService;
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.Endpoint#getRole()
     * @org.apache.xbean.XBean hide="true"
     */
    public Role getRole() {
        return Role.PROVIDER;
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.Endpoint#activate()
     */
    public void activate() throws Exception {
        logger = this.serviceUnit.getComponent().getLogger();
        ComponentContext ctx = this.serviceUnit.getComponent().getComponentContext();
        activated = ctx.activateEndpoint(service, endpoint);
        injectContext(new EndpointComponentContext(this));
        processor.start();
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.Endpoint#deactivate()
     */
    public void deactivate() throws Exception {
        ServiceEndpoint ep = activated;
        activated = null;
        processor.stop();
        ComponentContext ctx = this.serviceUnit.getComponent().getComponentContext();
        ctx.deactivateEndpoint(ep);
        injectContext(null);
    }

    protected void injectContext(ComponentContext context) {
        try {
            Method mth = pojo.getClass().getMethod("setContext", new Class[] { ComponentContext.class });
            mth.invoke(pojo, new Object[] { context });
        } catch (Exception e) {
            logger.debug("Unable to inject ComponentContext: " + e.getMessage());
        }
    }

    /**
     * Validate the endpoint at either deployment time for statically defined endpoints or at runtime for dynamic endpoints
     * 
     * @throws DeploymentException
     */
    public void validate() throws DeploymentException {
        try {
            registerService();
        } catch (Exception e) {
            throw ManagementSupport.failure(
                            "deploy", 
                            getServiceUnit().getComponent().getComponentName(), 
                            null, 
                            e);
        }
    }
    
    public void registerService() throws Exception {
        if (pojo == null) {
            if (pojoClass == null) {
                throw new IllegalArgumentException("Endpoint must have a non-null pojo or a pojoClass");
            }
            Class cl = Class.forName(pojoClass, true, getServiceUnit().getConfigurationClassLoader());
            pojo = cl.newInstance();
        }
        // Create factory
        XFire xfire = getXFire();
        ObjectServiceFactory factory = ServiceFactoryHelper.findServiceFactory(xfire, pojo.getClass(), annotations, typeMapping);
        Class serviceClass = pojo.getClass();
        if (serviceInterface != null) {
            serviceClass = Class.forName(serviceInterface, true, getServiceUnit().getConfigurationClassLoader());
        }

        this.definition = loadDefinition();
        if (definition != null) {
            if (definition.getServices().size() != 1) {
                throw new InvalidParameterException("The deployed wsdl defines more than one service");
            }
            javax.wsdl.Service wsdlSvc = (javax.wsdl.Service) definition.getServices().values().iterator().next();
            if (service == null) {
                service = wsdlSvc.getQName();
            } else if (!service.equals(wsdlSvc.getQName())) {
                throw new InvalidParameterException("The name of the Service defined by the deployed wsdl does not match the service name of the jbi endpoint");
            }
            if (wsdlSvc.getPorts().size() != 1) {
                throw new InvalidParameterException("The Service defined in the deployed wsdl must define exactly one Port");
            }
            Port wsdlPort = (Port) wsdlSvc.getPorts().values().iterator().next();
            if (endpoint == null) {
                endpoint = wsdlPort.getName();
            } else if (!endpoint.equals(wsdlPort.getName())) {
                throw new InvalidParameterException("The name of the Port defined by the deployed wsdl does not match the endpoint name of the jbi endpoint");
            }
            Binding wsdlBinding = wsdlPort.getBinding();
            if (wsdlBinding == null) {
                throw new InvalidParameterException("The Port defined in the deployed wsdl does not have any binding");
            }
            PortType wsdlPortType = wsdlBinding.getPortType();
            if (wsdlPortType == null) {
                throw new InvalidParameterException("The Binding defined in the deployed wsdl does not have reference a PortType");
            }
            if (interfaceName == null) {
                interfaceName = wsdlPortType.getQName();
            } else if (!interfaceName.equals(wsdlPortType.getQName())) {
                throw new InvalidParameterException("The name of the PortType defined by the deployed wsdl does not match the interface name of the jbi endpoint");
            }
            // Create the DOM document 
            definition = new WSDLFlattener(definition).getDefinition(interfaceName);
            description = WSDLFactory.newInstance().newWSDLWriter().getDocument(definition);
        }
        
        String svcLocalName = (service != null) ? service.getLocalPart() : null;
        String svcNamespace;
        if (interfaceName != null) {
            svcNamespace = interfaceName.getNamespaceURI();
        } else if (service != null) {
            svcNamespace = service.getNamespaceURI();
        } else {
            svcNamespace = null;
        }
        Map props = new HashMap();
        props.put(ObjectServiceFactory.PORT_TYPE, interfaceName);
        if (style != null) {
            props.put(ObjectServiceFactory.STYLE, style);
        }
        props.put(ObjectServiceFactory.USE, SoapConstants.USE_LITERAL);
        if (serviceInterface != null) {
            props.put(AnnotationServiceFactory.ALLOW_INTERFACE, Boolean.TRUE);
        }
        xfireService = factory.create(serviceClass, svcLocalName, svcNamespace, props);
        xfireService.setInvoker(new BeanInvoker(getPojo()));
        xfireService.setFaultSerializer(new JbiFaultSerializer());
        xfireService.setProperty(SoapConstants.MTOM_ENABLED, Boolean.toString(mtomEnabled));
        xfire.getServiceRegistry().register(xfireService);
        
        // If the wsdl has not been provided,
        // generate one
        if (this.description == null) {
            this.description = generateWsdl();

            // Check service name and endpoint name
            QName serviceName = xfireService.getName();
            QName interfName = xfireService.getServiceInfo().getPortType();
            String endpointName = null;
            if (service == null) {
                service = serviceName;
            } else if (!service.equals(serviceName)) {
                logger.warn("The service name defined in the wsdl (" + serviceName + 
                            ") does not match the service name defined in the endpoint spec (" + service + 
                            "). WSDL description may be unusable.");
            }
            if (interfaceName == null) {
                interfaceName = interfName;
            } else if (!interfaceName.equals(interfName)) {
                logger.warn("The interface name defined in the wsdl (" + interfName + 
                        ") does not match the service name defined in the endpoint spec (" + interfaceName + 
                        "). WSDL description may be unusable.");
            }

            // Parse the WSDL
            WSDLReader reader = WSDLFactory.newInstance().newWSDLReader(); 
            reader.setFeature(Constants.FEATURE_VERBOSE, false);
            definition = reader.readWSDL(null, description);

            javax.wsdl.Service svc = definition.getService(serviceName);
            if (svc != null) {
                if (svc.getPorts().values().size() == 1) {
                    Port port = (Port) svc.getPorts().values().iterator().next();
                    // Check if this is the same as defined in endpoint spec
                    endpointName = port.getName();
                    if (endpoint == null) {
                        endpoint = endpointName;
                    } else if (!endpoint.equals(endpointName)) {
                        // Override generated WSDL
                        port.setName(endpoint);
                        description = WSDLFactory.newInstance().newWSDLWriter().getDocument(definition);
                    }
                }
            }
            if (endpoint == null) {
                throw new IllegalArgumentException("endpoint name should be provided");
            }

            // Flatten it
            definition = new WSDLFlattener(definition).getDefinition(interfaceName);
            description = WSDLFactory.newInstance().newWSDLWriter().getDocument(definition);

            // Write WSDL
            if (logger.isDebugEnabled()) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                WSDLFactory.newInstance().newWSDLWriter().writeWSDL(definition, baos);
                logger.debug(baos.toString());
            }
        }
    }
    
    protected Definition loadDefinition() throws IOException, WSDLException {
        if (wsdlResource != null) {
            URL resource = wsdlResource.getURL();
            WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
            reader.setFeature(Constants.FEATURE_VERBOSE, false);
            return reader.readWSDL(null, resource.toString());
        }
        return null;
    }
    
    protected Document generateWsdl() throws SAXException, IOException, ParserConfigurationException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        getXFire().generateWSDL(xfireService.getSimpleName(), baos);
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document doc = factory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray()));
        return doc;
    }
    
    public XFire getXFire() {
        Jsr181Component component = (Jsr181Component) this.serviceUnit.getComponent();
        XFire xfire = component.getXFire();
        return xfire;
    }
    
    public String getPojoClass() {
        return pojoClass;
    }

    public void setPojoClass(String pojoClass) {
        this.pojoClass = pojoClass;
    }

    public String getAnnotations() {
        return annotations;
    }

    public void setAnnotations(String annotations) {
        this.annotations = annotations;
    }

    public String getTypeMapping() {
        return typeMapping;
    }

    public void setTypeMapping(String typeMapping) {
        this.typeMapping = typeMapping;
    }

    public ExchangeProcessor getProcessor() {
        return processor;
    }

    public String getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(String serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

}
