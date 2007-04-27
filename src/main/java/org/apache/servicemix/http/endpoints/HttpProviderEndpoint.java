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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.URI;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.common.ServiceUnit;
import org.apache.servicemix.common.endpoints.ProviderEndpoint;
import org.apache.servicemix.http.BasicAuthCredentials;
import org.apache.servicemix.http.HttpEndpointType;
import org.apache.servicemix.http.SslParameters;

/**
 * 
 * @author gnodet
 * @org.apache.xbean.XBean element="provider"
 */
public class HttpProviderEndpoint extends ProviderEndpoint implements ExchangeProcessor, HttpEndpointType {

    private SslParameters ssl;
    private BasicAuthCredentials basicAuthentication;
    private Map<String, HttpMethod> methods = new ConcurrentHashMap<String, HttpMethod>();
    private HttpProviderMarshaler marshaler = new DefaultHttpProviderMarshaler();
    
    public HttpProviderEndpoint() {
        super();
    }

    public HttpProviderEndpoint(DefaultComponent component, ServiceEndpoint endpoint) {
        super(component, endpoint);
    }

    public HttpProviderEndpoint(ServiceUnit serviceUnit, QName service, String endpoint) {
        super(serviceUnit, service, endpoint);
    }

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

    public void process(MessageExchange exchange) throws Exception {
        if (exchange.getStatus() == ExchangeStatus.DONE || exchange.getStatus() == ExchangeStatus.ERROR) {
            HttpMethod method = methods.remove(exchange.getExchangeId());
            if (method != null) {
                method.releaseConnection();
            }
            return;
        }
        //boolean txSync = exchange.isTransacted() && Boolean.TRUE.equals(exchange.getProperty(JbiConstants.SEND_SYNC));
        NormalizedMessage nm = exchange.getMessage("in");
        if (nm == null) {
            throw new IllegalStateException("Exchange has no input message");
        }
        String locationUri = marshaler.getDestinationUri(exchange, nm);
        HttpMethod method = marshaler.createMethod(exchange, nm);
        method.setURI(new URI(getRelUri(locationUri), false));
        boolean close = true;
        try {
                /*
                // Uncomment to avoid the http request being sent several times.
                // Can be useful when debugging
                //================================
                //method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, new HttpMethodRetryHandler() {
                //    public boolean retryMethod(HttpMethod method, IOException exception, int executionCount) {
                //        return false;
                //    }
                //});
                if (getBasicAuthentication() != null) {
                    getBasicAuthentication().applyCredentials( getClient() );
                }
                int response = getClient().executeMethod(getHostConfiguration(locationUri), method);
                if (response != HttpStatus.SC_OK && response != HttpStatus.SC_ACCEPTED) {
                    if (exchange instanceof InOnly == false) {
                        SoapReader reader = soapHelper.getSoapMarshaler().createReader();
                        Header contentType = method.getResponseHeader(AbstractProcessor.HEADER_CONTENT_TYPE);
                        soapMessage = reader.read(method.getResponseBodyAsStream(), 
                                                  contentType != null ? contentType.getValue() : null);
                        context.setFaultMessage(soapMessage);
                        soapHelper.onAnswer(context);
                        Fault fault = exchange.createFault();
                        fault.setProperty(JbiConstants.PROTOCOL_HEADERS, getHeaders(method));
                        soapHelper.getJBIMarshaler().toNMS(fault, soapMessage);
                        exchange.setFault(fault);
                        if (txSync) {
                            channel.sendSync(exchange);
                        } else {
                            methods.put(exchange.getExchangeId(), method);
                            channel.send(exchange);
                            close = false;
                        }
                        return;
                    } else {
                        throw new Exception("Invalid status response: " + response);
                    }
                }
                if (exchange instanceof InOut) {
                    NormalizedMessage msg = exchange.createMessage();
                    SoapReader reader = soapHelper.getSoapMarshaler().createReader();
                    Header contentType = method.getResponseHeader(AbstractProcessor.HEADER_CONTENT_TYPE);
                    soapMessage = reader.read(method.getResponseBodyAsStream(), 
                                              contentType != null ? contentType.getValue() : null);
                    context.setOutMessage(soapMessage);
                    soapHelper.onAnswer(context);
                    msg.setProperty(JbiConstants.PROTOCOL_HEADERS, getHeaders(method));
                    soapHelper.getJBIMarshaler().toNMS(msg, soapMessage);
                    ((InOut) exchange).setOutMessage(msg);
                    if (txSync) {
                        channel.sendSync(exchange);
                    } else {
                        methods.put(exchange.getExchangeId(), method);
                        channel.send(exchange);
                        close = false;
                    }
                } else if (exchange instanceof InOptionalOut) {
                    if (method.getResponseContentLength() == 0) {
                        exchange.setStatus(ExchangeStatus.DONE);
                        channel.send(exchange);
                    } else {
                        NormalizedMessage msg = exchange.createMessage();
                        SoapReader reader = soapHelper.getSoapMarshaler().createReader();
                        soapMessage = reader.read(method.getResponseBodyAsStream(), 
                                                  method.getResponseHeader(AbstractProcessor.HEADER_CONTENT_TYPE).getValue());
                        context.setOutMessage(soapMessage);
                        soapHelper.onAnswer(context);
                        msg.setProperty(JbiConstants.PROTOCOL_HEADERS, getHeaders(method));
                        soapHelper.getJBIMarshaler().toNMS(msg, soapMessage);
                        ((InOptionalOut) exchange).setOutMessage(msg);
                        if (txSync) {
                            channel.sendSync(exchange);
                        } else {
                            methods.put(exchange.getExchangeId(), method);
                            channel.send(exchange);
                            close = false;
                        }
                    }
                } else {
                    exchange.setStatus(ExchangeStatus.DONE);
                    channel.send(exchange);
                }
                */
        } finally {
            if (close) {
                method.releaseConnection();
            }
        }
    }

    private String getRelUri(String locationUri) {
        java.net.URI uri = java.net.URI.create(locationUri);
        String relUri = uri.getPath();
        if (!relUri.startsWith("/")) {
            relUri = "/" + relUri;
        }
        if (uri.getQuery() != null) {
            relUri += "?" + uri.getQuery();
        }
        if (uri.getFragment() != null) {
            relUri += "#" + uri.getFragment();
        }
        return relUri;
    }

    /*
    protected HttpClient getClient() {
        HttpComponent comp =  (HttpComponent) getServiceUnit().getComponent();
        return comp.getClient();
    }

    private HostConfiguration getHostConfiguration(String locationURI) throws Exception {
        HostConfiguration host;
        URI uri = new URI(locationURI, false);
        if (uri.getScheme().equals("https")) {
            ProtocolSocketFactory sf = new CommonsHttpSSLSocketFactory(
                            getSsl(),
                            getKeystoreManager());
            Protocol protocol = new Protocol("https", sf, 443);
            HttpHost httphost = new HttpHost(uri.getHost(), uri.getPort(), protocol);
            host = new HostConfiguration();
            host.setHost(httphost);
        } else {
            host = new HostConfiguration();
            host.setHost(uri.getHost(), uri.getPort());
        }

        return host;
    }
    */

}
