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
import javax.xml.namespace.QName;

import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.http.HttpComponent;
import org.apache.servicemix.http.HttpEndpointType;
import org.apache.servicemix.http.jetty.SmxHttpExchange;

/**
 * 
 * @author gnodet
 * @since 3.2
 * @org.apache.xbean.XBean element="provider"
 */
public class HttpProviderEndpoint extends ProviderEndpoint implements HttpEndpointType {

    //private SslParameters ssl;
    //private BasicAuthCredentials basicAuthentication;
    private HttpProviderMarshaler marshaler;
    private String locationURI;
    
    public HttpProviderEndpoint() {
        super();
    }

    public HttpProviderEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component, endpoint);
    }

    public HttpProviderEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

    public String getLocationURI() {
        return locationURI;
    }

    public void setLocationURI(String locationURI) {
        this.locationURI = locationURI;
    }

    public void start() throws Exception {
        if (marshaler == null) {
            DefaultHttpProviderMarshaler m = new DefaultHttpProviderMarshaler();
            m.setLocationURI(locationURI);
            marshaler = m;
        }
        super.start();
    }
    
    /*
    public BasicAuthCredentials getBasicAuthentication() {
        return basicAuthentication;
    }

    public void setBasicAuthentication(BasicAuthCredentials basicAuthentication) {
        this.basicAuthentication = basicAuthentication;
    }
    
    public SslParameters getSsl() {
        return ssl;
    }
    
    public void setSsl(SslParameters ssl) {
        this.ssl = ssl;
    }
    */

    /**
     * @return the marshaler
     */
    public HttpProviderMarshaler getMarshaler() {
        return marshaler;
    }

    /**
     * @param marshaler the marshaler to set
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
            boolean txSync = exchange.getStatus() == ExchangeStatus.ACTIVE 
                             && exchange.isTransacted() 
                             && Boolean.TRUE.equals(exchange.getProperty(JbiConstants.SEND_SYNC));
            if (txSync) {
                sendSync(exchange);
            } else {
                send(exchange);
            }
        } catch (Exception e) {
            throw (IOException) new IOException(e.getMessage()).initCause(e);
        }
    }

    protected org.mortbay.jetty.client.HttpClient getConnectionPool() {
        HttpComponent comp =  (HttpComponent) getServiceUnit().getComponent();
        return comp.getConnectionPool();
    }
    
    protected class Exchange extends SmxHttpExchange {
        MessageExchange jbiExchange;
        public Exchange(MessageExchange jbiExchange) {
            this.jbiExchange = jbiExchange;
        }
        protected void onResponseComplete() throws IOException {
            handle(this, jbiExchange);
        }
    }

}
