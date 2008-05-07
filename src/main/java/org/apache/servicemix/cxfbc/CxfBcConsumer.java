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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.activation.DataHandler;
import javax.jbi.component.ComponentContext;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.wsdl.WSDLException;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.ibm.wsdl.Constants;
import org.apache.cxf.Bus;
import org.apache.cxf.attachment.AttachmentImpl;
import org.apache.cxf.binding.AbstractBindingFactory;
import org.apache.cxf.binding.jbi.JBIFault;
import org.apache.cxf.binding.soap.SoapFault;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.MustUnderstandInterceptor;
import org.apache.cxf.binding.soap.interceptor.ReadHeadersInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapActionOutInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.AttachmentInInterceptor;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.interceptor.StaxOutInterceptor;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.invoker.Invoker;
import org.apache.cxf.service.model.BindingFaultInfo;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.MessagePartInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.ChainInitiationObserver;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.rm.Servant;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.apache.servicemix.common.endpoints.ConsumerEndpoint;
import org.apache.servicemix.cxfbc.interceptors.JbiInInterceptor;
import org.apache.servicemix.cxfbc.interceptors.JbiInWsdl1Interceptor;
import org.apache.servicemix.cxfbc.interceptors.JbiOperationInterceptor;
import org.apache.servicemix.cxfbc.interceptors.JbiOutWsdl1Interceptor;
import org.apache.servicemix.cxfbc.interceptors.MtomCheckInterceptor;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.soap.util.DomUtil;
import org.springframework.core.io.Resource;

/**
 * 
 * @author gnodet
 * @org.apache.xbean.XBean element="consumer"
 */
