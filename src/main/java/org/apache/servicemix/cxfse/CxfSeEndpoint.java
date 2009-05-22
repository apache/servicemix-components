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
package org.apache.servicemix.cxfse;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jbi.component.ComponentContext;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.messaging.RobustInOnly;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.ws.WebServiceRef;

import org.w3c.dom.Element;

import org.xml.sax.SAXException;

import org.apache.cxf.Bus;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.SoapVersion;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.ServiceImpl;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.jbi.JBIDestination;
import org.apache.cxf.transport.jbi.JBIDispatcherUtil;
import org.apache.cxf.transport.jbi.JBITransportFactory;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.cxfse.interceptors.AttachmentInInterceptor;
import org.apache.servicemix.cxfse.interceptors.AttachmentOutInterceptor;
import org.apache.servicemix.cxfse.support.ReflectionUtils;
import org.apache.servicemix.id.IdGenerator;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.soap.util.DomUtil;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * @author gnodet
 * @org.apache.xbean.XBean element="endpoint" description="an endpoint using
 *                         CXF's JAX-WS frontend"
 */
public class CxfSeEndpoint extends ProviderEndpoint implements InterceptorProvider {

    private static final IdGenerator ID_GENERATOR = new IdGenerator();

    private Object pojo;

    private EndpointImpl endpoint;

    private String address;

    private ServerFactoryBean sf;

    private Server server;

    private List<Interceptor> in = new CopyOnWriteArrayList<Interceptor>();

    private List<Interceptor> out = new CopyOnWriteArrayList<Interceptor>();

    private List<Interceptor> outFault = new CopyOnWriteArrayList<Interceptor>();

    private List<Interceptor> inFault = new CopyOnWriteArrayList<Interceptor>();

    private Map properties;

    private boolean mtomEnabled;

    private boolean useJBIWrapper = true;

    private boolean useSOAPEnvelope = true;

    private boolean useAegis;

    private QName pojoService;
    private String pojoEndpoint;
    private QName pojoInterfaceName;

    private Server soapBindingServer;
    
    /**
     * Returns the object implementing the endpoint's functionality. It is
     * returned as a generic Java <code>Object</code> that can be cast to the
     * proper type.
     * 
     * @return the pojo
     */
    public Object getPojo() {
        return pojo;
    }

    /**
     * Specifies the object implementing the endpoint's functionality. This
     * object should use the JAX-WS annotations.
     * 
     * @param pojo a JAX-WS annotated object
     * @org.apache.xbean.Property description="a bean configuring the JAX-WS
     *                            annotated implementation for the endpoint"
     */
    public void setPojo(Object pojo) {
        this.pojo = pojo;
    }

    /**
     * Returns the list of interceptors used to process fault messages being
     * sent back to the consumer.
     * 
     * @return a list of <code>Interceptor</code> objects
     */
    public List<Interceptor> getOutFaultInterceptors() {
        return outFault;
    }

    /**
     * Returns the list of interceptors used to process fault messages being
     * recieved by the endpoint.
     * 
     * @return a list of <code>Interceptor</code> objects
     */
    public List<Interceptor> getInFaultInterceptors() {
        return inFault;
    }

    /**
     * Returns the list of interceptors used to process messages being recieved
     * by the endpoint.
     * 
     * @return a list of <code>Interceptor</code> objects
     */
    public List<Interceptor> getInInterceptors() {
        return in;
    }

    /**
     * Returns the list of interceptors used to process responses being sent
     * back to the consumer.
     * 
     * @return a list of <code>Interceptor</code> objects
     */
    public List<Interceptor> getOutInterceptors() {
        return out;
    }

    /**
     * Specifies a list of interceptors used to process requests recieved by the
     * endpoint.
     * 
     * @param interceptors a list of <code>Interceptor</code> objects
     * @org.apache.xbean.Property description="a list of beans configuring
     *                            interceptors that process incoming requests"
     */
    public void setInInterceptors(List<Interceptor> interceptors) {
        in.addAll(interceptors);
    }

