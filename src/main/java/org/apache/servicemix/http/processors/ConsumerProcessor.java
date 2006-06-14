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
package org.apache.servicemix.http.processors;

import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.wsdl.Definition;
import javax.wsdl.factory.WSDLFactory;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.http.HttpEndpoint;
import org.apache.servicemix.http.HttpLifeCycle;
import org.apache.servicemix.http.HttpProcessor;
import org.apache.servicemix.http.ServerManager;
import org.apache.servicemix.http.SslParameters;
import org.apache.servicemix.http.jetty.JaasJettyPrincipal;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.soap.Context;
import org.apache.servicemix.soap.SoapFault;
import org.apache.servicemix.soap.SoapHelper;
import org.apache.servicemix.soap.marshalers.JBIMarshaler;
import org.apache.servicemix.soap.marshalers.SoapMessage;
import org.apache.servicemix.soap.marshalers.SoapWriter;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;
import org.w3c.dom.Element;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

public class ConsumerProcessor implements ExchangeProcessor, HttpProcessor {

    public static final URI IN_ONLY = URI.create("http://www.w3.org/2004/08/wsdl/in-only");
    public static final URI IN_OUT = URI.create("http://www.w3.org/2004/08/wsdl/in-out");
    public static final URI ROBUST_IN_ONLY = URI.create("http://www.w3.org/2004/08/wsdl/robust-in-only");
    
    protected HttpEndpoint endpoint;
    protected ContextHandler httpContext;
    protected ComponentContext context;
    protected DeliveryChannel channel;
    protected SoapHelper soapHelper;
    protected Map locks;
    protected Map exchanges;
        
    public ConsumerProcessor(HttpEndpoint endpoint) {
        this.endpoint = endpoint;
        this.soapHelper = new SoapHelper(endpoint);
        this.locks = new ConcurrentHashMap();
        this.exchanges = new ConcurrentHashMap();
    }
    
    public SslParameters getSsl() {
        return this.endpoint.getSsl();
    }
    
    public String getAuthMethod() {
        return this.endpoint.getAuthMethod();
    }
    
    public void process(MessageExchange exchange) throws Exception {
        Continuation cont = (Continuation) locks.remove(exchange.getExchangeId());
        if (cont != null) {
            exchanges.put(exchange.getExchangeId(), exchange);
            cont.resume();
        } else {
            throw new IllegalStateException("Exchange not found");
        }
    }

    public void start() throws Exception {
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        String url = endpoint.getLocationURI();
        httpContext = getServerManager().createContext(url, this);
        httpContext.start();
        context = endpoint.getServiceUnit().getComponent().getComponentContext();
        channel = context.getDeliveryChannel();
    }

    public void stop() throws Exception {
        httpContext.stop();
        getServerManager().remove(httpContext);
    }

