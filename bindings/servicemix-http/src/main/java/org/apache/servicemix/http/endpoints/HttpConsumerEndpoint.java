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
import java.net.URL;
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.JbiConstants;
import org.apache.servicemix.common.endpoints.ConsumerEndpoint;
import org.apache.servicemix.http.ContextManager;
import org.apache.servicemix.http.HttpComponent;
import org.apache.servicemix.http.HttpEndpointType;
import org.apache.servicemix.http.HttpProcessor;
import org.apache.servicemix.http.SslParameters;
import org.apache.servicemix.http.jetty.JaasJettyPrincipal;
import org.apache.servicemix.jbi.jaxp.SourceTransformer;
import org.mortbay.jetty.RetryRequest;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

import static org.apache.servicemix.http.jetty.ContinuationHelper.isNewContinuation;

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
    private URI defaultMep = JbiConstants.IN_OUT;
    private Map<String, Object> resources = new HashMap<String, Object>();
    private Map<String, Continuation> continuations = new ConcurrentHashMap<String, Continuation>();
    private Map<String, Object> mutexes = new ConcurrentHashMap<String, Object>();
    private Object httpContext;
    private boolean started = false;
    private LateResponseStrategy lateResponseStrategy = LateResponseStrategy.error;
    private boolean rewriteSoapAddress = false;

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
     * Returns the URI at which the endpoint listens for new requests.
     *
     * @return a string representing the endpoint's URI
     */
    public String getLocationURI() {
        return locationURI;
    }

    /**
     * Sets the URI at which an endpoint listens for requests.
     *
     * @param locationURI a string representing the URI
     * @org.apache.xbean.Property description="the URI at which the endpoint listens for requests"
     */
    public void setLocationURI(String locationURI) {
        this.locationURI = locationURI;
    }

    /**
     * Returns the timeout value for an HTTP endpoint.
     *
     * @return the timeout specified in milliseconds
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * Specifies the timeout value for an HTTP consumer endpoint. The timeout is specified in milliseconds. The default value is 0
     * which means that the endpoint will never timeout.
     *
     * @org.apache.xbean.Property description="the timeout is specified in milliseconds. The default value is 0 which means that the endpoint will never timeout."
     * @param timeout the length time, in milliseconds, to wait before timing out
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
     * Sets the class used to marshal messages.
     *
     * @param marshaler the marshaler to set
     * @org.apache.xbean.Property description="the bean used to marshal HTTP messages. The default is a <code>DefaultHttpConsumerMarshaler</code>."
     */
    public void setMarshaler(HttpConsumerMarshaler marshaler) {
        this.marshaler = marshaler;
    }

    /**
     * Returns a string describing the authentication scheme being used by an endpoint.
     *
     * @return a string representing the authentication method used by an endpoint
     */
    public String getAuthMethod() {
        return authMethod;
    }

    /**
     * Specifies the authentication method used by a secure endpoint. The authentication method is a string naming the scheme used
     * for authenticating users.
     *
     * @param authMethod a string naming the authentication scheme a secure endpoint should use
     * @org.apache.xbean.Property description="a string naming the scheme used for authenticating users"
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
     * Sets the properties used to configure SSL for the endpoint.
     *
     * @param ssl an <code>SslParameters</code> object containing the SSL properties
     * @org.apache.xbean.Property description="a bean containing the SSL configuration properties"
     */
    public void setSsl(SslParameters ssl) {
        this.ssl = ssl;
    }

    /**
     * Returns a URI representing the default message exachange pattern(MEP) used by an endpoint.
     *
     * @return a URI representing an endpoint's default MEP
     */
    public URI getDefaultMep() {
        return defaultMep;
    }

    /**
     * Sets the default message exchange pattern(MEP) for an endpoint. The default MEP is specified as a URI and the default is
     * <code>JbiConstants.IN_OUT</code>.
     *
     * @param defaultMep a URI representing the default MEP of the endpoint
     * @org.apache.xbean.Property description="a URI representing the endpoint's default MEP. The default is <code>JbiConstants.IN_OUT</code>."
     */
    public void setDefaultMep(URI defaultMep) {
        this.defaultMep = defaultMep;
    }

    public String getLateResponseStrategy() {
        return lateResponseStrategy.name();
    }

    /**
     * Set the strategy to be used for handling a late response from the ESB (i.e. a response that arrives after the HTTP request has timed out).
     * Defaults to <code>error</code>
     *
     * <ul>
     *     <li><code>error</code> will terminate the exchange with an ERROR status and log an exception for the late response</li>
     *     <li><code>warning</code> will end the exchange with a DONE status and log a warning for the late response instead</li>
     * </ul>
     *
     * @param value
     */
    public void setLateResponseStrategy(String value) {
        this.lateResponseStrategy = LateResponseStrategy.valueOf(value);
    }
    
    public boolean isRewriteSoapAddress() {
		return rewriteSoapAddress;
	}

    /**
     * Toggles the rewriting of the soap address based on the request info.
     * <p>
     * When active, the soap address in the wsdl will be updated according
     * to the protocol, host and port of the request.  This is useful when
     * listening on 0.0.0.0 or when behind a NAT (or reverse-proxy in some 
     * cases).<br />
     * This function only works on the main wsdl, not in imported wsdl-parts.
     * This means the service with its port must be declared in the main
     * wsdl.
     * </p><p>
     * By default it is activated.
     * </p>
     * @param value
     */
	public void setRewriteSoapAddress(boolean value) {
		this.rewriteSoapAddress = value;
	}

	public void activate() throws Exception {
        super.activate();
        loadStaticResources();
        httpContext = getServerManager().createContext(locationURI, this);
    }

    public void deactivate() throws Exception {
        getServerManager().remove(httpContext);
        httpContext = null;
        super.deactivate();
    }

    public void start() throws Exception {
        super.start();
        started = true;
    }

    public void stop() throws Exception {
        started = false;
        super.stop();
    }

    /*
     * Process the reponse message exchange
     */
    public void process(MessageExchange exchange) throws Exception {
        final String id = exchange.getExchangeId();
        final Object mutex = mutexes.get(id);

        // if the mutex is no longer available, the HTTP request timed out before the message exchange got handled by the ESB
        if (mutex == null) {
            handleLateResponse(exchange);
            return;
        }

        // Synchronize on the mutex object while we're tinkering with the continuation object
        synchronized (mutex) {
            final Continuation continuation = continuations.get(id);
            if (continuation != null && continuation.isPending()) {
                logger.debug("Resuming continuation for exchange: {}", exchange.getExchangeId());

                // in case of the JMS/JCA flow, you might have a different instance of the message exchange here
                continuation.setObject(exchange);

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
    }

    /*
     * Process the HTTP request/response - this method gets invoked:
     * - when a new HTTP request is received
     * - when a suspended HTTP request is being resumed
     *   (either because the exchange was received or because the request timed out)
     */
    public void process(HttpServletRequest request, HttpServletResponse response) throws Exception {

        MessageExchange exchange = null;

        try {
            // Handle WSDLs, XSDs
            if (handleStaticResource(request, response)) {
                return;
            }

            // configure the timeout
            long to = this.timeout;
            if (to == 0) {
                to = ((HttpComponent) getServiceUnit().getComponent()).getConfiguration().getConsumerProcessorSuspendTime();
            }

            final Continuation continuation = ContinuationSupport.getContinuation(request, null);
            exchange = (MessageExchange) continuation.getObject();
            final Object mutex = getOrCreateMutex(exchange);

            // Synchronize on the mutex object while we're tinkering with the continuation object
            synchronized (mutex) {
                if (isNewContinuation(continuation)) {
                    logger.debug("Receiving HTTP request: {}", request);

                    // send back HTTP status 503 (Not Avaialble) to reject any new requests if the endpoint is not started
                    if (!started) {
                        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Endpoint is stopped");
                        return;
                    }

                    // Create the exchange
                    exchange = createExchange(request);
                    final String id = exchange.getExchangeId();

                    // Put the exchange into the continuation for later retrieval and store the mutex/continuation objects
                    continuation.setObject(exchange);
                    mutexes.put(id, mutex);
                    continuations.put(id, continuation);

                    // Send the exchange and then suspend the HTTP request until the message exchange gets answered
                    send(exchange);

                    // Right after this if-block, we will try suspending the continuation
                    // If a SelectConnector is being used, the call to suspend will throw a RetryRequest
                    // This will free the thread - this method will be invoked again when the continuation gets resumed
                    logger.debug("Suspending continuation for exchange: {}", exchange.getExchangeId());
                } else {
                    logger.debug("Resuming HTTP request: {}", request);
                }

                boolean istimeout = !continuation.suspend(to);

                // Continuation is being ended (either because the message exchange was handled or the continuation timed out)
                // Cleaning up the stored objects for this continuation now
                exchange = doEndContinuation(continuation);

                // Timeout if SelectConnector is not used
                if (istimeout) {
                    throw new Exception("HTTP request has timed out for exchange: " + exchange.getExchangeId());
                }
            }

            // message exchange has been completed, so we're ready to send back an HTTP response now
            handleResponse(exchange, request, response);
        } catch (RetryRequest e) {
            // retrow the RetryRequest to allow Jetty to re-invoke this method when the continuation is being resumed
            throw e;
        } catch (Exception e) {
            sendError(exchange, e, request, response);
        }
    }

    /*
     * Handle the HTTP response based on the information in the message exchange we received
     */
    private void handleResponse(MessageExchange exchange, HttpServletRequest request, HttpServletResponse response) throws Exception {
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
                done(exchange);
            } catch (Exception e) {
                fail(exchange, e);
                throw e;
            }
        } else if (exchange.getStatus() == ExchangeStatus.DONE) {
            // This happens when there is no response to send back
            sendAccepted(exchange, request, response);
        }
    }

    /*
     * Handle a message exchange that is being received after the corresponding HTTP request has timed out
     */
    private void handleLateResponse(MessageExchange exchange) throws Exception {
        // if the exchange is no longer active by now, something else probably went wrong in the meanwhile
        if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            if (lateResponseStrategy == LateResponseStrategy.error) {
                fail(exchange, new Exception("HTTP request has timed out for exchange: {} " + exchange.getExchangeId()));
            } else {
                logger.warn("HTTP request has timed out for exchange: {}", exchange.getExchangeId());
                done(exchange);
            }
        }
    }

    /*
     * Get or create an object that can be used for synchronizing code blocks for a given exchange
     */
    private Object getOrCreateMutex(MessageExchange exchange) {
        Object result = null;

        // let's try to find the object that corresponds to the exchange first
        if (exchange != null) {
            result = mutexes.get(exchange.getExchangeId());
        }

        // no luck finding an existing object, let's create a new one
        if (result == null) {
            result = new Object();
        }

        return result;
    }

    /*
     * End the continuation by removing all objects stored on/for the continuation
     * and returning the MessageExchange that was represented by the continuation
     */
    private MessageExchange doEndContinuation(Continuation continuation) {
        final MessageExchange exchange = (MessageExchange) continuation.getObject();
        final String id = exchange.getExchangeId();

        continuation.setObject(null);
        mutexes.remove(id);
        continuations.remove(id);
        return exchange;
    }

    protected void loadStaticResources() throws Exception {
    }

    /**
     * Handle static resources
     *
     * @param request the http request
     * @param response the http response
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
        Object res;
        if (rewriteSoapAddress && path.equals(MAIN_WSDL) && getResource(path) instanceof Document) {
        	// determine the location based on the request
            String location = getLocationURI();
            try {
                URL listUrl = new URL(getLocationURI());
                URL requestUrl = new URL(request.getRequestURL().toString());
                URL acceptUri = new URL(requestUrl.getProtocol(), requestUrl.getHost(), requestUrl.getPort(),
                        listUrl.getFile());
                location = acceptUri.toExternalForm();
            
	            //Update the location for this request
	            Document copy = (Document) ((Document) getResource(path)).cloneNode(true);
	            updateSoapLocations(location, copy.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/soap12/", "address"));
	            updateSoapLocations(location, copy.getElementsByTagNameNS("http://schemas.xmlsoap.org/wsdl/soap/", "address"));
	            res = copy;
            } catch (Exception e) {
                logger.warn("Could not update soap location, using default", e);
                res = getResource(path);
            }
        } else {
        	res = getResource(path);
        }
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
        if (me.getEndpoint() == null) {
            configureExchangeTarget(me);
        }
        // If the user has been authenticated, put these informations on
        // the in NormalizedMessage.
        if (request.getUserPrincipal() instanceof JaasJettyPrincipal) {
            Subject subject = ((JaasJettyPrincipal) request.getUserPrincipal()).getSubject();
            me.getMessage("in").setSecuritySubject(subject);
        }
        return me;
    }

    public void sendAccepted(MessageExchange exchange, HttpServletRequest request,
                             HttpServletResponse response) throws Exception {
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
        if (locationURI == null || locationURI.trim().length() < 1) {
            throw new DeploymentException("The location URI is mandatory.");
        }
        if (endpoint != null && endpoint.contains(":")) {
            throw new DeploymentException("Endpoint name contains ':'. This character is not allowed as it can provide invalid WSDL.");
        }
        if (marshaler == null) {
            marshaler = new DefaultHttpConsumerMarshaler();
        }
        if (marshaler instanceof DefaultHttpConsumerMarshaler) {
            ((DefaultHttpConsumerMarshaler) marshaler).setDefaultMep(getDefaultMep());
        }
    }
    
    protected void updateSoapLocations(String location, NodeList addresses) {
        int i = 0;
        while (i < addresses.getLength()) {
            Element address = (Element) addresses.item(i);
            if (getLocationURI().equals(address.getAttribute("location"))) {
                address.setAttribute("location", location);
            }
            i++;
        }
    }

    /**
     * Determines how the HTTP consumer endpoint should handle a late response from the NMR
     */
    protected enum LateResponseStrategy {

        /**
         * Terminate the exchange with an ERROR status and log an exception for the late response
         */
        error,

        /**
         * End the exchange with a DONE status and log a warning for the late response
         */
        warning

    }
}
