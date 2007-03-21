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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.wsdl.Definition;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.WebFault;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jsr181.xfire.ServiceFactoryHelper.FixedJAXWSServiceFactory;
import org.codehaus.xfire.XFire;
import org.codehaus.xfire.annotations.AnnotationServiceFactory;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.client.XFireProxyFactory;
import org.codehaus.xfire.fault.XFireFault;
import org.codehaus.xfire.service.OperationInfo;
import org.codehaus.xfire.service.Service;
import org.codehaus.xfire.service.ServiceFactory;
import org.codehaus.xfire.util.jdom.StaxSerializer;
import org.jdom.Element;
import org.w3c.dom.Document;

import com.ibm.wsdl.Constants;

public class JbiProxy {
    
    private static final Log logger = LogFactory.getLog(JbiProxyFactoryBean.class);

    protected XFire xfire;
    protected ComponentContext context;
    protected QName interfaceName;
    protected QName serviceName;
    protected String endpointName;
    protected Object proxy;
    protected Class serviceClass;
    protected Definition description;
    protected ServiceEndpoint endpoint;
    protected boolean propagateSecurityContext;
    
    public static Object create(XFire xfire,
                                ComponentContext context,
                                QName interfaceName,
                                QName serviceName,
                                String endpointName,
                                Class serviceClass) throws Exception {
        JbiProxy p = new JbiProxy(xfire, context, serviceClass, interfaceName, serviceName, endpointName);
        return p.getProxy();
    }
    
    public static Object create(XFire xfire,
            ComponentContext context,
            QName interfaceName,
            QName serviceName,
            String endpointName,
            Class serviceClass,
            boolean propagateSecurityContext) throws Exception {
        JbiProxy p = new JbiProxy(xfire, context, serviceClass, interfaceName, serviceName, endpointName, propagateSecurityContext);
        return p.getProxy();
    }

    public JbiProxy(XFire xfire,
                    ComponentContext context,
                    Class serviceClass,
                    Definition description) throws Exception {
        this.xfire = xfire;
        this.context = context;
        this.serviceClass = serviceClass;
        this.description = description;
    }
    
    public JbiProxy(XFire xfire,
                    ComponentContext context,
                    Class serviceClass,
                    QName interfaceName,
                    QName serviceName,
                    String endpointName) throws Exception {
        this.xfire = xfire;
        this.context = context;
        this.interfaceName = interfaceName;
        this.serviceName = serviceName;
        this.endpointName = endpointName;
        this.serviceClass = serviceClass;
    }
    
    public JbiProxy(XFire xfire,
            ComponentContext context,
            Class serviceClass,
            QName interfaceName,
            QName serviceName,
            String endpointName,
            boolean propagateSecurityContext) throws Exception {
        this.xfire = xfire;
        this.context = context;
        this.interfaceName = interfaceName;
        this.serviceName = serviceName;
        this.endpointName = endpointName;
        this.serviceClass = serviceClass;
        this.propagateSecurityContext = propagateSecurityContext;
    }

    public Object getProxy() throws Exception {
        if (proxy == null) {
            Map props = new HashMap();
            props.put(AnnotationServiceFactory.ALLOW_INTERFACE, Boolean.TRUE);
            ServiceFactory factory = ServiceFactoryHelper.findServiceFactory(xfire, serviceClass, null, null);
            Service service = factory.create(serviceClass, props);
            JBIClient client;
            if (factory instanceof FixedJAXWSServiceFactory) {
                client = new JAXWSJBIClient(xfire, service);
            } else {
                client = new JBIClient(xfire, service);
            }
            if (interfaceName != null) {
                client.getService().setProperty(JbiChannel.JBI_INTERFACE_NAME, interfaceName);
            }
            if (serviceName != null) {
                client.getService().setProperty(JbiChannel.JBI_SERVICE_NAME, serviceName);
            }
            if (endpoint != null) {
                client.getService().setProperty(JbiChannel.JBI_ENDPOINT, endpoint);
            }
            client.getService().setProperty(JbiChannel.JBI_SECURITY_PROPAGATATION, Boolean.valueOf(propagateSecurityContext));
            XFireProxyFactory xpf = new XFireProxyFactory(xfire);
            proxy = xpf.create(client);
        }
        return proxy;
    }
    
