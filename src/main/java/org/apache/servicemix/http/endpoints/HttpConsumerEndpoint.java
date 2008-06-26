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
package org.apache.servicemix.http.endpoints;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jbi.management.DeploymentException;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.Fault;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.security.auth.Subject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.endpoints.ConsumerEndpoint;
import org.apache.servicemix.http.ContextManager;
import org.apache.servicemix.http.HttpComponent;
import org.apache.servicemix.http.HttpEndpointType;
import org.apache.servicemix.http.HttpProcessor;
import org.apache.servicemix.http.SslParameters;
import org.apache.servicemix.http.jetty.JaasJettyPrincipal;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.apache.servicemix.jbi.messaging.MessageExchangeSupport;
import org.mortbay.jetty.RetryRequest;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

/**
 * Plain HTTP consumer endpoint. This endpoint can be used to handle plain HTTP request (without SOAP) or to be able to
 * process the request in a non standard way. For HTTP requests, a WSDL2 HTTP binding can be used.
 * 
 * @author gnodet
 * @since 3.2
 * @org.apache.xbean.XBean element="consumer"
 */
public class HttpConsumerEndpoint extends ConsumerEndpoint implements HttpProcessor, HttpEndpointType {

    public static final String MAIN_WSDL = "main.wsdl";

    private String authMethod;
    private SslParameters ssl;
    private String locationURI;
    private HttpConsumerMarshaler marshaler;
    private long timeout; // 0 => default to the timeout configured on component
    private URI defaultMep = MessageExchangeSupport.IN_OUT;

    private Map<String, Object> resources = new HashMap<String, Object>();
    private Map<String, Continuation> locks = new ConcurrentHashMap<String, Continuation>();
    private Map<String, MessageExchange> exchanges = new ConcurrentHashMap<String, MessageExchange>();
    private Object httpContext;

    public HttpConsumerEndpoint() {
        super();
    }

