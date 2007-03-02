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
package org.apache.servicemix.jsr181.xfire;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jws.WebMethod;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.ws.RequestWrapper;
import javax.xml.ws.ResponseWrapper;
import javax.xml.ws.WebFault;

import org.codehaus.xfire.MessageContext;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.XFireRuntimeException;
import org.codehaus.xfire.aegis.AegisBindingProvider;
import org.codehaus.xfire.aegis.stax.ElementReader;
import org.codehaus.xfire.aegis.stax.ElementWriter;
import org.codehaus.xfire.aegis.type.DefaultTypeMappingRegistry;
import org.codehaus.xfire.aegis.type.Type;
import org.codehaus.xfire.aegis.type.TypeMappingRegistry;
import org.codehaus.xfire.annotations.AnnotationServiceFactory;
import org.codehaus.xfire.annotations.WebAnnotations;
import org.codehaus.xfire.annotations.commons.CommonsWebAttributes;
import org.codehaus.xfire.annotations.jsr181.Jsr181WebAnnotations;
import org.codehaus.xfire.exchange.InMessage;
import org.codehaus.xfire.exchange.MessageSerializer;
import org.codehaus.xfire.exchange.OutMessage;
import org.codehaus.xfire.fault.FaultSender;
import org.codehaus.xfire.fault.XFireFault;
import org.codehaus.xfire.handler.OutMessageSender;
import org.codehaus.xfire.jaxb2.JaxbType;
import org.codehaus.xfire.jaxws.handler.WebFaultHandler;
import org.codehaus.xfire.jaxws.type.JAXWSTypeRegistry;
import org.codehaus.xfire.service.FaultInfo;
import org.codehaus.xfire.service.OperationInfo;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.service.ServiceInfo;
import org.codehaus.xfire.service.binding.AbstractBinding;
import org.codehaus.xfire.service.binding.ObjectServiceFactory;
import org.codehaus.xfire.service.binding.PostInvocationHandler;
import org.codehaus.xfire.service.binding.ServiceInvocationHandler;
import org.codehaus.xfire.soap.AbstractSoapBinding;
import org.codehaus.xfire.transport.TransportManager;
import org.codehaus.xfire.util.ClassLoaderUtils;
import org.codehaus.xfire.util.STAXUtils;
import org.codehaus.xfire.util.stax.DepthXMLStreamReader;
import org.codehaus.xfire.xmlbeans.XmlBeansTypeRegistry;

public class ServiceFactoryHelper {
    
    public static final String TM_DEFAULT = "default";
    public static final String TM_XMLBEANS = "xmlbeans";
    public static final String TM_JAXB2 = "jaxb2";
    
    public static final String AN_JSR181 = "jsr181";
    public static final String AN_JAVA5 = "java5";
    public static final String AN_COMMONS = "commons";
    public static final String AN_NONE = "none";

    private static final Map knownTypeMappings;
    private static final Map knownAnnotations;
    
    static {
        knownTypeMappings = new HashMap();
        knownTypeMappings.put(TM_DEFAULT, new DefaultTypeMappingRegistry(true));
        knownTypeMappings.put(TM_XMLBEANS, new XmlBeansTypeRegistry());
        try {
            Class cl = Class.forName("org.codehaus.xfire.jaxb2.JaxbTypeRegistry");
            Object tr = cl.newInstance();
            knownTypeMappings.put(TM_JAXB2, tr);
        } catch (Throwable e) {
            // we are in jdk 1.4, do nothing
        }
        
        knownAnnotations = new HashMap();
        knownAnnotations.put(AN_COMMONS, new CommonsWebAttributes());
        try {
            Class cl = Class.forName("org.codehaus.xfire.annotations.jsr181.Jsr181WebAnnotations");
            Object wa = cl.newInstance();
            knownAnnotations.put(AN_JAVA5, wa);
        } catch (Throwable e) {
            // we are in jdk 1.4, do nothing
        }
    }
    