    /**
     * Specifies a list of interceptors used to process faults recieved by the
     * endpoint.
     * 
     * @param interceptors a list of <code>Interceptor</code> objects
     * @org.apache.xbean.Property description="a list of beans configuring
     *                            interceptors that process incoming faults"
     */
    public void setInFaultInterceptors(List<Interceptor> interceptors) {
        inFault.addAll(interceptors);
    }

    /**
     * Specifies a list of interceptors used to process responses sent by the
     * endpoint.
     * 
     * @param interceptors a list of <code>Interceptor</code> objects
     * @org.apache.xbean.Property description="a list of beans configuring
     *                            interceptors that process response messages"
     */
    public void setOutInterceptors(List<Interceptor> interceptors) {
        out.addAll(interceptors);
    }

    /**
     * Specifies a list of interceptors used to process faults sent by the
     * endpoint.
     * 
     * @param interceptors a list of <code>Interceptor</code> objects
     * @org.apache.xbean.Property description="a list of beans configuring
     *                            interceptors that process fault messages being
     *                            returned to the consumer"
     */
    public void setOutFaultInterceptors(List<Interceptor> interceptors) {
        outFault.addAll(interceptors);
    }

    public Map getProperties() {
        return properties;
    }

    public void setProperties(Map properties) {
        this.properties = properties;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.Endpoint#validate()
     */
    @Override
    public void validate() throws DeploymentException {
        if (getPojo() == null) {
            throw new DeploymentException("pojo must be set");
        }
        if (isUseAegis()) {
            sf = new ServerFactoryBean();
            sf.setServiceBean(getPojo());
            sf.setAddress("jbi://" + ID_GENERATOR.generateSanitizedId());
            sf.getServiceFactory().setDataBinding(new AegisDatabinding());
            sf.getServiceFactory().setPopulateFromClass(true);
            sf.setStart(false);
            if (isUseJBIWrapper()) {
                sf.setBindingId(org.apache.cxf.binding.jbi.JBIConstants.NS_JBI_BINDING);
            }
            server = sf.create();
            server.getEndpoint().getInInterceptors().addAll(getInInterceptors());
            server.getEndpoint().getInFaultInterceptors().addAll(getInFaultInterceptors());
            server.getEndpoint().getOutInterceptors().addAll(getOutInterceptors());
            server.getEndpoint().getOutFaultInterceptors().addAll(getOutFaultInterceptors());
            if (isMtomEnabled()) {
                server.getEndpoint().getInInterceptors().add(new AttachmentInInterceptor());
                server.getEndpoint().getOutInterceptors().add(new AttachmentOutInterceptor());
            }
            if (sf.getServiceFactory().getServiceQName() != null) {
                setPojoService(sf.getServiceFactory().getServiceQName());
                if (getService() == null) {
                    setService(sf.getServiceFactory().getServiceQName());
                }
            }
            if (sf.getServiceFactory().getEndpointInfo().getName() != null) {
                setPojoEndpoint(sf.getServiceFactory().getEndpointInfo().getName().getLocalPart());
                if (getEndpoint() == null) {
                    setEndpoint(sf.getServiceFactory().getEndpointInfo().getName().getLocalPart());
                }
            }
            if (sf.getServiceFactory().getInterfaceName() != null) {
                setPojoInterfaceName(sf.getServiceFactory().getInterfaceName());
                if (getInterfaceName() == null) {
                    setInterfaceName(sf.getServiceFactory().getInterfaceName());
                }
            }
        } else {
            JaxWsServiceFactoryBean serviceFactory = new JaxWsServiceFactoryBean();
            serviceFactory.setPopulateFromClass(true);
            endpoint = new EndpointImpl(getBus(), getPojo(), new JaxWsServerFactoryBean(serviceFactory));
            if (isUseJBIWrapper()) {
                endpoint.setBindingUri(org.apache.cxf.binding.jbi.JBIConstants.NS_JBI_BINDING);
            }
            endpoint.setInInterceptors(getInInterceptors());
            endpoint.setInFaultInterceptors(getInFaultInterceptors());
            endpoint.setOutInterceptors(getOutInterceptors());
            endpoint.setOutFaultInterceptors(getOutFaultInterceptors());
            if (isMtomEnabled()) {
                endpoint.getInInterceptors().add(new AttachmentInInterceptor());
                endpoint.getOutInterceptors().add(new AttachmentOutInterceptor());
            }
            JaxWsImplementorInfo implInfo = new JaxWsImplementorInfo(getPojo().getClass());
            setPojoService(implInfo.getServiceName());
            setPojoInterfaceName(implInfo.getInterfaceName());
            setPojoEndpoint(implInfo.getEndpointName().getLocalPart());
            if (getService() == null) {
                setService(implInfo.getServiceName());
            }
            if (getInterfaceName() == null) {
                setInterfaceName(implInfo.getInterfaceName());
            }
            if (getEndpoint() == null) {
                setEndpoint(implInfo.getEndpointName().getLocalPart());
            }

        }
        super.validate();

    }

    private void removeInterceptor(List<Interceptor> interceptors, String whichInterceptor) {
        for (Interceptor interceptor : interceptors) {
            if (interceptor.getClass().getName().endsWith(whichInterceptor)) {
                interceptors.remove(interceptor);
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#process(javax.jbi.messaging.MessageExchange)
     */
    @Override
    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() != ExchangeStatus.ACTIVE) {
            return;
        }
        JBIContext.setMessageExchange(exchange);
        try {
            QName opeName = exchange.getOperation();
            EndpointInfo ei = server.getEndpoint().getEndpointInfo();

            if (opeName == null) {
                // if interface only have one operation, may not specify the
                // opeName
                // in MessageExchange
                if (ei.getBinding().getOperations().size() == 1) {
                    opeName = ei.getBinding().getOperations().iterator().next().getName();
                    exchange.setOperation(opeName);
                } else {
                    NormalizedMessage nm = exchange.getMessage("in");
                    if (soapBindingServer == null) {
                        ServerFactoryBean sfForSoapBinding = new ServerFactoryBean();
                        sfForSoapBinding.setServiceBean(getPojo());
                        //sfForSoapBinding.setAddress("http://dummyaddress");
                        sfForSoapBinding.getServiceFactory().setPopulateFromClass(true);
                        sfForSoapBinding.setStart(false);
                        soapBindingServer = sfForSoapBinding.create();
                    }
                    Message message = soapBindingServer.getEndpoint().getBinding().createMessage();
                    opeName = findOperation(nm, message, exchange);
                    exchange.setOperation(opeName);

                }
            }
            JBITransportFactory jbiTransportFactory = (JBITransportFactory)getBus()
                .getExtension(ConduitInitiatorManager.class)
                .getConduitInitiator(CxfSeComponent.JBI_TRANSPORT_ID);

            exchange.setService(getPojoService());
            exchange.setInterfaceName(getPojoInterfaceName());
            JBIDestination jbiDestination = jbiTransportFactory.getDestination(getPojoService().toString()
                                                                               + getPojoInterfaceName().toString());
            DeliveryChannel dc = getContext().getDeliveryChannel();
            jbiTransportFactory.setDeliveryChannel(dc);

            jbiDestination.setDeliveryChannel(dc);
            if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                jbiDestination.getJBIDispatcherUtil().dispatch(exchange);
            }
            if (exchange instanceof InOnly || exchange instanceof RobustInOnly) {
                exchange.setStatus(ExchangeStatus.DONE);
                dc.send(exchange);
            }

        } finally {
            JBIContext.setMessageExchange(null);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#start()
     */
    @Override
    public void start() throws Exception {
        super.start();
        address = "jbi://" + ID_GENERATOR.generateSanitizedId();
        try {
            if (isUseAegis()) {
                server.start();
            } else {
                endpoint.publish(address);
                server = endpoint.getServer();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (getService() == null) {
            setService(server.getEndpoint().getService().getName());
        }
        if (getEndpoint() == null) {
            setEndpoint(server.getEndpoint().getEndpointInfo()
                .getName().getLocalPart());
        }
        setPojoService(server.getEndpoint().getService().getName());
        setPojoEndpoint(server.getEndpoint().getEndpointInfo()
                .getName().getLocalPart());
        if (!isUseJBIWrapper() && !isUseSOAPEnvelope()) {
            removeInterceptor(server.getEndpoint().getBinding().getInInterceptors(), "ReadHeadersInterceptor");
            removeInterceptor(server.getEndpoint().getBinding().getInFaultInterceptors(),
                              "ReadHeadersInterceptor");
            removeInterceptor(server.getEndpoint().getBinding().getOutInterceptors(), "SoapOutInterceptor");
            removeInterceptor(server.getEndpoint().getBinding().getOutFaultInterceptors(),
                              "SoapOutInterceptor");
            removeInterceptor(server.getEndpoint().getBinding().getOutInterceptors(), "StaxOutInterceptor");
        }

        try {
            definition = new ServiceWSDLBuilder(getBus(), server.getEndpoint().getService().getServiceInfos()
                .iterator().next()).build();
            description = WSDLFactory.newInstance().newWSDLWriter().getDocument(definition);
        } catch (WSDLException e) {
            throw new DeploymentException(e);
        }
        ReflectionUtils.doWithFields(getPojo().getClass(), new FieldCallback() {
            public void doWith(Field field) throws IllegalArgumentException, IllegalAccessException {
                if (field.getAnnotation(WebServiceRef.class) != null) {
                    ServiceImpl s = new ServiceImpl(getBus(), null, null, field.getType());
                    s
                        .addPort(new QName("port"), JBITransportFactory.TRANSPORT_ID,
                                 "jbi://" + ID_GENERATOR.generateSanitizedId());
                    Object o = s.getPort(new QName("port"), field.getType());
                    field.setAccessible(true);
                    field.set(getPojo(), o);
                }
            }
        });
        ReflectionUtils.callLifecycleMethod(getPojo(), PostConstruct.class);
        injectPojo();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#stop()
     */
    @Override
    public void stop() throws Exception {
        if (isUseAegis()) {
            server.stop();
        } else {
            endpoint.stop();
        }
        ReflectionUtils.callLifecycleMethod(getPojo(), PreDestroy.class);
        JBIDispatcherUtil.clean();
        JBITransportFactory jbiTransportFactory = (JBITransportFactory)getBus()
            .getExtension(ConduitInitiatorManager.class).getConduitInitiator(CxfSeComponent.JBI_TRANSPORT_ID);
        jbiTransportFactory.setDeliveryChannel(null);
        jbiTransportFactory.removeDestination(getPojoService().toString() + getPojoInterfaceName().toString());
        super.stop();
    }

    protected Bus getBus() {
        return ((CxfSeComponent)getServiceUnit().getComponent()).getBus();
    }
    
    private QName findOperation(NormalizedMessage nm, Message message, MessageExchange exchange)
        throws TransformerException, ParserConfigurationException, IOException, SAXException {
        // try to figure out the operationName based on the incoming message
        // payload and wsdl if use doc/literal/wrapped
        Element element = new SourceTransformer().toDOMElement(nm.getContent());

        if (!useJBIWrapper) {
            SoapVersion soapVersion = ((SoapMessage)message).getVersion();
            if (element != null) {
                Element bodyElement = (Element)element.getElementsByTagNameNS(
                                                                              element.getNamespaceURI(),
                                                                              soapVersion.getBody()
                                                                                  .getLocalPart()).item(0);
                if (bodyElement != null) {
                    element = (Element)bodyElement.getFirstChild();
                }
            }
        } else {
            element = DomUtil.getFirstChildElement(DomUtil.getFirstChildElement(element));
        }

        QName opeName = new QName(element.getNamespaceURI(), element.getLocalName());
        SoapBindingInfo binding = (SoapBindingInfo)soapBindingServer.getEndpoint().getEndpointInfo()
            .getBinding();
        for (BindingOperationInfo op : binding.getOperations()) {
            String style = binding.getStyle(op.getOperationInfo());
            if (style == null) {
                style = binding.getStyle();
            }
            if ("document".equals(style)) {
                if (op.getName().getLocalPart().equals(opeName.getLocalPart())) {
                    return new QName(getPojoService().getNamespaceURI(), opeName.getLocalPart());
                }
            } else {
                throw new Fault(new Exception("Operation must bound on this MessageExchange if use rpc mode"));
            }
        }
        throw new Fault(new Exception("Operation not bound on this MessageExchange"));

    }

    @PostConstruct
    protected void injectPojo() {
        try {
            ComponentContext context = getContext();
            Method mth = pojo.getClass().getMethod("setContext", new Class[] {ComponentContext.class});
            if (mth != null) {
                mth.invoke(pojo, new Object[] {context});
            }
        } catch (Exception e) {
            logger.debug("Unable to inject ComponentContext: " + e.getMessage());
        }

    }

    /**
     * Specifies if the endpoint can process messages with binary data.
     * 
     * @param mtomEnabled a <code>boolean</code>
     * @org.apache.xbean.Property description="Specifies if the service can
     *                            consume MTOM formatted binary data. The
     *                            default is <code>false</code>."
     */
    public void setMtomEnabled(boolean mtomEnabled) {
        this.mtomEnabled = mtomEnabled;
    }

    public boolean isMtomEnabled() {
        return mtomEnabled;
    }

    /**
     * Specifies if the endpoint expects messages that are encased in the JBI
     * wrapper used for SOAP messages. Ignore the value of useSOAPEnvelope if
     * useJBIWrapper is true
     * 
     * @org.apache.xbean.Property description="Specifies if the endpoint expects
     *                            to receive the JBI wrapper in the message
     *                            received from the NMR. The default is
     *                            <code>true</code>. Ignore the value of
     *                            useSOAPEnvelope if useJBIWrapper is true"
     */
    public void setUseJBIWrapper(boolean useJBIWrapper) {
        this.useJBIWrapper = useJBIWrapper;
    }

    public boolean isUseJBIWrapper() {
        return useJBIWrapper;
    }

    /**
     * Specifies if the endpoint expects soap messages when useJBIWrapper is
     * false, if useJBIWrapper is true then ignore useSOAPEnvelope
     * 
     * @org.apache.xbean.Property description="Specifies if the endpoint expects
     *                            soap messages when useJBIWrapper is false, if
     *                            useJBIWrapper is true then ignore
     *                            useSOAPEnvelope. The default is
     *                            <code>true</code>.
     */
    public void setUseSOAPEnvelope(boolean useSOAPEnvelope) {
        this.useSOAPEnvelope = useSOAPEnvelope;
    }

    public boolean isUseSOAPEnvelope() {
        return useSOAPEnvelope;
    }

    /**
     * Specifies if the endpoint use aegis databinding to marshell/unmarshell
     * message
     * 
     * @org.apache.xbean.Property description="Specifies if the endpoint use
     *                            aegis databinding to marshell/unmarshell
     *                            message. The default is <code>false</code>.
     */
    public void setUseAegis(boolean useAegis) {
        this.useAegis = useAegis;
    }

    public boolean isUseAegis() {
        return useAegis;
    }

    protected void setPojoService(QName pojoService) {
        this.pojoService = pojoService;
    }

    protected QName getPojoService() {
        return pojoService;
    }

    protected void setPojoEndpoint(String pojoEndpoint) {
        this.pojoEndpoint = pojoEndpoint;
    }

    protected String getPojoEndpoint() {
        return pojoEndpoint;
    }

    protected void setPojoInterfaceName(QName pojoInterfaceName) {
        this.pojoInterfaceName = pojoInterfaceName;
    }

    protected QName getPojoInterfaceName() {
        return pojoInterfaceName;
    }

}
