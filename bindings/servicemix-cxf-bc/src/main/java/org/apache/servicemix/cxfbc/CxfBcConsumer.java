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
package org.apache.servicemix.cxfbc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.activation.DataHandler;
import javax.jbi.JBIException;
import javax.jbi.component.ComponentContext;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.transaction.TransactionManager;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.soap.SOAPBinding;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import com.ibm.wsdl.extensions.soap.SOAPAddressImpl;
import com.ibm.wsdl.extensions.soap.SOAPBindingImpl;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;
import org.apache.cxf.bus.CXFBusImpl;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.catalog.OASISCatalogManager;
import org.apache.cxf.continuations.Continuation;
import org.apache.cxf.continuations.ContinuationProvider;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.frontend.WSDLGetInterceptor;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.BareOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.OneWayProcessorInterceptor;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.interceptor.InterceptorChain.State;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.FaultInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.ChainInitiationObserver;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.http_jetty.JettyHTTPDestination;
import org.apache.cxf.transport.http_jetty.JettyHTTPServerEngine;
import org.apache.cxf.transport.jms.JMSConfiguration;
import org.apache.cxf.transport.jms.JMSDestination;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.rm.Servant;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.apache.servicemix.common.endpoints.ConsumerEndpoint;
import org.apache.servicemix.common.security.AuthenticationService;
import org.apache.servicemix.cxfbc.interceptors.ExtractHeaderPartIntercepor;
import org.apache.servicemix.cxfbc.interceptors.JbiInInterceptor;
import org.apache.servicemix.cxfbc.interceptors.JbiInWsdl1Interceptor;
import org.apache.servicemix.cxfbc.interceptors.JbiJAASInterceptor;
import org.apache.servicemix.cxfbc.interceptors.JbiOperationInterceptor;
import org.apache.servicemix.cxfbc.interceptors.JbiOutWsdl1Interceptor;
import org.apache.servicemix.cxfbc.interceptors.MtomCheckInterceptor;
import org.apache.servicemix.cxfbc.interceptors.JbiFault;
import org.apache.servicemix.cxfbc.interceptors.SchemaValidationInInterceptor;
import org.apache.servicemix.cxfbc.interceptors.SchemaValidationOutInterceptor;
import org.apache.servicemix.cxfbc.interceptors.SetSoapVersionInterceptor;
import org.apache.servicemix.cxfbc.interceptors.SetStatusInterceptor;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.soap.util.DomUtil;
import org.eclipse.jetty.server.Handler;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 
 * @author gnodet
 * @org.apache.xbean.XBean element="consumer" description="a consumer endpoint that is capable of using SOAP/HTTP or SOAP/JMS"
 */
