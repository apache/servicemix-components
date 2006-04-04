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
package org.apache.servicemix.jsr181;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.MessageExchange.Role;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.Message;
import javax.wsdl.Port;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.common.xbean.XBeanServiceUnit;
import org.apache.servicemix.jsr181.xfire.JbiFaultSerializer;
import org.apache.servicemix.jsr181.xfire.JbiTransport;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.aegis.AegisBindingProvider;
import org.codehaus.xfire.aegis.type.DefaultTypeMappingRegistry;
import org.codehaus.xfire.aegis.type.TypeMappingRegistry;
import org.codehaus.xfire.annotations.AnnotationServiceFactory;
import org.codehaus.xfire.annotations.WebAnnotations;
import org.codehaus.xfire.annotations.commons.CommonsWebAttributes;
import org.codehaus.xfire.annotations.jsr181.Jsr181WebAnnotations;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.service.binding.BeanInvoker;
import org.codehaus.xfire.service.binding.ObjectServiceFactory;
import org.codehaus.xfire.soap.SoapConstants;
import org.codehaus.xfire.transport.TransportManager;
import org.codehaus.xfire.xmlbeans.XmlBeansTypeRegistry;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * 
 * @author gnodet
 * @version $Revision$
 * @org.apache.xbean.XBean element="endpoint"
 *                  description="A jsr181 endpoint"
 * 
 */
public class Jsr181Endpoint extends Endpoint {

    private static final Map knownTypeMappings;
    private static final Map knownAnnotations;
    
    static {
        knownTypeMappings = new HashMap();
        knownTypeMappings.put("default", new DefaultTypeMappingRegistry(true));
        knownTypeMappings.put("xmlbeans", new XmlBeansTypeRegistry());
        try {
            Class cl = Class.forName("org.codehaus.xfire.jaxb2.JaxbTypeRegistry");
            Object tr = cl.newInstance();
            knownTypeMappings.put("jaxb2", tr);
        } catch (Throwable e) {
            // we are in jdk 1.4, do nothing
        }
        
        knownAnnotations = new HashMap();
        knownAnnotations.put("jsr181", new Jsr181WebAnnotations());
        knownAnnotations.put("commons", new CommonsWebAttributes());
        try {
            Class cl = Class.forName("org.codehaus.xfire.annotations.jsr181.Jsr181WebAnnotations");
            Object wa = cl.newInstance();
            knownAnnotations.put("java5", wa);
        } catch (Throwable e) {
            // we are in jdk 1.4, do nothing
        }
    }
    
    protected Object pojo;
    protected String pojoClass;
    protected String annotations;
    protected String typeMapping;
    protected String serviceInterface;
    
    protected ServiceEndpoint activated;
    protected Service xfireService;
    protected ExchangeProcessor processor;
    
