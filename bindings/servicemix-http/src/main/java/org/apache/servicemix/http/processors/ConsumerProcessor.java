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

import org.apache.servicemix.common.EndpointComponentContext;
import org.apache.servicemix.common.JbiConstants;
import org.apache.servicemix.http.*;
import org.apache.servicemix.http.exception.HttpTimeoutException;
import org.apache.servicemix.http.exception.LateResponseException;
import org.apache.servicemix.http.jetty.JaasJettyPrincipal;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.soap.Context;
import org.apache.servicemix.soap.SoapExchangeProcessor;
import org.apache.servicemix.soap.SoapFault;
import org.apache.servicemix.soap.SoapHelper;
import org.apache.servicemix.soap.marshalers.JBIMarshaler;
import org.apache.servicemix.soap.marshalers.SoapMessage;
import org.apache.servicemix.soap.marshalers.SoapWriter;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.http.HttpMethods;
import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.*;
import javax.security.auth.Subject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.net.URI;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConsumerProcessor extends AbstractProcessor implements SoapExchangeProcessor, HttpProcessor {
    private static final String HTTP_METHOD  = "HTTP_METHOD";
    private static final String EXCHANGE = MessageExchange.class.getName();
    private static final String MUTEX = MessageExchange.class.getName() + "Mutex";
    private static final String EXCHANGEID = MessageExchange.class.getName() + "Mutex";
    private final Logger logger = LoggerFactory.getLogger(ConsumerProcessor.class);

    protected Object httpContext;
    protected ComponentContext context;
    protected DeliveryChannel channel;
    protected SoapHelper soapHelper;
    protected Map<String, Continuation> continuations = new ConcurrentHashMap<String, Continuation>();
    protected Map<String, Object> mutexes = new ConcurrentHashMap<String, Object>();
    private Map<String, MessageExchange> sentExchanges = new ConcurrentHashMap<String, MessageExchange>();
    protected int suspentionTime = 60000;
    protected boolean started = false;
    protected boolean supportAllHttpMethods;
    protected LateResponseStrategy lateResponseStrategy = LateResponseStrategy.error;

        
    public ConsumerProcessor(HttpEndpoint endpoint) {
        super(endpoint);
        this.soapHelper = new SoapHelper(endpoint);
        this.suspentionTime = endpoint.getTimeout();
        if (suspentionTime <= 0) {
            this.suspentionTime = getConfiguration().getConsumerProcessorSuspendTime();
        }
        this.supportAllHttpMethods = endpoint.isSupportAllHttpMethods();
    }
    
    public SslParameters getSsl() {
        return this.endpoint.getSsl();
    }
    
    public String getAuthMethod() {
        return this.endpoint.getAuthMethod();
    }
    
    public void process(MessageExchange exchange) throws Exception {
        final String id = exchange.getExchangeId();

        // Synchronize on the mutex object while we're tinkering with the continuation object,
        // this is still jetty, so do not trust jetty locks anymore
        final Continuation continuation = continuations.get(id);
        if (continuation != null){
            final Object mutex = continuation.getAttribute(MUTEX);
            if (mutex == null) {
                handleLateResponse(exchange);
                return;
            }
            synchronized (mutex) {
                if (!continuation.isExpired() && !continuation.isResumed()) {
                    logger.debug("Resuming continuation for exchange: {}", id);

                    // in case of the JMS/JCA flow, you might have a different instance of the message exchange here
                    continuation.setAttribute(EXCHANGE, exchange);

                    continuation.resume();


                    // if the continuation could no longer be resumed, the HTTP request might have timed out before the message
                    // exchange got handled by the ESB
                    if (!continuation.isResumed()) {
                        handleLateResponse(exchange);
                    }
                } else {
                    // it the continuation is no longer available or no longer pending, the HTTP request has time out before
                    // the message exchange got handled by the ESB
                    handleLateResponse(exchange);
                }
            }
        } else {
            handleLateResponse(exchange);
        }
    }

    public void init() throws Exception {
        String url = endpoint.getLocationURI();
        context = new EndpointComponentContext(endpoint);
        channel = context.getDeliveryChannel();
        httpContext = getServerManager().createContext(url, this);
    }

    public void shutdown() throws Exception {
        if (httpContext instanceof Server.Graceful){
            ((Server.Graceful)httpContext).setShutdown(true);
        }
        getServerManager().remove(httpContext);
    }

    public void start() throws Exception {
        started = true;
    }

    public void stop() throws Exception {
        started = false;
    }

    public void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        MessageExchange exchange;
        Continuation continuation = null;
        Object mutex = null;
        String id = null;

        try {
            // Handle WSDLs, XSDs
            if (handleStaticResource(request, response)) {
                return;
            }
            continuation = ContinuationSupport.getContinuation(request);
            mutex = getOrCreateMutex(continuation);

            boolean sendExchange = false;
            synchronized (mutex) {
                exchange = (MessageExchange) continuation.getAttribute(EXCHANGE);
                id = (String)continuation.getAttribute(EXCHANGEID);

                if (exchange == null) {
                    // Synchronize on the mutex object while we're (s)tinkering with the continuation object
                    // if this is  a timeout
                    if (continuation.isExpired()) {
                        throw new HttpTimeoutException(id);
                    }

                    logger.debug("Receiving HTTP request: {}", request);

                    // send back HTTP status 503 (Not Available) to reject any new requests if the endpoint is not started
                    if (!started) {
                        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Endpoint is stopped");
                        return;
                    }

                    continuation.setTimeout(suspentionTime);

                    // Create the exchange
                    exchange = createExchange(request);
                    id = exchange.getExchangeId();

                    continuation.setAttribute(MUTEX, mutex);
                    continuation.setAttribute(EXCHANGEID, id);
                    mutexes.put(id, mutex);
                    continuations.put(id, continuation);
                    sentExchanges.put(id, exchange);

                    logger.debug("Suspending continuation for exchange: {}", id);
                    continuation.suspend(response);
                    sendExchange = true;
                }
            }

            if (sendExchange) {
                send(exchange);
                return;
            }


            // message exchange has been completed, so we're ready to send back an HTTP response now
            logger.debug("Resuming HTTP request: {}", request);
            doClean(mutex, continuation, id);
            handleResponse(exchange, request, response);
        } catch (Exception e) {
            doClean(mutex, continuation, id);
            sendFault(e instanceof  SoapFault ? (SoapFault)e : new SoapFault(e), request, response);
        }
    }

    private void send(MessageExchange exchange) throws MessagingException {
        channel.send(exchange);
    }

    protected MessageExchange createExchange(HttpServletRequest request) throws Exception {
        Context ctx = createContext(request);
        request.setAttribute(Context.class.getName(), ctx);
        MessageExchange exchange = soapHelper.onReceive(ctx);

        NormalizedMessage inMessage = exchange.getMessage("in");
        if (getConfiguration().isWantHeadersFromHttpIntoExchange()) {
            Map<String, String> requestMessageHeaders = getHeaders(request);
            if (supportAllHttpMethods){
                requestMessageHeaders.put(HTTP_METHOD, request.getMethod());
            }
            inMessage.setProperty(JbiConstants.PROTOCOL_HEADERS, requestMessageHeaders);
        }
        return exchange;
    }


    /*
     * Get or create an object that can be used for synchronizing code blocks for a given exchange
     */
    private Object getOrCreateMutex(Continuation continuation) {
        Object result = null;

        // let's try to find the object that corresponds to the exchange first
        if (continuation != null) {
            result = continuation.getAttribute(MUTEX);
        }

        // no luck finding an existing object, let's create a new one
        if (result == null) {
            result = new Object();
        }

        return result;
    }

    private MessageExchange doClean(Object mutex, Continuation continuation, String exchangeId) {
        if (mutex != null) {
            synchronized (mutex) {
                if (exchangeId == null && continuation != null) {
                    exchangeId = (String) continuation.getAttribute(EXCHANGEID);
                }
                if (exchangeId != null && continuation == null) {
                    continuation = continuations.remove(exchangeId);
                }
                if (continuation != null) {
                    continuation.removeAttribute(EXCHANGEID);
                    continuation.removeAttribute(EXCHANGE);
                    continuation.removeAttribute(MUTEX);
                }
                if (exchangeId != null) {
                    mutexes.remove(exchangeId);
                    continuations.remove(exchangeId);
                    return sentExchanges.remove(exchangeId);
                }
            }
        }
        return null;
    }

    /*
     * Handle the HTTP response based on the information in the message exchange we received
     */
    private void handleResponse(MessageExchange exchange, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.ERROR) {
            Exception e = exchange.getError();
            if (e == null) {
                e = new Exception("Unknown error (exchange aborted ?)");
            }
            throw e;
        } else if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            try {
                Fault fault = exchange.getFault();
                if (fault != null) {
                    processFault(exchange, request, response);
                } else {
                    processResponse(exchange, request, response);
                }
                done(exchange);
            } catch (Exception e) {
                fail(exchange, e);
                throw e;
            }
        } else if (exchange.getStatus() == ExchangeStatus.DONE) {
            // This happens when there is no response to send back
            response.setStatus(HttpServletResponse.SC_ACCEPTED);
        }
    }

    private void done(MessageExchange exchange) throws MessagingException {
        exchange.setStatus(ExchangeStatus.DONE);
        channel.send(exchange);
    }

    private void fail(MessageExchange exchange, Exception e) throws MessagingException {
        exchange.setError(e);
        channel.send(exchange);
    }

    protected boolean handleStaticResource(HttpServletRequest request, HttpServletResponse response) throws Exception {
        logger.debug("Receiving HTTP request: {}", request);
        String httpMethod = request.getMethod();
        if (HttpMethods.GET.equals(httpMethod)) {
            processGetRequest(request, response);
            return true;
        }
        if (!HttpMethods.POST.equals(httpMethod) && !supportAllHttpMethods) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, httpMethod + " not supported");
            return true;
        }
        return false;
    }

    private Context createContext(HttpServletRequest request) throws Exception {
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
        return ctx;
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
        if (path.charAt(0) == '/') {
            path = path.substring(1);
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
        if (node == null && path.endsWith("main.wsdl")) {
            node = (Node) endpoint.getWsdls().get("main.wsdl");
        }
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


        /*
     * Handle a message exchange that is being received after the corresponding HTTP request has timed out
     */
    protected void handleLateResponse(MessageExchange exchange) throws Exception {
        // if the exchange is no longer active by now, something else probably went wrong in the meanwhile
        if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            if (lateResponseStrategy == LateResponseStrategy.error) {
                // ends the exchange in ERROR
                fail(exchange, new LateResponseException(exchange));
            } else {
                // let's log the exception message text, but end the exchange with DONE
                logger.warn(LateResponseException.createMessage(exchange));
                done(exchange);
            }
        }
    }

    /**
     * Set the strategy to be used for handling a late response from the ESB (i.e. a response that arrives after the HTTP request has timed out).
     * Defaults to <code>error</code>
     * <p/>
     * <ul>
     * <li><code>error</code> will terminate the exchange with an ERROR status and log an exception for the late response</li>
     * <li><code>warning</code> will end the exchange with a DONE status and log a warning for the late response instead</li>
     * </ul>
     *
     * @param value
     */
    public void setLateResponseStrategy(String value) {
        this.lateResponseStrategy = LateResponseStrategy.valueOf(value);
    }

    public String getLateResponseStrategy() {
        return lateResponseStrategy.name();
    }

    public boolean isSupportAllHttpMethods() {
        return supportAllHttpMethods;
    }

    public void setSupportAllHttpMethods(boolean supportAllHttpMethods) {
        this.supportAllHttpMethods = supportAllHttpMethods;
    }
}