public class CxfBcConsumer extends ConsumerEndpoint implements
        CxfBcEndpointWithInterceptor {

    List<Interceptor> in = new CopyOnWriteArrayList<Interceptor>();

    List<Interceptor> out = new CopyOnWriteArrayList<Interceptor>();

    List<Interceptor> outFault = new CopyOnWriteArrayList<Interceptor>();

    List<Interceptor> inFault = new CopyOnWriteArrayList<Interceptor>();

    private Resource wsdl;

    private Endpoint ep;

    private ChainInitiationObserver chain;

    private Server server;

    private Map<String, Message> messages = new ConcurrentHashMap<String, Message>();

    private boolean synchronous = true;

    private boolean isOneway;

    private String busCfg;

    private BindingFaultInfo faultWanted;

    private Bus bus;

    private boolean mtomEnabled;

    private String locationURI;

    private int timeout = 10;

    private boolean useJBIWrapper = true;

    /**
     * @return the wsdl
     */
    public Resource getWsdl() {
        return wsdl;
    }

    /**
     * @param wsdl
     *            the wsdl to set
     */
    public void setWsdl(Resource wsdl) {
        this.wsdl = wsdl;
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

    public void process(MessageExchange exchange) throws Exception {
        Message message = messages.remove(exchange.getExchangeId());
        message.getInterceptorChain().resume();
        if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            exchange.setStatus(ExchangeStatus.DONE);
            message.getExchange().get(ComponentContext.class)
                    .getDeliveryChannel().send(exchange);
        }
    }

    @Override
    public void start() throws Exception {
        super.start();
        server.start();
    }

    @Override
    public void stop() throws Exception {
        server.stop();
        super.stop();
    }

    @Override
    public void validate() throws DeploymentException {
        try {
            if (definition == null) {
                if (wsdl == null) {
                    throw new DeploymentException("wsdl property must be set");
                }
                description = DomUtil.parse(wsdl.getInputStream());
                WSDLFactory wsdlFactory = WSDLFactory.newInstance();
                WSDLReader reader = wsdlFactory.newWSDLReader();
                reader.setFeature(Constants.FEATURE_VERBOSE, false);
                // definition = reader.readWSDL(wsdl.getURL().toString(),
                // description);
                try {
                    // use wsdl manager to parse wsdl or get cached definition
                    definition = getBus().getExtension(WSDLManager.class)
                            .getDefinition(wsdl.getURL());
                } catch (WSDLException ex) {
                    // throw new ServiceConstructionException(new
                    // Message("SERVICE_CREATION_MSG", LOG), ex);
                }
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

            EndpointInfo ei = cxfService.getServiceInfos().iterator().next()
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

            ei.getBinding().setProperty(
                    AbstractBindingFactory.DATABINDING_DISABLED, Boolean.TRUE);

            cxfService.getInInterceptors().add(new MustUnderstandInterceptor());
            cxfService.getInInterceptors().add(new AttachmentInInterceptor());
            cxfService.getInInterceptors().add(new StaxInInterceptor());
            cxfService.getInInterceptors().add(
                    new ReadHeadersInterceptor(getBus()));
            cxfService.getInInterceptors().add(
                    new JbiOperationInterceptor());
            cxfService.getInInterceptors().add(
                    new JbiInWsdl1Interceptor(isUseJBIWrapper()));
            cxfService.getInInterceptors().add(new JbiInInterceptor());
            cxfService.getInInterceptors().add(new JbiInvokerInterceptor());
            cxfService.getInInterceptors().add(new JbiPostInvokerInterceptor());

            cxfService.getInInterceptors().add(new OutgoingChainInterceptor());

            cxfService.getOutInterceptors().add(
                    new JbiOutWsdl1Interceptor(isUseJBIWrapper()));

            cxfService.getOutInterceptors().add(new SoapActionOutInterceptor());
            cxfService.getOutInterceptors().add(new AttachmentOutInterceptor());
            cxfService.getOutInterceptors().add(
                    new MtomCheckInterceptor(isMtomEnabled()));
            cxfService.getOutInterceptors().add(new StaxOutInterceptor());
            cxfService.getOutInterceptors().add(
                    new SoapPreProtocolOutInterceptor());
            cxfService.getOutInterceptors().add(
                    new SoapOutInterceptor(getBus()));
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

            ep.getOutInterceptors().add(new SoapActionOutInterceptor());
            ep.getOutInterceptors().add(new AttachmentOutInterceptor());
            ep.getOutInterceptors().add(new StaxOutInterceptor());
            ep.getOutInterceptors().add(new SoapOutInterceptor(getBus()));

            cxfService.getInInterceptors().addAll(getBus().getInInterceptors());
            cxfService.getInFaultInterceptors().addAll(
                    getBus().getInFaultInterceptors());
            cxfService.getOutInterceptors().addAll(
                    getBus().getOutInterceptors());
            cxfService.getOutFaultInterceptors().addAll(
                    getBus().getOutFaultInterceptors());

            chain = new JbiChainInitiationObserver(ep, getBus());
            server = new ServerImpl(getBus(), ep, null, chain);

            super.validate();
        } catch (DeploymentException e) {
            throw e;
        } catch (Exception e) {
            throw new DeploymentException(e);
        }
    }

    protected Bus getBus() {
        if (getBusCfg() != null) {
            if (bus == null) {
                SpringBusFactory bf = new SpringBusFactory();
                bus = bf.createBus(getBusCfg());
            }
            return bus;
        } else {
            return ((CxfBcComponent) getServiceUnit().getComponent()).getBus();
        }
    }

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
            CxfBcConsumer.this.messages.put(exchange.getExchangeId(), message);
            CxfBcConsumer.this.isOneway = message.getExchange().get(
                    BindingOperationInfo.class).getOperationInfo().isOneWay();
            message.getExchange().setOneWay(CxfBcConsumer.this.isOneway);

            try {
                if (CxfBcConsumer.this.synchronous
                        && !CxfBcConsumer.this.isOneway) {
                    message.getInterceptorChain().pause();
                    context.getDeliveryChannel().sendSync(exchange,
                            timeout * 1000);
                    process(exchange);
                } else {
                    context.getDeliveryChannel().send(exchange);

                }
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
                        f = new JBIFault(
                                new org.apache.cxf.common.i18n.Message(
                                        "Fault occured", (ResourceBundle) null));
                        Element details = toElement(exchange.getFault()
                                .getContent());
                        f.setDetail(details);
                        
                    } else {
                        Element details = toElement(exchange.getFault()
                                .getContent());
                        
                        
                        details = (Element) details.getElementsByTagNameNS(
                                details.getNamespaceURI(), "Body").item(0);
                        assert details != null;
                        details = (Element) details.getElementsByTagNameNS(
                                details.getNamespaceURI(), "Fault").item(0);
                        assert details != null;
                        details = (Element) details.getElementsByTagName("detail").item(0);
                        assert details != null;
                        f = new SoapFault(
                                new org.apache.cxf.common.i18n.Message(
                                        "Fault occured", (ResourceBundle) null),
                                new QName(details.getNamespaceURI(), "detail"));
                        f.setDetail(details);

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

        // this method is used for ws-policy to set BindingFaultInfo
        protected void processFaultDetail(Fault fault, Message msg) {
            Element exDetail = (Element) DOMUtils.getChild(fault.getDetail(),
                    Node.ELEMENT_NODE);
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

    public void setBusCfg(String busCfg) {
        this.busCfg = busCfg;
    }

    public String getBusCfg() {
        return busCfg;
    }

    public void setMtomEnabled(boolean mtomEnabled) {
        this.mtomEnabled = mtomEnabled;
    }

    public boolean isMtomEnabled() {
        return mtomEnabled;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setUseJBIWrapper(boolean useJBIWrapper) {
        this.useJBIWrapper = useJBIWrapper;
    }

    public boolean isUseJBIWrapper() {
        return useJBIWrapper;
    }

}