    public Definition getDescription() throws Exception {
        if (this.description == null) {
            ServiceEndpoint[] endpoints = getEndpoints();
            if (endpoints == null || endpoints.length == 0) {
                throw new IllegalStateException("No endpoints found for interface " + interfaceName + ", serviceName " + serviceName + " and endpoint " + endpointName);
            }
            ServiceEndpoint endpoint = chooseEndpoint(endpoints);
            if (endpoint == null) {
                throw new IllegalStateException("No suitable endpoint found");
            }
            if (serviceName != null && endpointName != null) {
                this.endpoint = endpoint;
            }
            Document doc = context.getEndpointDescriptor(endpoint);
            WSDLReader reader = WSDLFactory.newInstance().newWSDLReader(); 
            reader.setFeature(Constants.FEATURE_VERBOSE, false);
            this.description = reader.readWSDL(null, doc);
        }
        return this.description;
    }
    
    protected ServiceEndpoint[] getEndpoints() throws JBIException {
        ServiceEndpoint[] endpoints;
        if (endpointName != null && serviceName != null) {
            ServiceEndpoint endpoint = context.getEndpoint(serviceName, endpointName);
            if (endpoint == null) {
                endpoints = new ServiceEndpoint[0];
            } else {
                this.endpoint = endpoint;
                endpoints = new ServiceEndpoint[] { endpoint };
            }
        } else if (serviceName != null) {
            endpoints = context.getEndpointsForService(serviceName);
        } else if (interfaceName != null) {
            endpoints = context.getEndpoints(interfaceName);
        } else {
            throw new IllegalStateException("One of interfaceName or serviceName should be provided");
        }
        return endpoints;
    }
    
    protected ServiceEndpoint chooseEndpoint(ServiceEndpoint[] endpoints) throws JBIException {
        for (int i = 0; i < endpoints.length; i++) {
            if (context.getEndpointDescriptor(endpoints[i]) != null) {
                return endpoints[i];
            }
        }
        return null;
    }
    
    protected static class JBIClient extends Client {

        public JBIClient(XFire xfire, Service service) throws Exception {
            super(xfire.getTransportManager().getTransport(JbiTransport.JBI_BINDING),
                  service, 
                  null);
            setXFire(xfire);
        }
    }
    
    protected static class JAXWSJBIClient extends JBIClient {
        public JAXWSJBIClient(XFire xfire, Service service) throws Exception {
            super(xfire, service);
        }
        public Object[] invoke(OperationInfo op, Object[] params) throws Exception {
            try {
                return super.invoke(op, params);
            } catch (Exception e) {
                throw translateException(op.getMethod(), e);
            }
        }
        protected Exception translateException(Method method, Exception t) {
            if (t instanceof XFireFault == false) {
                logger.debug("Exception is not an XFireFault");
                return t;
            }
            XFireFault xfireFault = (XFireFault) t;
            if (!xfireFault.hasDetails()) {
                logger.debug("XFireFault has no details");
                return t;
            }
            // Get first child element of <detail/>
            List details = xfireFault.getDetail().getContent();
            Element detail = null;
            for (Object o : details) {
                if (o instanceof Element) {
                    detail = (Element) o;
                    break;
                }
            }
            if (detail == null) {
                logger.debug("XFireFault has no element in <detail/>");
                return t;
            }
            QName qname = new QName(detail.getNamespaceURI(),
                                    detail.getName());
            Class<?>[] exceptions = method.getExceptionTypes();
            for (int i = 0; i < exceptions.length; i++) {
                logger.debug("Checking exception: " + exceptions[i]);
                WebFault wf = exceptions[i].getAnnotation(WebFault.class);
                if (wf == null) {
                    logger.debug("No WebFault annotation");
                    continue;
                }
                QName exceptionName = new QName(wf.targetNamespace(), wf.name());
                if (exceptionName.equals(qname)) {
                    try {
                        Method mth = exceptions[i].getMethod("getFaultInfo");
                        Class<?> infoClass = mth.getReturnType();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        XMLStreamWriter writer = XMLOutputFactory.newInstance().createXMLStreamWriter(baos);
                        new StaxSerializer().writeElement(detail, writer);
                        writer.close();
                        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
                        JAXBElement<?> obj = JAXBContext.newInstance(infoClass).createUnmarshaller().unmarshal(new StreamSource(bais), infoClass);
                        Constructor<?> cst = exceptions[i].getConstructor(String.class, infoClass);
                        Exception e = (Exception) cst.newInstance(xfireFault.toString(), obj.getValue());
                        return e;
                    } catch (Throwable e) {
                        logger.debug("Error: " + e);
                    }
                } else {
                    logger.debug("QName mismatch: element: " + qname + ", exception: " + exceptionName);
                }
            }
            return t;
        }
        
    }
    
}
