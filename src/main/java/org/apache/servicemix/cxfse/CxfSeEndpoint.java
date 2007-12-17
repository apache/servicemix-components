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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.jbi.component.ComponentContext;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.wsdl.WSDLException;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceRef;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.jbi.interceptor.JBIWrapperOutInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.InterceptorProvider;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.jaxws.ServiceImpl;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.jbi.JBIDestination;
import org.apache.cxf.transport.jbi.JBIDispatcherUtil;
import org.apache.cxf.transport.jbi.JBITransportFactory;
import org.apache.cxf.wsdl11.ServiceWSDLBuilder;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.cxfse.interceptors.AttachmentInInterceptor;
import org.apache.servicemix.cxfse.interceptors.WrapperOutInterceptor;
import org.apache.servicemix.cxfse.support.ReflectionUtils;
import org.apache.servicemix.id.IdGenerator;
import org.springframework.util.ReflectionUtils.FieldCallback;

/**
 * 
 * @author gnodet
 * @org.apache.xbean.XBean element="endpoint"
 */
public class CxfSeEndpoint extends ProviderEndpoint implements
        InterceptorProvider {

    private static final IdGenerator ID_GENERATOR = new IdGenerator();

    private Object pojo;

    private EndpointImpl endpoint;

    private String address;

    private List<Interceptor> in = new CopyOnWriteArrayList<Interceptor>();

    private List<Interceptor> out = new CopyOnWriteArrayList<Interceptor>();

    private List<Interceptor> outFault = new CopyOnWriteArrayList<Interceptor>();

    private List<Interceptor> inFault = new CopyOnWriteArrayList<Interceptor>();
    
    private Map properties;
    
    private boolean mtomEnabled;
    
    
    /**
     * @return the pojo
     */
    public Object getPojo() {
        return pojo;
    }

    /**
     * @param pojo
     *            the pojo to set
     */
    public void setPojo(Object pojo) {
        this.pojo = pojo;
    }

    public List<Interceptor> getOutFaultInterceptors() {
        return outFault;
    }

    public List<Interceptor> getInFaultInterceptors() {
        return inFault;
    }

    public List<Interceptor> getInInterceptors() {
        return in;
    }

    public List<Interceptor> getOutInterceptors() {
        return out;
    }

    public void setInInterceptors(List<Interceptor> interceptors) {
        in = interceptors;
    }

    public void setInFaultInterceptors(List<Interceptor> interceptors) {
        inFault = interceptors;
    }

    public void setOutInterceptors(List<Interceptor> interceptors) {
        out = interceptors;
    }

    public void setOutFaultInterceptors(List<Interceptor> interceptors) {
        outFault = interceptors;
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
        JaxWsServiceFactoryBean serviceFactory = new JaxWsServiceFactoryBean();
        serviceFactory.setPopulateFromClass(true);
        endpoint = new EndpointImpl(getBus(), getPojo(),
                new JaxWsServerFactoryBean(serviceFactory));
        endpoint.setBindingUri(org.apache.cxf.binding.jbi.JBIConstants.NS_JBI_BINDING);
        endpoint.setInInterceptors(getInInterceptors());
        endpoint.setInFaultInterceptors(getInFaultInterceptors());
        endpoint.setOutInterceptors(getOutInterceptors());
        endpoint.setOutFaultInterceptors(getOutFaultInterceptors());
        endpoint.getOutInterceptors().add(new WrapperOutInterceptor());
        if (isMtomEnabled()) {
            endpoint.getInInterceptors().add(new AttachmentInInterceptor());
        }
        JaxWsImplementorInfo implInfo = new JaxWsImplementorInfo(getPojo()
                .getClass());
        setService(implInfo.getServiceName());
        setInterfaceName(implInfo.getInterfaceName());
        setEndpoint(implInfo.getEndpointName().getLocalPart());
        super.validate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#process(javax.jbi.messaging.MessageExchange)
     */
    @Override
    public void process(MessageExchange exchange) throws Exception {
        JBITransportFactory jbiTransportFactory = (JBITransportFactory) getBus()
                .getExtension(ConduitInitiatorManager.class)
                .getConduitInitiator(CxfSeComponent.JBI_TRANSPORT_ID);
        JBIDestination jbiDestination = jbiTransportFactory
                .getDestination(exchange.getService().toString()
                        + exchange.getInterfaceName().toString());
        DeliveryChannel dc = getContext().getDeliveryChannel();
        jbiTransportFactory.setDeliveryChannel(dc);
        jbiDestination.setDeliveryChannel(dc);
        if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            jbiDestination.getJBIDispatcherUtil().dispatch(exchange);
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
        endpoint.publish(address);
        removeJBIWrapperInterceptor(endpoint.getServer().getEndpoint().getBinding().getOutInterceptors());
        setService(endpoint.getServer().getEndpoint().getService().getName());
        setEndpoint(endpoint.getServer().getEndpoint().getEndpointInfo()
                .getName().getLocalPart());
        try {
            definition = new ServiceWSDLBuilder(getBus(), endpoint.getServer()
                    .getEndpoint().getService().getServiceInfos().iterator()
                    .next()).build();
        } catch (WSDLException e) {
            throw new DeploymentException(e);
        }
        ReflectionUtils.doWithFields(getPojo().getClass(), new FieldCallback() {
            public void doWith(Field field) throws IllegalArgumentException,
                    IllegalAccessException {
                if (field.getAnnotation(WebServiceRef.class) != null) {
                    ServiceImpl s = new ServiceImpl(getBus(), null, null, field
                            .getType());
                    s.addPort(new QName("port"),
                            JBITransportFactory.TRANSPORT_ID, "jbi://"
                                    + ID_GENERATOR.generateSanitizedId());
                    Object o = s.getPort(new QName("port"), field.getType());
                    field.setAccessible(true);
                    field.set(getPojo(), o);
                }
            }
        });
        ReflectionUtils.callLifecycleMethod(getPojo(), PostConstruct.class);
        injectPojo();
    }

    private void removeJBIWrapperInterceptor(List<Interceptor> outInterceptors) {
                            
        Iterator<Interceptor> iter = outInterceptors.iterator();
        while (iter.hasNext()) {
            Interceptor interceptor = iter.next();
            if (interceptor instanceof JBIWrapperOutInterceptor) {
                outInterceptors.remove(interceptor);
                break;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.servicemix.common.endpoints.ProviderEndpoint#stop()
     */
    @Override
    public void stop() throws Exception {
        ReflectionUtils.callLifecycleMethod(getPojo(), PreDestroy.class);
        JBIDispatcherUtil.clean();
        //getBus().shutdown(true);
        JBITransportFactory jbiTransportFactory = (JBITransportFactory) getBus()
            .getExtension(ConduitInitiatorManager.class)
            .getConduitInitiator(CxfSeComponent.JBI_TRANSPORT_ID);
        jbiTransportFactory.removeDestination(getService().toString()
                    + getInterfaceName().toString());
        super.stop();
    }

    protected Bus getBus() {
        return ((CxfSeComponent) getServiceUnit().getComponent()).getBus();
    }

    
    @PostConstruct
    protected void injectPojo() {
        try {
            ComponentContext context = getContext();
            Method mth = pojo.getClass().getMethod("setContext", new Class[] {ComponentContext.class });
            if (mth != null) {
                mth.invoke(pojo, new Object[] {context});
            }
        } catch (Exception e) {
            logger.debug("Unable to inject ComponentContext: " + e.getMessage());
        }
        
    }

    public void setMtomEnabled(boolean mtomEnabled) {
        this.mtomEnabled = mtomEnabled;
    }

    public boolean isMtomEnabled() {
        return mtomEnabled;
    }
}
