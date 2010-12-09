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
import org.mortbay.util.ajax.WaitingContinuation;

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
  private Map<String, Continuation> locks = new ConcurrentHashMap<String, Continuation>();
  private Object httpContext;
  private boolean started = false;

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
   * @org.apache.xbean.Property description="the timeout is specified in milliseconds. The default value is 0 which
   *       means that the endpoint will never timeout."
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
   * @org.apache.xbean.Property description="the bean used to marshal HTTP messages. The default is a
   *                            <code>DefaultHttpConsumerMarshaler</code>."
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
   * @org.apache.xbean.Property description="a URI representing the endpoint's default MEP. The default is
   *                            <code>JbiConstants.IN_OUT</code>."
   */
  public void setDefaultMep(URI defaultMep) {
    this.defaultMep = defaultMep;
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

  public void process(MessageExchange exchange) throws Exception {
    // Receive the exchange response
    // First, check if the continuation has not been removed from the map,
    // which would mean it has timed out.  If this is the case, throw an exception
    // that will set the exchange status to ERROR.
    Continuation cont = locks.get(exchange.getExchangeId());
    if (cont == null) {
      throw new Exception("HTTP request has timed out for exchange: " + exchange.getExchangeId());
    }
    synchronized (cont) {
      if (logger.isDebugEnabled()) {
        logger.debug("Resuming continuation for exchange: " + exchange.getExchangeId());
      }
      // In case of the SEDA flow isn't used, the exchange could be a different instance, so it should be updated.
      cont.setObject(exchange);
      // Resume continuation
      cont.resume();
      if (!cont.isResumed()) {
        if (logger.isDebugEnabled()) {
          logger.debug("Could not resume continuation for exchange: " + exchange.getExchangeId());
        }
        throw new Exception("HTTP request has timed out for exchange: " + exchange.getExchangeId());
      }
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
      Continuation cont = createContinuation(request);

      long to = this.timeout;
      if (to == 0) {
        to = ((HttpComponent) getServiceUnit().getComponent()).getConfiguration().getConsumerProcessorSuspendTime();
      }

      if (!cont.isPending()) {
        // Check endpoint is started
        if (!started) {
          response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Endpoint is stopped");
          return;
        }
        // Create the exchange
        exchange = createExchange(request);
        // Put the exchange into the continuation for retrieval later.
        cont.setObject(exchange);
        // Put the continuation in a map under the exchange id key
        locks.put(exchange.getExchangeId(), cont);
        if (logger.isDebugEnabled()) {
          logger.debug("Suspending continuation for exchange: " + exchange.getExchangeId());
        }
        synchronized (cont) {
          // Send the exchange and then suspend the request.
          send(exchange);
          // Suspend the continuation for the configured timeout
          // If a SelectConnector is used, the call to suspend
          // will throw a RetryRequest exception
          // else, the call will block until the continuation is
          // resumed
          boolean istimeout = !cont.suspend(to);
          // The call has not thrown a RetryRequest, which means
          // we don't use a SelectConnector
          // and we must handle the exchange in this very method
          // call.
          // If result is false, the continuation has timed out.
          locks.remove(exchange.getExchangeId());

          // Timeout if SelectConnector is not used  
          if (istimeout) {
              throw new Exception("HTTP request has timed out for exchange: " + exchange.getExchangeId());
          }
        }
      } else {
        // The continuation is a retry.
        // This happens when the SelectConnector is used and in two cases:
        //  * the continuation has been resumed because the exchange has been received
        //  * the continuation has timed out
        boolean istimeout = !cont.suspend(to);
        exchange = (MessageExchange) cont.getObject();
        // Remove the continuation from the map, indicating it has been processed or timed out
        locks.remove(exchange.getExchangeId());

        // Timeout
        if (istimeout) {
            throw new Exception("HTTP request has timed out for exchange: " + exchange.getExchangeId());
        }
      }
      // At this point, we have received the exchange response,
      // so process it and send back the HTTP response
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
    } catch (RetryRequest e) {
      throw e;
    } catch (Exception e) {
      sendError(exchange, e, request, response);
    }
  }

  private Continuation createContinuation(HttpServletRequest request) {
    // not giving a specific mutex will synchronize on the continuation itself
    Continuation continuation = ContinuationSupport.getContinuation(request, null);
    if (continuation instanceof WaitingContinuation) {
      return continuation;
    } else {
      // wrap the continuation to avoid a deadlock between this endpoint and the Jetty continuation timeout mechanism
      // the endpoint now synchronizes on the wrapper while Jetty synchronizes on the continuation itself
      return new ContinuationWrapper(continuation);
    }
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

  /*
   * Continuation wrapper just delegates everything to the underlying Continuation
   */
  private static final class ContinuationWrapper implements Continuation {

    private final Continuation continuation;

    private ContinuationWrapper(Continuation continuation) {
      super();
      this.continuation = continuation;
    }

    public Object getObject() {
      return continuation.getObject();
    }

    public boolean isNew() {
      return continuation.isNew();
    }

    public boolean isPending() {
      return continuation.isPending();
    }

    public boolean isResumed() {
      return continuation.isResumed();
    }

    public void reset() {
      continuation.reset();
    }

    public void resume() {
      continuation.resume();
    }

    public void setObject(Object o) {
      continuation.setObject(o);
    }

    public boolean suspend(long timeout) {
      return continuation.suspend(timeout);
    }
  }
}
