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
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.activation.DataHandler;
import javax.jbi.component.ComponentContext;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.InOptionalOut;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
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

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.ibm.wsdl.Constants;
import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.common.EndpointDeliveryChannel;
import org.apache.servicemix.common.ManagementSupport;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.common.tools.wsdl.WSDLFlattener;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.StAXSourceTransformer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jsr181.xfire.JbiFaultSerializer;
import org.apache.servicemix.jsr181.xfire.JbiTransport;
import org.apache.servicemix.jsr181.xfire.ServiceFactoryHelper;
import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.annotations.AnnotationServiceFactory;
import org.codehaus.xfire.attachments.Attachment;
import org.codehaus.xfire.attachments.Attachments;
import org.codehaus.xfire.attachments.JavaMailAttachments;
import org.codehaus.xfire.attachments.SimpleAttachment;
import org.codehaus.xfire.exchange.InMessage;
import org.codehaus.xfire.fault.XFireFault;
import org.codehaus.xfire.jaxb2.JaxbType;
import org.codehaus.xfire.service.OperationInfo;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.service.binding.ObjectServiceFactory;
import org.codehaus.xfire.service.invoker.BeanInvoker;
import org.codehaus.xfire.soap.SoapConstants;
import org.codehaus.xfire.transport.Channel;
import org.codehaus.xfire.transport.Transport;
import org.springframework.core.io.Resource;

/**
 * 
 * @author gnodet
 * @version $Revision$
 * @org.apache.xbean.XBean element="endpoint"
 *                  description="A jsr181 endpoint"
 * 
 */
public class Jsr181Endpoint extends ProviderEndpoint {

    public static final String SOAP_FAULT_CODE = "org.apache.servicemix.soap.fault.code";
    public static final String SOAP_FAULT_SUBCODE = "org.apache.servicemix.soap.fault.subcode";
    public static final String SOAP_FAULT_REASON = "org.apache.servicemix.soap.fault.reason";
    public static final String SOAP_FAULT_NODE = "org.apache.servicemix.soap.fault.node";
    public static final String SOAP_FAULT_ROLE = "org.apache.servicemix.soap.fault.role";
    
    protected Object pojo;
    protected String pojoClass;
    protected String annotations;
    protected String typeMapping;
    protected String serviceInterface;
    protected String style = "wrapped";
    
    protected Service xfireService;
    protected Resource wsdlResource;
    protected boolean mtomEnabled;
    protected Map properties;
    protected StAXSourceTransformer transformer;

    /* should the payload be automaticaly validated by the ws engine
     * if not set then it is up to the engine to decide
     */
    private Boolean validationEnabled;
    