    public HttpConsumerEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component, endpoint);
    }

    public HttpConsumerEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    /**
     * @return the locationUri
     */
    public String getLocationURI() {
        return locationURI;
    }

    /**
     * @param locationURI
     *            the locationUri to set
     */
    public void setLocationURI(String locationURI) {
        this.locationURI = locationURI;
    }

    /**
     * @return the timeout
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * @param timeout
     *            the timeout to set
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * @return the marshaler
     */
    public HttpConsumerMarshaler getMarshaler() {
        return marshaler;
    }

    /**
     * @param marshaler
     *            the marshaler to set
     */
    public void setMarshaler(HttpConsumerMarshaler marshaler) {
        this.marshaler = marshaler;
    }

    /**
     * @return the authMethod
     */
    public String getAuthMethod() {
        return authMethod;
    }

    /**
     * @param authMethod
     *            the authMethod to set
     */
    public void setAuthMethod(String authMethod) {
        this.authMethod = authMethod;
    }

    /**
     * @return the sslParameters
     */
    public SslParameters getSsl() {
        return ssl;
    }

    /**
     * @param ssl
     *            the sslParameters to set
     */
    public void setSsl(SslParameters ssl) {
        this.ssl = ssl;
    }

    /**
     * @return defaultMep of the endpoint
     */
    public URI getDefaultMep() {
        return defaultMep;
    }

    /**
     * @param defaultMep -
     *            defaultMep of the endpoint
     */
    public void setDefaultMep(URI defaultMep) {
        this.defaultMep = defaultMep;
    }

    public void start() throws Exception {
        super.start();
        loadStaticResources();
        httpContext = getServerManager().createContext(locationURI, this);
    }

    public void stop() throws Exception {
        getServerManager().remove(httpContext);
        httpContext = null;
        super.stop();
    }

    public void process(MessageExchange exchange) throws Exception {
        Continuation cont = locks.remove(exchange.getExchangeId());
        if (cont == null) {
            throw new Exception("HTTP request has timed out");
        }
        synchronized (cont) {
            if (logger.isDebugEnabled()) {
                logger.debug("Resuming continuation for exchange: " + exchange.getExchangeId());
            }
            exchanges.put(exchange.getExchangeId(), exchange);
            cont.resume();
        }
    }

    public void process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (logger.isDebugEnabled()) {
            logger.debug("Receiving HTTP request: " + request);
        }
        MessageExchange exchange = null;
        try {
            // Handle WSDLs, XSDs
            if (handleStaticResource(request, response)) {
                return;
            }
            // Not giving a specific mutex will synchronize on the continuation itself
            Continuation cont = ContinuationSupport.getContinuation(request, null);
            // If the continuation is not a retry
            if (!cont.isPending()) {
                exchange = createExchange(request);
                locks.put(exchange.getExchangeId(), cont);
                request.setAttribute(MessageExchange.class.getName(), exchange.getExchangeId());
                synchronized (cont) {
                    send(exchange);
                    if (logger.isDebugEnabled()) {
                        logger.debug("Suspending continuation for exchange: " + exchange.getExchangeId());
                    }
                    long to = this.timeout;
                    if (to == 0) {
                        to = ((HttpComponent) getServiceUnit().getComponent()).getConfiguration()
                                            .getConsumerProcessorSuspendTime();
                    }
                    exchanges.put(exchange.getExchangeId(), exchange);
                    boolean result = cont.suspend(to);
                    exchange = exchanges.remove(exchange.getExchangeId());
                    if (!result) {
                        locks.remove(exchange.getExchangeId());
                        throw new Exception("Exchange timed out");
                    }
                    request.removeAttribute(MessageExchange.class.getName());
                }
            } else {
                String id = (String) request.getAttribute(MessageExchange.class.getName());
                locks.remove(id);
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
                Exception e = exchange.getError();
                if (e == null) {
                    e = new Exception("Unkown error (exchange aborted ?)");
                }
                throw e;
            } else if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                try {
                    Fault fault = exchange.getFault();
                    if (fault != null) {
                        sendFault(exchange, fault, request, response);
                    } else {
                        NormalizedMessage outMsg = exchange.getMessage("out");
                        if (outMsg != null) {
                            sendOut(exchange, outMsg, request, response);
                        }
                    }
                    exchange.setStatus(ExchangeStatus.DONE);
                    send(exchange);
                } catch (Exception e) {
                    exchange.setError(e);
                    send(exchange);
                    throw e;
                }
            } else if (exchange.getStatus() == ExchangeStatus.DONE) {
                // This happens when there is no response to send back
                sendAccepted(exchange, request, response);
            }
        } catch (RetryRequest e) {
            throw e;
        } catch (Exception e) {
            sendError(exchange, e, request, response);
        }
    }

    protected void loadStaticResources() {
        // TODO: load wsdl
    }

    /**
     * Handle static resources
     * 
     * @param request
     *            the http request
     * @param response
     *            the http response
     * @return <code>true</code> if the request has been handled
     * @throws IOException
     * @throws ServletException
     */
    protected boolean handleStaticResource(HttpServletRequest request, HttpServletResponse response)
        throws IOException, ServletException {
        if (!"GET".equals(request.getMethod())) {
            return false;
        }
        String query = request.getQueryString();
        if (query != null && query.trim().equalsIgnoreCase("wsdl") && getResource(MAIN_WSDL) != null) {
            String uri = request.getRequestURI();
            if (!uri.endsWith("/")) {
                uri += "/";
            }
            uri += MAIN_WSDL;
            response.sendRedirect(uri);
            return true;
        }
        String path = request.getPathInfo();
        if (path.lastIndexOf('/') >= 0) {
            path = path.substring(path.lastIndexOf('/') + 1);
        }
        Object res = getResource(path);
        if (res == null) {
            return false;
        }
        if (res instanceof Node) {
            response.setStatus(200);
            response.setContentType("text/xml");
            try {
                new SourceTransformer().toResult(new DOMSource((Node) res),
                                new StreamResult(response.getOutputStream()));
            } catch (TransformerException e) {
                throw new ServletException("Error while sending xml resource", e);
            }
        } else if (res != null) {
            // TODO: handle other static resources ...
            throw new ServletException("Unable to serialize resource");
        } else {
            return false;
        }
        return true;
    }

    protected Object getResource(String path) {
        return resources.get(path);
    }

    protected void addResource(String path, Object resource) {
        resources.put(path, resource);
    }

    protected ContextManager getServerManager() {
        HttpComponent comp = (HttpComponent) getServiceUnit().getComponent();
        return comp.getServer();
    }

    public MessageExchange createExchange(HttpServletRequest request) throws Exception {
        MessageExchange me = marshaler.createExchange(request, getContext());
        configureExchangeTarget(me);
        // If the user has been authenticated, put these informations on
        // the in NormalizedMessage.
        if (request.getUserPrincipal() instanceof JaasJettyPrincipal) {
            Subject subject = ((JaasJettyPrincipal) request.getUserPrincipal()).getSubject();
            me.getMessage("in").setSecuritySubject(subject);
        }
        return me;
    }

    public void sendAccepted(MessageExchange exchange, HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        marshaler.sendAccepted(exchange, request, response);
    }

    public void sendError(MessageExchange exchange, Exception error, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        marshaler.sendError(exchange, error, request, response);
    }

    public void sendFault(MessageExchange exchange, Fault fault, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        marshaler.sendFault(exchange, fault, request, response);
    }

    public void sendOut(MessageExchange exchange, NormalizedMessage outMsg, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        marshaler.sendOut(exchange, outMsg, request, response);
    }

    public void validate() throws DeploymentException {
        super.validate();
        if (marshaler == null) {
            marshaler = new DefaultHttpConsumerMarshaler();
        }
        if (marshaler instanceof DefaultHttpConsumerMarshaler) {
            ((DefaultHttpConsumerMarshaler) marshaler).setDefaultMep(getDefaultMep());
        }
    }
}