    public void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if ("GET".equals(request.getMethod())) {
            String query = request.getQueryString();
            if (query != null && query.trim().equalsIgnoreCase("wsdl")) {
                String uri = request.getRequestURI();
                if (!uri.endsWith("/")) {
                    uri += "/";
                }
                uri += "main.wsdl";
                response.sendRedirect(uri);
                return;
            }
            String path = request.getPathInfo();
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (path.endsWith(".wsdl")) {
                Definition def = (Definition) endpoint.getWsdls().get(path);
                generateWSDL(response, def);
                return;
            } else if (path.endsWith(".xsd")) {
                Element el = (Element) endpoint.getWsdls().get(path);
                generateXSD(response, el);
                return;
            }
        }
        if (!"POST".equals(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, request.getMethod() + " not supported");
            return;
        }
        // Not giving a specific mutex will synchronize on the contination itself
        Continuation cont = ContinuationSupport.getContinuation(request, null);
        MessageExchange exchange;
        // If the continuation is not a retry
        if (!cont.isPending()) {
            try {
                SoapMessage message = soapHelper.getSoapMarshaler().createReader().read(
                                            request.getInputStream(), 
                                            request.getHeader("Content-Type"));
                Context context = soapHelper.createContext(message);
                if (request.getUserPrincipal() != null) {
                    if (request.getUserPrincipal() instanceof JaasJettyPrincipal) {
                        Subject subject = ((JaasJettyPrincipal) request.getUserPrincipal()).getSubject();
                        context.getInMessage().setSubject(subject);
                    } else {
                        context.getInMessage().addPrincipal(request.getUserPrincipal());
                    }
                }
                request.setAttribute(Context.class.getName(), context);
                exchange = soapHelper.onReceive(context);
                NormalizedMessage inMessage = exchange.getMessage("in");
                inMessage.setProperty(JbiConstants.PROTOCOL_HEADERS, getHeaders(request));
                locks.put(exchange.getExchangeId(), cont);
                request.setAttribute(MessageExchange.class.getName(), exchange.getExchangeId());
                ((BaseLifeCycle) endpoint.getServiceUnit().getComponent().getLifeCycle()).sendConsumerExchange(exchange, endpoint);
                // TODO: make this timeout configurable
                boolean result = cont.suspend(1000 * 60); // 60 s
                if (!result) {
                    throw new Exception("Error sending exchange: aborted");
                }
            } catch (SoapFault fault) {
                sendFault(fault, request, response);
                return;
            }
        } else {
            String id = (String) request.getAttribute(MessageExchange.class.getName());
            exchange = (MessageExchange) exchanges.remove(id);
            request.removeAttribute(MessageExchange.class.getName());
            boolean result = cont.suspend(0); 
            // Check if this is a timeout
            if (exchange == null) {
                throw new IllegalStateException("Exchange not found");
            }
            if (!result) {
                throw new Exception("Timeout");
            }
        }
        if (exchange.getStatus() == ExchangeStatus.ERROR) {
            if (exchange.getError() != null) {
                throw new Exception(exchange.getError());
            } else {
                throw new Exception("Unknown Error");
            }
        } else if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            try {
                if (exchange.getFault() != null) {
                    SoapFault fault = new SoapFault(
                                    (QName) exchange.getFault().getProperty(JBIMarshaler.SOAP_FAULT_CODE), 
                                    (QName) exchange.getFault().getProperty(JBIMarshaler.SOAP_FAULT_SUBCODE), 
                                    (String) exchange.getFault().getProperty(JBIMarshaler.SOAP_FAULT_REASON), 
                                    (URI) exchange.getFault().getProperty(JBIMarshaler.SOAP_FAULT_NODE), 
                                    (URI) exchange.getFault().getProperty(JBIMarshaler.SOAP_FAULT_ROLE), 
                                    exchange.getFault().getContent());
                    sendFault(fault, request, response);
                } else {
                    NormalizedMessage outMsg = exchange.getMessage("out");
                    if (outMsg != null) {
                        Context context = (Context) request.getAttribute(Context.class.getName());
                        SoapMessage out = soapHelper.onReply(context, outMsg);
                        SoapWriter writer = soapHelper.getSoapMarshaler().createWriter(out);
                        response.setContentType(writer.getContentType());
                        writer.write(response.getOutputStream());
                    }
                }
            } finally {
                exchange.setStatus(ExchangeStatus.DONE);
                channel.send(exchange);
            }
        }
    }
    
    protected void sendFault(SoapFault fault, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (SoapFault.SENDER.equals(fault.getCode())) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        Context context = (Context) request.getAttribute(Context.class.getName());
        SoapMessage soapFault = soapHelper.onFault(context, fault);
        SoapWriter writer = soapHelper.getSoapMarshaler().createWriter(soapFault);
        response.setContentType(writer.getContentType());
        writer.write(response.getOutputStream());
    }
    
    protected Map getHeaders(HttpServletRequest request) {
        Map headers = new HashMap();
        Enumeration enumeration = request.getHeaderNames();
        while (enumeration.hasMoreElements()) {
            String name = (String) enumeration.nextElement();
            String value = request.getHeader(name);
            headers.put(name, value);
        }
        return headers;
    }
    
    protected ServerManager getServerManager() {
        HttpLifeCycle lf =  (HttpLifeCycle) endpoint.getServiceUnit().getComponent().getLifeCycle();
        return lf.getServer();
    }
    
    protected void generateWSDL(HttpServletResponse response, Definition def) throws Exception {
        if (def == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "No wsdl is available for this service");
            return;
        }
        response.setStatus(200);
        response.setContentType("text/xml");
        WSDLFactory.newInstance().newWSDLWriter().writeWSDL(def, response.getOutputStream());
    }
    
    protected void generateXSD(HttpServletResponse response, Element element) throws Exception {
        if (element == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to find requested resource");
            return;
        }
        response.setStatus(200);
        response.setContentType("text/xml");
        new SourceTransformer().toResult(new DOMSource(element), new StreamResult(response.getOutputStream()));
    }

}
