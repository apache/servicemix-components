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
package org.apache.servicemix.http.processors;

import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.common.EndpointComponentContext;
import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.http.ContextManager;
import org.apache.servicemix.http.HttpComponent;
import org.apache.servicemix.http.HttpEndpoint;
import org.apache.servicemix.http.HttpProcessor;
import org.apache.servicemix.http.SslParameters;
import org.apache.servicemix.http.jetty.JaasJettyPrincipal;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.soap.Context;
import org.apache.servicemix.soap.SoapFault;
import org.apache.servicemix.soap.SoapHelper;
import org.apache.servicemix.soap.marshalers.JBIMarshaler;
import org.apache.servicemix.soap.marshalers.SoapMessage;
import org.apache.servicemix.soap.marshalers.SoapWriter;
import org.mortbay.jetty.RetryRequest;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

public class ConsumerProcessor extends AbstractProcessor implements ExchangeProcessor, HttpProcessor {

    public static final URI IN_ONLY = URI.create("http://www.w3.org/2004/08/wsdl/in-only");
    public static final URI IN_OUT = URI.create("http://www.w3.org/2004/08/wsdl/in-out");
    public static final URI ROBUST_IN_ONLY = URI.create("http://www.w3.org/2004/08/wsdl/robust-in-only");
    
    private static Log log = LogFactory.getLog(ConsumerProcessor.class);

    protected Object httpContext;
    protected ComponentContext context;
    protected DeliveryChannel channel;
    protected SoapHelper soapHelper;
    protected Map<String, Continuation> locks;
    protected Map<String, MessageExchange> exchanges;
    protected int suspentionTime = 60000;
        
    public ConsumerProcessor(HttpEndpoint endpoint) {
        super(endpoint);
        this.soapHelper = new SoapHelper(endpoint);
        this.locks = new ConcurrentHashMap<String, Continuation>();
        this.exchanges = new ConcurrentHashMap<String, MessageExchange>();
        this.suspentionTime = getConfiguration().getConsumerProcessorSuspendTime();
    }
    
    public SslParameters getSsl() {
        return this.endpoint.getSsl();
    }
    
    public String getAuthMethod() {
        return this.endpoint.getAuthMethod();
    }
    
    public void process(MessageExchange exchange) throws Exception {
        Continuation cont = locks.remove(exchange.getExchangeId());
        if (cont != null) {
            synchronized (cont) {
                if (log.isDebugEnabled()) {
                    log.debug("Resuming continuation for exchange: " + exchange.getExchangeId());
                }
                exchanges.put(exchange.getExchangeId(), exchange);
                cont.resume();
            }
        } else {
            throw new IllegalStateException("Exchange not found");
        }
    }

    public void start() throws Exception {
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        String url = endpoint.getLocationURI();
        context = new EndpointComponentContext(endpoint);
        channel = context.getDeliveryChannel();
        httpContext = getServerManager().createContext(url, this);
    }

    public void stop() throws Exception {
        getServerManager().remove(httpContext);
    }