    public Jsr181Endpoint() {
        processor = new Jsr181ExchangeProcessor(this);
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
        injectContext(new EndpointComponentContext(ctx, this));
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

    public void registerService() throws Exception {
        if (pojo == null) {
            ClassLoader classLoader = ((XBeanServiceUnit) getServiceUnit()).getConfigurationClassLoader();
            Class cl = Class.forName(pojoClass, true, classLoader);
            pojo = cl.newInstance();
        }
        // Determine annotations
        WebAnnotations wa = null;
        String selectedAnnotations = null;
        if (annotations != null) {
            selectedAnnotations = annotations;
            if (!annotations.equals("none")) {
                wa = (WebAnnotations) knownAnnotations.get(annotations);
                if (wa == null) {
                    throw new Exception("Unrecognized annotations: " + annotations);
                }
            }
        } else {
            for (Iterator it = knownAnnotations.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                WebAnnotations w = (WebAnnotations) entry.getValue();
                if (w.hasWebServiceAnnotation(pojo.getClass())) {
                    selectedAnnotations = (String) entry.getKey();
                    wa = w;
                    break;
                }
            }
        }
        // Determine TypeMappingRegistry
        TypeMappingRegistry tm = null;
        String selectedTypeMapping = null;
        if (typeMapping == null) {
            selectedTypeMapping = (wa == null) ? "default" : "jaxb2";
        } else {
            selectedTypeMapping = typeMapping;
        }
        tm = (TypeMappingRegistry) knownTypeMappings.get(selectedTypeMapping);
        if (tm == null) {
            throw new Exception("Unrecognized typeMapping: " + typeMapping);
        }
        // Create factory
        XFire xfire = getXFire();
        ObjectServiceFactory factory = null;
        Class serviceClass = pojo.getClass();
        if (serviceInterface != null) {
            serviceClass = Class.forName(serviceInterface);
        }
        if (wa == null) {
            factory = new ObjectServiceFactory(xfire.getTransportManager(),
                                               new AegisBindingProvider(tm));
        } else if (selectedAnnotations.equals("java5") && selectedTypeMapping.equals("jaxb2")) {
            try {
                Class clazz = Class.forName("org.codehaus.xfire.jaxws.JAXWSServiceFactory");
                Constructor ct = clazz.getDeclaredConstructor(new Class[] { TransportManager.class });
                factory = (ObjectServiceFactory) ct.newInstance(new Object[] { xfire.getTransportManager() });
            } catch (Exception e) {
                factory = new AnnotationServiceFactory(wa, 
                        xfire.getTransportManager(), 
                        new AegisBindingProvider(tm));
            }
        } else {
            factory = new AnnotationServiceFactory(wa, 
                                                   xfire.getTransportManager(), 
                                                   new AegisBindingProvider(tm));
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
        props.put(ObjectServiceFactory.STYLE, SoapConstants.STYLE_WRAPPED);
        props.put(ObjectServiceFactory.USE, SoapConstants.USE_LITERAL);
        factory.getSoap11Transports().clear();
        factory.getSoap12Transports().clear();
        factory.getSoap11Transports().add(JbiTransport.JBI_BINDING);
        xfireService = factory.create(serviceClass, svcLocalName, svcNamespace, props);
        xfireService.setInvoker(new BeanInvoker(getPojo()));
        xfireService.setFaultSerializer(new JbiFaultSerializer(getConfiguration()));
        xfire.getServiceRegistry().register(xfireService);
        this.description = generateWsdl();
        
        // If the both interfaceName and serviceName are provided with non matching namespaces,
        // the generated wsdl is bad. We have to keep only the port type definition.
        if (this.service != null && interfaceName != null &&
            !this.service.getNamespaceURI().equals(interfaceName.getNamespaceURI())) {
            // Parse the WSDL
            definition = WSDLFactory.newInstance().newWSDLReader().readWSDL(null, description);
            // Get the service and port definition
            javax.wsdl.Service svc = definition.getService(new QName(this.interfaceName.getNamespaceURI(), this.service.getLocalPart()));
            Port port = svc != null && svc.getPorts().size() == 1 ? (Port) svc.getPorts().values().iterator().next() : null;
            if (port != null) {
                // If the endpoint name has not been defined, retrieve it now
                if (endpoint == null) {
                    endpoint = port.getName();
                }
                // Now, remove binding and service infos and change target namespace
                definition.removeBinding(port.getBinding().getQName());
                definition.removeService(svc.getQName());
                description = WSDLFactory.newInstance().newWSDLWriter().getDocument(definition);
            }
        }
        // Write WSDL
        if (logger.isTraceEnabled()) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            WSDLFactory.newInstance().newWSDLWriter().writeWSDL(definition, baos);
            logger.trace(baos.toString());
        }
        
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
        definition = WSDLFactory.newInstance().newWSDLReader().readWSDL(null, description);
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
        Jsr181LifeCycle jsr181LifeCycle = (Jsr181LifeCycle) this.serviceUnit.getComponent().getLifeCycle();
        XFire xfire = jsr181LifeCycle.getXFire();
        return xfire;
    }
    
    public Jsr181ConfigurationMBean getConfiguration() {
        Jsr181LifeCycle jsr181LifeCycle = (Jsr181LifeCycle) this.serviceUnit.getComponent().getLifeCycle();
        Jsr181ConfigurationMBean configuration = jsr181LifeCycle.getConfiguration();
        return configuration;
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
