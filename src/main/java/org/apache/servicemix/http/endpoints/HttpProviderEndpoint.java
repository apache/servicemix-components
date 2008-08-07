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

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.jbi.management.DeploymentException;
import javax.xml.namespace.QName;

import org.apache.servicemix.common.JbiConstants;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.http.HttpComponent;
import org.apache.servicemix.http.HttpEndpointType;
import org.apache.servicemix.http.jetty.SmxHttpExchange;
import org.mortbay.jetty.client.HttpClient;

/**
 * a plain HTTP provider. This type of endpoint can be used to send non-SOAP requests to HTTP endpoints.
 * 
 * @author gnodet
 * @since 3.2
 * @org.apache.xbean.XBean element="provider"
 */
public class HttpProviderEndpoint extends ProviderEndpoint implements HttpEndpointType {

    // private SslParameters ssl;
    // private BasicAuthCredentials basicAuthentication;
    private HttpProviderMarshaler marshaler;
    private String locationURI;
    private int clientSoTimeout = 60000;
    private HttpClient jettyClient;

    public HttpProviderEndpoint() {
        super();
    }

    public HttpProviderEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component, endpoint);
    }

    public HttpProviderEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    /**
     * Returns the URI to which the endpoint sends requests.
     * 
     * @return a string representing the URI to which requests are sent
     */
    public String getLocationURI() {
        return locationURI;
    }

    /**
     * Sets the URI to which an endpoint sends requests.
     * 
     * @param locationURI a string representing the URI
     * @org.apache.xbean.Property description="the URI to which the endpoint sends requests"
     */
    public void setLocationURI(String locationURI) {
        this.locationURI = locationURI;
    }

    /**
     * @return the marshaler
     */
    public HttpProviderMarshaler getMarshaler() {
        return marshaler;
    }

    /**
     * Sets the class used to marshal messages.
     * 
     * @param marshaler the marshaler to set
     * @org.apache.xbean.Property description="the bean used to marshal HTTP messages. The deafult is a
     *                            <code>DefaultHttpProviderMarshaler</code>."
     */
    public void setMarshaler(HttpProviderMarshaler marshaler) {
        this.marshaler = marshaler;
    }

    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
            NormalizedMessage nm = exchange.getMessage("in");
            if (nm == null) {
                throw new IllegalStateException("Exchange has no input message");
            }
            SmxHttpExchange httpEx = new Exchange(exchange);
            marshaler.createRequest(exchange, nm, httpEx);
            getConnectionPool().send(httpEx);
        }
    }

    protected void handle(SmxHttpExchange httpExchange, MessageExchange exchange) throws IOException {
        try {
            marshaler.handleResponse(exchange, httpExchange);
        } catch (Exception e) {
            exchange.setError(e);
        }
        try {
            boolean txSync = exchange.getStatus() == ExchangeStatus.ACTIVE && exchange.isTransacted()
                             && Boolean.TRUE.equals(exchange.getProperty(JbiConstants.SEND_SYNC));
            if (txSync) {
                sendSync(exchange);
            } else {
                send(exchange);
            }
        } catch (Exception e) {
            throw (IOException)new IOException(e.getMessage()).initCause(e);
        }
    }

    protected void handleConnectionFailed(Throwable throwable, MessageExchange exchange) {
        try {
            Exception e;
            if (throwable instanceof Exception) {
                e = (Exception)throwable;
            } else {
                e = new Exception(throwable);
            }
            exchange.setError(e);
            send(exchange);
        } catch (Exception e) {
            logger.warn("Unable to send back exchange in error", e);
        }
    }

    protected org.mortbay.jetty.client.HttpClient getConnectionPool() throws Exception {
        if (jettyClient == null) {
            HttpComponent comp = (HttpComponent)getServiceUnit().getComponent();
            if (comp.getConfiguration().isJettyClientPerProvider()) {
                jettyClient = comp.getNewJettyClient(comp);
            } else {
                // return shared client
                jettyClient = comp.getConnectionPool();
            }
            jettyClient.setSoTimeout(getClientSoTimeout());
        }
        return jettyClient;
    }

    protected class Exchange extends SmxHttpExchange {
        MessageExchange jbiExchange;

        public Exchange(MessageExchange jbiExchange) {
            this.jbiExchange = jbiExchange;
        }

        protected void onResponseComplete() throws IOException {
            handle(this, jbiExchange);
        }

        protected void onConnectionFailed(Throwable throwable) {
            handleConnectionFailed(throwable, jbiExchange);
        }

        protected void onException(Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    public int getClientSoTimeout() {
        return clientSoTimeout;
    }

    /**
     * Sets the number of milliseconds the endpoint will block while attempting to read a request. The default value is 60000.
     * Setting this to 0 specifies that the endpoint will never timeout.
     * 
     * @param clientTimeout an int specifying the number of milliseconds the socket will block while attempting to read a request
     * @org.apache.xbean.Property description="the number of milliseconds the endpoint will block while attempting to read a request. The default value is 60000. Setting this to 0 specifies that the endpoint will never timeout."
     */
    public void setClientSoTimeout(int clientTimeout) {
        this.clientSoTimeout = clientTimeout;
    }

    public void validate() throws DeploymentException {
        super.validate();
        if (marshaler == null) {
            marshaler = new DefaultHttpProviderMarshaler();
        }
        if (marshaler instanceof DefaultHttpProviderMarshaler && locationURI != null) {
            ((DefaultHttpProviderMarshaler)marshaler).setLocationURI(locationURI);
        }
    }

}