    public Jsr181Endpoint() {
        this.transformer = new StAXSourceTransformer();
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
     * @return the validationEnabled
     */
    public Boolean isValidationEnabled() {
        return validationEnabled;
    }

    /**
     * @param validationEnabled the validationEnabled to set
     */
    public void setValidationEnabled(Boolean validationEnabled) {
        this.validationEnabled = validationEnabled;
    }

    /**
     * @return the properties
     */
    public Map getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     */
    public void setProperties(Map properties) {
        this.properties = properties;
    }

    /**
     * @return Returns the xfireService.
     */
    public Service getXFireService() {
        return xfireService;
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.endpoints.SimpleEndpoint#start()
     */
    public void start() throws Exception {
        super.start();
        injectPojo(getContext(), getContainer());
    }

    /* (non-Javadoc)
     * @see org.servicemix.common.endpoints.SimpleEndpoint#stop()
     */
    public void stop() throws Exception {
        injectPojo(null, null);
        super.stop();
    }

    protected void injectPojo(ComponentContext context, JBIContainer container) {
        try {
            Method mth = pojo.getClass().getMethod("setContext", new Class[] {ComponentContext.class });
            mth.invoke(pojo, new Object[] {context });
        } catch (Exception e) {
            logger.debug("Unable to inject ComponentContext: " + e.getMessage());
        }
        try {
            Method mth = pojo.getClass().getMethod("setContainer", new Class[] {JBIContainer.class });
            mth.invoke(pojo, new Object[] {container });
        } catch (Exception e) {
            logger.debug("Unable to inject JBIContainer: " + e.getMessage());
        }
    }
    
    protected JBIContainer getContainer() {
        try {
            ComponentContext ctx = getServiceUnit().getComponent().getComponentContext();
            Field field = ctx.getClass().getDeclaredField("container");
            field.setAccessible(true);
            return (JBIContainer) field.get(ctx);
        } catch (Exception e) {
            logger.debug("Unable to retrieve JBIContainer: " + e.getMessage());
            return null;
        }
    }

    /**
     * Validate the endpoint at either deployment time for statically
     * defined endpoints or at runtime for dynamic endpoints
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
        ObjectServiceFactory factory = ServiceFactoryHelper.findServiceFactory(xfire, 
                pojo.getClass(), annotations, typeMapping);
        Class serviceClass = pojo.getClass();
        if (serviceInterface != null) {
            serviceClass = Class.forName(serviceInterface, true, getServiceUnit().getConfigurationClassLoader());
        }

        this.definition = loadDefinition();
        if (definition != null) {
            updateDescription();
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
        if (properties != null) {
            props.putAll(properties);
        }
        xfireService = factory.create(serviceClass, svcLocalName, svcNamespace, props);
        xfireService.setInvoker(new BeanInvoker(getPojo()));
        xfireService.setFaultSerializer(new JbiFaultSerializer());
        xfireService.setProperty(SoapConstants.MTOM_ENABLED, Boolean.toString(mtomEnabled));
        if (validationEnabled != null) {
            if ("jaxb2".equals(typeMapping)) {
                xfireService.setProperty(JaxbType.ENABLE_VALIDATION, validationEnabled.toString());
            } else {
                throw new IllegalArgumentException("Currently you can controll validation only for jaxb2 mapping. "
                                                   + typeMapping + " is not supported.");
            }
        }
        xfire.getServiceRegistry().register(xfireService);
        
        // If the wsdl has not been provided,
        // generate one
        if (this.description == null) {
            createDescription();
        }
    }

    protected void createDescription() throws SAXException, IOException,
            ParserConfigurationException, WSDLException, Exception {
        this.description = generateWsdl();

        // Check service name and endpoint name
        QName serviceName = xfireService.getName();
        QName interfName = xfireService.getServiceInfo().getPortType();
        String endpointName = null;
        if (service == null) {
            service = serviceName;
        } else if (!service.equals(serviceName)) {
            logger.warn("The service name defined in the wsdl (" + serviceName
                        + ") does not match the service name defined in the endpoint spec (" + service 
                        + "). WSDL description may be unusable.");
        }
        if (interfaceName == null) {
            interfaceName = interfName;
        } else if (!interfaceName.equals(interfName)) {
            logger.warn("The interface name defined in the wsdl (" + interfName 
                    + ") does not match the service name defined in the endpoint spec (" + interfaceName 
                    + "). WSDL description may be unusable.");
        }

        // Parse the WSDL
        WSDLReader reader = WSDLFactory.newInstance().newWSDLReader(); 
        reader.setFeature(Constants.FEATURE_VERBOSE, false);
        definition = reader.readWSDL(null, description);

        javax.wsdl.Service svc = definition.getService(serviceName);
        if (svc != null && svc.getPorts().values().size() == 1) {
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

    protected void updateDescription() throws Exception {
        if (definition.getServices().size() != 1) {
            throw new IllegalArgumentException("The deployed wsdl defines more than one service");
        }
        javax.wsdl.Service wsdlSvc = (javax.wsdl.Service) definition.getServices().values().iterator().next();
        if (service == null) {
            service = wsdlSvc.getQName();
        } else if (!service.equals(wsdlSvc.getQName())) {
            throw new IllegalArgumentException("The name of the Service defined by the deployed wsdl"
                    + " does not match the service name of the jbi endpoint");
        }
        if (wsdlSvc.getPorts().size() != 1) {
            throw new IllegalArgumentException("The Service defined in the deployed wsdl"
                    + " must define exactly one Port");
        }
        Port wsdlPort = (Port) wsdlSvc.getPorts().values().iterator().next();
        if (endpoint == null) {
            endpoint = wsdlPort.getName();
        } else if (!endpoint.equals(wsdlPort.getName())) {
            throw new IllegalArgumentException("The name of the Port defined by the deployed wsdl does"
                    + " not match the endpoint name of the jbi endpoint");
        }
        Binding wsdlBinding = wsdlPort.getBinding();
        if (wsdlBinding == null) {
            throw new IllegalArgumentException("The Port defined in the deployed wsdl"
                    + " does not have any binding");
        }
        PortType wsdlPortType = wsdlBinding.getPortType();
        if (wsdlPortType == null) {
            throw new IllegalArgumentException("The Binding defined in the"
                    + " deployed wsdl does not have reference a PortType");
        }
        if (interfaceName == null) {
            interfaceName = wsdlPortType.getQName();
        } else if (!interfaceName.equals(wsdlPortType.getQName())) {
            throw new IllegalArgumentException("The name of the PortType defined by the deployed"
                    + " wsdl does not match the interface name of the jbi endpoint");
        }
        // Create the DOM document 
        definition = new WSDLFlattener(definition).getDefinition(interfaceName);
        description = WSDLFactory.newInstance().newWSDLWriter().getDocument(definition);
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
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray()));
    }
    
    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            return;
        } else if (exchange.getStatus() == ExchangeStatus.ERROR) {
            return;
        }

        // TODO: clean this code
        XFire xfire = getXFire();
        Service service = getXFireService();
        Transport t = xfire.getTransportManager().getTransport(JbiTransport.JBI_BINDING);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Channel c = t.createChannel();
        MessageContext ctx = new MessageContext();
        ctx.setXFire(xfire);
        ctx.setService(service);
        ctx.setProperty(Channel.BACKCHANNEL_URI, out);
        ctx.setExchange(new org.codehaus.xfire.exchange.MessageExchange(ctx));
        InMessage msg = new InMessage();
        ctx.getExchange().setInMessage(msg);
        if (exchange.getOperation() != null) {
            OperationInfo op = service.getServiceInfo().getOperation(exchange.getOperation().getLocalPart());
            if (op != null) {
                ctx.getExchange().setOperation(op);
            }
        }
        ctx.setCurrentMessage(msg);
        NormalizedMessage in = exchange.getMessage("in");
        msg.setXMLStreamReader(transformer.toXMLStreamReader(in.getContent()));
        if (in.getAttachmentNames() != null && in.getAttachmentNames().size() > 0) {
            JavaMailAttachments attachments = new JavaMailAttachments();
            for (Iterator it = in.getAttachmentNames().iterator(); it.hasNext();) {
                String name = (String) it.next();
                DataHandler dh = in.getAttachment(name);
                attachments.addPart(new SimpleAttachment(name, dh));
            }
            msg.setAttachments(attachments);
        }
        JBIContext.setMessageExchange(exchange);
        try {
            c.receive(ctx, msg);
        } finally {
            EndpointDeliveryChannel.setEndpoint(null);
        }
        c.close();
        
        // Set response or DONE status
        if (exchange instanceof InOut || exchange instanceof InOptionalOut) {
            if (ctx.getExchange().hasFaultMessage() && ctx.getExchange().getFaultMessage().getBody() != null) {
                String charSet = ctx.getExchange().getFaultMessage().getEncoding();
                Fault fault = exchange.getFault();
                if (fault == null) {
                    fault = exchange.createFault();
                    exchange.setFault(fault);
                }
                fault.setContent(new StringSource(out.toString(charSet)));
                XFireFault xFault = (XFireFault) ctx.getExchange().getFaultMessage().getBody();
                fault.setProperty(SOAP_FAULT_CODE, xFault.getFaultCode());
                fault.setProperty(SOAP_FAULT_REASON, xFault.getReason());
                fault.setProperty(SOAP_FAULT_ROLE, xFault.getRole());
                fault.setProperty(SOAP_FAULT_SUBCODE, xFault.getSubCode());
            } else {
                String charSet = ctx.getOutMessage().getEncoding();
                NormalizedMessage outMsg = exchange.getMessage("out");
                if (outMsg == null) {
                    outMsg = exchange.createMessage();
                    exchange.setMessage(outMsg, "out");
                }
                Attachments attachments = ctx.getCurrentMessage().getAttachments();
                if (attachments != null) {
                    for (Iterator it = attachments.getParts(); it.hasNext();) {
                        Attachment att = (Attachment) it.next();
                        outMsg.addAttachment(att.getId(), att.getDataHandler());
                    }
                }
                outMsg.setContent(new StringSource(out.toString(charSet)));
            }
            if (exchange.isTransacted() && Boolean.TRUE.equals(exchange.getProperty(JbiConstants.SEND_SYNC))) {
                sendSync(exchange);
            } else {
                send(exchange);
            }
        } else {
            done(exchange);
        }
    }

    public XFire getXFire() {
        Jsr181Component component = (Jsr181Component) this.serviceUnit.getComponent();
        return component.getXFire();
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

    public String getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(String serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

}