    public static ObjectServiceFactory findServiceFactory(
                        XFire xfire,
                        Class serviceClass,
                        String annotations, 
                        String typeMapping) throws Exception {
        // jsr181 is synonymous to java5
        if (annotations != null && AN_JSR181.equals(annotations)) {
            annotations = AN_JAVA5;
        }
        // Determine annotations
        WebAnnotations wa = null;
        String selectedAnnotations = null;
        if (annotations != null) {
            selectedAnnotations = annotations;
            if (!annotations.equals(AN_NONE)) {
                wa = (WebAnnotations) knownAnnotations.get(annotations);
                if (wa == null) {
                    throw new Exception("Unrecognized annotations: " + annotations);
                }
            }
        } else {
            for (Iterator it = knownAnnotations.entrySet().iterator(); it.hasNext();) {
                Map.Entry entry = (Map.Entry) it.next();
                WebAnnotations w = (WebAnnotations) entry.getValue();
                if (w.hasWebServiceAnnotation(serviceClass)) {
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
            selectedTypeMapping = (wa == null) ? TM_DEFAULT : TM_JAXB2;
        } else {
            selectedTypeMapping = typeMapping;
        }
        tm = (TypeMappingRegistry) knownTypeMappings.get(selectedTypeMapping);
        if (tm == null) {
            throw new Exception("Unrecognized typeMapping: " + typeMapping);
        }
        // Create factory
        ObjectServiceFactory factory = null;
        if (wa == null) {
            factory = new ObjectServiceFactory(xfire.getTransportManager(),
                                               new AegisBindingProvider(tm));
        } else if (selectedAnnotations.equals(AN_JAVA5) && 
                   selectedTypeMapping.equals(TM_JAXB2)) {
            try {
                factory = new FixedJAXWSServiceFactory(xfire.getTransportManager());
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
        // Register only JBI transport in the factory
        factory.getSoap11Transports().clear();
        factory.getSoap12Transports().clear();
        factory.getSoap11Transports().add(JbiTransport.JBI_BINDING);
        return factory;
    }
    
    public static class FixedJAXWSServiceFactory extends AnnotationServiceFactory {
        public FixedJAXWSServiceFactory(TransportManager transportManager) {
            super(new Jsr181WebAnnotations(), 
                  transportManager, 
                  new AegisBindingProvider(new JAXWSTypeRegistry()));
        }
        protected void registerHandlers(Service service)
        {
            service.addInHandler(new ServiceInvocationHandler());
            service.addInHandler(new PostInvocationHandler());
            service.addOutHandler(new OutMessageSender());
            service.addFaultHandler(new FaultSender());
            service.addFaultHandler(new WebFaultHandler());
        }
        public String getOperationName(ServiceInfo service, Method method) {
            Annotation[] annotations = method.getAnnotations();
            for (int i = 0; i < annotations.length; i++) {
                if (annotations[i] instanceof WebMethod) {
                    if (((WebMethod) annotations[i]).operationName() != null &&
                        ((WebMethod) annotations[i]).operationName().length() > 0) {
                        return ((WebMethod) annotations[i]).operationName();
                    }
                }
            }
            return super.getOperationName(service, method);
        }        
        @Override
        protected OperationInfo addOperation(Service endpoint, Method method, String style)
        {
            OperationInfo op = super.addOperation(endpoint, method, style);
            
            return op;
        }

        @Override
        protected FaultInfo addFault(Service service, OperationInfo op, Class exClazz)
        {
            FaultInfo info = super.addFault(service, op, exClazz);
            
            return info;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected boolean isFaultInfoClass(Class exClass) 
        {
            return exClass.isAnnotationPresent(WebFault.class);
        }

        @SuppressWarnings("unchecked")
        @Override
        protected QName getFaultName(Service service, OperationInfo o, Class exClass, Class beanClass)
        {
            WebFault webFault = (WebFault) exClass.getAnnotation(WebFault.class);
            
            if (webFault == null || webFault.name().equals(""))
                return super.getFaultName(service, o, exClass, beanClass);

            String ns = webFault.targetNamespace();
            if (ns == null) ns = service.getTargetNamespace();
            
            return new QName(ns, webFault.name());
        }

        protected MessageSerializer getSerializer(AbstractSoapBinding binding)
        {
           return new FixedJAXWSBinding(super.getSerializer(binding));
        }

        public void createBindingOperation(Service service, AbstractSoapBinding binding, OperationInfo op)
        {
            super.createBindingOperation(service, binding, op);
            binding.setSerializer(op, new FixedJAXWSOperationBinding(op, super.getSerializer(binding)));
        }
        @Override
        protected QName createInputMessageName(OperationInfo op)
        {
            if (op.getMethod().isAnnotationPresent(RequestWrapper.class))
            {
                RequestWrapper wrapper = op.getMethod().getAnnotation(RequestWrapper.class);
                
                String ns = wrapper.targetNamespace();
                if (ns.length() == 0) ns = op.getService().getPortType().getNamespaceURI();

                String name = wrapper.localName();
                if (name.length() == 0) name = op.getName();
                
                return new QName(ns, name);
            }
            
            return super.createInputMessageName(op);
        }

        @Override
        protected QName createOutputMessageName(OperationInfo op)
        {
            if (op.getMethod().isAnnotationPresent(ResponseWrapper.class))
            {
                ResponseWrapper wrapper = op.getMethod().getAnnotation(ResponseWrapper.class);
                
                String ns = wrapper.targetNamespace();
                if (ns.length() == 0) ns = op.getService().getPortType().getNamespaceURI();

                String name = wrapper.localName();
                if (name.length() == 0) name = op.getName();
                
                return new QName(ns, name);
            }
            
            return super.createOutputMessageName(op);
        }
    }
    
    public static class FixedJAXWSOperationBinding implements MessageSerializer {
        private MessageSerializer delegate;

        private boolean processInput = false;
        private List inputPDs = new ArrayList();
        private Class inputClass;
        
        private boolean processOutput = false;
        private List outputPDs = new ArrayList();
        private Class outputClass;
        
        public FixedJAXWSOperationBinding(OperationInfo op, MessageSerializer delegate) {
            this.delegate = delegate;
            
            Method declared = op.getMethod();
            if (declared.isAnnotationPresent(RequestWrapper.class))
            {
                this.processInput = true;
                RequestWrapper wrapper = op.getMethod().getAnnotation(RequestWrapper.class);
                
                try
                {
                    inputClass = ClassLoaderUtils.loadClass(wrapper.className(), getClass());
                    String[] inputOrder = ((XmlType) inputClass.getAnnotation(XmlType.class)).propOrder();
                    BeanInfo inputBeanInfo = Introspector.getBeanInfo(inputClass);
                    
                    PropertyDescriptor[] pds = inputBeanInfo.getPropertyDescriptors();
                    for (int i = 0; i < inputOrder.length; i++)
                    {
                        inputPDs.add(getPropertyDescriptor(pds, inputOrder[i]));
                    }
                }
                catch (ClassNotFoundException e)
                {
                    throw new XFireRuntimeException("Could not load request class for operation " + op.getName(), e);
                }
                catch (IntrospectionException e)
                {
                    throw new XFireRuntimeException("Could introspect request class for operation " + op.getName(), e);
                }
            }
            
            if (declared.isAnnotationPresent(ResponseWrapper.class))
            {
                this.processOutput = true;
                ResponseWrapper wrapper = op.getMethod().getAnnotation(ResponseWrapper.class);

                try
                {
                    outputClass = ClassLoaderUtils.loadClass(wrapper.className(), getClass());
                    String[] outputOrder = ((XmlType) outputClass.getAnnotation(XmlType.class)).propOrder();
                    BeanInfo outputBeanInfo = Introspector.getBeanInfo(outputClass);
                    
                    PropertyDescriptor[] pds = outputBeanInfo.getPropertyDescriptors();
                    for (int i = 0; i < outputOrder.length; i++)
                    {
                        outputPDs.add(getPropertyDescriptor(pds, outputOrder[i]));
                    }
                }
                catch (ClassNotFoundException e)
                {
                    throw new XFireRuntimeException("Could not load response class for operation " + op.getName(), e);
                }
                catch (IntrospectionException e)
                {
                    throw new XFireRuntimeException("Could introspect response class for operation " + op.getName(), e);
                }
            }
        }

        protected PropertyDescriptor getPropertyDescriptor(PropertyDescriptor[] descriptors, String name)
        {
            for (int i = 0; i < descriptors.length; i++)
            {
                if (descriptors[i].getName().equals(name))
                    return descriptors[i];
            }
            
            return null;
        }
        
        public void readMessage(InMessage message, MessageContext context)
            throws XFireFault
        {
            if (AbstractBinding.isClientModeOn(context)) {
                if (processOutput) {
                    Service service = context.getService();
                    AegisBindingProvider provider = (AegisBindingProvider) service.getBindingProvider();
                    Type type = provider.getType(service, outputClass);
                    Object in = type.readObject(new ElementReader(message.getXMLStreamReader()), context);
                    List<Object> parameters = new ArrayList<Object>();
                    for (Iterator itr = outputPDs.iterator(); itr.hasNext();) {
                        PropertyDescriptor pd = (PropertyDescriptor) itr.next();
                        try {
                            Object val = getReadMethod(outputClass, pd).invoke(in, new Object[] {});
                            parameters.add(val);
                        } catch (Exception e) {
                            throw new XFireRuntimeException("Couldn't read property " + pd.getName(), e);
                        }
                    }
                    message.setBody(parameters);
                } else {
                    delegate.readMessage(message, context);
                }
            } else {
                if (processInput) {
                    Service service = context.getService();
                    AegisBindingProvider provider = (AegisBindingProvider) service.getBindingProvider();
                    Type type = provider.getType(service, inputClass);
                    Object in = type.readObject(new ElementReader(message.getXMLStreamReader()), context);
                    List<Object> parameters = new ArrayList<Object>();
                    for (Iterator itr = inputPDs.iterator(); itr.hasNext();) {
                        PropertyDescriptor pd = (PropertyDescriptor) itr.next();
                        try {
                            Object val = getReadMethod(inputClass, pd).invoke(in, new Object[] {});
                            parameters.add(val);
                        } catch (Exception e) {
                            throw new XFireRuntimeException("Couldn't read property " + pd.getName(), e);
                        }
                    }
                    message.setBody(parameters);
                } else {
                    delegate.readMessage(message, context);
                }
            }
        }

        public void writeMessage(OutMessage message, XMLStreamWriter writer, MessageContext context)
            throws XFireFault
        {
            if (processOutput)
            {
                Object[] params = (Object[]) message.getBody();
                
                Service service = context.getService();
                AegisBindingProvider provider = (AegisBindingProvider) service.getBindingProvider();
                
                Type type = provider.getType(service, outputClass);

                Object out;
                try
                {
                    out = outputClass.newInstance();
                }
                catch (Exception e)
                {
                    throw new XFireRuntimeException("Could not instantiate resposne class " + outputClass.getName(), e);
                }
                
                for (int i = 0; i < outputPDs.size(); i++)
                {
                    PropertyDescriptor pd = (PropertyDescriptor) outputPDs.get(i);
                    Object val = params[i];
                    
                    if (val == null) continue;
                    
                    try
                    {
                        getWriteMethod(pd).invoke(out, new Object[] {val});
                    }
                    catch (Exception e)
                    {
                        throw new XFireRuntimeException("Couldn't read property " + pd.getName(), e);
                    }
                }
                ((JaxbType) type).writeObject(out, new ElementWriter(writer), context);
            }
            else
            {
                delegate.writeMessage(message, writer, context);
            }
        }
        
        protected Method getReadMethod(Class clazz, PropertyDescriptor pd) 
        {
            Method mth = pd.getReadMethod();
            if (mth == null && pd.getPropertyType() == Boolean.class) {
                String name = pd.getName();
                name = name.substring(0, 1).toUpperCase() + name.substring(1);
                name = "is" + name;
                try 
                {
                    mth = clazz.getMethod(name, new Class[0]);
                    if (mth != null) {
                        pd.setReadMethod(mth);
                    }
                } 
                catch (IntrospectionException e) 
                {
                    // do nothing
                }
                catch (NoSuchMethodException e) 
                {
                    // do nothing
                }
            }
            return mth;
        }
        
        protected Method getWriteMethod(PropertyDescriptor pd) 
        {
            return pd.getWriteMethod();
        }
    }
    
    public static class FixedJAXWSBinding 
    extends AbstractBinding
    implements MessageSerializer
{
    private MessageSerializer delegate;
    private Map<OperationInfo, FixedJAXWSOperationBinding> op2Binding = 
        new HashMap<OperationInfo, FixedJAXWSOperationBinding>();
    
    public FixedJAXWSBinding(MessageSerializer delegate)
    {
        super();

        this.delegate = delegate;
    }

    public void readMessage(InMessage message, MessageContext context)
        throws XFireFault
    {
        Service endpoint = context.getService();
        
        DepthXMLStreamReader dr = new DepthXMLStreamReader(context.getInMessage().getXMLStreamReader());

        if ( !STAXUtils.toNextElement(dr) )
            throw new XFireFault("There must be a method name element.", XFireFault.SENDER);
        
        OperationInfo op = context.getExchange().getOperation();

        if (!isClientModeOn(context) && op == null)
        {
            op = endpoint.getServiceInfo().getOperation( dr.getLocalName() );

            if (op != null)
            {
                setOperation(op, context);
    
                FixedJAXWSOperationBinding opBinding = getOperationBinding(op);
                opBinding.readMessage(message, context);
                return;
            }
        }

        delegate.readMessage(message, context);
    }

    private FixedJAXWSOperationBinding getOperationBinding(OperationInfo op)
    {
        FixedJAXWSOperationBinding opBinding = (FixedJAXWSOperationBinding) op2Binding.get(op);
        if (opBinding == null)
        {
            opBinding = new FixedJAXWSOperationBinding(op, delegate);
            op2Binding.put(op, opBinding);
        }
        return opBinding;
    }

    public void writeMessage(OutMessage message, XMLStreamWriter writer, MessageContext context)
        throws XFireFault
    {
        OperationInfo op = context.getExchange().getOperation();
        
    }
}

}
