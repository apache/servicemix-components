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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;

import org.apache.servicemix.JbiConstants;
import org.apache.servicemix.common.BaseLifeCycle;
import org.apache.servicemix.common.ExchangeProcessor;
import org.apache.servicemix.http.HttpEndpoint;
import org.apache.servicemix.http.HttpLifeCycle;
import org.apache.servicemix.http.HttpProcessor;
import org.apache.servicemix.http.ServerManager;
import org.apache.servicemix.soap.SoapFault;
import org.apache.servicemix.soap.SoapHelper;
import org.apache.servicemix.soap.handlers.AddressingInHandler;
import org.apache.servicemix.soap.marshalers.JBIMarshaler;
import org.apache.servicemix.soap.marshalers.SoapMarshaler;
import org.apache.servicemix.soap.marshalers.SoapMessage;
import org.mortbay.jetty.RetryRequest;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.util.ajax.Continuation;
import org.mortbay.util.ajax.ContinuationSupport;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;

public class ConsumerProcessor implements ExchangeProcessor, HttpProcessor {

    public static final URI IN_ONLY = URI.create("http://www.w3.org/2004/08/wsdl/in-only");
    public static final URI IN_OUT = URI.create("http://www.w3.org/2004/08/wsdl/in-out");
    public static final URI ROBUST_IN_ONLY = URI.create("http://www.w3.org/2004/08/wsdl/robust-in-only");
    
    protected HttpEndpoint endpoint;
    protected ContextHandler httpContext;
    protected ComponentContext context;
    protected DeliveryChannel channel;
    protected SoapMarshaler soapMarshaler;
    protected JBIMarshaler jbiMarshaler;
    protected SoapHelper soapHelper;
    protected Map locks;
        
    public ConsumerProcessor(HttpEndpoint endpoint) {
        this.endpoint = endpoint;
        this.soapMarshaler = new SoapMarshaler(endpoint.isSoap());
        this.soapHelper = new SoapHelper(endpoint);
        this.soapHelper.addPolicy(new AddressingInHandler());
        this.jbiMarshaler = new JBIMarshaler();
        this.locks = new ConcurrentHashMap();
    }
    
    public void process(MessageExchange exchange) throws Exception {
        Continuation cont = (Continuation) locks.remove(exchange.getExchangeId());
        if (cont != null) {
            //System.err.println("Notifying: " + exchange.getExchangeId());
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
        if (!"POST".equals(request.getMethod())) {
            throw new UnsupportedOperationException(request.getMethod() + " not supported");
        }
        QName envelopeName = null;
        try {
            // Not giving a specific mutex will synchronize on the contination itself
            Continuation cont = ContinuationSupport.getContinuation(request, null);
            MessageExchange exchange;
            // If the continuation is not a retry
            if (!cont.isPending()) {
                SoapMessage message = soapMarshaler.createReader().read(request.getInputStream(), 
                                                                        request.getHeader("Content-Type"));
                exchange = soapHelper.createExchange(message);
                //System.err.println("Handling: " + exchange.getExchangeId());
                NormalizedMessage inMessage = exchange.getMessage("in");
                inMessage.setProperty(JbiConstants.PROTOCOL_HEADERS, getHeaders(request));
                locks.put(exchange.getExchangeId(), cont);
                request.setAttribute(MessageExchange.class.getName(), exchange);
                //System.err.println("Sending: " + exchange.getExchangeId());
                ((BaseLifeCycle) endpoint.getServiceUnit().getComponent().getLifeCycle()).sendConsumerExchange(exchange, this);
                // TODO: make this timeout configurable
                boolean result = cont.suspend(1000 * 60); // 60 s
                if (!result) {
                    throw new Exception("Error sending exchange: aborted");
                }
                //System.err.println("Continuing: " + exchange.getExchangeId());
            } else {
                exchange = (MessageExchange) request.getAttribute(MessageExchange.class.getName());
                request.removeAttribute(MessageExchange.class.getName());
                boolean result = cont.suspend(0); 
                // Check if this is a timeout
                if (exchange == null) {
                    throw new IllegalStateException("Exchange not found");
                }
                //System.err.println("Processing: " + exchange.getExchangeId());
                if (!result) {
                    throw new Exception("Timeout");
                }
            }
            if (exchange.getStatus() == ExchangeStatus.ERROR) {
                exchange.setStatus(ExchangeStatus.DONE);
                channel.send(exchange);
                if (exchange.getError() != null) {
                    throw new Exception(exchange.getError());
                } else if (exchange.getFault() != null) {
                    throw new SoapFault(SoapFault.RECEIVER, null, null, null, exchange.getFault().getContent());
                } else {
                    throw new Exception("Unkown Error");
                }
            } else if (exchange.getStatus() == ExchangeStatus.ACTIVE) {
                NormalizedMessage outMsg = exchange.getMessage("out");
                if (outMsg != null) {
                    SoapMessage out = new SoapMessage();
                    jbiMarshaler.fromNMS(out, outMsg);
                    soapMarshaler.createWriter(out).write(response.getOutputStream());
                }
                exchange.setStatus(ExchangeStatus.DONE);
                channel.send(exchange);
            }
        } catch (SoapFault fault) {
            if (SoapFault.SENDER.equals(fault.getCode())) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            } else {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }
            SoapMessage soapFault = new SoapMessage();
            soapFault.setFault(fault);
            soapFault.setEnvelopeName(envelopeName);
            soapMarshaler.createWriter(soapFault).write(response.getOutputStream());
        } catch (RetryRequest e) {
            throw e;
        } catch (Exception e) {
            throw e;
        }
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

}