public class CxfBcConsumer extends ConsumerEndpoint implements
        CxfBcEndpointWithInterceptor {

    List<Interceptor<? extends Message>> in = new CopyOnWriteArrayList<Interceptor<? extends Message>>();

    List<Interceptor<? extends Message>> out = new CopyOnWriteArrayList<Interceptor<? extends Message>>();

    List<Interceptor<? extends Message>> outFault = new CopyOnWriteArrayList<Interceptor<? extends Message>>();

    List<Interceptor<? extends Message>> inFault = new CopyOnWriteArrayList<Interceptor<? extends Message>>();

    private Resource wsdl;

    private Endpoint ep;

    private ChainInitiationObserver chain;

    private Server server;

    private Map<String, Message> messages = new ConcurrentHashMap<String, Message>();

    private boolean synchronous;

    private boolean isOneway;

    private String busCfg;

    private BindingFaultInfo faultWanted;

    private Bus bus;

    private Bus providedBus;
    
    private boolean mtomEnabled;

    private String locationURI;

    private long timeout = 0; // default is NO_TIMEOUT

    private boolean useJBIWrapper = true;
    
    private boolean useSOAPEnvelope = true;
    
    private EndpointInfo ei;

    private boolean started;
  
    private List<AbstractFeature> features = new CopyOnWriteArrayList<AbstractFeature>();

    private boolean transactionEnabled;
   
    private ClassLoader suClassLoader;
   
    private boolean x509;
    
    private boolean schemaValidationEnabled;
    
    private boolean delegateToJaas = true;
    
    private String jaasDomain = "servicemix-domain";
    
    private Map<String, Object> properties = new ConcurrentHashMap<String, Object>();

    /**
     * @return the wsdl
     */
    public Resource getWsdl() {
        return wsdl;
    }

    /**
          * Specifies the location of the WSDL defining the endpoint's interface.
          *
          * @param wsdl the location of the WSDL contract as a <code>Resource</code> object
          * @org.apache.xbean.Property description="the location of the WSDL document defining the endpoint's interface"
          **/
    public void setWsdl(Resource wsdl) {
        this.wsdl = wsdl;
    }

    /**
        * Returns the list of interceptors used to process fault messages being
        * sent to the provider.
        *
        * @return a list of <code>Interceptor</code> objects
        * */
    public List<Interceptor<? extends Message>> getOutFaultInterceptors() {
        return outFault;
    }

    /**
        * Returns the list of interceptors used to process fault messages being
        * recieved by the endpoint.
        *
        * @return a list of <code>Interceptor</code> objects
        * */
    public List<Interceptor<? extends Message>> getInFaultInterceptors() {
        return inFault;
    }

    /**
        * Returns the list of interceptors used to process responses being 
        * recieved by the endpoint.
        *
        * @return a list of <code>Interceptor</code> objects
        * */
    public List<Interceptor<? extends Message>> getInInterceptors() {
        return in;
    }

    /**
        * Returns the list of interceptors used to process requests being
        * sent to the provider.
        *
        * @return a list of <code>Interceptor</code> objects
        * */
    public List<Interceptor<? extends Message>> getOutInterceptors() {
        return out;
    }

    /**
        * Specifies a list of interceptors used to process responses recieved
        * by the endpoint.
        *
        * @param interceptors   a list of <code>Interceptor</code> objects
        * @org.apache.xbean.Property description="a list of beans configuring interceptors that process incoming responses"
        * */
    public void setInInterceptors(List<Interceptor<? extends Message>> interceptors) {
        in.addAll(interceptors);
    }

    /**
        * Specifies a list of interceptors used to process faults recieved by
         * the endpoint.
        *
        * @param interceptors   a list of <code>Interceptor</code> objects
        * @org.apache.xbean.Property description="a list of beans configuring interceptors that process incoming faults"
        * */
    public void setInFaultInterceptors(List<Interceptor<? extends Message>> interceptors) {
        inFault.addAll(interceptors);
    }

    /**
        * Specifies a list of interceptors used to process requests sent by 
        * the endpoint.
        *
        * @param interceptors   a list of <code>Interceptor</code> objects
        * @org.apache.xbean.Property description="a list of beans configuring interceptors that process requests"
        * */
    public void setOutInterceptors(List<Interceptor<? extends Message>> interceptors) {
        out.addAll(interceptors);
    }

    /**
        * Specifies a list of interceptors used to process faults sent by 
        * the endpoint.
        *
        * @param interceptors   a list of <code>Interceptor</code> objects
        * @org.apache.xbean.Property description="a list of beans configuring interceptors that process fault messages being returned to the consumer"
        * */
    public void setOutFaultInterceptors(List<Interceptor<? extends Message>> interceptors) {
        outFault.addAll(interceptors);
    }

    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.DONE) {
            Message message = messages.get(exchange.getExchangeId());
            if (message == null) {
                return;
            }
            boolean oneway = message.getExchange().get(BindingOperationInfo.class)
                .getOperationInfo().isOneWay();
            if (oneway) {
                //ensure remove message if oneway to avoid memory leak
                messages.remove(exchange.getExchangeId());
            }
            return;
        }
        Message message = messages.remove(exchange.getExchangeId());
        message.setContent(MessageExchange.class, exchange);
        message.put("needSetDone", Boolean.TRUE);
        
        boolean oneway = message.getExchange().get(BindingOperationInfo.class)
                .getOperationInfo().isOneWay();
        if (!isSynchronous() && !oneway 
            && !isServletTransport()) {
            if (isNativeAsyn(message)) {
                message.getInterceptorChain().resume();
            } else {
                ContinuationProvider continuationProvider = (ContinuationProvider) message
                        .get(ContinuationProvider.class.getName());
                Continuation continuation = continuationProvider
                        .getContinuation();
                synchronized(continuation) {
                	if (continuation.isPending()) {
                		continuation.resume();
                	}
                }
            }
        }
    }

    private boolean isServletTransport() {
        return locationURI != null && locationURI.startsWith("/");
    }

    @Override
    public void activate() throws Exception {
        super.activate();
        try {
            registerListServiceHandler();
            applyFeatures();
            checkJmsTransportTransaction();
        } catch (Exception ex){
            super.deactivate();
            throw ex;
        }
    }

    private void checkJmsTransportTransaction() {
        Destination destination = server.getDestination();
        if (destination instanceof JMSDestination) {
            JMSDestination jmsDestination = (JMSDestination)destination;
            JMSConfiguration jmsConfig = jmsDestination.getJmsConfig();
            if (jmsConfig.isSessionTransacted()) {
                TransactionManager tm = (TransactionManager) getContext().getTransactionManager();
                if (tm == null) {
                    throw new IllegalStateException("No TransactionManager available");
                } else if (tm instanceof PlatformTransactionManager) {
                    jmsConfig.setSessionTransacted(true);
                    jmsConfig.setTransactionManager((PlatformTransactionManager)tm);
                    jmsConfig.setUseJms11(false);
                    setSynchronous(true);
                    transactionEnabled = true;
                }
            }
        } 
        
    }
    
    private void applyFeatures() {
        if (getFeatures() != null) {
            for (AbstractFeature feature : getFeatures()) {
                feature.initialize(server, getBus());
            }
        }
    }

    

    private void registerListServiceHandler() {
        if (server.getDestination() instanceof JettyHTTPDestination) {
            JettyHTTPDestination jettyDest = (JettyHTTPDestination) server.getDestination();
            JettyHTTPServerEngine jettyEng = (JettyHTTPServerEngine) jettyDest.getEngine();
            List<Handler> handlers = jettyEng.getHandlers();
            if (handlers == null) {
                handlers = new ArrayList<Handler>();
                jettyEng.setHandlers(handlers);
            }
            handlers.add(new ListServiceHandler(((CxfBcComponent) getServiceUnit().getComponent()).getAllBuses(),
                                                (CxfBcComponent) getServiceUnit().getComponent()));
        }
    }

    @Override
    public void start() throws Exception {
        server.start();
        super.start();
        this.started = true;
    }

    @Override
    public void stop() throws Exception {
        this.started = false;
        getBus().getExtension(WSDLManager.class).removeDefinition(definition);
        super.stop();
    }

    @Override
    public void deactivate() throws Exception {
        server.stop();
        if (!isComponentBus()) {
            Map<String, Bus> allBuses = ((CxfBcComponent) getServiceUnit().getComponent()).getAllBuses();
            //if use the endpoint own bus, then shutdown it
            if (providedBus != null) {
                if (allBuses.keySet().contains(providedBus.getId())) {
                    allBuses.remove(providedBus.getId());
                }
                providedBus = null;
            } else {
                if (bus != null) {
                    if (allBuses.keySet().contains(bus.getId())) {
                        allBuses.remove(bus.getId());
                    }
                    bus.shutdown(true);
                    bus = null;
                }
            }
        }
        super.deactivate();
    }

    @Override
    public void validate() throws DeploymentException {
        try {
            suClassLoader = Thread.currentThread().getContextClassLoader();
            if (definition == null) {
                
                retrieveWSDL();
            }
            if (service == null) {
                // looking for the servicename according to targetServiceName
                // first
                if (definition.getServices().containsKey(getTargetService())) {
                    service = getTargetService();
                } else {
                    service = (QName) definition.getServices().keySet()
                            .iterator().next();
                }
            }
            WSDLServiceFactory factory = new WSDLServiceFactory(getBus(),
                    definition, service);
            
            Service cxfService = factory.create();

            ei = cxfService.getServiceInfos().iterator().next()
                    .getEndpoints().iterator().next();
            for (ServiceInfo serviceInfo : cxfService.getServiceInfos()) {
                if (serviceInfo.getName().equals(service)
                        && getEndpoint() != null
                        && serviceInfo.getEndpoint(new QName(serviceInfo
                                .getName().getNamespaceURI(), getEndpoint())) != null) {
                    ei = serviceInfo.getEndpoint(new QName(serviceInfo
                            .getName().getNamespaceURI(), getEndpoint()));

                }
            }

            if (endpoint == null) {
                endpoint = ei.getName().getLocalPart();
            }

            if (locationURI != null) {
                ei.setAddress(locationURI);
            }
            cxfService.getInInterceptors().add(new AbstractPhaseInterceptor<Message>(Phase.PRE_PROTOCOL) {
                public void handleMessage(Message message) throws Fault {
                    if (!started) {
                        throw new Fault(new Exception("Endpoint is stopped"));
                    }
                }
            });
            cxfService.getInInterceptors().add(new MustUnderstandInterceptor());
            cxfService.getInInterceptors().add(new AttachmentInInterceptor());
            cxfService.getInInterceptors().add(new StaxInInterceptor());
            cxfService.getInInterceptors().add(WSDLGetInterceptor.INSTANCE);
            cxfService.getInInterceptors().add(new OneWayProcessorInterceptor());
            cxfService.getInInterceptors().add(
                    new ReadHeadersInterceptor(getBus()));
            cxfService.getInInterceptors().add(
                    new JbiOperationInterceptor());
            cxfService.getInInterceptors().add(
                    new JbiInWsdl1Interceptor(isUseJBIWrapper(), isUseSOAPEnvelope()));
            if (isSchemaValidationEnabled()) {
                cxfService.getInInterceptors().add(new SchemaValidationInInterceptor(
                        isUseJBIWrapper(), isUseSOAPEnvelope()));
            }
            if (isSchemaValidationEnabled()) {
                cxfService.getOutInterceptors().add(new SchemaValidationOutInterceptor(
                        isUseJBIWrapper(), isUseSOAPEnvelope()));
            }
            cxfService.getInInterceptors().add(new JbiInInterceptor());
            cxfService.getInInterceptors().add(new JbiJAASInterceptor(
                    AuthenticationService.Proxy.create(
                        ((CxfBcComponent)this.getServiceUnit().getComponent())
                            .getAuthenticationService()), isX509(), isDelegateToJaas(),
                            this.jaasDomain));
            cxfService.getInInterceptors().add(new JbiInvokerInterceptor());
            cxfService.getInInterceptors().add(new JbiPostInvokerInterceptor());

            cxfService.getInInterceptors().add(new OutgoingChainInterceptor());

            cxfService.getOutInterceptors().add(
                    new JbiOutWsdl1Interceptor(isUseJBIWrapper(), isUseSOAPEnvelope()));
            cxfService.getOutInterceptors().add(
                    new ExtractHeaderPartIntercepor());
            cxfService.getOutInterceptors().add(
                    new SetSoapVersionInterceptor());
            cxfService.getOutInterceptors().add(
                    new SetStatusInterceptor());
            
            cxfService.getOutInterceptors().add(new AttachmentOutInterceptor());
            cxfService.getOutInterceptors().add(
                    new MtomCheckInterceptor(isMtomEnabled()));
            cxfService.getOutInterceptors().add(new StaxOutInterceptor());
            /*cxfService.getOutInterceptors().add(
                    new SoapPreProtocolOutInterceptor());
            cxfService.getOutInterceptors().add(
                    new SoapOutInterceptor(getBus()));*/
            cxfService.getOutFaultInterceptors().add(
                    new SoapOutInterceptor(getBus()));

            
            ep = new EndpointImpl(getBus(), cxfService, ei);
            getInInterceptors().addAll(getBus().getInInterceptors());
            getInFaultInterceptors().addAll(getBus().getInFaultInterceptors());
            getOutInterceptors().addAll(getBus().getOutInterceptors());
            getOutFaultInterceptors()
                    .addAll(getBus().getOutFaultInterceptors());

            cxfService.getInInterceptors().addAll(getInInterceptors());
            cxfService.getInFaultInterceptors()
                    .addAll(getInFaultInterceptors());
            cxfService.getOutInterceptors().addAll(getOutInterceptors());
            cxfService.getOutFaultInterceptors().addAll(
                    getOutFaultInterceptors());

            ep.getInInterceptors().addAll(getInInterceptors());
            ep.getInFaultInterceptors().addAll(getInFaultInterceptors());
            ep.getOutInterceptors().addAll(getOutInterceptors());
            ep.getOutFaultInterceptors().addAll(getOutFaultInterceptors());

            ep.getOutInterceptors().add(new AttachmentOutInterceptor());
            ep.getOutInterceptors().add(new StaxOutInterceptor());
            ep.getOutInterceptors().add(new SoapOutInterceptor(getBus()));
            ep.putAll(this.getProperties());

            cxfService.getInInterceptors().addAll(getBus().getInInterceptors());
            cxfService.getInFaultInterceptors().addAll(
                    getBus().getInFaultInterceptors());
            cxfService.getOutInterceptors().addAll(
                    getBus().getOutInterceptors());
            cxfService.getOutFaultInterceptors().addAll(
                    getBus().getOutFaultInterceptors());

            chain = new JbiChainInitiationObserver(ep, getBus());
            removeDatabindingInterceptprs();
            addRMFeatureRequiredInterceptors();
            server = new ServerImpl(getBus(), ep, null, chain);
            super.validate();
        } catch (DeploymentException e) {
            throw e;
        } catch (Exception e) {
            throw new DeploymentException(e);
        }
    }

    private void addRMFeatureRequiredInterceptors() {
        CXFBusImpl bus = (CXFBusImpl) getBus();
        if (bus.getFeatures() != null) {
            for (AbstractFeature feature : bus.getFeatures()){
                if (feature.getClass().getName().equals("org.apache.cxf.ws.rm.feature.RMFeature")) {
                    bus.getOutInterceptors().add(new BareOutInterceptor());
                }
            }
        }
    }

    private void removeDatabindingInterceptprs() {
        for (Interceptor interceptor : ep.getBinding().getInInterceptors()) {
            if (interceptor.getClass().getName().equals("org.apache.cxf.interceptor.DocLiteralInInterceptor")
                || interceptor.getClass().getName().equals("org.apache.cxf.binding.soap.interceptor.SoapHeaderInterceptor")
                || interceptor.getClass().getName().equals("org.apache.cxf.binding.soap.interceptor.RPCInInterceptor")) {
                ep.getBinding().getInInterceptors().remove(interceptor);
            }
        }
        
        for (Interceptor interceptor : ep.getBinding().getOutInterceptors()) {
            if (interceptor.getClass().getName().equals("org.apache.cxf.interceptor.WrappedOutInterceptor")
                || interceptor.getClass().getName().equals("org.apache.cxf.interceptor.BareOutInterceptor")) {
                ep.getBinding().getOutInterceptors().remove(interceptor);
            }
        }
    }
    
    
    private boolean isNativeAsyn(Message message) {
        boolean ret = false;
        AddressingProperties addressingProperties = (AddressingProperties) message.get(WSAUtils.WSA_HEADERS_INBOUND);
        if (addressingProperties != null 
               && !ContextUtils.isGenericAddress(addressingProperties.getReplyTo())) {
            //it's decoupled endpoint, so already switch thread and
            //use executors, which means underlying transport won't 
            //be block, so we shouldn't rely on continuation in 
            //this case, as the SuspendedInvocationException can't be 
            //caught by underlying transport. We just need pause/resume InterceptorChain
            //before/after send/receive MessageExchange for async
            return true;
        }
        return ret;
    }
    
    private void retrieveWSDL() throws JBIException, WSDLException, DeploymentException, IOException {
        if (wsdl == null) {
            ServiceEndpoint targetEndpoint = null;
            // the user has provided the targetService and targetEndpoint attributes
            if (getTargetService() != null && getTargetEndpoint() != null) {
                targetEndpoint = getServiceUnit().getComponent().getComponentContext().getEndpoint(getTargetService(), getTargetEndpoint());
            }
            // the user has provided only the targetService attribute
            if (getTargetService() != null && getTargetEndpoint() == null) {
                ServiceEndpoint[] endpoints = getServiceUnit().getComponent().getComponentContext().getEndpointsForService(getTargetService());
                if (endpoints != null && endpoints.length > 0) {
                    targetEndpoint = endpoints[0];
                }
            }
            // the user has provided only the targetInterfaceName attribute
            if (getTargetEndpoint() == null && getTargetInterface() != null) {
                ServiceEndpoint[] endpoints = getServiceUnit().getComponent().getComponentContext().getEndpoints(getTargetInterface());
                if (endpoints != null && endpoints.length > 0) {
                    targetEndpoint = endpoints[0];
                }
            }
            
            if (targetEndpoint == null) {
                throw new DeploymentException("The target endpoint is not found.");
            }
            
            description = this.getServiceUnit().getComponent().getComponentContext().getEndpointDescriptor(targetEndpoint);
            definition = getBus().getExtension(WSDLManager.class).getDefinition((Element)description.getFirstChild());
            List address = definition.getService(getTargetService()).getPort(getTargetEndpoint()).getExtensibilityElements();
            if (address == null || address.size() == 0) {
                SOAPAddressImpl soapAddress = new SOAPAddressImpl();
                //specify default transport if there is no one in the internal wsdl
                soapAddress.setLocationURI("http://localhost");
                definition.getService(getTargetService()).getPort(getTargetEndpoint()).addExtensibilityElement(soapAddress);
            }
            List binding = definition.getService(getTargetService()).getPort(getTargetEndpoint()).getBinding().getExtensibilityElements();
            if (binding == null || binding.size() == 0) {
                //no binding info in the internal wsdl so we need add default soap11 binding
                SOAPBinding soapBinding = new SOAPBindingImpl();
                soapBinding.setTransportURI("http://schemas.xmlsoap.org/soap/http");
                soapBinding.setStyle("document");
                definition.getService(getTargetService()).getPort(getTargetEndpoint()).getBinding().
                addExtensibilityElement(soapBinding);
            }
        } else {
            description = DomUtil.parse(wsdl.getInputStream());
            try {
                //ensure the jax-ws-catalog is loaded
                OASISCatalogManager.getCatalogManager(getBus()).loadContextCatalogs();
                // use wsdl manager to parse wsdl or get cached
                // definition
                definition = getBus().getExtension(WSDLManager.class)
                        .getDefinition(wsdl.getURL());
            } catch (WSDLException ex) {
                // throw new ServiceConstructionException(new
                // Message("SERVICE_CREATION_MSG", LOG), ex);
            }
        }
    }

    protected Bus getBus() {
        Bus ret = null;
        if (isServletTransport()) {
            //it's serlvet transport, using the bus from CXFNonSpringServlet
            ret = BusFactory.getDefaultBus();
            return ret;
        }
        if (providedBus != null) {
            ret = providedBus;
        } else if (getBusCfg() != null) {
            if (bus == null) {
                SpringBusFactory bf = new SpringBusFactory();
                bus = bf.createBus(getBusCfg());
                if (isServletTransport()) {
                    //it's in the servlet container
                    //set this bus so that it could be used in CXFManagerServlet
                    BusFactory.setDefaultBus(bus);
                }
            }
            ret = bus;
        } else {
        	bus = ((CxfBcComponent) getServiceUnit().getComponent()).getBus();
            ret = bus;
        }
        Map<String, Bus> allBuses = ((CxfBcComponent) getServiceUnit().getComponent()).getAllBuses();
        if (!allBuses.keySet().contains(ret.getId())) {
            allBuses.put(ret.getId(), ret);
        }
        return ret;
    }

    private boolean isComponentBus() {
        return getBus() ==  ((CxfBcComponent) getServiceUnit().getComponent()).getBus();
    }

    /**
           * Specifies the HTTP address to which requests are sent. This value
           * will overide any value specified in the WSDL.
           *
           * @param locationURI the URI as a string
           * @org.apache.xbean.Property description="the HTTP address to which requests are sent. This value will overide any value specified in the WSDL."
           **/
    public void setLocationURI(String locationURI) {
        this.locationURI = locationURI;
    }

    public String getLocationURI() {
        return locationURI;
    }

    protected class JbiChainInitiationObserver extends ChainInitiationObserver {

        public JbiChainInitiationObserver(Endpoint endpoint, Bus bus) {
            super(endpoint, bus);
        }

        protected void setExchangeProperties(Exchange exchange, Message m) {
            super.setExchangeProperties(exchange, m);
            exchange.put(ComponentContext.class, CxfBcConsumer.this
                    .getContext());
            exchange.put(CxfBcConsumer.class, CxfBcConsumer.this);
        }
      
        public void onMessage(Message m) {
            m.put("suClassloader", suClassLoader);
            ClassLoader oldCl = Thread.currentThread().getContextClassLoader();
            if (oldCl != suClassLoader) {
                try {
                    Thread.currentThread().setContextClassLoader(suClassLoader);
                    super.onMessage(m);
                } finally {
                    Thread.currentThread().setContextClassLoader(oldCl);
                }
            } else {
                super.onMessage(m);
            }
        }

    }

    public class JbiInvokerInterceptor extends
            AbstractPhaseInterceptor<Message> {

        public JbiInvokerInterceptor() {
            super(Phase.INVOKE);
        }

        private Object getInvokee(Message message) {
            Object invokee = message.getContent(List.class);
            if (invokee == null) {
                invokee = message.getContent(Object.class);
            }
            return invokee;
        }

        private void copyJaxwsProperties(Message inMsg, Message outMsg) {
            outMsg.put(Message.WSDL_OPERATION, inMsg
                    .get(Message.WSDL_OPERATION));
            outMsg.put(Message.WSDL_SERVICE, inMsg.get(Message.WSDL_SERVICE));
            outMsg.put(Message.WSDL_INTERFACE, inMsg
                    .get(Message.WSDL_INTERFACE));
            outMsg.put(Message.WSDL_PORT, inMsg.get(Message.WSDL_PORT));
            outMsg.put(Message.WSDL_DESCRIPTION, inMsg
                    .get(Message.WSDL_DESCRIPTION));
        }

        public void handleMessage(final Message message) throws Fault {

            final Exchange cxfExchange = message.getExchange();
            final Endpoint endpoint = cxfExchange.get(Endpoint.class);
            final Service service = endpoint.getService();
            final Invoker invoker = service.getInvoker();

            
            
            if (invoker instanceof Servant) {
                // it's rm request, run the invocation directly in bc, not send
                // to se.

                Exchange runableEx = message.getExchange();

                Object result = invoker.invoke(runableEx, getInvokee(message));
                if (!cxfExchange.isOneWay()) {
                    Endpoint end = cxfExchange.get(Endpoint.class);

                    Message outMessage = runableEx.getOutMessage();
                    if (outMessage == null) {
                        outMessage = end.getBinding().createMessage();
                        cxfExchange.setOutMessage(outMessage);
                    }
                    copyJaxwsProperties(message, outMessage);
                    if (result != null) {
                        MessageContentsList resList = null;
                        if (result instanceof MessageContentsList) {
                            resList = (MessageContentsList) result;
                        } else if (result instanceof List) {
                            resList = new MessageContentsList((List) result);
                        } else if (result.getClass().isArray()) {
                            resList = new MessageContentsList((Object[]) result);
                        } else {
                            outMessage.setContent(Object.class, result);
                        }
                        if (resList != null) {
                            outMessage.setContent(List.class, resList);
                        }
                    }
                }

                return;
            }

            MessageExchange exchange = message
                    .getContent(MessageExchange.class);
            ComponentContext context = message.getExchange().get(
                    ComponentContext.class);
            CxfBcConsumer.this.configureExchangeTarget(exchange);
            CxfBcConsumer.this.isOneway = message.getExchange().get(
                    BindingOperationInfo.class).getOperationInfo().isOneWay();
            message.getExchange().setOneWay(CxfBcConsumer.this.isOneway);

            try {
            	if (CxfBcConsumer.this.isOneway) {
                        CxfBcConsumer.this.messages.put(exchange.getExchangeId(), message);
            		context.getDeliveryChannel().send(exchange);
            	} else if ((CxfBcConsumer.this.isSynchronous()
                        && !CxfBcConsumer.this.isOneway)
                        || isServletTransport()) {
                    CxfBcConsumer.this.messages.put(exchange.getExchangeId(), message);
                    context.getDeliveryChannel().sendSync(exchange,
                            timeout * 1000);
                    process(exchange);
                } else {
                    if (isNativeAsyn(message)) {
                        synchronized (message) {

                            if (!((PhaseInterceptorChain)message.getInterceptorChain()).getState()
                                    .equals(State.PAUSED)) {
                                CxfBcConsumer.this.messages.put(exchange
                                        .getExchangeId(), message);
                                context.getDeliveryChannel().send(exchange);
                                message.getInterceptorChain().pause();
                            } else {
                                // retry or timeout
                                if (!((PhaseInterceptorChain)message.getInterceptorChain()).getState()
                                        .equals(State.EXECUTING)) {
                                    messages.remove(exchange.getExchangeId());
                                    // exchange timeout
                                    throw new Exception("Exchange timed out: "
                                            + exchange.getExchangeId());
                                }

                            }

                        }
                    } else {
                        synchronized (((ContinuationProvider) message
                                .get(ContinuationProvider.class.getName()))
                                .getContinuation()) {

                            ContinuationProvider continuationProvider = (ContinuationProvider) message
                                    .get(ContinuationProvider.class.getName());
                            Continuation continuation = continuationProvider
                                    .getContinuation();
                            if (continuation.isNew()) {
                            	continuation.suspend(timeout * 1000);
                                CxfBcConsumer.this.messages.put(exchange
                                        .getExchangeId(), message);
                                context.getDeliveryChannel().send(exchange);
                            } else if (!continuation.isResumed()) {
                                if (!continuation.isPending()) {
                                    messages.remove(exchange.getExchangeId());
                                    continuation.reset();
                                    // exchange timeout
                                    throw new Exception("Exchange timed out: "
                                            + exchange.getExchangeId());
                                }
                            }
                        }
                    }
                }
            } catch (org.apache.cxf.continuations.SuspendedInvocationException e) {
                throw e;
            } catch (Exception e) {
                throw new Fault(e);
            }
        }

    }

    protected class JbiPostInvokerInterceptor extends
            AbstractPhaseInterceptor<Message> {
        public JbiPostInvokerInterceptor() {
            super(Phase.POST_INVOKE);
            addBefore(OutgoingChainInterceptor.class.getName());
        }

        public void handleMessage(final Message message) throws Fault {
        	MessageExchange exchange = message
                    .getContent(MessageExchange.class);
            Exchange ex = message.getExchange();
            if (exchange.getStatus() == ExchangeStatus.ERROR) {
                throw new Fault(exchange.getError());
            }
            if (!ex.isOneWay()) {
                if (exchange.getFault() != null) {
                    Fault f = null;
                    if (isUseJBIWrapper()) {
                        f = new JbiFault(
                                new org.apache.cxf.common.i18n.Message(
                                        "Fault occured", (ResourceBundle) null));
                        if (exchange.getProperty("faultstring") != null) {
                            f.setMessage((String)exchange.getProperty("faultstring"));
                        }
                        if (exchange.getProperty("faultcode") != null) {
                            f.setFaultCode((QName) exchange
                                    .getProperty("faultcode"));
                        } 
                        if (exchange.getProperty("hasdetail") == null) {
                            Element details = toElement(exchange.getFault()
                                .getContent());
                            f.setDetail(details);
                        }

                        
                    } else {
                        Element details = toElement(exchange.getFault()
                                .getContent());
                                     
                        if (isUseSOAPEnvelope()) {
                        	details = (Element) details.getElementsByTagNameNS(
                                details.getNamespaceURI(), "Body").item(0);
                        	assert details != null;
                        	details = (Element) details.getElementsByTagNameNS(
                                details.getNamespaceURI(), "Fault").item(0);
                        }
                        assert details != null;
                        if (exchange.getProperty("faultstring") != null) {
                            details = (Element) details.getElementsByTagName("faultstring").item(0);
                        } else {
                            details = (Element) details.getElementsByTagName("detail").item(0) == null ?
                                    (Element) details.getElementsByTagName("soap:Detail").item(0):
                                        (Element) details.getElementsByTagName("detail").item(0);
                          
                        }

                        assert details != null;
                        f = new SoapFault(
                                new org.apache.cxf.common.i18n.Message(
                                        "Fault occured", (ResourceBundle) null),
                                new QName(details.getNamespaceURI(), "detail"));
                        f.setDetail(details);
                        if (exchange.getProperty("faultstring") != null) {
                            f.setMessage((String)exchange.getProperty("faultstring"));
                        }
                        
                        if (exchange.getProperty("faultcode") != null) {
                            f.setFaultCode((QName) exchange
                                    .getProperty("faultcode"));
                        } 

                    }
                    processFaultDetail(f, message);
                    message.put(BindingFaultInfo.class, faultWanted);
                    

                    throw f;
                } else if (exchange.getMessage("out") != null) {
                    Endpoint endpoint = ex.get(Endpoint.class);
                    Message outMessage = ex.getOutMessage();
                    if (outMessage == null) {
                        outMessage = endpoint.getBinding().createMessage();
                        ex.setOutMessage(outMessage);
                    }
                    outMessage.setContent(MessageExchange.class, exchange);
                    outMessage.put("needSetDone", message.get("needSetDone"));
                    NormalizedMessage norMessage = (NormalizedMessage) exchange
                            .getMessage("out");

                    if (outMessage instanceof SoapMessage) {
                        AddressingProperties addressingProperties = WSAUtils
                                .getCXFAddressingPropertiesFromMap((Map<String, String>) norMessage
                                        .getProperty(WSAUtils.WSA_HEADERS_OUTBOUND));
                        outMessage.put(WSAUtils.WSA_HEADERS_OUTBOUND,
                                addressingProperties);
                    }
                    List<Attachment> attachmentList = new ArrayList<Attachment>();
                    outMessage.setContent(Source.class, exchange.getMessage(
                            "out").getContent());
                    Set attachmentNames = norMessage.getAttachmentNames();
                    
                    Iterator iter = attachmentNames.iterator();
                    while (iter.hasNext()) {
                        String id = (String)iter.next();
                        DataHandler dh = norMessage.getAttachment(id);
                        attachmentList.add(new AttachmentImpl(id, dh));
                    }

                    outMessage.setAttachments(attachmentList);
                }
                
            }
            
        }
        
        public void handleFault(Message message) {
            if (transactionEnabled) {
                //detect if the fault is defined in the wsdl, which means need return to client and 
                //jms transactionManger just commit
                Exchange ex = message.getExchange();
                BindingOperationInfo boi = ex.get(BindingOperationInfo.class);
                for (BindingFaultInfo bfi : boi.getFaults()) {
                    FaultInfo fi = bfi.getFaultInfo();
                    //get fault details
                    MessagePartInfo mpi = fi.getMessageParts().get(0);
                    if (mpi != null) {
                        Fault fault = (Fault) message.getContent(Exception.class);
                        Element detail = fault.getDetail();
                        if (detail != null 
                                && detail.getFirstChild().getLocalName().equals(mpi.getName().getLocalPart())) {
                            //it's fault defined in the wsdl, so let it go back to the client
                            return;
                        }
                    }
                }
                //this exception is undefined in the wsdl, so tell the transactionManager injected into
                //jms transport need rollback
                throw new Error("rollback");
            }
            MessageExchange exchange = message.getContent(MessageExchange.class);
            try {
                if (message.get("needSetDone") != null && message.get("needSetDone").equals(Boolean.TRUE)
                    && exchange.getStatus() == ExchangeStatus.ACTIVE) {
                    exchange.setStatus(ExchangeStatus.DONE);
                    message.getExchange().get(ComponentContext.class).getDeliveryChannel().send(exchange);
                }
            } catch (Exception e) {
                throw new Fault(e);
            }
        }

        // this method is used for ws-policy to set BindingFaultInfo
        protected void processFaultDetail(Fault fault, Message msg) {
            if (fault.getDetail() == null) {
                return;
            }
            Element exDetail = (Element) DOMUtils.getChild(fault.getDetail(),
                    Node.ELEMENT_NODE);
            if (exDetail == null) {
                return;
            }

            QName qname = new QName(exDetail.getNamespaceURI(), exDetail
                    .getLocalName());

            faultWanted = null;
            BindingOperationInfo boi = msg.getExchange().get(
                    BindingOperationInfo.class);
            if (boi.isUnwrapped()) {
                boi = boi.getWrappedOperation();
            }
            for (BindingFaultInfo bfi : boi.getFaults()) {
                for (MessagePartInfo mpi : bfi.getFaultInfo().getMessageParts()) {
                    if (qname.equals(mpi.getConcreteName())) {
                        faultWanted = bfi;
                        msg.put(BindingFaultInfo.class, faultWanted);
                        break;
                    }
                }
                if (faultWanted != null) {
                    break;
                }
            }

        }

    }

    private static Element toElement(Source src) throws Fault {
        try {
            SourceTransformer transformer = new SourceTransformer();
            Element ret = transformer.toDOMElement(src);
            ret = removeEmptyDefaultTns(ret);
            return ret;
        } catch (Exception e) {
            throw new Fault(e);
        }
    }

    private static Element removeEmptyDefaultTns(Element ret) {
        // to make unquailied fault work
        if (ret.hasAttribute("xmlns")
                && ret.getAttribute("xmlns").length() == 0) {
            ret.removeAttribute("xmlns");
        }
        NodeList nodes = ret.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element) {
                Element ele = (Element) nodes.item(i);
                ele = removeEmptyDefaultTns(ele);

            }
        }
        return ret;
    }

    /**
        * Specifies the location of the CXF configuraiton file used to configure
        * the CXF bus. This allows you to access features like JMS runtime 
        * behavior and WS-RM.
        *
        * @param busCfg a string containing the relative path to the configuration file
        * @org.apache.xbean.Property description="the location of the CXF configuration file used to configure the CXF bus. This allows you to configure features like WS-RM and JMS runtime behavior."
        **/
    public void setBusCfg(String busCfg) {
        this.busCfg = busCfg;
    }

    public String getBusCfg() {
        return busCfg;
    }

    /**
          * Specifies if the endpoint can support binnary attachments.
          *
          * @param  mtomEnabled a boolean
          * @org.apache.xbean.Property description="Specifies if MTOM / attachment support is enabled. Default is <code>false</code>."
          **/
    public void setMtomEnabled(boolean mtomEnabled) {
        this.mtomEnabled = mtomEnabled;
    }

    public boolean isMtomEnabled() {
        return mtomEnabled;
    }

    /**
          * Specifies the interval for which the endpoint will wait for a 
          * response, This is specified in seconds.
          *
          * @param  timeout the number of second to wait for a response
          * @org.apache.xbean.Property description="the number of second the endpoint will wait for a response. The default is unlimited."
          **/
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getTimeout() {
        return timeout;
    }

    /**
     * Specifies if the endpoint expects messages to use the JBI wrapper 
     * for SOAP messages.
     *
     * @param  useJBIWrapper a boolean
     * @org.apache.xbean.Property description="Specifies if the JBI wrapper is sent in the body of the message. Default is <code>true</code>.
     * 	Ignore the value of useSOAPEnvelope if useJBIWrapper is true"
     **/
    public void setUseJBIWrapper(boolean useJBIWrapper) {
        this.useJBIWrapper = useJBIWrapper;
    }

    public boolean isUseJBIWrapper() {
        return useJBIWrapper;
    }
   
    /**
     * Specifies if the endpoint expects soap messages when useJBIWrapper is false, 
     * if useJBIWrapper is true then ignore useSOAPEnvelope
     *
     * @org.apache.xbean.Property description="Specifies if the endpoint expects soap messages when useJBIWrapper is false, 
     * 				if useJBIWrapper is true then ignore useSOAPEnvelope. The  default is <code>true</code>.
     * */
	public void setUseSOAPEnvelope(boolean useSOAPEnvelope) {
		this.useSOAPEnvelope = useSOAPEnvelope;
	}

	public boolean isUseSOAPEnvelope() {
		return useSOAPEnvelope;
	}
    
    /**
     * Specifies if the endpoint expects send messageExchange by sendSync
     * @param  synchronous a boolean
     * @org.apache.xbean.Property description="Specifies if the endpoint expects send messageExchange by sendSync .
     * Default is <code>true</code>."
     **/
    public void setSynchronous(boolean synchronous) {
        this.synchronous = synchronous;
    }

    public boolean isSynchronous() {
        return synchronous;
    }
    
    /**
     * Specifies the cxf features set for this endpoint
     *
     * @param  features a list of <code>AbstractFeature</code> objects
     * @org.apache.xbean.Property description="Specifies the cxf features set for this endpoint"
     **/
    public void setFeatures(List<AbstractFeature> features) {
        this.features = features;
    }

    public List<AbstractFeature> getFeatures() {
        return features;
    }
 
    /**
     * Specifies if the endpoint use X.509 Certificate to do the authentication.
     * 
     * @param x509
     *            a boolean
     * @org.apache.xbean.Property description="Specifies if the endpoint use X.509 Certificate to do the authentication.
     *  Default is <code>false</code>. 
     */
    public void setX509(boolean x509) {
        this.x509 = x509;
    }

    public boolean isX509() {
        return x509;
    }
    
    public boolean isSchemaValidationEnabled() {
        return schemaValidationEnabled;
    }

    /**
     * Specifies if the endpoint use schemavalidation for the incoming/outgoing message.
     * 
     * @param schemaValidationEnabled
     *            a boolean
     * @org.apache.xbean.Property description="Specifies if the endpoint use schemavalidation for the incoming/outgoing message.
     *  Default is <code>false</code>. 
     */

    public void setSchemaValidationEnabled(boolean schemaValidationEnabled) {
        this.schemaValidationEnabled = schemaValidationEnabled;
    }

    /**
     * Specifies a preconfigured CXF bus to use.
     *
     * @param providedBus   
     * @org.apache.xbean.Property description="a preconfigured CXF Bus object to use; overrides busCfg"
     * */
     public void setProvidedBus(Bus providedBus) {
         this.providedBus = providedBus;
     }
     
     public Bus getProvidedBus() {
         return this.providedBus;
     }

     /**
      * Specifies if the endpoint delegate to JAASAuthenticationService to do the authentication.
      * 
      * @param delegateToJaas
      *            a boolean
      * @org.apache.xbean.Property description="Specifies if the endpoint delegate to JAASAuthenticationService to do the authentication.
      *  Default is <code>true</code>. 
      */
     public void setDelegateToJaas(boolean delegateToJaas) {
         this.delegateToJaas = delegateToJaas;
     }

     public boolean isDelegateToJaas() {
         return delegateToJaas;
     }

     /**
      * Sets arbitrary properties that are added to the CXF context at
      * the Endpoint level.
      *
      * @param properties
      *             the properties to add
      * @org.apache.xbean.Property description="Sets arbitrary properties that are added to the CXF context at the Endpoint level"             
      */
     public void setProperties(Map<String, Object> properties) {
         this.properties.putAll(properties);
     }
     
     public Map<String, Object> getProperties() {
         return this.properties;
     }

     /**
      * Specifies the jaasDomain of this cxfbc consumer endpoint
      *
      * @param jaasDomain the jaasDomain as a string
      * @org.apache.xbean.Property description="jaasDomain of this cxfbc consumer endpoint"
      **/
     public void setJaasDomain(String jaasDomain) {
         this.jaasDomain = jaasDomain;
     }

     public String getJaasDomain() {
        return jaasDomain;
     }
}
