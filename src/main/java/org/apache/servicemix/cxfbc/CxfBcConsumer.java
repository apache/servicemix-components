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

import com.ibm.wsdl.Constants;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

import javax.jbi.component.ComponentContext;
import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import org.w3c.dom.Element;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.AbstractBindingFactory;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.endpoint.ServerImpl;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.ChainInitiationObserver;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.apache.servicemix.common.endpoints.ConsumerEndpoint;
import org.apache.servicemix.cxfbc.interceptors.JbiInInterceptor;
import org.apache.servicemix.cxfbc.interceptors.JbiInWsdl1Interceptor;
import org.apache.servicemix.cxfbc.interceptors.JbiOperationInterceptor;
import org.apache.servicemix.cxfbc.interceptors.JbiOutWsdl1Interceptor;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.soap.util.DomUtil;
import org.springframework.core.io.Resource;

/**
 * 
 * @author gnodet
 * @org.apache.xbean.XBean element="consumer"
 */
public class CxfBcConsumer extends ConsumerEndpoint implements CxfBcEndpointType {

    private Resource wsdl;
    private Endpoint ep;
    private ChainInitiationObserver chain;
    private Server server;
    private Map<String, Message> messages = new ConcurrentHashMap<String, Message>();
    private boolean synchronous = true;
    
    /**
     * @return the wsdl
     */
    public Resource getWsdl() {
        return wsdl;
    }

    /**
     * @param wsdl the wsdl to set
     */
    public void setWsdl(Resource wsdl) {
        this.wsdl = wsdl;
    }

    @Override
    public String getLocationURI() {
        // TODO Auto-generated method stub
        return null;
    }

    public void process(MessageExchange exchange) throws Exception {
        Message message = messages.remove(exchange.getExchangeId());
        message.getInterceptorChain().resume();
        if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            exchange.setStatus(ExchangeStatus.DONE);
            message.getExchange().get(ComponentContext.class).getDeliveryChannel().send(exchange);
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
                definition = reader.readWSDL(wsdl.getURL().toString(), description);
            }
            if (service == null) {
                service = (QName) definition.getServices().keySet().iterator().next(); 
            }
            WSDLServiceFactory factory = new WSDLServiceFactory(getBus(), definition, service);
            Service service = factory.create();
            EndpointInfo ei = service.getServiceInfos().iterator().next().getEndpoints().iterator().next();
            if (endpoint == null) {
                endpoint = ei.getName().getLocalPart();
            }
            ei.getBinding().setProperty(AbstractBindingFactory.DATABINDING_DISABLED, Boolean.TRUE);
            service.getInInterceptors().add(new JbiOperationInterceptor());
            service.getInInterceptors().add(new JbiInWsdl1Interceptor());
            service.getInInterceptors().add(new JbiInInterceptor());
            service.getInInterceptors().add(new JbiInvokerInterceptor());
            service.getInInterceptors().add(new JbiPostInvokerInterceptor());
            service.getInInterceptors().add(new OutgoingChainInterceptor());
            service.getOutInterceptors().add(new JbiOutWsdl1Interceptor());
            ep = new EndpointImpl(getBus(), service, ei);
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
        return ((CxfBcComponent) getServiceUnit().getComponent()).getBus(); 
    }
    
    protected class JbiChainInitiationObserver extends ChainInitiationObserver {

        public JbiChainInitiationObserver(Endpoint endpoint, Bus bus) {
            super(endpoint, bus);
        }
        
        protected void setExchangeProperties(Exchange exchange, Message m) {
            super.setExchangeProperties(exchange, m);
            exchange.put(ComponentContext.class, CxfBcConsumer.this.getContext());
            exchange.put(CxfBcConsumer.class, CxfBcConsumer.this);
        }

    }
    
    protected class JbiInvokerInterceptor extends AbstractPhaseInterceptor<Message> {

        public JbiInvokerInterceptor() {
            super(Phase.INVOKE);
        }

        public void handleMessage(final Message message) throws Fault {
            MessageExchange exchange = message.getContent(MessageExchange.class);
            ComponentContext context = message.getExchange().get(ComponentContext.class);
            CxfBcConsumer.this.configureExchangeTarget(exchange);
            CxfBcConsumer.this.messages.put(exchange.getExchangeId(), message);
            message.getInterceptorChain().pause();
            try {
                if (CxfBcConsumer.this.synchronous) {
                    context.getDeliveryChannel().sendSync(exchange);
                    process(exchange);
                } else {
                    context.getDeliveryChannel().send(exchange);
                }
            } catch (Exception e) {
                throw new Fault(e);
            }
        }

    }
    
    protected static class JbiPostInvokerInterceptor extends AbstractPhaseInterceptor<Message> {
        public JbiPostInvokerInterceptor() {
            super(Phase.POST_INVOKE);
            addBefore(OutgoingChainInterceptor.class.getName());
        }

        public void handleMessage(final Message message) throws Fault {
            MessageExchange exchange = message.getContent(MessageExchange.class);
            Exchange ex = message.getExchange();
            if (exchange.getStatus() == ExchangeStatus.ERROR) {
                throw new Fault(exchange.getError());
            }
            if (!ex.isOneWay()) {
                if (exchange.getFault() != null) {
                    Fault f = new Fault(new org.apache.cxf.common.i18n.Message("Fault occured", (ResourceBundle) null));
                    Element details = toElement(exchange.getFault().getContent());
                    Element d = f.getOrCreateDetail();
                    d.appendChild(d.getOwnerDocument().importNode(details, true));
                    throw f;
                } else if (exchange.getMessage("out") != null) {
                    Endpoint ep = ex.get(Endpoint.class);
                    Message outMessage = ex.getOutMessage();
                    if (outMessage == null) {
                        outMessage = ep.getBinding().createMessage();
                        ex.setOutMessage(outMessage);
                    }
                    outMessage.setContent(Source.class, exchange.getMessage("out").getContent());
                }
            }
        }
    }
    
    private static Element toElement(Source src) throws Fault {
        try {
            return new SourceTransformer().toDOMElement(src);
        } catch (Exception e) {
            throw new Fault(e);
        }
    }
}
