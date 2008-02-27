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

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import com.ibm.wsdl.Constants;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.AbstractBindingFactory;
import org.apache.cxf.binding.soap.SoapMessage;

import org.apache.cxf.binding.soap.interceptor.SoapOutInterceptor;
import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointImpl;
import org.apache.cxf.interceptor.AttachmentOutInterceptor;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.StaxOutInterceptor;


import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.phase.PhaseChainCache;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.phase.PhaseManager;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.jbi.JBIMessageHelper;
import org.apache.cxf.wsdl11.WSDLServiceFactory;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.cxfbc.interceptors.JbiOutInterceptor;
import org.apache.servicemix.cxfbc.interceptors.JbiOutWsdl1Interceptor;
import org.apache.servicemix.cxfbc.interceptors.MtomCheckInterceptor;
import org.apache.servicemix.soap.util.DomUtil;
import org.springframework.core.io.Resource;



/**
 * 
 * @author gnodet
 * @org.apache.xbean.XBean element="provider"
 */
public class CxfBcProvider extends ProviderEndpoint implements
        CxfBcEndpointWithInterceptor {

    private static final Logger LOG = LogUtils.getL7dLogger(org.apache.servicemix.cxfbc.CxfBcProvider.class);
    
    
    List<Interceptor> in = new CopyOnWriteArrayList<Interceptor>();

    List<Interceptor> out = new CopyOnWriteArrayList<Interceptor>();

    List<Interceptor> outFault = new CopyOnWriteArrayList<Interceptor>();

    List<Interceptor> inFault = new CopyOnWriteArrayList<Interceptor>();
    
    private Resource wsdl;
    
    private String busCfg;
    
    private Bus bus;
    
    private URI locationURI;
    
    private EndpointInfo ei;
    
    private Endpoint ep;

    private Conduit conduit;
    
    private Service cxfService;
    
    private boolean mtomEnabled;
    
    public void processExchange(MessageExchange exchange) {
        
    }

    public void process(MessageExchange exchange) throws Exception {
        NormalizedMessage nm = exchange.getMessage("in");
        
               
        CxfBcProviderMessageObserver obs = new CxfBcProviderMessageObserver(exchange, this);
        conduit.setMessageObserver(obs);
        SoapMessage message = new SoapMessage(new MessageImpl());
        message.put(MessageExchange.class, exchange);
        Exchange cxfExchange = new ExchangeImpl();
        message.setExchange(cxfExchange);
        cxfExchange.setOutMessage(message);       
        
        QName opeName = exchange.getOperation();
        BindingOperationInfo boi = null;
        if (opeName == null) {
            // if interface only have one operation, may not specify the opeName in MessageExchange
            boi = ei.getBinding().getOperations().iterator().next();
        } else {
            boi = ei.getBinding().getOperation(exchange.getOperation());   
        }
         
        cxfExchange.put(BindingOperationInfo.class, boi);
        cxfExchange.put(Endpoint.class, ep);
        cxfExchange.put(Service.class, cxfService);
        PhaseChainCache outboundChainCache = new PhaseChainCache();
        PhaseManager pm = getBus().getExtension(PhaseManager.class);
        List<Interceptor> outList = new ArrayList<Interceptor>();
        if (isMtomEnabled()) {
            outList.add(new JbiOutInterceptor());
            outList.add(new MtomCheckInterceptor(true));
            outList.add(new AttachmentOutInterceptor());
            
        }
        
        
        outList.add(new JbiOutWsdl1Interceptor());
        outList.add(new SoapPreProtocolOutInterceptor());
        outList.add(new SoapOutInterceptor(getBus()));
        PhaseInterceptorChain outChain = outboundChainCache.get(pm.getOutPhases(), outList);
        outChain.add(getOutInterceptors());
        outChain.add(getOutFaultInterceptors());
        message.setInterceptorChain(outChain);
        InputStream is = JBIMessageHelper.convertMessageToInputStream(nm.getContent());
        
        StreamSource source = new StreamSource(is);
        message.setContent(Source.class, source);
        
        message.setContent(InputStream.class, is);
        
        conduit.prepare(message);
        OutputStream os = message.getContent(OutputStream.class);
        XMLStreamWriter writer = message.getContent(XMLStreamWriter.class);
        

        String encoding = getEncoding(message);
        
        try {
            writer = StaxOutInterceptor.getXMLOutputFactory(message).createXMLStreamWriter(os, encoding);
        } catch (XMLStreamException e) {
            //
        }
        message.setContent(XMLStreamWriter.class, writer);
        message.put(org.apache.cxf.message.Message.REQUESTOR_ROLE, true);
        outChain.doIntercept(message);
        XMLStreamWriter xtw = message.getContent(XMLStreamWriter.class);
        if (xtw != null) {
            xtw.writeEndDocument();
            xtw.close();
        }

        os.flush();
        is.close();
        os.close();

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

    public void setWsdl(Resource wsdl) {
        this.wsdl = wsdl;
    }

    public Resource getWsdl() {
        return wsdl;
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
                definition = reader.readWSDL(wsdl.getURL().toString(),
                        description);
                WSDLServiceFactory factory = new WSDLServiceFactory(getBus(),
                        definition, service);
                cxfService = factory.create();
                ei = cxfService.getServiceInfos().iterator().next()
                        .getEndpoints().iterator().next();
                for (ServiceInfo serviceInfo : cxfService.getServiceInfos()) {
                    if (serviceInfo.getName().equals(service)
                        && getEndpoint() != null 
                        && serviceInfo.getEndpoint(new QName(
                                serviceInfo.getName().getNamespaceURI(), getEndpoint())) != null) {
                        ei = serviceInfo.getEndpoint(new QName(
                                    serviceInfo.getName().getNamespaceURI(), getEndpoint()));
                 
                    }
                }

                if (endpoint == null) {
                    endpoint = ei.getName().getLocalPart();
                }
                ei.getBinding().setProperty(
                        AbstractBindingFactory.DATABINDING_DISABLED, Boolean.TRUE);
                
                if (locationURI == null) {
                    // if not specify target address, get it from the wsdl
                    locationURI = new URI(ei.getAddress());
                    LOG.fine("address is " + locationURI.toString());
                }
                ep = new EndpointImpl(getBus(), cxfService, ei);
                
                //init transport
                ei.setAddress(locationURI.toString());
                
                ConduitInitiatorManager conduitMgr = getBus().getExtension(ConduitInitiatorManager.class);
                ConduitInitiator conduitInit = conduitMgr.getConduitInitiator("http://schemas.xmlsoap.org/soap/http");
                conduit = conduitInit.getConduit(ei);

                

                super.validate();
            }
        } catch (DeploymentException e) {
            throw e;
        } catch (Exception e) {
            throw new DeploymentException(e);
        }
    }

    @Override
    public void start() throws Exception {
        super.start();
        
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
    
    public void setBusCfg(String busCfg) {
        this.busCfg = busCfg;
    }

    public String getBusCfg() {
        return busCfg;
    }

    public void setLocationURI(URI locationURI) {
        this.locationURI = locationURI;
    }

    public URI getLocationURI() {
        return locationURI;
    }
    
    private String getEncoding(Message message) {
        Exchange ex = message.getExchange();
        String encoding = (String)message.get(Message.ENCODING);
        if (encoding == null && ex.getInMessage() != null) {
            encoding = (String) ex.getInMessage().get(Message.ENCODING);
            message.put(Message.ENCODING, encoding);
        }
        
        if (encoding == null) {
            encoding = "UTF-8";
            message.put(Message.ENCODING, encoding);
        }
        return encoding;
    }
    
    Endpoint getCxfEndpoint() {
        return this.ep;
    }
    
    EndpointInfo getEndpointInfo() {
        return this.ei;
    }

    public void setMtomEnabled(boolean mtomEnabled) {
        this.mtomEnabled = mtomEnabled;
    }

    public boolean isMtomEnabled() {
        return mtomEnabled;
    }

}