    public void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Receiving HTTP request: " + request);
        }
        if ("GET".equals(request.getMethod())) {
            processGetRequest(request, response);
            return;
        }
        if (!"POST".equals(request.getMethod())) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, request.getMethod() + " not supported");
            return;
        }
        // Not giving a specific mutex will synchronize on the contination itself
        Continuation cont = ContinuationSupport.getContinuation(request, null);
        MessageExchange exchange;
        // If the continuation is not a retry
        if (!cont.isPending() && cont.isNew()) {
            try {
                SoapMessage message = soapHelper.getSoapMarshaler().createReader().read(
                                            request.getInputStream(), 
                                            request.getHeader(HEADER_CONTENT_TYPE));
                Context ctx = soapHelper.createContext(message);
                if (request.getUserPrincipal() != null) {
                    if (request.getUserPrincipal() instanceof JaasJettyPrincipal) {
                        Subject subject = ((JaasJettyPrincipal) request.getUserPrincipal()).getSubject();
                        ctx.getInMessage().setSubject(subject);
                    } else {
                        ctx.getInMessage().addPrincipal(request.getUserPrincipal());
                    }
                }
                request.setAttribute(Context.class.getName(), ctx);
                exchange = soapHelper.onReceive(ctx);
                NormalizedMessage inMessage = exchange.getMessage("in");
                if (getConfiguration().isWantHeadersFromHttpIntoExchange()) {
                    inMessage.setProperty(JbiConstants.PROTOCOL_HEADERS, getHeaders(request));
                }
                locks.put(exchange.getExchangeId(), cont);
                request.setAttribute(MessageExchange.class.getName(), exchange.getExchangeId());
                synchronized (cont) {
                    channel.send(exchange);
                    if (log.isDebugEnabled()) {
                        log.debug("Suspending continuation for exchange: " + exchange.getExchangeId());
                    }
                    boolean result = cont.suspend(suspentionTime);
                    exchanges.remove(exchange.getExchangeId());
                    if (!result) {
                        throw new Exception("Error sending exchange: aborted");
                    }
                    request.removeAttribute(MessageExchange.class.getName());
                }
            } catch (RetryRequest retry) {
                throw retry;
            } catch (SoapFault fault) {
                sendFault(fault, request, response);
                return;
            } catch (Exception e) {
                SoapFault fault = new SoapFault(e); 
                sendFault(fault, request, response);
                return;
            }
        } else {
            String id = (String) request.getAttribute(MessageExchange.class.getName());
            exchange = exchanges.remove(id);
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
                    processFault(exchange, request, response);
                } else {
                    processResponse(exchange, request, response);
                }
            } finally {
                exchange.setStatus(ExchangeStatus.DONE);
                channel.send(exchange);
            }
        } else if (exchange.getStatus() == ExchangeStatus.DONE) {
            // This happens when there is no response to send back
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
        }
    }

    private void processResponse(MessageExchange exchange, HttpServletRequest request, HttpServletResponse response) throws Exception {
        NormalizedMessage outMsg = exchange.getMessage("out");
        if (outMsg != null) {
            Context ctx = (Context) request.getAttribute(Context.class.getName());
            SoapMessage out = soapHelper.onReply(ctx, outMsg);
            SoapWriter writer = soapHelper.getSoapMarshaler().createWriter(out);
            response.setContentType(writer.getContentType());
            writer.write(response.getOutputStream());
        }
    }

    private void processFault(MessageExchange exchange, HttpServletRequest request, HttpServletResponse response) throws Exception {
        SoapFault fault = new SoapFault(
                        (QName) exchange.getFault().getProperty(JBIMarshaler.SOAP_FAULT_CODE),
                        (QName) exchange.getFault().getProperty(JBIMarshaler.SOAP_FAULT_SUBCODE),
                        (String) exchange.getFault().getProperty(JBIMarshaler.SOAP_FAULT_REASON),
                        (URI) exchange.getFault().getProperty(JBIMarshaler.SOAP_FAULT_NODE),
                        (URI) exchange.getFault().getProperty(JBIMarshaler.SOAP_FAULT_ROLE),
                        exchange.getFault().getContent());
        sendFault(fault, request, response);
    }

    private void processGetRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
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
        if (path.lastIndexOf('/') >= 0) {
            path = path.substring(path.lastIndexOf('/') + 1);
        }

        // Set protocol, host, and port in the component
        HttpComponent comp = (HttpComponent) endpoint.getServiceUnit().getComponent();
        comp.setProtocol(request.getScheme());
        comp.setHost(request.getServerName());
        comp.setPort(request.getServerPort());
        comp.setPath(request.getContextPath());

        // Reload the wsdl
        endpoint.reloadWsdl();

        Node node = (Node) endpoint.getWsdls().get(path);
        generateDocument(response, node);
    }

    protected void sendFault(SoapFault fault, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (SoapFault.SENDER.equals(fault.getCode())) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        } else {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
        Context ctx = (Context) request.getAttribute(Context.class.getName());
        SoapMessage soapFault = soapHelper.onFault(ctx, fault);
        SoapWriter writer = soapHelper.getSoapMarshaler().createWriter(soapFault);
        response.setContentType(writer.getContentType());
        writer.write(response.getOutputStream());
    }
    
    protected Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<String, String>();
        Enumeration<?> enumeration = request.getHeaderNames();
        while (enumeration.hasMoreElements()) {
            String name = (String) enumeration.nextElement();
            String value = request.getHeader(name);
            headers.put(name, value);
        }
        return headers;
    }
    
    protected ContextManager getServerManager() {
        HttpComponent comp =  (HttpComponent) endpoint.getServiceUnit().getComponent();
        return comp.getServer();
    }
    
    protected void generateDocument(HttpServletResponse response, Node node) throws Exception {
        if (node == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Unable to find requested resource");
            return;
        }
        response.setStatus(200);
        response.setContentType("text/xml");
        new SourceTransformer().toResult(new DOMSource(node), new StreamResult(response.getOutputStream()));
    }

}
